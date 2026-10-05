package com.bendey.restaurant.core.realtime.pending

import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.pendingapproval.PendingApprovalOrder
import com.bendey.restaurant.core.domain.pendingapproval.PendingApprovalRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PendingApprovalStoreTest {
    private fun order(id: Int) = PendingApprovalOrder(
        orderId = id, orderNumber = id, sessionId = 1, tableName = "4", orderType = "dine_in", notes = null,
        createdAt = "", customerName = null, customerPhone = null, total = 10.0, items = emptyList(),
    )

    private class FakeRepo(var list: AppResult<List<PendingApprovalOrder>>) : PendingApprovalRepository {
        var approved = mutableListOf<Int>()
        override suspend fun listPending() = list
        override suspend fun approve(orderId: Int): AppResult<Unit> {
            approved += orderId
            list = AppResult.Success((list as AppResult.Success).data.filterNot { it.orderId == orderId })
            return AppResult.Success(Unit)
        }
        override suspend fun reject(orderId: Int, reason: String): AppResult<Unit> = AppResult.Error("no")
    }

    @Test fun deshabilitadoNoConsulta() = runBlocking {
        val store = PendingApprovalStore(FakeRepo(AppResult.Success(listOf(order(1)))))
        store.refresh()
        assertEquals(0, store.state.value.count)
        assertFalse(store.state.value.loaded)
    }

    @Test fun refrescaYCuenta() = runBlocking {
        val store = PendingApprovalStore(FakeRepo(AppResult.Success(listOf(order(1), order(2)))))
        store.setEnabled(true)
        store.refresh()
        assertEquals(2, store.state.value.count)
        assertTrue(store.state.value.loaded)
    }

    @Test fun errorNoSeConfundeConVacio() = runBlocking {
        val repo = FakeRepo(AppResult.Success(listOf(order(1))))
        val store = PendingApprovalStore(repo)
        store.setEnabled(true)
        store.refresh()
        repo.list = AppResult.Error("sin red")
        store.refresh()
        assertTrue(store.state.value.error)
        assertEquals(1, store.state.value.count) // conserva lo ultimo que vio
    }

    @Test fun aprobarQuitaDeLaCola() = runBlocking {
        val repo = FakeRepo(AppResult.Success(listOf(order(1), order(2))))
        val store = PendingApprovalStore(repo)
        store.setEnabled(true)
        store.refresh()
        assertTrue(store.approve(1) is AppResult.Success)
        assertEquals(listOf(2), store.state.value.orders.map { it.orderId })
    }

    @Test fun rechazoFallidoNoQuitaNada() = runBlocking {
        val store = PendingApprovalStore(FakeRepo(AppResult.Success(listOf(order(1)))))
        store.setEnabled(true)
        store.refresh()
        assertTrue(store.reject(1, "x") is AppResult.Error)
        assertEquals(1, store.state.value.count)
    }

    @Test fun cambiarDePermisoLimpia() = runBlocking {
        val store = PendingApprovalStore(FakeRepo(AppResult.Success(listOf(order(1)))))
        store.setEnabled(true)
        store.refresh()
        store.setEnabled(false)
        assertEquals(0, store.state.value.count)
    }
}
