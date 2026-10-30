package com.bendey.restaurant.core.data.receipt

import com.bendey.restaurant.core.domain.billing.AffectationTotal
import com.bendey.restaurant.core.domain.billing.SalePrintData
import com.bendey.restaurant.core.domain.billing.SalePrintEc
import com.bendey.restaurant.core.domain.billing.SalePrintLine
import com.bendey.restaurant.core.domain.locale.LabelKey
import com.bendey.restaurant.core.domain.locale.TaxRateOption
import com.bendey.restaurant.core.domain.locale.TenantLocale
import com.bendey.restaurant.core.domain.locale.TenantLocaleHolder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EcRideModelTest {
    private val ec = TenantLocale(
        country = "EC", currency = "USD", currencySymbol = "$", symbolSpacing = false,
        taxName = "IVA", taxAuthority = "SRI",
        taxRates = listOf(
            TaxRateOption("4", "IVA 15%", 15.0, isDefault = true),
            TaxRateOption("0", "IVA 0%", 0.0),
        ),
    )

    @After fun reset() = TenantLocaleHolder.set(TenantLocale.PE)

    private fun factura(
        code: String = "01",
        client: String? = "Ana",
        doc: String? = "1710034065",
        docType: String? = "05",
        auth: String? = null,
        authAt: String? = null,
    ) = SalePrintData(
        docType = "FACTURA", sunatCode = code, series = "001-001", correlative = 7, number = "001-001-000000007",
        issueDate = "30/10/2026", companyName = "Acme", companyRuc = "1790012345001", companyAddress = null,
        branchName = null, clientName = client, clientDocNumber = doc, clientDocType = docType,
        items = listOf(SalePrintLine("Pizza", 2.0, 5.75, 12.65, discount = 0.5, code = "P1", subtotal = 11.0)),
        subtotal = 11.5, taxAmount = 1.65, total = 13.15, currency = "USD", payments = emptyList(), legendText = null,
        affectationTotals = mapOf("4" to AffectationTotal(10.0, 1.5), "0" to AffectationTotal(1.5, 0.0)),
        accessKey = "3010202601179", authorizationNumber = auth,
        ec = SalePrintEc(authorizedAt = authAt),
    )

    @Test fun `solo factura NC y ND de un pais distinto de Peru usan el RIDE`() {
        assertTrue(EcRideModel.usesEcRide(factura("01"), ec))
        assertTrue(EcRideModel.usesEcRide(factura("04"), ec))
        assertTrue(EcRideModel.usesEcRide(factura("05"), ec))
        assertFalse(EcRideModel.usesEcRide(factura("00"), ec)) // nota de venta
        assertFalse(EcRideModel.usesEcRide(factura("01"), TenantLocale.PE))
    }

    @Test fun `consumidor final por tipo 07, nueves o sin cliente`() {
        assertTrue(EcRideModel.isFinalConsumer(factura(docType = "07", doc = "9999999999999")))
        assertTrue(EcRideModel.isFinalConsumer(factura(docType = "05", doc = "9999999999")))
        assertTrue(EcRideModel.isFinalConsumer(factura(client = null, doc = null, docType = null)))
        assertFalse(EcRideModel.isFinalConsumer(factura()))
    }

    @Test fun `linea del detalle usa la base sin IVA y reconstruye el precio unitario`() {
        val a = EcRideModel.lineAmounts(factura().items.single())
        assertEquals(11.0, a.base, 0.0)
        assertEquals(0.5, a.discount, 0.0)
        assertEquals(5.75, a.unitPrice, 1e-9) // (11 + 0.5) / 2
        // Backend viejo sin subtotal: cae al total de la línea.
        assertEquals(12.65, EcRideModel.lineAmounts(SalePrintLine("x", 1.0, 12.65, 12.65)).base, 0.0)
    }

    @Test fun `cantidad entera sin decimales y fraccion con dos`() {
        assertEquals("2", EcRideModel.qty(2.0))
        assertEquals("1.50", EcRideModel.qty(1.5))
        assertEquals("5.75", EcRideModel.fixed2(5.749))
    }

    @Test fun `numero de autorizacion es la clave hasta que el SRI autoriza`() {
        assertEquals("3010202601179", EcRideModel.authText(factura()))
        assertFalse(EcRideModel.isAuthorized(factura()))
        val ok = factura(auth = "9999", authAt = "30/10/2026 10:15:00")
        assertEquals("9999", EcRideModel.authText(ok))
        assertTrue(EcRideModel.isAuthorized(ok))
        assertFalse(EcRideModel.isAuthorized(factura(auth = "9999", authAt = null)))
    }

    @Test fun `totales del A4 por tarifa, subtotal, descuento, IVA y valor total`() {
        TenantLocaleHolder.set(ec)
        val tax = LabelKey.TAX_NAME.text
        val rows = EcRideModel.ridTotals(factura(), ec)
        assertEquals(
            listOf("SUBTOTAL 15 %", "SUBTOTAL 0 %", "SUBTOTAL SIN IMPUESTOS", "TOTAL DESCUENTO", "${tax.uppercase()} 15 %", "VALOR TOTAL"),
            rows.map { it.label },
        )
        assertEquals(10.0, rows[0].amount, 0.0)
        assertEquals(0.5, rows[3].amount, 0.0)
        assertTrue(rows.last().strong)
        assertEquals(13.15, rows.last().amount, 0.0)
    }

    @Test fun `la propina aparece solo si hay recargo`() {
        TenantLocaleHolder.set(ec)
        assertFalse(EcRideModel.ridTotals(factura(), ec).any { it.label == "PROPINA" })
        val conPropina = factura().copy(serviceChargeAmount = 1.0)
        assertTrue(EcRideModel.ridTotals(conPropina, ec).any { it.label == "PROPINA" })
        assertTrue(EcRideModel.ticketTotals(conPropina, ec).any { it.label == "Propina" })
    }

    @Test fun `totales del ticket empiezan por el subtotal`() {
        TenantLocaleHolder.set(ec)
        val tax = LabelKey.TAX_NAME.text
        val labels = EcRideModel.ticketTotals(factura(), ec).map { it.label }
        assertEquals(listOf("Subtotal", "Subtotal 15 %", "Subtotal 0 %", "Descuento", "$tax 15 %"), labels)
    }

    @Test fun `informacion adicional omite correo y telefono del consumidor final`() {
        val withEmail = factura().copy(
            clientPhone = "0999", notes = "Mesa 4", ec = SalePrintEc(clientEmail = "ana@x.ec"),
        )
        assertEquals(
            listOf("Correo" to "ana@x.ec", "Teléfono" to "0999", "Observaciones" to "Mesa 4"),
            EcRideModel.additionalInfo(withEmail),
        )
        val cons = withEmail.copy(clientDocType = "07", clientDocNumber = "9999999999999")
        assertEquals(listOf("Observaciones" to "Mesa 4"), EcRideModel.additionalInfo(cons))
    }

    @Test fun `bloque de comprobante que se modifica solo en NC y ND con afectado`() {
        assertFalse(EcRideModel.hasAffectedDoc(factura("01").copy(affectedDocNumber = "001-001-000000005")))
        assertTrue(EcRideModel.hasAffectedDoc(factura("04").copy(affectedDocNumber = "001-001-000000005")))
        assertFalse(EcRideModel.hasAffectedDoc(factura("04")))
    }

    @Test fun `codigo de barras Code128 de la clave y corte de texto largo`() {
        val key = "3010202601179001234567890011001001000000071234567"
        val (modules, runs) = EcRidePdf.code128Runs(key)!!
        assertTrue(modules > 100)
        assertTrue(runs.isNotEmpty())
        assertTrue(runs.all { (start, len) -> start >= 0 && len > 0 && start + len <= modules })

        val parts = EcRidePdf.breakLong("ABCDEFGHIJ", 3.5f) { it.length.toFloat() }
        assertEquals(listOf("ABC", "DEF", "GHI", "J"), parts)
        val wrapped = EcRidePdf.wrap("uno dos tres", 7f) { it.length.toFloat() }
        assertEquals(listOf("uno dos", "tres"), wrapped)
    }
}
