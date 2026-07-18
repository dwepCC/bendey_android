package com.bendey.restaurant.core.realtime.dispatcher

import com.bendey.restaurant.core.realtime.DomainEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton

enum class ConnectionState { DISCONNECTED, CONNECTING, READY, RECONNECTING }

enum class DiscardReason { DUPLICATE, VALIDATION, UNKNOWN_DOMAIN, UNSUPPORTED_SCHEMA }

data class ObservabilitySnapshot(
    val eventsReceivedTotal: Long = 0,
    val eventsProcessedTotal: Long = 0,
    val eventsDiscardedTotal: Long = 0,
    val eventsDuplicatedTotal: Long = 0,
    val eventsUnknownDomainTotal: Long = 0,
    val eventsUnsupportedSchemaTotal: Long = 0,
    val patchesAppliedTotal: Long = 0,
    val patchesMissingTotal: Long = 0,
    val recoveriesTotal: Long = 0,
    val recoveriesByPolicy: Map<String, Long> = emptyMap(),
    val reconnectsTotal: Long = 0,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val lastEventLatencyMs: Long? = null,
    val avgProcessingMs: Double = 0.0,
    val queueSize: Int = 0,
    val lastRecoveryAt: String? = null,
    val lastEventAt: String? = null,
)

private const val MAX_LATENCY_SAMPLES = 100

/** Puerto de `dispatcher/observability.ts` (Tauri). */
@Singleton
class RealtimeObservability @Inject constructor() {
    private val _snapshot = MutableStateFlow(ObservabilitySnapshot())
    val snapshot: StateFlow<ObservabilitySnapshot> = _snapshot.asStateFlow()

    private val processingSamples = ArrayDeque<Double>()
    private var queueSize = 0

    fun getSnapshot(): ObservabilitySnapshot = _snapshot.value

    fun setConnectionState(state: ConnectionState) {
        _snapshot.update { s ->
            val reconnects = if (state == ConnectionState.RECONNECTING && s.connectionState != ConnectionState.RECONNECTING) {
                s.reconnectsTotal + 1
            } else {
                s.reconnectsTotal
            }
            s.copy(connectionState = state, reconnectsTotal = reconnects)
        }
    }

    fun recordEventReceived() {
        queueSize++
        _snapshot.update { it.copy(eventsReceivedTotal = it.eventsReceivedTotal + 1, queueSize = queueSize) }
    }

    fun recordEventDiscarded(reason: DiscardReason) {
        dequeue()
        _snapshot.update { s ->
            s.copy(
                eventsDiscardedTotal = s.eventsDiscardedTotal + 1,
                eventsDuplicatedTotal = if (reason == DiscardReason.DUPLICATE) s.eventsDuplicatedTotal + 1 else s.eventsDuplicatedTotal,
                eventsUnknownDomainTotal = if (reason == DiscardReason.UNKNOWN_DOMAIN) s.eventsUnknownDomainTotal + 1 else s.eventsUnknownDomainTotal,
                eventsUnsupportedSchemaTotal = if (reason == DiscardReason.UNSUPPORTED_SCHEMA) s.eventsUnsupportedSchemaTotal + 1 else s.eventsUnsupportedSchemaTotal,
                queueSize = queueSize,
            )
        }
    }

    fun recordEventProcessed(event: DomainEvent, processingMs: Double) {
        processingSamples.addLast(processingMs)
        if (processingSamples.size > MAX_LATENCY_SAMPLES) processingSamples.removeFirst()
        val avg = processingSamples.average()

        val latency = try {
            val occurred = Instant.parse(event.occurredAt)
            (System.currentTimeMillis() - occurred.toEpochMilli()).coerceAtLeast(0)
        } catch (_: DateTimeParseException) {
            null
        }

        dequeue()
        _snapshot.update {
            it.copy(
                eventsProcessedTotal = it.eventsProcessedTotal + 1,
                avgProcessingMs = avg,
                lastEventLatencyMs = latency ?: it.lastEventLatencyMs,
                lastEventAt = Instant.now().toString(),
                queueSize = queueSize,
            )
        }
    }

    fun recordPatch(applied: Boolean) {
        _snapshot.update {
            if (applied) {
                it.copy(patchesAppliedTotal = it.patchesAppliedTotal + 1)
            } else {
                it.copy(patchesMissingTotal = it.patchesMissingTotal + 1)
            }
        }
    }

    fun recordRecovery(policy: String) {
        _snapshot.update { s ->
            val byPolicy = s.recoveriesByPolicy.toMutableMap()
            byPolicy[policy] = (byPolicy[policy] ?: 0) + 1
            s.copy(
                recoveriesTotal = s.recoveriesTotal + 1,
                recoveriesByPolicy = byPolicy,
                lastRecoveryAt = Instant.now().toString(),
            )
        }
    }

    private fun dequeue() {
        queueSize = (queueSize - 1).coerceAtLeast(0)
    }

    fun resetForTests() {
        processingSamples.clear()
        queueSize = 0
        _snapshot.value = ObservabilitySnapshot()
    }
}
