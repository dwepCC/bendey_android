package com.bendey.restaurant.feature.subscription

import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * EL TEXTO QUE ACOMPAÑA AL QR.
 *
 * Quien carga estos datos en el Panel Central escribe *asteriscos* para resaltar, como en WhatsApp.
 * Lo que se prueba aquí es sobre todo que nada se pierda por el camino: el número del titular es el
 * dato con el que el cliente verifica a quién le está pagando.
 */
class NegritasTest {

    /** El texto llega completo, con o sin marcas. */
    @Test
    fun `conserva todo el texto`() {
        assertEquals("Yape a 987654321", conNegritas("Yape a 987654321").text)
        assertEquals("Yape a 987654321", conNegritas("Yape a *987654321*").text)
        assertEquals("Juan Perez · 987654321", conNegritas("*Juan Perez* · *987654321*").text)
    }

    /**
     * La primera versión partía el texto con `split(Regex)` copiando el código de la web. En Kotlin
     * eso descarta los grupos capturados, así que el número entre asteriscos —justo el dato que se
     * quería resaltar— desaparecía de la pantalla.
     */
    @Test
    fun `no se come lo que estaba entre asteriscos`() {
        val resultado = conNegritas("Titular: *Maria Lopez*")
        assertTrue(
            "el nombre resaltado se perdio: ${resultado.text}",
            resultado.text.contains("Maria Lopez"),
        )
    }

    /** Lo marcado sale en negrita, y solo eso. */
    @Test
    fun `resalta unicamente lo que estaba marcado`() {
        val resultado = conNegritas("Yape a *987654321* (Bendey)")
        val negritas = resultado.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertEquals(1, negritas.size)
        val marcado = resultado.text.substring(negritas[0].start, negritas[0].end)
        assertEquals("987654321", marcado)
    }

    /** Varias marcas en la misma línea. */
    @Test
    fun `admite varios resaltados`() {
        val resultado = conNegritas("*Juan Perez*\n*987654321*")
        val negritas = resultado.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertEquals(2, negritas.size)
        assertEquals("Juan Perez\n987654321", resultado.text)
    }

    /**
     * UN ASTERISCO SUELTO NO ES UNA MARCA.
     *
     * El campo es de texto libre: si alguien escribe un asterisco sin cerrar, el texto tiene que
     * salir tal cual y no tragarse el resto del renglón.
     */
    @Test
    fun `el asterisco sin cerrar se muestra tal cual`() {
        assertEquals("Yape a *987654321", conNegritas("Yape a *987654321").text)
        assertEquals("2 * 3", conNegritas("2 * 3").text)
    }

    /** Sin datos cargados no hay nada que dibujar. */
    @Test
    fun `el texto vacio no rompe`() {
        assertEquals("", conNegritas("").text)
    }
}
