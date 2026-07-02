package com.bendey.restaurant.core.data.printer.printserver

import com.bendey.restaurant.core.domain.billing.SalePrintData
import com.bendey.restaurant.core.domain.restaurant.ComandaLine
import com.bendey.restaurant.core.domain.restaurant.PrecuentaData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

sealed class RemotePrintResult {
    data object Success : RemotePrintResult()
    data class Error(val message: String, val code: String = "unknown") : RemotePrintResult()
}

@Singleton
class PrintServerClient @Inject constructor(
    @Named("printServer") okHttpClient: OkHttpClient,
    private val headersProvider: PrintServerClientHeaders,
    private val connectionManager: PrintServerConnectionManager,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = okHttpClient.newBuilder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(130, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun printComandaRound(
        server: PrintServerSelection,
        tableName: String?,
        orderNumber: Int,
        waiterName: String?,
        comandas: List<ComandaLine>,
    ): RemotePrintResult {
        val jobId = newPrintJobId()
        val body = RemoteComandaJobRequest(
            jobId = jobId,
            tableName = tableName,
            orderNumber = orderNumber,
            waiterName = waiterName,
            comandas = comandas.map { it.toRemoteDto() },
        )
        return postWithRetry(server, "/v1/print/comanda", json.encodeToString(RemoteComandaJobRequest.serializer(), body))
    }

    suspend fun printPrecuenta(server: PrintServerSelection, precuenta: PrecuentaData): RemotePrintResult {
        val jobId = newPrintJobId()
        val body = precuenta.toRemoteJob(jobId)
        return postWithRetry(server, "/v1/print/precuenta", json.encodeToString(RemotePrecuentaJobRequest.serializer(), body))
    }

    suspend fun printDocument(server: PrintServerSelection, data: SalePrintData): RemotePrintResult {
        val jobId = newPrintJobId()
        val body = data.toRemoteJob(jobId)
        return postWithRetry(server, "/v1/print/document", json.encodeToString(RemoteDocumentJobRequest.serializer(), body))
    }

    suspend fun printTest(server: PrintServerSelection, kind: String): RemotePrintResult {
        val jobId = newPrintJobId()
        val body = RemoteTestJobRequest(jobId = jobId, kind = kind)
        return postWithRetry(server, "/v1/print/test", json.encodeToString(RemoteTestJobRequest.serializer(), body))
    }

    private suspend fun postWithRetry(
        server: PrintServerSelection,
        path: String,
        payload: String,
        maxAttempts: Int = 3,
    ): RemotePrintResult {
        var current = server
        repeat(maxAttempts) { attempt ->
            when (val result = post(current, path, payload)) {
                is RemotePrintResult.Success -> return result
                is RemotePrintResult.Error -> {
                    if (result.code in RETRYABLE_CODES && attempt < maxAttempts - 1) {
                        delay(500L * (attempt + 1))
                        connectionManager.rediscoverAndMatch(current)?.let { updated ->
                            current = updated
                        }
                    } else {
                        return result
                    }
                }
            }
        }
        return RemotePrintResult.Error("No se pudo conectar al servidor de impresión", "connection_error")
    }

    private suspend fun post(server: PrintServerSelection, path: String, payload: String): RemotePrintResult =
        withContext(Dispatchers.IO) {
        val host = server.resolvedHost()
        if (host.isBlank()) return@withContext RemotePrintResult.Error("Servidor no configurado", "server_stopped")
        val url = "http://$host:${server.port}$path"
        val builder = Request.Builder()
            .url(url)
            .post(payload.toRequestBody(mediaType))
            .header("Accept", "application/json")

        headersProvider.build(
            tenant = server.tenant,
            branchName = server.branchName,
            appVersion = server.appVersion,
        ).forEach { (k, v) -> builder.header(k, v) }

        return@withContext try {
            client.newCall(builder.build()).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (raw.isBlank()) {
                    return@use if (response.isSuccessful) {
                        RemotePrintResult.Success
                    } else {
                        mapHttpError(response.code)
                    }
                }
                val parsed = json.decodeFromString(PrintJobResponse.serializer(), raw)
                when {
                    parsed.isSuccess -> RemotePrintResult.Success
                    parsed.duplicate -> RemotePrintResult.Success
                    else -> RemotePrintResult.Error(
                        mapErrorMessage(parsed.message, parsed.errorCode),
                        parsed.errorCode ?: "unknown",
                    )
                }
            }
        } catch (e: Exception) {
            RemotePrintResult.Error(mapException(e), exceptionCode(e))
        }
    }

    private fun mapHttpError(code: Int): RemotePrintResult.Error = when (code) {
        503 -> RemotePrintResult.Error("Servidor de impresión detenido o cola no disponible", "server_stopped")
        408, 504 -> RemotePrintResult.Error("Tiempo de espera agotado", "timeout")
        else -> RemotePrintResult.Error("Error HTTP $code", "connection_error")
    }

    private fun mapException(e: Exception): String = when (e) {
        is SocketTimeoutException -> "Tiempo de espera agotado al conectar con el servidor"
        is ConnectException, is UnknownHostException -> "No se pudo conectar al servidor. Verifique la red local."
        is IOException -> e.message ?: "Error de conexión con el servidor"
        else -> e.message ?: "Error de impresión remota"
    }

    private fun exceptionCode(e: Exception): String = when (e) {
        is SocketTimeoutException -> "timeout"
        is ConnectException, is UnknownHostException -> "connection_error"
        else -> "connection_error"
    }

    private fun mapErrorMessage(message: String?, code: String?): String {
        val base = message?.takeIf { it.isNotBlank() } ?: "Error de impresión remota"
        return when (code) {
            "duplicate" -> "Trabajo duplicado (ya procesado)"
            "timeout" -> "Tiempo de espera agotado en el servidor"
            "server_stopped" -> "Servidor de impresión detenido"
            "queue_unavailable" -> "Cola de impresión no disponible"
            "printer_offline" -> "Impresora no disponible en el servidor"
            "port_in_use" -> "Puerto ocupado en el servidor"
            else -> base
        }
    }

    companion object {
        private val RETRYABLE_CODES = setOf("connection_error", "timeout", "server_stopped")
    }
}
