package com.bendey.restaurant.core.realtime.domains

import com.bendey.restaurant.core.realtime.DomainEvent
import com.bendey.restaurant.core.realtime.store.RealtimeStore

data class DomainHandlerContext(
    val event: DomainEvent,
    val recordPatch: (Boolean) -> Unit,
)

typealias DomainHandler = (DomainHandlerContext) -> Unit

/** Puerto de `domains/registry.ts` (Tauri) — contrato `DomainModule`. */
interface DomainModule {
    val name: String
    val eventPrefixes: List<String>
    fun registerHandlers(register: (String, DomainHandler) -> Unit)
    fun getStores(): List<RealtimeStore<*>>
    fun matchesEventType(type: String): Boolean
}
