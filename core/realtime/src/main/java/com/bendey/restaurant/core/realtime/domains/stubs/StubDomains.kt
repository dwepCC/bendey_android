package com.bendey.restaurant.core.realtime.domains.stubs

import com.bendey.restaurant.core.realtime.domains.DomainHandler
import com.bendey.restaurant.core.realtime.domains.DomainModule
import com.bendey.restaurant.core.realtime.store.RealtimeStore

/** Dominio preparado — handlers en fases futuras / ERP. Puerto de `domains/stubs.ts` (Tauri). */
fun createStubDomain(domainName: String, prefixes: List<String>): DomainModule = object : DomainModule {
    override val name: String = domainName
    override val eventPrefixes: List<String> = prefixes

    override fun matchesEventType(type: String): Boolean = prefixes.any { type.startsWith(it) }

    override fun registerHandlers(register: (String, DomainHandler) -> Unit) {
        /* futuro */
    }

    override fun getStores(): List<RealtimeStore<*>> = emptyList()
}

val inventoryDomain = createStubDomain("inventory", listOf("inventory."))
val salesDomain = createStubDomain("sales", listOf("sale."))
val billingDomain = createStubDomain("billing", listOf("billing."))
val dashboardDomain = createStubDomain("dashboard", listOf("dashboard."))
val cashDomain = createStubDomain("cash", listOf("cash."))
val purchasesDomain = createStubDomain("purchases", listOf("purchase."))
val contactsDomain = createStubDomain("contacts", listOf("contact."))
val deliveryDomain = createStubDomain("delivery", listOf("delivery."))
