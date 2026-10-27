package com.bendey.restaurant.core.domain.locale

import com.bendey.restaurant.core.domain.billing.docTypeShortLabel
import com.bendey.restaurant.core.domain.contacts.ContactDocType
import com.bendey.restaurant.core.domain.sales.VentasTab
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Catálogo de etiquetas por país (I5, BENDEY-PAQUETE-DE-PAIS-DISENO.md §6.3). Perú debe salir
 * EXACTAMENTE igual que el bloque `locale.labels` del backend (`pkg/country/labels.go`).
 */
class LabelKeyTest {

    // Copia literal del bloque `locale.labels` de Perú que manda el backend.
    private val backendPE = mapOf(
        "tax_id" to "RUC", "national_id" to "DNI", "foreign_id" to "Carnet de extranjería",
        "passport" to "Pasaporte", "business_name" to "Razón social", "final_consumer" to "Clientes Varios",
        "tax_name" to "IGV", "tax_authority" to "SUNAT", "tax.taxed" to "Gravado", "tax.exempt" to "Exonerado",
        "tax.unaffected" to "Inafecto", "service_charge" to "Recargo al consumo", "doc.invoice" to "Factura",
        "doc.receipt" to "Boleta", "doc.sales_note" to "Nota de venta", "doc.credit_note" to "Nota de crédito",
        "doc.debit_note" to "Nota de débito", "doc.despatch" to "Guía de remisión", "geo.region" to "Departamento",
        "geo.province" to "Provincia", "geo.district" to "Distrito", "geo.code" to "Ubigeo",
    )

    private val ec = TenantLocale(
        country = "EC", currency = "USD", currencySymbol = "$", symbolSpacing = false,
        taxName = "IVA", taxAuthority = "SRI",
        labels = mapOf(
            "tax_id" to "RUC", "national_id" to "Cédula", "final_consumer" to "Consumidor final",
            "tax_name" to "IVA", "tax_authority" to "SRI", "doc.receipt" to "Factura",
            "geo.region" to "Provincia", "geo.province" to "Cantón", "geo.district" to "Parroquia",
            "doc.invoice" to "  ", // en blanco: cuenta como ausente
        ),
    )

    @After fun restore() = TenantLocaleHolder.reset()

    @Test fun catalogHasExactlyTheBackendKeysAndPeruTexts() {
        assertEquals(22, LabelKey.entries.size)
        assertEquals(backendPE, LabelKey.LABELS_PE)
        assertEquals(backendPE, TenantLocale.PE_LABELS)
        for ((k, v) in backendPE) assertEquals(k, v, TenantLocale.PE.label(k))
    }

    @Test fun peruByDefaultWithoutSession() {
        for (key in LabelKey.entries) assertEquals(key.pe, key.text)
        assertEquals("BOLETA", LabelKey.DOC_RECEIPT.upper)
        assertEquals("CLIENTES VARIOS", LabelKey.FINAL_CONSUMER.upper)
        assertEquals("Recargo al Consumo", LabelKey.SERVICE_CHARGE.peruAs("Recargo al Consumo"))
    }

    @Test fun peruIgnoresDifferentServerLabels() {
        val raro = TenantLocale.PE.copy(labels = mapOf("tax_name" to "IVA", "doc.receipt" to "Otra cosa"))
        assertEquals("IGV", raro.label(LabelKey.TAX_NAME))
        assertEquals("Boleta", raro.label("doc.receipt"))
        TenantLocaleHolder.set(raro)
        assertEquals("IGV", LabelKey.TAX_NAME.text)
        assertEquals("Recargo al Consumo", LabelKey.SERVICE_CHARGE.peruAs("Recargo al Consumo"))
        // Sin país también es Perú.
        assertEquals("DNI", raro.copy(country = "").label(LabelKey.NATIONAL_ID))
    }

    @Test fun otherCountryUsesServerAndFallsBackToPeru() {
        assertEquals("Cédula", ec.label(LabelKey.NATIONAL_ID))
        assertEquals("IVA", ec.label(LabelKey.TAX_NAME))
        assertEquals("Parroquia", ec.label("geo.district"))
        // Claves que el servidor no mandó (o mandó en blanco): texto peruano.
        assertEquals("Ubigeo", ec.label(LabelKey.GEO_CODE))
        assertEquals("Factura", ec.label(LabelKey.DOC_INVOICE))
        assertEquals("Recargo al consumo", ec.label(LabelKey.SERVICE_CHARGE))
        // Clave fuera del catálogo: el respaldo que dé quien llama.
        assertEquals("x", ec.label("no.existe", fallback = "x"))

        TenantLocaleHolder.set(ec)
        assertEquals("SRI", LabelKey.TAX_AUTHORITY.text)
        assertEquals("CONSUMIDOR FINAL", LabelKey.FINAL_CONSUMER.upper)
        assertEquals("Recargo al consumo", LabelKey.SERVICE_CHARGE.peruAs("Recargo al Consumo"))
    }

    @Test fun peruVariantsBuiltAtCallSitesStayIdentical() {
        assertEquals(listOf("Sin RUC", "DNI", "RUC", "Carnet extranjería", "Pasaporte"), ContactDocType.entries.map { it.label })
        assertEquals("Boleta", docTypeShortLabel("BOLETA", "03"))
        assertEquals("Nota de venta", docTypeShortLabel("NOTA DE VENTA", "00"))
        assertEquals(
            listOf("Todas las ventas", "Solo notas de venta", "Solo boletas", "Solo facturas", "Facturas y boletas", "Notas de crédito"),
            VentasTab.entries.map { it.label },
        )

        TenantLocaleHolder.set(ec)
        assertEquals("Cédula", ContactDocType.DNI.label)
        assertEquals("IVA", ec.label(LabelKey.TAX_NAME))
    }
}
