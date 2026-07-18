package com.bendey.restaurant.core.realtime

import com.bendey.restaurant.core.realtime.domains.DomainRegistry
import com.bendey.restaurant.core.realtime.domains.restaurant.RestaurantDomain
import com.bendey.restaurant.core.realtime.domains.stubs.billingDomain
import com.bendey.restaurant.core.realtime.domains.stubs.cashDomain
import com.bendey.restaurant.core.realtime.domains.stubs.contactsDomain
import com.bendey.restaurant.core.realtime.domains.stubs.dashboardDomain
import com.bendey.restaurant.core.realtime.domains.stubs.deliveryDomain
import com.bendey.restaurant.core.realtime.domains.stubs.inventoryDomain
import com.bendey.restaurant.core.realtime.domains.stubs.purchasesDomain
import com.bendey.restaurant.core.realtime.domains.stubs.salesDomain
import com.bendey.restaurant.core.realtime.effects.SideEffectRunner
import com.bendey.restaurant.core.realtime.effects.SoundSideEffect
import com.bendey.restaurant.core.realtime.effects.analyticsSideEffect
import com.bendey.restaurant.core.realtime.effects.badgeSideEffect
import com.bendey.restaurant.core.realtime.effects.navigationSideEffect
import com.bendey.restaurant.core.realtime.effects.notificationSideEffect
import com.bendey.restaurant.core.realtime.effects.toastSideEffect
import javax.inject.Inject
import javax.inject.Singleton

/** Bootstrap idempotente — paridad FRT-1 (`platform.ts` en Tauri). */
@Singleton
class RealtimePlatform @Inject constructor(
    private val domainRegistry: DomainRegistry,
    private val restaurantDomain: RestaurantDomain,
    private val sideEffectRunner: SideEffectRunner,
    private val soundSideEffect: SoundSideEffect,
) {
    @Volatile
    private var initialized = false

    @Synchronized
    fun init() {
        if (initialized) return
        initialized = true
        registerDomains()
        registerSideEffects()
    }

    private fun registerDomains() {
        domainRegistry.register(restaurantDomain)
        domainRegistry.register(inventoryDomain)
        domainRegistry.register(salesDomain)
        domainRegistry.register(billingDomain)
        domainRegistry.register(dashboardDomain)
        domainRegistry.register(cashDomain)
        domainRegistry.register(purchasesDomain)
        domainRegistry.register(contactsDomain)
        domainRegistry.register(deliveryDomain)
    }

    private fun registerSideEffects() {
        sideEffectRunner.register(soundSideEffect)
        sideEffectRunner.register(toastSideEffect)
        sideEffectRunner.register(navigationSideEffect)
        sideEffectRunner.register(badgeSideEffect)
        sideEffectRunner.register(notificationSideEffect)
        sideEffectRunner.register(analyticsSideEffect)
    }
}
