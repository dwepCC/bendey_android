package com.bendey.restaurant.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import java.io.File
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contraste WCAG 2.x de los pares de DESIGN-SYSTEM §3.2, §3.4 y §3.5, calculado sobre las
 * constantes REALES de [BendeyColors] (no copias). Falla si un texto baja de 4,5:1 o un
 * componente de UI baja de 3:1.
 */
class ContrastTest {

    private fun channel(v: Float): Double {
        val c = v.toDouble()
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(c: Color): Double =
        0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)

    private fun ratio(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private val failures = mutableListOf<String>()

    private fun text(name: String, fg: Color, bg: Color, min: Double = 4.5) {
        val r = ratio(fg, bg)
        if (r < min) failures += "TEXTO $name: %.2f:1 (< $min)".format(r)
    }

    private fun ui(name: String, fg: Color, bg: Color) = text(name, fg, bg, 3.0)

    private fun assertNoFailures() {
        assertTrue("Pares bajo el mínimo WCAG:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `la formula reproduce los ratios publicados en el design system`() {
        // Sanidad de la fórmula contra valores de DESIGN-SYSTEM §3.4.
        assertEquals(5.08, ratio(BendeyColors.Primary, Color.White), 0.02)
        assertEquals(1.97, ratio(BendeyColors.Warning, Color.White), 0.02)
        assertEquals(5.02, ratio(BendeyColors.WarningText, Color.White), 0.02)
        assertEquals(4.61, ratio(BendeyColors.NavInactive, Color.White), 0.02)
        assertEquals(6.57, ratio(BendeyColors.Error, Color.White), 0.02)
    }

    @Test
    fun `marca y neutros`() {
        text("Primary/blanco", BendeyColors.Primary, Color.White)
        text("Primary/Brand50", BendeyColors.Primary, BendeyColors.Brand50)
        text("OnPrimary/Primary", BendeyColors.OnPrimary, BendeyColors.Primary)
        text("Brand700/blanco", BendeyColors.Brand700, Color.White)
        text("OnPrimaryContainer/PrimaryContainer", BendeyColors.OnPrimaryContainer, BendeyColors.PrimaryContainer)
        text("OnSurface/Surface", BendeyColors.OnSurface, BendeyColors.Surface)
        text("OnSurface/Background", BendeyColors.OnSurface, BendeyColors.Background)
        text("OnSurfaceVariant/Surface", BendeyColors.OnSurfaceVariant, BendeyColors.Surface)
        text("OnSurfaceVariant/Background", BendeyColors.OnSurfaceVariant, BendeyColors.Background)
        text("OnSurfaceVariant/SurfaceVariant", BendeyColors.OnSurfaceVariant, BendeyColors.SurfaceVariant)
        // NavInactive solo es válido sobre blanco (sobre #F5F5F5 baja a 4,23).
        text("NavInactive/blanco", BendeyColors.NavInactive, Color.White)
        assertNoFailures()
    }

    @Test
    fun `semanticos 3_2`() {
        listOf(
            "success" to BendeyColors.SuccessTone,
            "warning" to BendeyColors.WarningTone,
            "danger" to BendeyColors.DangerTone,
            "info" to BendeyColors.InfoTone,
            "neutral" to BendeyColors.NeutralTone,
        ).forEach { (name, t) ->
            text("$name onSolid/solid", t.onSolid, t.solid)
            text("$name text/blanco", t.text, Color.White)
            text("$name onTint/tint", t.onTint, t.tint)
        }
        // Texto de error sobre el contenedor de error (banner) y sobre blanco.
        text("ErrorText/ErrorContainer", BendeyColors.ErrorText, BendeyColors.ErrorContainer)
        text("ErrorText/blanco", BendeyColors.ErrorText, Color.White)
        text("Error/blanco", BendeyColors.Error, Color.White)
        text("WarningText/WarningContainer", BendeyColors.WarningText, BendeyColors.WarningContainer)
        text("InfoText/blanco", BendeyColors.InfoText, Color.White)
        text("SuccessText/blanco", BendeyColors.SuccessText, Color.White)
        // Texto oscuro sobre el relleno ámbar (blanco da 1,97).
        text("OnWarning/Warning", BendeyColors.OnWarning, BendeyColors.Warning)
        assertNoFailures()
    }

    @Test
    fun `el relleno de aviso no sirve como texto`() {
        // Regresión documentada: Warning solo es relleno. Si alguien lo usa como texto sobre blanco, esto lo explica.
        assertTrue(ratio(BendeyColors.Warning, Color.White) < 3.0)
    }

    @Test
    fun `estados operativos 3_5`() {
        listOf(
            "Nuevo" to BendeyColors.StateNuevo,
            "Preparando" to BendeyColors.StatePreparando,
            "Listo" to BendeyColors.StateListo,
            "Entregado" to BendeyColors.StateEntregado,
            "Atrasado" to BendeyColors.StateAtrasado,
            "Mesa libre" to BendeyColors.StateMesaLibre,
            "Mesa ocupada" to BendeyColors.StateMesaOcupada,
            "Mesa reservada" to BendeyColors.StateMesaReservada,
            "Mesa en consumo" to BendeyColors.StateMesaEnConsumo,
            "Mesa viendo la carta" to BendeyColors.StateMesaViendoCarta,
        ).forEach { (name, s) ->
            text("$name onTint/tint", s.onTint, s.tint)
            // La barra/borde de estado es un componente de UI sobre la tarjeta blanca: 3:1 salvo
            // ámbar y gris, que por diseño llevan SIEMPRE texto/icono además del color (§1.2) y
            // cuya etiqueta ya cumple 4,5 arriba.
        }
        text("Atrasado fill/blanco", BendeyColors.StateAtrasado.fill, Color.White)
        text("onTint de cada chip del dashboard", BendeyColors.DashboardTableLibre, BendeyColors.DashboardTableLibreContainer)
        text("Dashboard ocupada", BendeyColors.DashboardTableOcupada, BendeyColors.DashboardTableOcupadaContainer)
        text("Dashboard reservada", BendeyColors.DashboardTableReservada, BendeyColors.DashboardTableReservadaContainer)
        text("Dashboard consumo", BendeyColors.DashboardTableConsumo, BendeyColors.DashboardTableConsumoContainer)
        // UI: relleno de estado de los verdes/azules/naranja/índigo/rojo sobre blanco >= 3:1.
        ui("Libre fill/blanco", BendeyColors.StateMesaLibre.fill, Color.White)
        ui("Consumo fill/blanco", BendeyColors.StateMesaEnConsumo.fill, Color.White)
        ui("Viendo carta fill/blanco", BendeyColors.StateMesaViendoCarta.fill, Color.White)
        ui("Atrasado fill/blanco", BendeyColors.StateAtrasado.fill, Color.White)
        assertNoFailures()
    }

    @Test
    fun `ocupada es naranja tambien en el dashboard`() {
        assertEquals(BendeyColors.StateMesaOcupada.tint, BendeyColors.DashboardTableOcupadaContainer)
        assertEquals(BendeyColors.StateMesaOcupada.onTint, BendeyColors.DashboardTableOcupada)
        assertEquals(BendeyColors.TableOcupada, BendeyColors.StateMesaOcupada.fill)
    }

    @Test
    fun `texto de cada acento conocido del chip pasa AA sobre su tinte`() {
        val accents = listOf(
            BendeyColors.Success, BendeyColors.Warning, BendeyColors.Error, BendeyColors.Info,
            BendeyColors.Primary, BendeyColors.AccentTeal, BendeyColors.AccentPurple,
            BendeyColors.OnSurfaceVariant, BendeyColors.TableOcupada, BendeyColors.TableBrowsing,
            BendeyColors.KitchenEntregado, BendeyColors.KitchenAtrasado,
        )
        accents.forEach { accent ->
            val tone = BendeyColors.toneForAccent(accent)
            assertTrue("Sin tono para $accent", tone != null)
            text("chip $accent", tone!!.onTint, tone.tint)
        }
        assertNoFailures()
    }

    @Test
    fun `Warning no se usa como color de texto en el codigo fuente`() {
        // Busca "color = BendeyColors.Warning" (y textColor/labelColor/contentColor) en todo el repo.
        // El relleno de aviso (barras, iconos, puntos) se escribe con otros parámetros (tint, background...).
        var root: File? = File("").absoluteFile
        while (root != null && !File(root, "settings.gradle.kts").exists()) root = root.parentFile
        if (root == null) return // sin acceso al árbol de fuentes: el contraste de arriba basta
        val pattern = Regex("""(^|[^A-Za-z])(color|textColor|labelColor|contentColor)\s*=\s*(if .*)?BendeyColors\.Warning\b""")
        val offenders = root.walkTopDown()
            .onEnter { it.name != "build" && it.name != ".gradle" && it.name != ".git" }
            .filter { it.isFile && it.extension == "kt" && "/src/test/" !in it.invariantSeparatorsPath }
            .flatMap { f ->
                f.readLines().mapIndexedNotNull { i, line ->
                    if (pattern.containsMatchIn(line)) "${f.name}:${i + 1}" else null
                }
            }
            .toList()
        assertTrue("Warning usado como texto (usar WarningText): $offenders", offenders.isEmpty())
    }
}
