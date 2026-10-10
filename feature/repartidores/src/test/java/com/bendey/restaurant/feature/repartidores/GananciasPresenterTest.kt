package com.bendey.restaurant.feature.repartidores

import com.bendey.restaurant.core.domain.delivery.DeliveryEarningsRepository
import com.bendey.restaurant.core.domain.delivery.EarningsPayout
import com.bendey.restaurant.core.domain.delivery.PendingEarning
import com.bendey.restaurant.core.domain.model.AppResult
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** §27: pestaña Ganancias (pagar con confirmación, errores de caja, refresco). */
class GananciasPresenterTest {

    private class FakeRepo(var items: List<PendingEarning>) : DeliveryEarningsRepository {
        val paid = CopyOnWriteArrayList<Int>()
        @Volatile var payError: String? = null
        @Volatile var loadError: String? = null
        @Volatile var booked = true

        override suspend fun pending(): AppResult<List<PendingEarning>> =
            loadError?.let { AppResult.Error(it) } ?: AppResult.Success(items)

        override suspend fun payout(driverId: Int): AppResult<EarningsPayout> {
            payError?.let { return AppResult.Error(it) }
            val e = items.first { it.driverId == driverId }
            paid += driverId
            items = items.filter { it.driverId != driverId }
            return AppResult.Success(EarningsPayout(e.name, e.total, e.deliveries, booked))
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val repo = FakeRepo(listOf(PendingEarning(1, "Luis", 4, 20.0), PendingEarning(2, "Ana", 1, 5.0)))
    private val messages = CopyOnWriteArrayList<String>()

    private fun presenter(canPay: Boolean = true): GananciasPresenter {
        val p = GananciasPresenter(scope, repo)
        p.setCanPay(canPay)
        scope.launch { p.messages.collect { messages += it } }
        return p
    }

    @After fun tearDown() = scope.cancel()

    private fun await(cond: () -> Boolean) = runBlocking { withTimeout(3000) { while (!cond()) delay(10) } }

    private fun loaded(p: GananciasPresenter): GananciasPresenter {
        p.refresh()
        await { !p.state.value.loading }
        return p
    }

    @Test fun cargaLoQueSeDebeYSumaElTotal() {
        val p = loaded(presenter())
        assertEquals(2, p.state.value.items.size)
        assertEquals(25.0, p.state.value.grandTotal, 0.001)
    }

    @Test fun sinGananciasPendientesLaListaQuedaVacia() {
        repo.items = emptyList()
        val p = loaded(presenter())
        assertTrue(p.state.value.items.isEmpty())
        assertNull(p.state.value.loadError)
    }

    @Test fun pagarPideConfirmacionYNoLlamaAlServidorHastaConfirmar() {
        val p = loaded(presenter())
        p.askPay(p.state.value.items[0])
        assertNotNull(p.state.value.confirm)
        assertTrue(repo.paid.isEmpty())
        p.dismissPay()
        assertNull(p.state.value.confirm)
        assertTrue(repo.paid.isEmpty())
    }

    @Test fun sinPermisoDeCajaNoSePuedePagar() {
        val p = loaded(presenter(canPay = false))
        p.askPay(p.state.value.items[0])
        assertNull(p.state.value.confirm)
        p.confirmPay()
        assertTrue(repo.paid.isEmpty())
    }

    @Test fun confirmarPagaYRefrescaLaLista() {
        val p = loaded(presenter())
        p.askPay(p.state.value.items[0])
        p.confirmPay()
        await { repo.paid.isNotEmpty() && p.state.value.items.size == 1 && !p.state.value.busy }
        assertEquals(listOf(1), repo.paid.toList())
        assertNull(p.state.value.confirm)
        await { messages.isNotEmpty() }
        assertEquals("Pagaste S/ 20.00 a Luis por 4 entregas.", messages[0])
    }

    @Test fun sinCajaAbiertaMuestraElErrorYNoQuitaAlRepartidor() {
        val p = loaded(presenter())
        repo.payError = "Abre tu caja para pagar las ganancias del repartidor."
        p.askPay(p.state.value.items[0])
        p.confirmPay()
        await { messages.isNotEmpty() && !p.state.value.busy }
        assertEquals("Abre tu caja para pagar las ganancias del repartidor.", messages[0])
        assertEquals(2, p.state.value.items.size)
        assertNull(p.state.value.confirm)
    }

    @Test fun siOtroCajeroYaPagoElDialogoSeCierraAlRefrescar() {
        val p = loaded(presenter())
        p.askPay(p.state.value.items[0])
        repo.items = listOf(PendingEarning(2, "Ana", 1, 5.0))
        p.refresh()
        await { p.state.value.items.size == 1 }
        assertNull(p.state.value.confirm)
    }

    @Test fun unFalloAlCargarSeInformaSinPerderLoQueYaHabia() {
        val p = loaded(presenter())
        repo.loadError = "Sin conexión"
        p.refresh()
        await { p.state.value.loadError != null }
        assertEquals(2, p.state.value.items.size)
    }
}
