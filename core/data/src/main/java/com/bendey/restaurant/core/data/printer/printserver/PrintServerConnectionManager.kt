package com.bendey.restaurant.core.data.printer.printserver

import com.bendey.restaurant.core.data.printer.PrinterPreferencesStore
import com.bendey.restaurant.core.data.printer.PrinterSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class PrintServerConnectionManager @Inject constructor(
    private val discovery: PrintServerDiscovery,
    private val preferencesStore: PrinterPreferencesStore,
    @Named("printServer") okHttpClient: OkHttpClient,
) {
    private val healthClient = okHttpClient.newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun resolveServer(settings: PrinterSettings): PrintServerSelection? {
        if (settings.deliveryMode != PrintDeliveryMode.SERVER) return null
        val current = settings.printServer ?: return null
        if (!current.isReady()) return null

        // IP manual: no reemplazar por mDNS si el usuario configuró host a mano.
        if (current.manualHost.isNotBlank()) {
            return current
        }

        if (ping(current)) return current
        return rediscoverAndMatch(current)
    }

    suspend fun rediscoverAndMatch(previous: PrintServerSelection?): PrintServerSelection? {
        val found = discovery.discover(timeoutMs = 6_000)
        if (found.isEmpty()) return previous?.takeIf { it.isReady() }

        val match = when {
            previous != null && previous.serverId.isNotBlank() ->
                found.firstOrNull { it.serverId == previous.serverId }
            previous != null && previous.displayName.isNotBlank() ->
                found.firstOrNull { it.displayName == previous.displayName }
            else -> found.minByOrNull { it.latencyMs }
        } ?: found.minByOrNull { it.latencyMs }
            ?: return previous?.takeIf { it.isReady() }

        val updated = PrintServerSelection(
            serverId = match.serverId,
            displayName = match.displayName,
            host = match.host,
            port = match.port,
            tenant = match.tenant,
            branchName = match.branchName,
            branchId = match.branchId,
            hostname = match.hostname,
            appVersion = match.appVersion,
            manualHost = previous?.manualHost.orEmpty(),
        )

        if (previous == null || previous.host != updated.host || previous.port != updated.port) {
            val settings = preferencesStore.settings.first()
            preferencesStore.save(
                settings.copy(
                    deliveryMode = PrintDeliveryMode.SERVER,
                    printServer = updated,
                ),
            )
        }
        return updated
    }

    suspend fun ping(server: PrintServerSelection): Boolean = withContext(Dispatchers.IO) {
        val host = server.resolvedHost()
        if (host.isBlank()) return@withContext false
        val request = Request.Builder()
            .url("http://$host:${server.port}/v1/health")
            .get()
            .header("Accept", "application/json")
            .build()
        try {
            healthClient.newCall(request).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun pingWithRetry(server: PrintServerSelection, attempts: Int = 3): Boolean {
        repeat(attempts) { attempt ->
            if (ping(server)) return true
            if (attempt < attempts - 1) delay(400L * (attempt + 1))
        }
        return false
    }
}
