package com.bendey.restaurant.core.data.receipt

import com.bendey.restaurant.core.domain.billing.SalePrintData
import com.bendey.restaurant.core.domain.billing.SalePrintLine
import com.bendey.restaurant.core.domain.billing.SalePrintPayment
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.NumberFormat
import java.util.Locale

/**
 * EL TICKET SOLO REFLEJA LO QUE YA CALCULÓ EL BACKEND — nunca recalcula el Recargo al Consumo.
 *
 * Mismos casos que buildSaleDocumentEscPos.test.ts (Tauri) y printers.service.ts (ERP): el ticket
 * de Android tiene que quedar igual a como estaba antes de esta funcionalidad cuando la venta no
 * tiene RC, y mostrar la línea exacta cuando sí lo tiene.
 */
class ReceiptTicketLayoutServiceChargeTest {

    private val money: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "PE"))

    private fun boleta(
        serviceChargeAmount: Double = 0.0,
        serviceChargeRate: Double = 0.0,
        total: Double = 16.0,
    ): SalePrintData = SalePrintData(
        docType = "BOLETA",
        sunatCode = "03",
        series = "B001",
        correlative = 474,
        number = "B001-00000474",
        issueDate = "2026-08-19",
        companyName = "CCORI WORKERS SERVICE S.A.C.",
        companyRuc = "20604560111",
        companyAddress = null,
        branchName = null,
        clientName = "Clientes Varios",
        clientDocNumber = "99999999",
        items = listOf(
            SalePrintLine(description = "6 Alitas", quantity = 1.0, unitPrice = 16.0, total = 16.0),
        ),
        subtotal = 13.56,
        taxAmount = 2.44,
        total = total,
        serviceChargeAmount = serviceChargeAmount,
        serviceChargeRate = serviceChargeRate,
        currency = "PEN",
        payments = listOf(SalePrintPayment(method = "cash", amount = total)),
        legendText = null,
    )

    // RC OFF: el ticket debe verse exactamente como antes de esta funcionalidad.
    @Test
    fun `no imprime ninguna linea de Recargo cuando la venta no lo tiene`() {
        val lines = ReceiptTicketLayout.build(boleta(), money).map { it.text }
        assertFalse(lines.any { it.contains("Recargo") })
    }

    // RC ON sin tasa conocida (comprobante viejo): mantiene el label genérico de siempre.
    @Test
    fun `imprime el Recargo al Consumo generico cuando no se conoce la tasa`() {
        val data = boleta().copy(subtotal = 100.0, taxAmount = 18.0, serviceChargeAmount = 5.0, total = 123.0)
        val lines = ReceiptTicketLayout.build(data, money).map { it.text }
        assertTrue(lines.any { it.contains("Recargo Consumo") && it.contains(money.format(5.0)) })
        assertTrue(lines.any { it.contains("TOTAL A PAGAR") && it.contains(money.format(123.0)) })
    }

    // RC ON con tasa congelada: el label corto reemplaza al genérico.
    @Test
    fun `imprime RC con el porcentaje cuando se conoce la tasa`() {
        val data = boleta(serviceChargeAmount = 5.0, serviceChargeRate = 5.0, total = 123.0)
            .copy(subtotal = 100.0, taxAmount = 18.0)
        val lines = ReceiptTicketLayout.build(data, money).map { it.text }
        assertTrue(lines.any { it.contains("RC 5%") && it.contains(money.format(5.0)) })
        assertFalse(lines.any { it.contains("Recargo Consumo") })
    }
}
