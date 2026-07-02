package com.bendey.restaurant.core.data.printer.printserver

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

/**
 * Busca servidores Bendey en la subred local probando HTTP /v1/health.
 * Funciona aunque mDNS esté bloqueado por firewall (común en Windows).
 */
@Singleton
class PrintServerLanProbe @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("printServer") okHttpClient: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val client = okHttpClient.newBuilder()
        .connectTimeout(800, TimeUnit.MILLISECONDS)
        .readTimeout(1500, TimeUnit.MILLISECONDS)
        .build()

    suspend fun scan(port: Int = DEFAULT_PRINT_SERVER_PORT): List<DiscoveredPrintServer> =
        withContext(Dispatchers.IO) {
            val localIp = deviceLanIpv4(context) ?: return@withContext emptyList()
            val prefix = subnetPrefixFromIp(localIp) ?: return@withContext emptyList()
            val self = localIp.substringAfterLast('.').toIntOrNull()

            val hosts = (1..254).filter { it != self }
            val found = mutableListOf<DiscoveredPrintServer>()

            coroutineScope {
                hosts.chunked(24).forEach { batch ->
                    val batchResults = batch.map { lastOctet ->
                        async {
                            probeHost("$prefix$lastOctet", port)
                        }
                    }.awaitAll()
                    found.addAll(batchResults.filterNotNull())
                }
            }

            found.sortedBy { it.latencyMs }.distinctBy { it.host }
        }

    private fun probeHost(host: String, port: Int): DiscoveredPrintServer? {
        val healthUrl = "http://$host:$port/v1/health"
        return try {
            var ok = false
            val latency = measureTimeMillis {
                client.newCall(
                    Request.Builder()
                        .url(healthUrl)
                        .get()
                        .header("Accept", "application/json")
                        .build(),
                ).execute().use { response ->
                    ok = response.isSuccessful
                }
            }
            if (!ok) return null

            val meta = fetchStatus(host, port)
            DiscoveredPrintServer(
                serverId = "$host:$port",
                displayName = meta?.displayName?.ifBlank { "Bendey ($host)" } ?: "Bendey ($host)",
                host = host,
                port = port,
                tenant = meta?.tenant.orEmpty(),
                branchName = meta?.branchName.orEmpty(),
                branchId = meta?.branchId ?: 0,
                hostname = meta?.computerName.orEmpty(),
                appVersion = meta?.appVersion.orEmpty(),
                latencyMs = latency,
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchStatus(host: String, port: Int): LanProbeStatusMeta? {
        val request = Request.Builder()
            .url("http://$host:$port/v1/status")
            .get()
            .header("Accept", "application/json")
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val raw = response.body?.string().orEmpty()
                if (raw.isBlank()) return null
                json.decodeFromString(LanProbeStatusMeta.serializer(), raw)
            }
        } catch (_: Exception) {
            null
        }
    }
}

@kotlinx.serialization.Serializable
private data class LanProbeStatusMeta(
    val displayName: String = "",
    val localIp: String = "",
    val tenant: String = "",
    val branchName: String = "",
    val branchId: Int = 0,
    val computerName: String = "",
    val appVersion: String = "",
)
