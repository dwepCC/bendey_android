package com.bendey.restaurant.core.domain.products

import com.bendey.restaurant.core.domain.model.AppResult
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductStockTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun await(cond: () -> Boolean) = runBlocking { withTimeout(3000) { while (!cond()) delay(10) } }

    private class FakeRepo(var result: AppResult<ProductStock>) : ProductStockRepository {
        val calls = AtomicInteger()
        override suspend fun stock(productId: Int): AppResult<ProductStock> {
            calls.incrementAndGet()
            return result
        }
    }

    @Test
    fun `nivel, cantidades y cuándo va el ícono`() {
        assertEquals(StockLevel.OUT, ProductStockRules.level(0.0, 5.0))
        assertEquals(StockLevel.LOW, ProductStockRules.level(5.0, 5.0))
        assertEquals(StockLevel.OK, ProductStockRules.level(3.0, 0.0))
        assertEquals("12", ProductStockRules.qty(12.0))
        assertEquals("1.5", ProductStockRules.qty(1.5))
        assertTrue(ProductStockRules.showsIcon(showPosStock = true, manageStock = true))
        assertFalse(ProductStockRules.showsIcon(showPosStock = true, manageStock = false))
        assertFalse(ProductStockRules.showsIcon(showPosStock = false, manageStock = true))
    }

    @Test
    fun `abre, consulta una vez por ventana de caché y se cierra`() {
        val repo = FakeRepo(AppResult.Success(ProductStock(total = 8.0, minStock = 2.0, presentations = emptyList())))
        var clock = 1_000L
        val p = ProductStockPresenter(scope, repo) { clock }

        p.open(7, "Gaseosa")
        await { p.state.value?.loading == false }
        assertEquals(8.0, p.state.value?.stock?.total ?: 0.0, 0.0)
        assertEquals("Gaseosa", p.state.value?.productName)

        p.dismiss()
        assertNull(p.state.value)
        p.open(7, "Gaseosa") // dentro de los 30 s: sin otra petición
        assertEquals(1, repo.calls.get())
        assertEquals(false, p.state.value?.loading)

        clock += ProductStockPresenter.CACHE_MS + 1
        p.open(7, "Gaseosa")
        await { repo.calls.get() == 2 && p.state.value?.loading == false }
    }

    @Test
    fun `un error se muestra en el diálogo`() {
        val repo = FakeRepo(AppResult.Error("sin red"))
        val p = ProductStockPresenter(scope, repo)
        p.open(3, "Lomo")
        await { p.state.value?.loading == false }
        assertEquals("sin red", p.state.value?.error)
    }
}
