package com.bendey.restaurant.core.realtime.store

import com.bendey.restaurant.core.domain.restaurant.RestaurantTable
import com.bendey.restaurant.core.domain.restaurant.TableStatus
import com.bendey.restaurant.core.realtime.dispatcher.RealtimeBatch
import kotlin.test.Test
import kotlin.test.assertEquals

/** Puerto de `createStore.test.ts` (Tauri) — batch notify once. */
class RealtimeStoreTest {

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

    @Test
    fun batchCommitsOncePerBatch() {
        RealtimeBatch.resetForTests()
        val store = createRealtimeStore<RestaurantTable>(getId = { it.id })
        store.upsert(sampleTable(1))
        val versionAfterUpsert = store.getSnapshot().meta.version

        RealtimeBatch.run {
            store.patch("1") { copy(status = TableStatus.OCUPADA) }
            store.patch("1") { copy(guests = 4) }
        }

        val snapshot = store.getSnapshot()
        assertEquals(versionAfterUpsert + 1, snapshot.meta.version)
        assertEquals(TableStatus.OCUPADA, snapshot.entities["1"]?.status)
        assertEquals(4, snapshot.entities["1"]?.guests)
    }

    @Test
    fun patchMissingReturnsResultMissing() {
        val store = createRealtimeStore<RestaurantTable>(getId = { it.id })
        val result = store.patch("999") { copy(status = TableStatus.OCUPADA) }
        assertEquals(PatchResult.MISSING, result)
    }

    @Test
    fun resetClearsSnapshot() {
        val store = createRealtimeStore<RestaurantTable>(getId = { it.id })
        store.upsert(sampleTable(1))
        store.reset()
        assertEquals(0, store.getSnapshot().entities.size)
        assertEquals(0, store.getSnapshot().ids.size)
    }
}
