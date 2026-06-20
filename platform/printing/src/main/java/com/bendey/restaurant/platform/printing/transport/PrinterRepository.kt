package com.bendey.restaurant.platform.printing.transport

import com.bendey.restaurant.platform.printing.escpos.ComandaItem
import com.bendey.restaurant.platform.printing.escpos.ComandaLayoutBuilder
import com.bendey.restaurant.platform.printing.escpos.ComandaPrintInput
import com.bendey.restaurant.platform.printing.escpos.ComandaTextSize
import com.bendey.restaurant.platform.printing.escpos.DocumentLayoutBuilder
import com.bendey.restaurant.platform.printing.escpos.DocumentPrintInput
import com.bendey.restaurant.platform.printing.escpos.PaperWidthMm
import com.bendey.restaurant.platform.printing.escpos.PrecuentaItem
import com.bendey.restaurant.platform.printing.escpos.PrecuentaLayoutBuilder
import com.bendey.restaurant.platform.printing.escpos.PrecuentaPrintInput

enum class PrinterConnectionType {
    BLUETOOTH,
    TCP,
}

data class PrinterTarget(
    val type: PrinterConnectionType,
    val bluetoothAddress: String? = null,
    val tcpHost: String? = null,
    val tcpPort: Int = 9100,
    val paperWidth: PaperWidthMm = PaperWidthMm.W80,
)

sealed class PrintResult {
    data object Success : PrintResult()
    data class Error(val message: String, val cause: Throwable? = null) : PrintResult()
}

interface PrinterTransport {
    suspend fun print(payload: ByteArray, target: PrinterTarget): PrintResult
    suspend fun isBluetoothEnabled(): Boolean
    suspend fun getPairedDevices(): List<BluetoothDeviceInfo>
    suspend fun connectBluetooth(address: String): PrintResult
    suspend fun disconnectBluetooth()
}

data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
)

interface PrinterRepository {
    suspend fun printComanda(target: PrinterTarget, input: ComandaPrintInput): PrintResult
    suspend fun printPrecuenta(target: PrinterTarget, input: PrecuentaPrintInput): PrintResult
    suspend fun printDocument(target: PrinterTarget, input: DocumentPrintInput): PrintResult
    suspend fun printTestPage(target: PrinterTarget, label: String): PrintResult
    suspend fun getPairedDevices(): List<BluetoothDeviceInfo>
    suspend fun connectBluetooth(address: String): PrintResult
}

class PrinterRepositoryImpl(
    private val transport: PrinterTransport,
) : PrinterRepository {

    override suspend fun printComanda(target: PrinterTarget, input: ComandaPrintInput): PrintResult {
        val payload = ComandaLayoutBuilder.build(input.copy(paperWidth = target.paperWidth))
        return transport.print(payload, target)
    }

    override suspend fun printPrecuenta(target: PrinterTarget, input: PrecuentaPrintInput): PrintResult {
        val payload = PrecuentaLayoutBuilder.build(input.copy(paperWidth = target.paperWidth))
        return transport.print(payload, target)
    }

    override suspend fun printDocument(target: PrinterTarget, input: DocumentPrintInput): PrintResult {
        val payload = DocumentLayoutBuilder.build(
            input.copy(paperWidth = target.paperWidth),
        )
        return transport.print(payload, target)
    }

    override suspend fun printTestPage(target: PrinterTarget, label: String): PrintResult {
        val sample = ComandaPrintInput(
            tableName = "Mesa 05",
            orderNumber = 42,
            waiterName = "Juan Perez",
            items = listOf(
                ComandaItem(
                    productName = "Pollo a la brasa",
                    quantity = 2.0,
                    modifierLines = listOf("Papas extra"),
                    notes = "Sin ají",
                ),
                ComandaItem(productName = "Chicha morada", quantity = 1.0),
            ),
            paperWidth = target.paperWidth,
            textSize = ComandaTextSize.DEFAULT,
        )
        val payload = if (label.contains("precuenta", ignoreCase = true)) {
            PrecuentaLayoutBuilder.build(
                PrecuentaPrintInput(
                    tableName = "Mesa 05",
                    items = listOf(
                        PrecuentaItem("Pollo a la brasa", 2.0, 38.0),
                        PrecuentaItem("Chicha morada", 1.0, 8.0),
                    ),
                    total = 84.0,
                    paperWidth = target.paperWidth,
                ),
            )
        } else {
            ComandaLayoutBuilder.build(sample)
        }
        return transport.print(payload, target)
    }

    override suspend fun getPairedDevices(): List<BluetoothDeviceInfo> =
        transport.getPairedDevices()

    override suspend fun connectBluetooth(address: String): PrintResult =
        transport.connectBluetooth(address)
}
