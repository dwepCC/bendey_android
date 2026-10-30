package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.network.dto.PrintDataDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** `print_data.ec` y los campos nuevos del RIDE llegan al dominio; sin ellos (Perú) todo queda en null. */
class PrintDataEcMapperTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val ecJson = """
        {"doc_type":"FACTURA","sunat_code":"01","series":"001-001","correlative":7,"issue_date":"30/10/2026",
         "currency":"USD","access_key":"3010202601179","authorization_number":"3010202601179",
         "fiscal_environment":"pruebas","note_reason":"","notes":"Mesa 4",
         "affected_doc_number":"001-001-000000005","affected_issue_date":"01/10/2026",
         "client":{"doc_type":"05","doc_number":"1710034065","business_name":"Ana","address":"Quito","phone":"0999"},
         "company":{"ruc":"1790012345001","business_name":"ACME SA","trade_name":"Acme","additional_notes":"Gracias"},
         "branch":{"name":"Matriz","address":"Av. Sol"},
         "items":[{"code":"P1","description":"Pizza","quantity":2,"unit_price":5.75,"discount":0.5,"subtotal":11.0,"total":12.65}],
         "ec":{"authorized_at":"30/10/2026 10:15:00","matriz_address":"Av. Matriz","accounting_required":true,
               "special_contributor":"123","withholding_agent":"","rimpe_legend":"CONTRIBUYENTE RÉGIMEN RIMPE",
               "client_email":"ana@x.ec","payments":[{"code":"01","label":"SIN UTILIZACION DEL SISTEMA FINANCIERO","amount":12.65}]}}
    """.trimIndent()

    @Test
    fun `mapea ec y los campos del RIDE`() {
        val d = json.decodeFromString<PrintDataDto>(ecJson).toDomain()
        assertEquals("001-001-000000007", d.number)
        assertEquals("3010202601179", d.authorizationNumber)
        assertEquals("30/10/2026 10:15:00", d.ec?.authorizedAt)
        assertEquals("Av. Matriz", d.ec?.matrizAddress)
        assertEquals(true, d.ec?.accountingRequired)
        assertEquals("123", d.ec?.specialContributor)
        assertNull(d.ec?.withholdingAgent) // vacío = no es agente
        assertEquals("ana@x.ec", d.ec?.clientEmail)
        assertEquals("01", d.ec?.payments?.single()?.code)
        assertEquals(12.65, d.ec?.payments?.single()?.amount ?: 0.0, 0.0)
        assertEquals("001-001-000000005", d.affectedDocNumber)
        assertEquals("01/10/2026", d.affectedIssueDate)
        assertNull(d.noteReason) // vacío → null
        assertEquals("Mesa 4", d.notes)
        assertEquals("0999", d.clientPhone)
        assertEquals("Quito", d.clientAddress)
        assertEquals("05", d.clientDocType)
        assertEquals("Gracias", d.companyAdditionalNotes)
        assertEquals("Av. Sol", d.branchAddress)
        val line = d.items.single()
        assertEquals("P1", line.code)
        assertEquals(11.0, line.subtotal, 0.0)
        assertEquals(0.5, line.discount, 0.0)
    }

    @Test
    fun `un print_data de Peru sin los campos nuevos sigue mapeando`() {
        val d = json.decodeFromString<PrintDataDto>(
            """{"doc_type":"BOLETA","sunat_code":"03","series":"B001","correlative":1,"issue_date":"2026-08-19","total":10.0}""",
        ).toDomain()
        assertNull(d.ec)
        assertNull(d.authorizationNumber)
        assertNull(d.affectedDocNumber)
        assertNull(d.notes)
        assertNull(d.clientPhone)
        assertFalse(d.items.any())
    }
}
