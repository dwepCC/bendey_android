package com.bendey.restaurant.core.realtime.domains.delivery

import com.bendey.restaurant.core.realtime.delivery.DeliveryBoardStore
import com.bendey.restaurant.core.realtime.domains.DomainHandler
import com.bendey.restaurant.core.realtime.domains.DomainHandlerContext
import com.bendey.restaurant.core.realtime.domains.DomainModule
import com.bendey.restaurant.core.realtime.store.RealtimeStore
import javax.inject.Inject
import javax.inject.Singleton

private val DELIVERY_PREFIXES = listOf("delivery.")

/** Eventos que obligan a volver a pedir el tablero (el servidor arma las 5 secciones, no se parchea a mano). */
object DeliveryEvents {
    const val BOARD_UPDATED = "delivery.board.updated"
    const val ASSIGNMENT_UPDATED = "delivery.assignment.updated"
    const val STATUS_UPDATED = "delivery.status.updated"
}

/**
 * D1: dominio Delivery (antes un stub). Escucha `delivery.board.updated` (el principal: payload
 * `{session_id, reason}`) y los eventos previos `delivery.assignment.updated` / `delivery.status.updated`, que
 * el backend mantiene. Cualquiera de ellos = refrescar el tablero, coalescido (ráfagas => 1 consulta).
 * Gemelo de `lib/realtime/domains/delivery` de Tauri.
 */
@Singleton
class DeliveryDomain @Inject constructor(
    private val store: DeliveryBoardStore,
) : DomainModule {
    override val name: String = "delivery"
    override val eventPrefixes: List<String> = DELIVERY_PREFIXES

    override fun matchesEventType(type: String): Boolean = DELIVERY_PREFIXES.any { type.startsWith(it) }

    override fun registerHandlers(register: (String, DomainHandler) -> Unit) {
        register(DeliveryEvents.BOARD_UPDATED, ::refetchBoard)
        register(DeliveryEvents.ASSIGNMENT_UPDATED, ::refetchBoard)
        register(DeliveryEvents.STATUS_UPDATED, ::refetchBoard)
    }

    override fun getStores(): List<RealtimeStore<*>> = emptyList()

    private fun refetchBoard(ctx: DomainHandlerContext) {
        ctx.recordPatch(false)
        store.scheduleRefresh()
    }
}
