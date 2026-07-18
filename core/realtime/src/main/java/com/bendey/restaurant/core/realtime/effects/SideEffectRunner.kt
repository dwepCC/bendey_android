package com.bendey.restaurant.core.realtime.effects

import com.bendey.restaurant.core.realtime.DomainEvent
import javax.inject.Inject
import javax.inject.Singleton

data class SideEffectContext(
    val restaurantPermissions: List<String>,
)

interface SideEffect {
    val name: String
    fun matches(event: DomainEvent, ctx: SideEffectContext): Boolean
    fun run(event: DomainEvent, ctx: SideEffectContext)
}

/** Puerto de `effects/runner.ts` (Tauri). */
@Singleton
class SideEffectRunner @Inject constructor() {
    private val effects = mutableListOf<SideEffect>()
    private var getContext: () -> SideEffectContext = { SideEffectContext(emptyList()) }

    fun register(effect: SideEffect) {
        effects.add(effect)
    }

    fun setContextProvider(fn: () -> SideEffectContext) {
        getContext = fn
    }

    fun runAll(event: DomainEvent) {
        val ctx = getContext()
        for (effect in effects) {
            try {
                if (effect.matches(event, ctx)) effect.run(event, ctx)
            } catch (_: Exception) {
                /* side effect */
            }
        }
    }

    fun resetForTests() {
        effects.clear()
        getContext = { SideEffectContext(emptyList()) }
    }
}
