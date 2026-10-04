package com.bendey.restaurant.core.domain.copy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmptyStatesCopyTest {
    @Test
    fun `ningun texto contiene jerga ni usted`() {
        val all = EmptyStatesCopy.all.flatMap {
            listOfNotNull(it.title, it.description, it.action, it.noPermissionDescription)
        } + listOf(LoadErrorCopy.TITLE, LoadErrorCopy.DESCRIPTION, LoadErrorCopy.RETRY)
        all.forEach { assertTrue(ForbiddenTerms.find(it).isEmpty(), "Texto prohibido en: $it -> ${ForbiddenTerms.find(it)}") }
    }

    @Test
    fun `todo vacio tiene titulo y descripcion`() {
        EmptyStatesCopy.all.forEach {
            assertTrue(it.title.isNotBlank() && it.description.isNotBlank())
        }
    }

    @Test
    fun `descripcion segun permiso`() {
        assertEquals("Pídele al administrador que cree las mesas.", EmptyStatesCopy.salas.descriptionFor(false))
        assertEquals(EmptyStatesCopy.salas.description, EmptyStatesCopy.salas.descriptionFor(true))
        assertEquals(EmptyStatesCopy.clientes.description, EmptyStatesCopy.clientes.descriptionFor(false))
    }

    @Test
    fun `el detector atrapa jerga`() {
        assertTrue(ForbiddenTerms.find("Aperturar caja").isNotEmpty())
        assertTrue(ForbiddenTerms.find("Agregue un cliente").isNotEmpty())
        assertTrue(ForbiddenTerms.find("Abre tu caja").isEmpty())
    }

    @Test
    fun `decisor de estado`() {
        assertEquals(ListViewState.Content, ListStateDecider.decide(false, null, 3))
        assertEquals(ListViewState.Content, ListStateDecider.decide(false, "falló", 3))
        assertEquals(ListViewState.Loading, ListStateDecider.decide(true, null, 0))
        assertEquals(ListViewState.Error("falló"), ListStateDecider.decide(false, "falló", 0))
        assertEquals(ListViewState.Error("falló"), ListStateDecider.decide(false, "falló", 0, hasActiveFilters = true))
        assertEquals(ListViewState.EmptyFiltered, ListStateDecider.decide(false, null, 0, hasActiveFilters = true))
        assertEquals(ListViewState.EmptyCreated, ListStateDecider.decide(false, "  ", 0))
    }

    /** R2a: un solo nombre de salón. "Salas", "piso" y "ambiente" ya no se muestran. */
    @Test
    fun `ningun texto usa terminos de salon retirados`() {
        val retired = Regex("\b(salas?|configurar mesas|gesti[oó]n de mesas|pisos?|ambientes?)\b", RegexOption.IGNORE_CASE)
        EmptyStatesCopy.all.flatMap {
            listOfNotNull(it.title, it.description, it.action, it.noPermissionDescription)
        }.forEach { assertTrue(!retired.containsMatchIn(it), "término retirado en: $it") }
    }
}
