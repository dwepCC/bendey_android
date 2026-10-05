package com.bendey.restaurant.core.realtime.effects

import com.bendey.restaurant.core.domain.permission.RestaurantPermissions
import com.bendey.restaurant.core.domain.kitchen.shouldPlayNewOrderSound
import com.bendey.restaurant.core.realtime.DomainEvent
import com.bendey.restaurant.core.realtime.NewOrderEventTypes
import com.bendey.restaurant.core.realtime.NewOrderSoundPlayer
import com.bendey.restaurant.core.realtime.RealtimeSchema
import com.bendey.restaurant.core.realtime.pending.PendingApprovalStore
import com.bendey.restaurant.core.domain.pendingapproval.PendingApprovalLogic
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
            shouldPlayNewOrderSound(event.type) &&
            RestaurantPermissions.canReceiveNewOrderSound(ctx.restaurantPermissions)

    override fun run(event: DomainEvent, ctx: SideEffectContext) {
        soundPlayer.play()
    }
}

/**
 * R10.2: `restaurant.order.pending_approval` (pedido del QR por revisar) suena como "nuevo pedido" y avisa
 * en pantalla, SOLO a quien puede revisar la cola. `menu.order.created` no suena (regla R7). El conteo del
 * badge lo refresca el handler del dominio (`RestaurantHandlers`).
 */
@Singleton
class PendingApprovalSideEffect @Inject constructor(
    private val soundPlayer: NewOrderSoundPlayer,
    private val store: PendingApprovalStore,
) : SideEffect {
    override val name: String = "PendingApproval"

    override fun matches(event: DomainEvent, ctx: SideEffectContext): Boolean =
        PendingApprovalLogic.shouldPlaySound(event.type, ctx.restaurantPermissions)

    override fun run(event: DomainEvent, ctx: SideEffectContext) {
        soundPlayer.play()
        store.notifyArrival(RealtimeSchema.readDataString(event, "table_name"))
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
