package com.bendey.restaurant.core.realtime.effects

import com.bendey.restaurant.core.domain.permission.RestaurantPermissions
import com.bendey.restaurant.core.realtime.DomainEvent
import com.bendey.restaurant.core.realtime.NewOrderEventTypes
import com.bendey.restaurant.core.realtime.NewOrderSoundPlayer
import javax.inject.Inject
import javax.inject.Singleton

/** Puerto de `effects/index.ts` (Tauri) — Sound activo, resto reservado (stub). */
@Singleton
class SoundSideEffect @Inject constructor(
    private val soundPlayer: NewOrderSoundPlayer,
) : SideEffect {
    override val name: String = "Sound"

    override fun matches(event: DomainEvent, ctx: SideEffectContext): Boolean =
        event.type in NewOrderEventTypes.ALERT_TYPES &&
            RestaurantPermissions.canReceiveNewOrderSound(ctx.restaurantPermissions)

    override fun run(event: DomainEvent, ctx: SideEffectContext) {
        soundPlayer.play()
    }
}

private class StubSideEffect(override val name: String) : SideEffect {
    override fun matches(event: DomainEvent, ctx: SideEffectContext): Boolean = false
    override fun run(event: DomainEvent, ctx: SideEffectContext) = Unit
}

val toastSideEffect: SideEffect = StubSideEffect("Toast")
val navigationSideEffect: SideEffect = StubSideEffect("Navigation")
val badgeSideEffect: SideEffect = StubSideEffect("Badge")
val notificationSideEffect: SideEffect = StubSideEffect("Notification")
val analyticsSideEffect: SideEffect = StubSideEffect("Analytics")
