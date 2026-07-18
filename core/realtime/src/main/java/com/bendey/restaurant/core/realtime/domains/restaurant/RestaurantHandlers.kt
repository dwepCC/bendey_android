package com.bendey.restaurant.core.realtime.domains.restaurant

import com.bendey.restaurant.core.domain.restaurant.TableStatus
import com.bendey.restaurant.core.realtime.RealtimeSchema
import com.bendey.restaurant.core.realtime.UiPresence
import com.bendey.restaurant.core.realtime.domains.DomainHandler
import com.bendey.restaurant.core.realtime.domains.DomainHandlerContext
import com.bendey.restaurant.core.realtime.recovery.RecoveryPolicy
import com.bendey.restaurant.core.realtime.recovery.RecoveryRequest
import com.bendey.restaurant.core.realtime.recovery.RecoveryScheduler
import com.bendey.restaurant.core.realtime.recovery.RecoveryScope
import com.bendey.restaurant.core.realtime.recovery.RestaurantHydrators
import com.bendey.restaurant.core.realtime.store.RestaurantStores
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Puerto 1:1 de `domains/restaurant/handlers.ts` (Tauri) — 14 event.type -> handler.
 * No simplificar `onBillClosed` ni `onOrderCreated`: la lógica `UiPresence` evita GET
 * innecesarios y evita omitir GET necesarios (ANDROID_REALTIME_PORTING_GUIDE.md §12).
 */
@Singleton
class RestaurantHandlers @Inject constructor(
    private val hydrators: RestaurantHydrators,
    private val scheduler: RecoveryScheduler,
    private val stores: RestaurantStores,
) {
    fun buildHandlers(): Map<String, DomainHandler> = mapOf(
        "restaurant.table.updated" to ::onTableUpdated,
        "restaurant.session.opened" to ::onSessionOpened,
        "restaurant.session.updated" to ::onSessionUpdated,
        "restaurant.session.closed" to ::onSessionClosed,
        "restaurant.session.moved" to ::onSessionMoved,
        "restaurant.order.created" to ::onOrderCreated,
        "restaurant.order.updated" to ::onOrderCreated,
        "restaurant.order.cancelled" to ::onSessionClosed,
        "restaurant.comanda.updated" to ::onComandaUpdated,
        "restaurant.bill.closed" to ::onBillClosed,
        "restaurant.kitchen.updated" to ::onComandaUpdated,
        "menu.order.created" to ::onOrderCreated,
        "menu.order.accepted" to ::onOrderCreated,
        "menu.session.updated" to ::onSessionUpdated,
    )

    private fun markTableFree(tableId: Int, recordPatch: (Boolean) -> Unit) {
        val applied = hydrators.patchTableFromSessionEvent(tableId) {
            copy(
                status = TableStatus.LIBRE,
                sessionId = null,
                totalAmount = 0.0,
                guests = null,
                waiterName = null,
                browsingOnly = false,
            )
        }
        recordPatch(applied)
        if (!applied) {
            scheduler.schedule(
                RecoveryRequest(RecoveryPolicy.PARTIAL, RecoveryScope(domain = "restaurant", slice = "tables"), "table_missing"),
            )
        }
    }

    private fun onTableUpdated(ctx: DomainHandlerContext) {
        ctx.recordPatch(false)
        scheduler.schedule(
            RecoveryRequest(RecoveryPolicy.PARTIAL, RecoveryScope(domain = "restaurant", slice = "tables"), "table_updated"),
        )
    }

    private fun onSessionOpened(ctx: DomainHandlerContext) {
        val event = ctx.event
        val tableId = RealtimeSchema.readEventScopeId(event, "table_id")
        val eventSessionId = RealtimeSchema.readEventScopeId(event, "session_id")
        if (tableId == null || eventSessionId == null) return

        // Las sesiones abiertas por el menú digital (cliente escaneó el QR) todavía
        // no ocupan la mesa: recién la ocupan con el primer pedido (onOrderCreated).
        // Se refleja como "viendo la carta" para que el staff no la vea libre sin más.
        val browsingOnly = RealtimeSchema.readDataString(event, "order_source") == "digital_menu"
        val eventGuests = RealtimeSchema.readDataDouble(event, "guests")?.toInt()
        val applied = hydrators.patchTableFromSessionEvent(tableId) {
            if (browsingOnly) {
                copy(status = TableStatus.LIBRE, sessionId = eventSessionId, browsingOnly = true)
            } else {
                copy(
                    status = TableStatus.OCUPADA,
                    sessionId = eventSessionId,
                    guests = eventGuests ?: guests,
                    browsingOnly = false,
                )
            }
        }
        ctx.recordPatch(applied)
        if (!applied) {
            scheduler.schedule(
                RecoveryRequest(RecoveryPolicy.PARTIAL, RecoveryScope(domain = "restaurant", slice = "tables"), "patch_missing"),
            )
        }
    }

    private fun onSessionUpdated(ctx: DomainHandlerContext) {
        val event = ctx.event
        val tableId = RealtimeSchema.readEventScopeId(event, "table_id")
        val sessionId = RealtimeSchema.readEventScopeId(event, "session_id")
        val eventTotalAmount = RealtimeSchema.readDataDouble(event, "total_amount")

        var applied = false
        if (tableId != null && eventTotalAmount != null) {
            applied = hydrators.patchTableFromSessionEvent(tableId) { copy(totalAmount = eventTotalAmount) } || applied
        }
        if (sessionId != null && eventTotalAmount != null) {
            applied = hydrators.patchSessionFromEvent(sessionId) { copy(totalAmount = eventTotalAmount) } || applied
        }
        ctx.recordPatch(applied)

        if (sessionId != null && UiPresence.sessions.contains(sessionId) && !applied) {
            scheduler.schedule(
                RecoveryRequest(
                    RecoveryPolicy.ENTITY,
                    RecoveryScope(domain = "restaurant", entity = "session", entityId = sessionId),
                    "payload_insufficient",
                ),
            )
        }
    }

    private fun onSessionClosed(ctx: DomainHandlerContext) {
        val event = ctx.event
        val tableId = RealtimeSchema.readEventScopeId(event, "table_id")
        val sessionId = RealtimeSchema.readEventScopeId(event, "session_id")

        if (tableId != null) markTableFree(tableId, ctx.recordPatch)
        if (sessionId != null) {
            stores.sessions.remove(sessionId.toString())
            ctx.recordPatch(true)
        }

        scheduler.schedule(
            RecoveryRequest(RecoveryPolicy.PARTIAL, RecoveryScope(domain = "restaurant", slice = "operational"), "session_closed"),
        )
        if (UiPresence.posOrders) {
            scheduler.schedule(
                RecoveryRequest(RecoveryPolicy.PARTIAL, RecoveryScope(domain = "restaurant", slice = "orders"), "session_closed"),
            )
        }
    }

    private fun onSessionMoved(ctx: DomainHandlerContext) {
        ctx.recordPatch(false)
        scheduler.schedule(
            RecoveryRequest(RecoveryPolicy.PARTIAL, RecoveryScope(domain = "restaurant", slice = "tables"), "session_moved"),
        )
    }

    private fun onOrderCreated(ctx: DomainHandlerContext) {
        val event = ctx.event
        val tableId = RealtimeSchema.readEventScopeId(event, "table_id")
        val sessionId = RealtimeSchema.readEventScopeId(event, "session_id")

        if (tableId != null) {
            val applied = hydrators.patchTableFromSessionEvent(tableId) {
                copy(status = TableStatus.OCUPADA, browsingOnly = false)
            }
            ctx.recordPatch(applied)
        }

        if (sessionId != null && UiPresence.sessions.contains(sessionId)) {
            scheduler.schedule(
                RecoveryRequest(
                    RecoveryPolicy.ENTITY,
                    RecoveryScope(domain = "restaurant", entity = "session", entityId = sessionId),
                    "order_created",
                ),
            )
        }
        if (UiPresence.kitchen) {
            scheduler.schedule(
                RecoveryRequest(RecoveryPolicy.PARTIAL, RecoveryScope(domain = "restaurant", slice = "kitchen"), "order_created"),
            )
        }
        if (UiPresence.posOrders) {
            scheduler.schedule(
                RecoveryRequest(RecoveryPolicy.PARTIAL, RecoveryScope(domain = "restaurant", slice = "orders"), "order_created"),
            )
        }
        scheduler.schedule(
            RecoveryRequest(RecoveryPolicy.PARTIAL, RecoveryScope(domain = "restaurant", slice = "operational"), "order_created"),
        )
    }

    private fun onBillClosed(ctx: DomainHandlerContext) {
        val event = ctx.event
        val tableId = RealtimeSchema.readEventScopeId(event, "table_id")
        val sessionId = RealtimeSchema.readEventScopeId(event, "session_id")
        val saleId = RealtimeSchema.readEventScopeId(event, "sale_id")
        val sessionClosed = RealtimeSchema.readDataBoolean(event, "session_closed") == true
        val total = RealtimeSchema.readDataDouble(event, "total")
        val paymentStatus = RealtimeSchema.readDataString(event, "payment_status")

        if (sessionClosed && tableId != null) markTableFree(tableId, ctx.recordPatch)
        if (sessionId != null) stores.sessions.remove(sessionId.toString())
        if (saleId != null && sessionId != null) {
            hydrators.recordBillClosed(saleId, sessionId, total ?: 0.0, paymentStatus)
        }

        if (UiPresence.posOrders) {
            scheduler.schedule(
                RecoveryRequest(RecoveryPolicy.PARTIAL, RecoveryScope(domain = "restaurant", slice = "orders"), "bill_closed"),
            )
        }
        if (UiPresence.caja) {
            scheduler.schedule(
                RecoveryRequest(RecoveryPolicy.PARTIAL, RecoveryScope(domain = "restaurant", slice = "cash"), "bill_closed"),
            )
        }
        val dashboardRange = UiPresence.dashboardRange
        if (UiPresence.dashboard && dashboardRange != null) {
            scheduler.schedule(
                RecoveryRequest(
                    RecoveryPolicy.PARTIAL,
                    RecoveryScope(
                        domain = "restaurant",
                        slice = "dashboard-operacion",
                        dateFrom = dashboardRange.from,
                        dateTo = dashboardRange.to,
                    ),
                    "bill_closed",
                ),
            )
        }
        scheduler.schedule(
            RecoveryRequest(RecoveryPolicy.PARTIAL, RecoveryScope(domain = "restaurant", slice = "operational"), "bill_closed"),
        )
        ctx.recordPatch(true)
    }

    private fun onComandaUpdated(ctx: DomainHandlerContext) {
        ctx.recordPatch(false)
        if (UiPresence.kitchen) {
            scheduler.schedule(
                RecoveryRequest(RecoveryPolicy.PARTIAL, RecoveryScope(domain = "restaurant", slice = "kitchen"), "comanda_updated"),
            )
        }
    }
}
