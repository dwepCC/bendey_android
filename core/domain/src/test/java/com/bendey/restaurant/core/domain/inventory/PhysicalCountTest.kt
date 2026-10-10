package com.bendey.restaurant.core.domain.inventory

import com.bendey.restaurant.core.domain.contacts.PurchaseHistoryPage
import com.bendey.restaurant.core.domain.contacts.PurchaseHistoryRules
import com.bendey.restaurant.core.domain.contacts.PurchaseSale
import com.bendey.restaurant.core.domain.model.AppResult
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Conteo físico y valorización (lógica pura + presentador) y reglas del historial de compras del cliente. */
class PhysicalCountTest {
    @Test fun cantidadContadaValida() {
        assertEquals(3.0, PhysicalCountRules.parseCounted("3")!!, 0.0)
        assertEquals(2.5, PhysicalCountRules.parseCounted("2,5")!!, 0.0)
        assertEquals(0.0, PhysicalCountRules.parseCounted("0")!!, 0.0)
        assertNull(PhysicalCountRules.parseCounted(""))
        assertNull(PhysicalCountRules.parseCounted("-1"))
        assertNull(PhysicalCountRules.parseCounted("abc"))
    }

    @Test fun lineasSoloConLoContado() {
        val counted = mapOf((1 to 0) to "5", (2 to 0) to "", (3 to 0) to "0")
        val lines = PhysicalCountRules.buildLines(counted)
        assertEquals(listOf(CountLineInput(1, 0, 5.0), CountLineInput(3, 0, 0.0)), lines.sortedBy { it.productId })
        assertNull(PhysicalCountRules.validate(counted))
    }

    @Test fun validaVacioInvalidoYTope() {
        assertEquals("Cuenta al menos un producto.", PhysicalCountRules.validate(emptyMap()))
        assertNotNull(PhysicalCountRules.validate(mapOf((1 to 0) to "-2")))
        assertNotNull(PhysicalCountRules.validate(mapOf((1 to 0) to "x")))
        val many = (1..501).associate { (it to 0) to "1" }
        assertNotNull(PhysicalCountRules.validate(many))
        assertNull(PhysicalCountRules.validate((1..500).associate { (it to 0) to "1" }))
    }

    @Test fun etiquetasDeDiferencia() {
        assertEquals("Sin diferencia", PhysicalCountRules.differenceLabel(0.0))
        assertEquals("+3 (sobra)", PhysicalCountRules.differenceLabel(3.0))
        assertEquals("−2.5 (falta)", PhysicalCountRules.differenceLabel(-2.5))
        assertEquals("+S/ 12.00", PhysicalCountRules.signedMoney(12.0))
        assertEquals("−S/ 4.50", PhysicalCountRules.signedMoney(-4.5))
        assertEquals("S/ 0.00", PhysicalCountRules.signedMoney(0.0))
    }

    @Test fun soloLasLineasConDiferenciaOError() {
        val r = PhysicalCountResult(
            lines = listOf(
                CountResultLine(1, 0, "A", 5.0, 5.0, 0.0, 2.0, 0.0, false, null),
                CountResultLine(2, 0, "B", 5.0, 3.0, -2.0, 2.0, -4.0, false, null),
                CountResultLine(3, 0, "C", 1.0, 1.0, 0.0, 0.0, 0.0, false, "tiene series"),
            ),
            totalValueDiff = -4.0,
        )
        assertEquals(listOf("B", "C"), PhysicalCountRules.withDifference(r).map { it.name })
    }

    @Test fun filtraLaValorizacionPorTexto() {
        val rows = listOf(
            ValuationRow(1, 0, "AR01", "Arroz extra", 10.0, 3.0, 30.0, false),
            ValuationRow(2, 0, "PO02", "Pollo", -2.0, 12.0, -24.0, true),
        )
        assertEquals(2, PhysicalCountRules.filter(rows, "").size)
        assertEquals(listOf("Pollo"), PhysicalCountRules.filter(rows, "poll").map { it.name })
        assertEquals(listOf("Arroz extra"), PhysicalCountRules.filter(rows, "ar01").map { it.name })
        assertTrue(rows[1].negative)
    }

    // ---- presentador ----

    private class FakeRepo : InventoryCountRepository {
        val calls = CopyOnWriteArrayList<Triple<List<CountLineInput>, String, Boolean>>()
        override suspend fun valuation(branchId: Int?, q: String): AppResult<InventoryValuation> = AppResult.Success(InventoryValuation())
        override suspend fun physicalCount(branchId: Int, lines: List<CountLineInput>, note: String, apply: Boolean): AppResult<PhysicalCountResult> {
            calls += Triple(lines, note, apply)
            val res = lines.map {
                CountResultLine(it.productId, it.presentationId, "P${it.productId}", 10.0, it.counted, it.counted - 10.0, 2.0, (it.counted - 10.0) * 2.0, apply, null)
            }
            return AppResult.Success(
                PhysicalCountResult(applied = apply, lines = res, adjustedCount = if (apply) res.size else 0, totalValueDiff = res.sumOf { it.valueDiff }),
            )
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val repo = FakeRepo()
    private fun presenter() = PhysicalCountPresenter(scope, repo) {
        AppResult.Success(CountProductsLoad(listOf(CountProduct(1, "A1", "Arroz", "KG"), CountProduct(2, "B2", "Pollo", "KG")), skippedByPresentation = 1))
    }

    @After fun tearDown() = scope.cancel()
    private fun await(cond: () -> Boolean) = runBlocking { withTimeout(3000) { while (!cond()) delay(10) } }

    @Test fun calcularNoAplicaYAplicarSi() {
        val p = presenter()
        p.start(7)
        await { p.state.value.products.size == 2 }
        assertEquals(1, p.state.value.skippedByPresentation)
        assertFalse(p.state.value.canCalculate) // nada contado
        p.setCounted(1, "8")
        p.setCounted(2, "")
        assertEquals(1, p.state.value.countedCount)
        assertTrue(p.state.value.canCalculate)
        assertFalse(p.state.value.canApply) // sin vista previa no se aplica
        p.calculate()
        await { p.state.value.preview != null }
        assertFalse(repo.calls.single().third) // apply = false
        assertEquals(-4.0, p.state.value.preview!!.totalValueDiff, 0.0001)
        assertTrue(p.state.value.canApply)
        // Cambiar una cantidad invalida la vista previa.
        p.setCounted(1, "9")
        assertNull(p.state.value.preview)
        p.calculate()
        await { p.state.value.preview != null }
        p.openConfirm()
        assertTrue(p.state.value.confirmOpen)
        p.setNote("fin de mes")
        p.apply()
        await { p.state.value.applyResult != null && !p.state.value.applying }
        val applied = repo.calls.last()
        assertTrue(applied.third)
        assertEquals("fin de mes", applied.second)
        assertTrue(p.state.value.counted.isEmpty())
        assertFalse(p.state.value.confirmOpen)
    }

    @Test fun sinDiferenciasNoSePuedeAplicar() {
        val p = presenter()
        p.start(7)
        await { p.state.value.products.size == 2 }
        p.setCounted(1, "10") // igual al sistema (10)
        p.calculate()
        await { p.state.value.preview != null }
        assertFalse(p.state.value.canApply)
    }

    // ---- historial de compras ----

    @Test fun clienteGenericoSinHistorial() {
        assertTrue(PurchaseHistoryRules.isGenericClient("99999999", "CLIENTES VARIOS"))
        assertTrue(PurchaseHistoryRules.isGenericClient("00000000", "Cualquiera"))
        assertTrue(PurchaseHistoryRules.isGenericClient("9999-9999", "x"))
        assertFalse(PurchaseHistoryRules.isGenericClient("10401409781", "Ana Perez"))
    }

    @Test fun paginaSinRepetirVentas() {
        fun sale(id: Int) = PurchaseSale(id, "B001-$id", "boleta", "2026-10-0$id", 10.0, "issued", null)
        val first = PurchaseHistoryPage(sales = listOf(sale(1), sale(2)), total = 3, page = 1, limit = 2)
        assertTrue(first.hasMore)
        val second = PurchaseHistoryPage(sales = listOf(sale(2), sale(3)), total = 3, page = 2, limit = 2)
        val merged = PurchaseHistoryRules.append(first, second)
        assertEquals(listOf(1, 2, 3), merged.sales.map { it.id })
        assertFalse(merged.hasMore)
        assertEquals("Boleta", PurchaseHistoryRules.docLabel("boleta"))
        assertEquals("3", PurchaseHistoryRules.quantity(3.0))
        assertEquals("2.50", PurchaseHistoryRules.quantity(2.5))
    }
}
