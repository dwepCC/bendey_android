package com.bendey.restaurant.feature.dashboard

import com.bendey.restaurant.core.domain.dashboard.DashboardPaymentSlice
import org.junit.Assert.assertEquals
import org.junit.Test

/** Panel «Hoy» del demo Sazón Criolla (27-oct): salían «cash» y «directo» crudos. */
class DashboardLabelsTest {
    @Test
    fun `traduce cash y los tipos de pedido`() {
        assertEquals("Efectivo", dashboardPaymentMethodLabel("cash"))
        assertEquals("Yape", dashboardPaymentMethodLabel("yape"))
        assertEquals("Venta directa", dashboardOrderTypeLabel("directo"))
        assertEquals("Mesa", dashboardOrderTypeLabel("dine_in"))
        assertEquals("Menu digital", dashboardOrderTypeLabel("menu_digital"))
    }

    @Test
    fun `estado paid de una nota de venta es Cobrada`() {
        assertEquals("Cobrada", recentSessionStatusLabel("paid"))
        assertEquals("Cobrada", recentSessionStatusLabel("billed"))
    }

    @Test
    fun `junta cash y efectivo en una sola fila`() {
        val grouped = groupPaymentSlicesByLabel(
            listOf(
                DashboardPaymentSlice(method = "yape", amount = 90.0, count = 2),
                DashboardPaymentSlice(method = "cash", amount = 70.0, count = 3),
                DashboardPaymentSlice(method = "efectivo", amount = 40.0, count = 1),
            ),
        )
        assertEquals(listOf("Efectivo", "Yape"), grouped.map { it.method })
        assertEquals(110.0, grouped[0].amount, 0.001)
        assertEquals(4, grouped[0].count)
    }
}
