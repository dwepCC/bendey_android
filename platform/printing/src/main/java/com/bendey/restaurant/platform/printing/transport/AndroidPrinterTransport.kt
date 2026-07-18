package com.bendey.restaurant.platform.printing.transport

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidPrinterTransport @Inject constructor(
    @ApplicationContext private val context: Context,
) : PrinterTransport {

    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var bluetoothSocket: BluetoothSocket? = null

    /**
     * Sockets bloqueantes (TCP/Bluetooth) no exponen timeout de escritura en la API pública de
     * Android — solo de lectura/conexión. Si la impresora deja de aceptar datos a mitad de un
     * ticket grande (con logo), `write()` puede bloquear el hilo indefinidamente. Este watchdog
     * cierra el socket tras [WRITE_TIMEOUT_MS] si la escritura no terminó, lo que fuerza a que
     * `write()` lance una excepción en vez de colgarse para siempre.
     */
    private fun writeWatchdog(onTimeout: () -> Unit): Pair<Timer, AtomicBoolean> {
        val timedOut = AtomicBoolean(false)
        val timer = Timer(true).apply {
            schedule(
                object : TimerTask() {
                    override fun run() {
                        timedOut.set(true)
                        onTimeout()
                    }
                },
                WRITE_TIMEOUT_MS,
            )
        }
        return timer to timedOut
    }

    private fun adapter(): BluetoothAdapter? {
        val mgr = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return mgr?.adapter
    }

    private fun hasBtPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val connect = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT,
            ) == PackageManager.PERMISSION_GRANTED
            val scan = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN,
            ) == PackageManager.PERMISSION_GRANTED
            return connect && scan
        }
        @Suppress("DEPRECATION")
        val bt = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH,
        ) == PackageManager.PERMISSION_GRANTED
        val loc = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return bt && loc
    }

    @SuppressLint("MissingPermission")
    private fun deviceName(device: BluetoothDevice): String = try {
        if (hasBtPermission()) device.name?.takeIf { it.isNotBlank() } ?: "Dispositivo"
        else "Dispositivo"
    } catch (_: SecurityException) {
        "Dispositivo"
    }

    override suspend fun isBluetoothEnabled(): Boolean = withContext(Dispatchers.IO) {
        adapter()?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    override suspend fun getPairedDevices(): List<BluetoothDeviceInfo> = withContext(Dispatchers.IO) {
        if (!hasBtPermission()) return@withContext emptyList()
        val ad = adapter() ?: return@withContext emptyList()
        ad.bondedDevices?.map { device ->
            BluetoothDeviceInfo(name = deviceName(device), address = device.address)
        }.orEmpty()
    }

    @SuppressLint("MissingPermission")
    override suspend fun connectBluetooth(address: String): PrintResult = withContext(Dispatchers.IO) {
        if (!hasBtPermission()) {
            return@withContext PrintResult.Error("Permisos Bluetooth no concedidos")
        }
        try {
            disconnectBluetoothInternal()
            val ad = adapter() ?: return@withContext PrintResult.Error("Bluetooth no disponible")
            val device = ad.getRemoteDevice(address)
            val socket = device.createRfcommSocketToServiceRecord(sppUuid)
            socket.connect()
            bluetoothSocket = socket
            PrintResult.Success
        } catch (e: Exception) {
            PrintResult.Error("No se pudo conectar: ${e.message}", e)
        }
    }

    override suspend fun disconnectBluetooth() = withContext(Dispatchers.IO) {
        disconnectBluetoothInternal()
    }

    override suspend fun print(payload: ByteArray, target: PrinterTarget): PrintResult =
        withContext(Dispatchers.IO) {
            when (target.type) {
                PrinterConnectionType.BLUETOOTH -> printBluetooth(payload)
                PrinterConnectionType.TCP -> printTcp(payload, target.tcpHost.orEmpty(), target.tcpPort)
            }
        }

    private fun printBluetooth(bytes: ByteArray): PrintResult {
        val socket = bluetoothSocket ?: return PrintResult.Error("Impresora Bluetooth no conectada")
        if (!socket.isConnected) return PrintResult.Error("Conexión Bluetooth perdida")
        // Cerrar el socket desde el watchdog interrumpe el write() bloqueado con una excepción.
        val (watchdog, timedOut) = writeWatchdog { disconnectBluetoothInternal() }
        return try {
            socket.outputStream.write(bytes)
            socket.outputStream.flush()
            PrintResult.Success
        } catch (e: Exception) {
            // Cualquier falla de escritura deja el socket en estado indeterminado — se limpia
            // aquí para que el próximo intento no reutilice un socket roto.
            disconnectBluetoothInternal()
            if (timedOut.get()) {
                PrintResult.Error("Tiempo de espera agotado enviando datos por Bluetooth")
            } else {
                PrintResult.Error("Error al imprimir BT: ${e.message}", e)
            }
        } finally {
            watchdog.cancel()
        }
    }

    private fun printTcp(bytes: ByteArray, host: String, port: Int): PrintResult {
        if (host.isBlank()) return PrintResult.Error("Host TCP vacío")
        val socket = Socket()
        val (watchdog, timedOut) = writeWatchdog {
            try {
                socket.close()
            } catch (_: Exception) {
            }
        }
        return try {
            socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
            socket.getOutputStream().use { out ->
                out.write(bytes)
                out.flush()
            }
            PrintResult.Success
        } catch (e: Exception) {
            if (timedOut.get()) {
                PrintResult.Error("Tiempo de espera agotado enviando datos a la impresora")
            } else {
                PrintResult.Error("Error TCP: ${e.message}", e)
            }
        } finally {
            watchdog.cancel()
            try {
                socket.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun disconnectBluetoothInternal() {
        try {
            bluetoothSocket?.close()
        } catch (_: Exception) {
        }
        bluetoothSocket = null
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 8_000
        const val WRITE_TIMEOUT_MS = 15_000L
    }
}
