package com.bendey.restaurant.core.realtime.domains.restaurant

import com.bendey.restaurant.core.realtime.domains.DomainHandler
import com.bendey.restaurant.core.realtime.domains.DomainModule
import com.bendey.restaurant.core.realtime.store.RealtimeStore
import com.bendey.restaurant.core.realtime.store.RestaurantStores
import javax.inject.Inject
import javax.inject.Singleton

private val RESTAURANT_PREFIXES = listOf("restaurant.", "menu.")

/** Puerto de `domains/restaurant/index.ts` (Tauri). */
@Singleton
class RestaurantDomain @Inject constructor(
    private val handlers: RestaurantHandlers,
    private val stores: RestaurantStores,
) : DomainModule {
    override val name: String = "restaurant"
    override val eventPrefixes: List<String> = RESTAURANT_PREFIXES

    override fun matchesEventType(type: String): Boolean = RESTAURANT_PREFIXES.any { type.startsWith(it) }

    override fun registerHandlers(register: (String, DomainHandler) -> Unit) {
        handlers.buildHandlers().forEach { (type, handler) -> register(type, handler) }
    }

    override fun getStores(): List<RealtimeStore<*>> = stores.all()
}
