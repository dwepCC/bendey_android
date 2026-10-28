package com.bendey.restaurant.core.data.locale

import com.bendey.restaurant.core.domain.locale.LabelKey
import com.bendey.restaurant.core.domain.locale.TenantLocale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TenantLocaleJsonTest {

    // Golden de PE del backend (pkg/country/view_test.go), dentro de la respuesta completa.
    private val capabilitiesPE = """
        {"schema_version":149,"features":{"delivery":true},
         "locale":{"country":"PE","currency":"PEN","currency_symbol":"S/","symbol_spacing":true,"decimals":2,
           "number_locale":"es-PE","time_zone":"America/Lima","date_format":"dd/MM/yyyy","phone_prefix":"+51",
           "tax_name":"IGV","tax_authority":"SUNAT",
           "identity_types":[{"key":"national_id","code":"1","label":"DNI","min":8,"max":8,"numeric":true}],
           "doc_kinds":[{"kind":"receipt","code":"03","label":"Boleta de venta","requires_buyer_id":false,"final_consumer_max":0}],
           "geo_levels":[{"key":"region","label":"Departamento"}],
           "labels":{"tax_id":"RUC","national_id":"DNI","foreign_id":"Carnet de extranjería","passport":"Pasaporte","business_name":"Razón social","final_consumer":"Clientes Varios","tax_name":"IGV","tax_authority":"SUNAT","tax.taxed":"Gravado","tax.exempt":"Exonerado","tax.unaffected":"Inafecto","service_charge":"Recargo al consumo","doc.invoice":"Factura","doc.receipt":"Boleta","doc.sales_note":"Nota de venta","doc.credit_note":"Nota de crédito","doc.debit_note":"Nota de débito","doc.despatch":"Guía de remisión","geo.region":"Departamento","geo.province":"Provincia","geo.district":"Distrito","geo.code":"Ubigeo"}}}
    """.trimIndent()

    private val capabilitiesEC = """
        {"schema_version":150,"features":{},
         "locale":{"country":"EC","currency":"USD","currency_symbol":"$","symbol_spacing":false,"decimals":2,
           "number_locale":"es-EC","time_zone":"America/Guayaquil","date_format":"dd/MM/yyyy","phone_prefix":"+593",
           "tax_name":"IVA","tax_authority":"SRI","labels":{"tax_id":"RUC","final_consumer":"Consumidor final"}}}
    """.trimIndent()

    @Test fun peruGoldenIsExactlyThePeruDefault() {
        assertEquals(TenantLocale.PE, TenantLocaleJson.fromCapabilities(capabilitiesPE))
    }

    @Test fun ecuadorIsRead() {
        val ec = TenantLocaleJson.fromCapabilities(capabilitiesEC)!!
        assertEquals("EC", ec.country)
        assertEquals("USD", ec.currency)
        assertEquals("$", ec.currencySymbol)
        assertEquals(false, ec.symbolSpacing)
        assertEquals("America/Guayaquil", ec.timeZone)
        assertEquals("Consumidor final", ec.label("final_consumer"))
        // Etiqueta que no vino: texto peruano de siempre.
        assertEquals("Recargo al consumo", ec.label("service_charge"))
    }

    @Test fun oldServerWithoutLocaleGivesNull() {
        assertNull(TenantLocaleJson.fromCapabilities("""{"schema_version":140,"features":{}}"""))
        assertNull(TenantLocaleJson.localeObjectOf("""{"schema_version":140,"features":{}}"""))
    }

    @Test fun garbageNeverThrows() {
        assertNull(TenantLocaleJson.fromCapabilities(""))
        assertNull(TenantLocaleJson.fromCapabilities("<html>502</html>"))
        assertNull(TenantLocaleJson.fromCapabilities("""{"locale":"PE"}"""))
        assertNull(TenantLocaleJson.fromCapabilities("""[1,2]"""))
        assertNull(TenantLocaleJson.fromLocaleJson(null))
        assertNull(TenantLocaleJson.fromLocaleJson("{roto"))
    }

    @Test fun missingOrWrongFieldsFallBackToPeru() {
        val l = TenantLocaleJson.fromCapabilities(
            """{"locale":{"country":"EC","currency_symbol":"","decimals":"dos","symbol_spacing":"si","time_zone":"Nowhere/City"}}""",
        )!!
        assertEquals("EC", l.country)
        assertEquals("PEN", l.currency)
        assertEquals("S/", l.currencySymbol) // vacío: el de Perú
        assertEquals(2, l.decimals)
        assertEquals(true, l.symbolSpacing)
        assertEquals("America/Lima", l.timeZone)
    }

    @Test fun savedObjectRoundTrips() {
        val raw = TenantLocaleJson.localeObjectOf(capabilitiesEC)
        assertNotNull(raw)
        assertEquals(TenantLocaleJson.fromCapabilities(capabilitiesEC), TenantLocaleJson.fromLocaleJson(raw))
    }

    // --- I5: bloque `labels` -------------------------------------------------------------------

    @Test fun peruGoldenLabelsAreTheCatalog() {
        val pe = TenantLocaleJson.fromCapabilities(capabilitiesPE)!!
        assertEquals(LabelKey.LABELS_PE, pe.labels)
        for (key in LabelKey.entries) assertEquals(key.pe, pe.label(key))
    }

    @Test fun ecuadorLabelsAreReadAndMissingOnesFallBackToPeru() {
        val ec = TenantLocaleJson.fromCapabilities(
            """{"locale":{"country":"EC","labels":{"national_id":"Cédula","tax_name":"IVA","geo.district":"Parroquia"}}}""",
        )!!
        assertEquals("Cédula", ec.label(LabelKey.NATIONAL_ID))
        assertEquals("IVA", ec.label(LabelKey.TAX_NAME))
        assertEquals("Parroquia", ec.label(LabelKey.GEO_DISTRICT))
        assertEquals("Ubigeo", ec.label(LabelKey.GEO_CODE))
    }

    @Test fun labelsAbsentOrOfAnotherTypeAreIgnored() {
        val sinLabels = TenantLocaleJson.fromCapabilities("""{"locale":{"country":"EC"}}""")!!
        assertEquals(LabelKey.LABELS_PE, sinLabels.labels)

        val labelsLista = TenantLocaleJson.fromCapabilities("""{"locale":{"country":"EC","labels":["IVA"]}}""")!!
        assertEquals(LabelKey.LABELS_PE, labelsLista.labels)

        val labelsTexto = TenantLocaleJson.fromCapabilities("""{"locale":{"country":"EC","labels":"IVA"}}""")!!
        assertEquals(LabelKey.LABELS_PE, labelsTexto.labels)

        // Valores que no son texto (número, null, objeto) o vacíos se descartan uno a uno.
        val mezclado = TenantLocaleJson.fromCapabilities(
            """{"locale":{"country":"EC","labels":{"tax_name":18,"national_id":null,"tax_id":{"x":1},"geo.code":"","final_consumer":"Consumidor final"}}}""",
        )!!
        assertEquals("IGV", mezclado.label(LabelKey.TAX_NAME))
        assertEquals("DNI", mezclado.label(LabelKey.NATIONAL_ID))
        assertEquals("RUC", mezclado.label(LabelKey.TAX_ID))
        assertEquals("Ubigeo", mezclado.label(LabelKey.GEO_CODE))
        assertEquals("Consumidor final", mezclado.label(LabelKey.FINAL_CONSUMER))
    }

    @Test fun ecuadorReadsIdentityTypesFinalConsumerMaxAndTaxRates() {
        val ec = TenantLocaleJson.fromCapabilities(
            """
            {"locale":{"country":"EC","currency":"USD","currency_symbol":"$","symbol_spacing":false,
              "identity_types":[
                {"key":"tax_id","code":"04","label":"RUC","min":13,"max":13,"numeric":true},
                {"key":"national_id","code":"05","label":"Cédula","min":10,"max":10,"numeric":true},
                {"key":"other","code":"07","label":"Consumidor final","min":13,"max":13,"numeric":true},
                {"label":"sin código"}],
              "doc_kinds":[{"kind":"invoice","code":"01","label":"Factura","final_consumer_max":50}],
              "tax_rates":[{"code":"4","label":"IVA 15%","rate":15,"default":true},{"code":"0","label":"IVA 0%","rate":0},
                {"code":"x","label":"roto","rate":"quince"}]}}
            """.trimIndent(),
        )!!
        assertEquals(listOf("04", "05", "07"), ec.identityTypes.map { it.code })
        assertEquals(13, ec.identityByKey("tax_id")?.maxLength)
        assertEquals("07", ec.finalConsumerIdentity?.code)
        assertEquals(50.0, ec.finalConsumerMax!!, 0.0)
        assertEquals(listOf("4", "0"), ec.taxRates.map { it.code })
        assertEquals("4", ec.defaultTaxCode)
        assertEquals(15.0, ec.taxRateOf("4")!!, 0.0)
        assertNull(ec.taxRateOf("10"))
    }

    @Test fun peruKeepsCountryCatalogsEmpty() {
        val pe = TenantLocaleJson.fromCapabilities(capabilitiesPE)!!
        assertEquals(emptyList<Any>(), pe.identityTypes)
        assertEquals(emptyList<Any>(), pe.taxRates)
        assertNull(pe.finalConsumerMax)
        assertNull(pe.countryTaxRates)
        assertNull(pe.defaultTaxCode)
    }

    @Test fun peruIgnoresServerLabelsThatDiffer() {
        val pe = TenantLocaleJson.fromCapabilities("""{"locale":{"country":"PE","labels":{"tax_name":"IVA"}}}""")!!
        assertEquals("IGV", pe.label(LabelKey.TAX_NAME))
    }
}
