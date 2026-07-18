package com.bendey.restaurant.core.realtime.dispatcher

import com.bendey.restaurant.core.realtime.DomainEvent
import com.bendey.restaurant.core.realtime.RealtimeSchema
import com.bendey.restaurant.core.realtime.domains.DomainRegistry
import com.bendey.restaurant.core.realtime.effects.SideEffectRunner
import javax.inject.Inject
import javax.inject.Singleton

private const val DEDUPE_TTL_MS = 5 * 60 * 1000L
private const val MAX_DEDUPE = 500

/**
 * Pipeline de despacho de eventos realtime. Puerto de `dispatcher/index.ts` (Tauri):
 * recibido -> validar type -> schema -> dedupe -> validate branch/tenant -> domain registry
 * -> batch -> side effects -> observability.
 */
@Singleton
class RealtimeDispatcher @Inject constructor(
    private val domainRegistry: DomainRegistry,
    private val observability: RealtimeObservability,
    private val sideEffects: SideEffectRunner,
) {
    private val seenIds = LinkedHashMap<String, Long>()
    private var getValidateContext: () -> ValidateContext = { ValidateContext() }

    fun setValidateContextProvider(fn: () -> ValidateContext) {
        getValidateContext = fn
    }

    fun dispatch(event: DomainEvent) {
        val startedAt = System.nanoTime()
        observability.recordEventReceived()

        if (event.type.isBlank()) {
            observability.recordEventDiscarded(DiscardReason.VALIDATION)
            return
        }

        val schemaVersion = RealtimeSchema.getSchemaVersion(event)
        if (!RealtimeSchema.isSupportedSchemaVersion(schemaVersion)) {
            observability.recordEventDiscarded(DiscardReason.UNSUPPORTED_SCHEMA)
            return
        }

        if (event.id.isNotBlank() && isDuplicate(event.id)) {
            observability.recordEventDiscarded(DiscardReason.DUPLICATE)
            return
        }

        val validation = RealtimeValidate.validateEvent(event, getValidateContext())
        if (validation is ValidateResult.Fail) {
            observability.recordEventDiscarded(DiscardReason.VALIDATION)
            return
        }

        val domain = domainRegistry.resolve(event.type)
        if (domain == null) {
            observability.recordEventDiscarded(DiscardReason.UNKNOWN_DOMAIN)
            return
        }

        val recordPatch: (Boolean) -> Unit = { applied -> observability.recordPatch(applied) }

        RealtimeBatch.run {
            domainRegistry.handle(event, recordPatch)
        }

        sideEffects.runAll(event)

        val processingMs = (System.nanoTime() - startedAt) / 1_000_000.0
        observability.recordEventProcessed(event, processingMs)
    }

    private fun isDuplicate(id: String): Boolean {
        val now = System.currentTimeMillis()
        if (seenIds.containsKey(id)) return true
        seenIds[id] = now
        if (seenIds.size > MAX_DEDUPE) pruneDedupe(now)
        return false
    }

    private fun pruneDedupe(now: Long) {
        val it = seenIds.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            if (now - entry.value > DEDUPE_TTL_MS) it.remove()
        }
    }

    fun resetForTests() {
        seenIds.clear()
    }
}
