package com.bendey.restaurant.core.realtime.dispatcher

import com.bendey.restaurant.core.domain.restaurant.RestaurantTable
import com.bendey.restaurant.core.domain.restaurant.TableStatus
import com.bendey.restaurant.core.realtime.DomainEvent
import com.bendey.restaurant.core.realtime.RealtimeSchema
import com.bendey.restaurant.core.realtime.domains.DomainHandler
import com.bendey.restaurant.core.realtime.domains.DomainModule
import com.bendey.restaurant.core.realtime.domains.DomainRegistry
import com.bendey.restaurant.core.realtime.effects.SideEffectRunner
import com.bendey.restaurant.core.realtime.store.PatchResult
import com.bendey.restaurant.core.realtime.store.RealtimeStore
import com.bendey.restaurant.core.realtime.store.createRealtimeStore
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

/** Puerto de `dispatcher.test.ts` (Tauri) — dedupe por event.id, session.opened aplica patch. */
class RealtimeDispatcherTest {

    private fun sampleTable(id: Int) = RestaurantTable(
        id = id,
        floorId = 1,
        floorName = "Salón",
        name = "Mesa $id",
        capacity = 4,
        status = TableStatus.LIBRE,
        active = true,
        sessionId = null,
        totalAmount = null,
        waiterName = null,
        guests = null,
    )

    private fun buildEvent(id: String, type: String, scope: Map<String, Int> = emptyMap()): DomainEvent = DomainEvent(
        v = 1,
        id = id,
        type = type,
        tenantId = 42,
        branchId = 1,
        occurredAt = Instant.now().toString(),
        scope = JsonObject(scope.mapValues { JsonPrimitive(it.value) }),
    )

    @Test
    fun dedupeByEventId() {
        val registry = DomainRegistry()
        var handleCount = 0
        val domain = object : DomainModule {
            override val name = "test"
            override val eventPrefixes = listOf("test.")
            override fun matchesEventType(type: String) = type.startsWith("test.")
            override fun registerHandlers(register: (String, DomainHandler) -> Unit) {
                register("test.event") { ctx -> handleCount++; ctx.recordPatch(true) }
            }
            override fun getStores(): List<RealtimeStore<*>> = emptyList()
        }
        registry.register(domain)
        val observability = RealtimeObservability()
        val dispatcher = RealtimeDispatcher(registry, observability, SideEffectRunner())

        val event = buildEvent(id = "evt-1", type = "test.event")
        dispatcher.dispatch(event)
        dispatcher.dispatch(event)

        assertEquals(1, handleCount)
        assertEquals(2, observability.getSnapshot().eventsReceivedTotal)
        assertEquals(1, observability.getSnapshot().eventsDuplicatedTotal)
    }

    @Test
    fun sessionOpenedPatchesTable() {
        val registry = DomainRegistry()
        val tablesStore = createRealtimeStore<RestaurantTable>(getId = { it.id })
        tablesStore.upsert(sampleTable(12))

        val domain = object : DomainModule {
            override val name = "restaurant"
            override val eventPrefixes = listOf("restaurant.")
            override fun matchesEventType(type: String) = type.startsWith("restaurant.")
            override fun registerHandlers(register: (String, DomainHandler) -> Unit) {
                register("restaurant.session.opened") { ctx ->
                    val tableId = RealtimeSchema.readEventScopeId(ctx.event, "table_id")
                    val sessionId = RealtimeSchema.readEventScopeId(ctx.event, "session_id")
                    val applied = if (tableId != null && sessionId != null) {
                        tablesStore.patch(tableId.toString()) {
                            copy(status = TableStatus.OCUPADA, sessionId = sessionId)
                        } == PatchResult.APPLIED
                    } else {
                        false
                    }
                    ctx.recordPatch(applied)
                }
            }
            override fun getStores(): List<RealtimeStore<*>> = listOf(tablesStore)
        }
        registry.register(domain)
        val dispatcher = RealtimeDispatcher(registry, RealtimeObservability(), SideEffectRunner())

        val event = buildEvent(
            id = "evt-2",
            type = "restaurant.session.opened",
            scope = mapOf("table_id" to 12, "session_id" to 155),
        )
        dispatcher.dispatch(event)

        val table = tablesStore.getSnapshot().entities["12"]
        assertEquals(TableStatus.OCUPADA, table?.status)
        assertEquals(155, table?.sessionId)
    }

    @Test
    fun unknownDomainIsDiscarded() {
        val registry = DomainRegistry()
        val observability = RealtimeObservability()
        val dispatcher = RealtimeDispatcher(registry, observability, SideEffectRunner())
        val event = buildEvent(id = "evt-3", type = "unregistered.domain.event")
        dispatcher.dispatch(event)
        assertEquals(1, observability.getSnapshot().eventsUnknownDomainTotal)
    }
}
