package com.bendey.restaurant.core.data.printer.printserver

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.system.measureTimeMillis

private const val TAG = "PrintServerDiscovery"

@Singleton
class PrintServerDiscovery @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("printServer") okHttpClient: OkHttpClient,
    private val lanProbe: PrintServerLanProbe,
) {
    private val nsdManager: NsdManager? =
        context.getSystemService(Context.NSD_SERVICE) as? NsdManager

    private val nsdExecutor = Executors.newSingleThreadExecutor()

    private val pingClient = okHttpClient.newBuilder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    /**
     * 1) mDNS/NSD (rápido si el firewall lo permite)
     * 2) Escaneo LAN por HTTP (fallback — funciona cuando mDNS está bloqueado en Windows)
     */
    suspend fun discover(timeoutMs: Long = 12_000): List<DiscoveredPrintServer> {
        val nsdResults = discoverViaNsd(minOf(timeoutMs / 2, 8_000))
        if (nsdResults.isNotEmpty()) return nsdResults

        Log.i(TAG, "mDNS sin resultados — escaneando subred LAN por HTTP…")
        return lanProbe.scan()
    }

    private suspend fun discoverViaNsd(timeoutMs: Long): List<DiscoveredPrintServer> =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                val manager = nsdManager
                if (manager == null) {
                    cont.resume(emptyList())
                    return@suspendCancellableCoroutine
                }

                val found = ConcurrentHashMap<String, DiscoveredPrintServer>()
                val serviceCallbacks = CopyOnWriteArrayList<NsdManager.ServiceInfoCallback>()
                val pendingResolves = AtomicInteger(0)
                var finished = false

                fun tryFinish() {
                    if (finished || !cont.isActive) return
                    if (pendingResolves.get() <= 0) {
                        finished = true
                        cont.resume(found.values.sortedBy { it.latencyMs }.distinctBy { it.serverId })
                    }
                }

                val multicastLock = (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager)
                    ?.createMulticastLock("bendey-print-mdns")
                    ?.apply {
                        setReferenceCounted(false)
                        acquire()
                    }

                val discoveryListener = object : NsdManager.DiscoveryListener {
                    override fun onDiscoveryStarted(serviceType: String) {
                        Log.d(TAG, "NSD started: $serviceType")
                    }

                    override fun onServiceFound(service: NsdServiceInfo) {
                        if (!serviceTypeMatches(service.serviceType)) return
                        Log.d(TAG, "NSD service found: ${service.serviceName}")
                        pendingResolves.incrementAndGet()
                        resolveServiceInfo(manager, service, found, serviceCallbacks) {
                            pendingResolves.decrementAndGet()
                            tryFinish()
                        }
                    }

                    override fun onServiceLost(service: NsdServiceInfo) {
                        val id = service.serviceName
                        found.entries.removeIf { it.value.displayName == id || it.key.contains(id) }
                    }

                    override fun onDiscoveryStopped(serviceType: String) = Unit

                    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                        Log.w(TAG, "NSD start failed: $errorCode")
                        stopDiscoverySession(manager, serviceCallbacks, this, multicastLock)
                        if (cont.isActive && !finished) {
                            finished = true
                            cont.resume(emptyList())
                        }
                    }

                    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
                }

                manager.discoverServices(
                    PRINT_SERVER_SERVICE_TYPE,
                    NsdManager.PROTOCOL_DNS_SD,
                    discoveryListener,
                )

                cont.invokeOnCancellation {
                    stopDiscoverySession(manager, serviceCallbacks, discoveryListener, multicastLock)
                }

                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    stopDiscoverySession(manager, serviceCallbacks, discoveryListener, multicastLock)
                    if (cont.isActive && !finished) {
                        finished = true
                        cont.resume(found.values.sortedBy { it.latencyMs }.distinctBy { it.serverId })
                    }
                }, timeoutMs)
            }
        }

    private fun stopDiscoverySession(
        manager: NsdManager,
        serviceCallbacks: CopyOnWriteArrayList<NsdManager.ServiceInfoCallback>,
        discoveryListener: NsdManager.DiscoveryListener,
        multicastLock: WifiManager.MulticastLock?,
    ) {
        serviceCallbacks.forEach { callback ->
            runCatching { manager.unregisterServiceInfoCallback(callback) }
        }
        serviceCallbacks.clear()
        runCatching { manager.stopServiceDiscovery(discoveryListener) }
        runCatching { if (multicastLock?.isHeld == true) multicastLock.release() }
    }

    private fun resolveServiceInfo(
        manager: NsdManager,
        service: NsdServiceInfo,
        found: ConcurrentHashMap<String, DiscoveredPrintServer>,
        serviceCallbacks: CopyOnWriteArrayList<NsdManager.ServiceInfoCallback>,
        onComplete: () -> Unit,
    ) {
        var completed = false
        fun completeOnce() {
            if (completed) return
            completed = true
            onComplete()
        }

        @Suppress("DEPRECATION")
        manager.resolveService(service, nsdExecutor, object : NsdManager.ResolveListener {
            override fun onServiceResolved(resolved: NsdServiceInfo) {
                recordDiscoveredServer(resolved, found)
                completeOnce()
            }

            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "NSD resolve failed: $errorCode for ${serviceInfo.serviceName}")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    resolveWithServiceCallback(manager, service, found, serviceCallbacks) {
                        completeOnce()
                    }
                } else {
                    completeOnce()
                }
            }
        })
    }

    private fun resolveWithServiceCallback(
        manager: NsdManager,
        service: NsdServiceInfo,
        found: ConcurrentHashMap<String, DiscoveredPrintServer>,
        serviceCallbacks: CopyOnWriteArrayList<NsdManager.ServiceInfoCallback>,
        onComplete: () -> Unit,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            onComplete()
            return
        }
        var done = false
        fun finish() {
            if (done) return
            done = true
            onComplete()
        }

        val callback = object : NsdManager.ServiceInfoCallback {
            override fun onServiceUpdated(resolved: NsdServiceInfo) {
                recordDiscoveredServer(resolved, found)
                finish()
            }

            override fun onServiceLost() = finish()

            override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) = finish()

            override fun onServiceInfoCallbackUnregistered() = Unit
        }
        serviceCallbacks.add(callback)
        manager.registerServiceInfoCallback(service, nsdExecutor, callback)

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            finish()
        }, 4_000)
    }

    private fun recordDiscoveredServer(
        resolved: NsdServiceInfo,
        found: ConcurrentHashMap<String, DiscoveredPrintServer>,
    ) {
        val attrs = resolved.attributes
        fun attr(key: String): String {
            val bytes = attrs[key] ?: return ""
            return String(bytes, Charsets.UTF_8).trim()
        }

        val host = resolveHostAddress(resolved, ::attr)
        if (host.isBlank()) {
            Log.w(TAG, "NSD resolved sin IP: ${resolved.serviceName}")
            return
        }

        val latency = measureLatency(host, resolved.port)
        val server = DiscoveredPrintServer(
            serverId = attr("id").ifBlank { "${resolved.serviceName}@$host" },
            displayName = attr("display").ifBlank { resolved.serviceName },
            host = host,
            port = resolved.port,
            tenant = attr("tenant"),
            branchName = attr("branch"),
            branchId = attr("branchId").toIntOrNull() ?: 0,
            hostname = attr("hostname"),
            appVersion = attr("app"),
            latencyMs = latency,
        )
        found[server.serverId] = server
        Log.i(TAG, "NSD server: ${server.displayName} @ ${server.host}:${server.port}")
    }

    private fun resolveHostAddress(
        resolved: NsdServiceInfo,
        attr: (String) -> String,
    ): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val fromAddresses = resolved.hostAddresses
                ?.asSequence()
                ?.mapNotNull { it.hostAddress?.trim()?.takeIf { address -> address.isNotEmpty() } }
                ?.firstOrNull { isPrivateLanIpv4(it) }
            if (!fromAddresses.isNullOrBlank()) return fromAddresses
        } else {
            @Suppress("DEPRECATION")
            val legacyHost = resolved.host?.hostAddress?.trim().orEmpty()
            if (legacyHost.isNotBlank() && isPrivateLanIpv4(legacyHost)) return legacyHost
        }
        val txtIp = attr("ip")
        return if (isPrivateLanIpv4(txtIp)) txtIp else txtIp
    }

    private fun serviceTypeMatches(type: String?): Boolean {
        if (type.isNullOrBlank()) return false
        val normalized = type.trim().lowercase().removeSuffix(".")
        return normalized == "_bendey-print._tcp" ||
            normalized == "_bendey-print._tcp.local" ||
            normalized.contains("bendey-print")
    }

    private fun measureLatency(host: String, port: Int): Long {
        val request = Request.Builder().url("http://$host:$port/v1/health").get().build()
        return try {
            measureTimeMillis {
                pingClient.newCall(request).execute().close()
            }
        } catch (_: Exception) {
            9999L
        }
    }
}
