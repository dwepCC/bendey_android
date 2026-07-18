package com.bendey.restaurant.core.realtime.domains

import com.bendey.restaurant.core.realtime.DomainEvent
import com.bendey.restaurant.core.realtime.store.RealtimeStore
import javax.inject.Inject
import javax.inject.Singleton

/** Puerto de `domains/registry.ts` (Tauri). */
@Singleton
class DomainRegistry @Inject constructor() {
    private val modules = mutableListOf<DomainModule>()
    private val handlersByType = mutableMapOf<String, MutableList<DomainHandler>>()

    fun register(module: DomainModule) {
        modules.add(module)
        module.registerHandlers { type, handler ->
            handlersByType.getOrPut(type) { mutableListOf() }.add(handler)
        }
    }

    fun resolve(type: String): DomainModule? = modules.firstOrNull { it.matchesEventType(type) }

    fun handle(event: DomainEvent, recordPatch: (Boolean) -> Unit) {
        val handlers = handlersByType[event.type].orEmpty()
        val mod = resolve(event.type)
        if (mod == null && handlers.isEmpty()) return

        val ctx = DomainHandlerContext(event, recordPatch)
        handlers.forEach { it(ctx) }
    }

    fun getAllStores(): List<RealtimeStore<*>> = modules.flatMap { it.getStores() }

    fun getModules(): List<DomainModule> = modules.toList()

    fun resetForTests() {
        modules.clear()
        handlersByType.clear()
    }
}
