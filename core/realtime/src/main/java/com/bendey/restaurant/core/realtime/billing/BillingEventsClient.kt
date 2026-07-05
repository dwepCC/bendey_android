package com.bendey.restaurant.core.realtime.billing

import com.bendey.restaurant.core.network.serialization.ApiJson
import com.bendey.restaurant.core.network.session.NetworkSessionProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import javax.inject.Inject
import javax.inject.Singleton

/** SSE `/api/billing/events` — paridad con useBillingEvents (Capacitor). */
@Singleton
class BillingEventsClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val sessionProvider: NetworkSessionProvider,
) {
    private val _events = MutableSharedFlow<BillingStatusEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<BillingStatusEvent> = _events.asSharedFlow()

    private var eventSource: EventSource? = null

    fun connect() {
        disconnect()
        val token = sessionProvider.token()?.trim().orEmpty()
        val base = sessionProvider.tenantApiBaseUrl()?.trim()?.trimEnd('/').orEmpty()
        val slug = sessionProvider.tenantSlug()?.trim().orEmpty()
        val url = buildBillingEventsUrl(base, token, slug) ?: return

        val requestBuilder = Request.Builder().url(url).get()
        if (slug.isNotBlank()) {
            requestBuilder.header("X-Tenant-Slug", slug)
        }
        eventSource = EventSources.createFactory(okHttpClient).newEventSource(
            requestBuilder.build(),
            object : EventSourceListener() {
                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String,
                ) {
                    if (type != "billing.status.updated") return
                    parseEvent(data)?.let { _events.tryEmit(it) }
                }
            },
        )
    }

    fun disconnect() {
        eventSource?.cancel()
        eventSource = null
    }

    private fun parseEvent(data: String): BillingStatusEvent? {
        return try {
            val dto = ApiJson.decodeFromString(BillingStatusEventDto.serializer(), data)
            if (dto.saleId <= 0) return null
            BillingStatusEvent(
                saleId = dto.saleId,
                status = dto.status,
                pipelineStatus = dto.pipelineStatus,
                sunatMessage = dto.sunatMessage,
            )
        } catch (_: Exception) {
            null
        }
    }
}

@Serializable
private data class BillingStatusEventDto(
    @SerialName("sale_id") val saleId: Int = 0,
    val status: String = "",
    @SerialName("pipeline_status") val pipelineStatus: String? = null,
    @SerialName("sunat_message") val sunatMessage: String? = null,
)
