package com.bendey.restaurant.core.domain.products

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Evidencia de regresión: ProductFormInput debe incluir todos los campos en equals()
 * para que MutableStateFlow persista cambios de checkbox/switch.
 */
class ProductFormInputStateFlowTest {

    @Test
    fun togglingAvailableForSale_producesDistinctFormInstances() {
        val before = ProductFormInput(name = "Pizza", code = "P001", availableForSale = true)
        val after = before.copy(availableForSale = false)

        assertNotEquals(before, after, "El toggle del checkbox debe cambiar la igualdad del formulario")
    }

    @Test
    fun stateFlowPersistsWhenOnlyCheckboxFieldChanges() {
        val flow = MutableStateFlow(ProductFormInput(name = "Pizza", code = "P001", availableForSale = true))
        val before = flow.value

        flow.update { current -> current.copy(availableForSale = false) }

        assertNotEquals(before, flow.value, "StateFlow debe aceptar el nuevo valor del formulario")
        assertFalse(flow.value.availableForSale, "El checkbox debe quedar desactivado en el estado")
        assertTrue(before.availableForSale, "El estado anterior debe haber estado activo")
    }
}
