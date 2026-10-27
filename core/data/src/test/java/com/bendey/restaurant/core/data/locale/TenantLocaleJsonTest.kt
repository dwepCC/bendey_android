package com.bendey.restaurant.core.data.locale

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
           "labels":{"final_consumer":"Clientes Varios","national_id":"DNI","service_charge":"Recargo al consumo","tax_id":"RUC"}}}
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
}
