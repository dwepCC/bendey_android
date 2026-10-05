package com.bendey.restaurant.core.designsystem.guard

import com.bendey.restaurant.core.designsystem.guard.SourceScan.Source
import com.bendey.restaurant.core.designsystem.theme.BendeyTypography
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards de R11 (polish): accesibilidad, tamaño táctil, texto, formas, sombras, fechas y avisos.
 * Fallan si reaparece la deriva que R11 limpió. Cada excepción está en una lista con su MOTIVO;
 * añadir una excepción exige escribir por qué (DESIGN-SYSTEM §12).
 *
 * Qué NO se vigila (a propósito): iconos decorativos junto a texto (`Icon(..., contentDescription =
 * null)` dentro de un botón/fila con etiqueta de texto) — son correctos en null. Solo se vigila
 * lo que es acción por sí mismo.
 */
class UiDriftGuardTest {

    private val sources: List<Source> by lazy { SourceScan.productionSources() }

    private fun report(rule: String, violations: List<String>) {
        assertTrue(
            "$rule — ${violations.size} violación(es):\n" + violations.joinToString("\n") { "  $it" },
            violations.isEmpty(),
        )
    }

    private fun Source.at(index: Int) = "$relPath:${lineOf(index)}"

    @Test fun scannerFindsTheSources() {
        assertTrue("El guard no encontró fuentes (¿ruta mal?)", sources.size > 100)
    }

    // ------------------------------------------------------------------ 1. Accesibilidad

    /** Wrappers genéricos cuyo contenido llega por lambda: la etiqueta la pone quien los llama. */
    private val iconButtonWrapperFiles = setOf("BendeyIconButton.kt", "BendeyCompactIconButton.kt")

    private val iconActionCalls = setOf(
        "IconButton", "FilledIconButton", "FilledTonalIconButton", "OutlinedIconButton", "IconToggleButton",
        "BendeyIconButton", "BendeyCompactIconButton",
    )

    @Test fun everyIconButtonHasAContentDescription() {
        val v = mutableListOf<String>()
        for (s in sources) {
            if (s.fileName in iconButtonWrapperFiles) continue
            for (c in SourceScan.calls(s.code, iconActionCalls)) {
                val hasNull = Regex("""contentDescription\s*=\s*null""").containsMatchIn(c.text)
                val hasAny = "contentDescription" in c.text
                if (hasNull || !hasAny) v += "${s.at(c.start)} ${c.name} sin contentDescription"
            }
        }
        report("Todo botón de icono necesita contentDescription en español", v)
    }

    @Test fun clickableIconsAndImagesHaveADescription() {
        val v = mutableListOf<String>()
        for (s in sources) {
            for (c in SourceScan.calls(s.code, setOf("Icon", "Image", "AsyncImage"))) {
                val nullDesc = Regex("""contentDescription\s*=\s*null""").containsMatchIn(c.text)
                val clickable = Regex("""\.(clickable|combinedClickable|toggleable|selectable)\b""").containsMatchIn(c.text)
                if (nullDesc && clickable) v += "${s.at(c.start)} ${c.name} clicable con contentDescription = null"
            }
        }
        report("Un icono/imagen clicable no puede ser decorativo", v)
    }

    /**
     * `Box`/`Row`/`Surface` clicables que no tienen texto propio y cuyo único hijo es un icono con
     * contentDescription = null: se detectan arriba solo si el icono lleva el clickable. Las filas
     * con `Text(` heredan su etiqueta del texto (semántica fusionada de Compose) y son válidas.
     */

    // ------------------------------------------------------------------ 2. Tamaños táctiles

    private val touchMinDp = 44.0

    /**
     * Excepciones de tamaño táctil: `archivo:motivo`. Vacía a propósito — R11 corrigió los sitios
     * que había. Si hace falta una, escribe aquí por qué un control interactivo puede ser < 44 dp.
     */
    private val touchExceptions = emptyMap<String, String>()

    private val surfaceCalls = setOf("Surface", "Card", "ElevatedCard", "OutlinedCard")
    private val controlCalls = setOf(
        "Button", "TextButton", "OutlinedButton", "FilledTonalButton", "ElevatedButton", "Checkbox",
        "RadioButton", "Switch", "FilterChip", "AssistChip", "SuggestionChip", "InputChip",
        "BendeyPrimaryButton", "BendeySecondaryButton", "BendeyTextButton", "BendeyDestructiveButton",
    ) + surfaceCalls

    @Test fun interactiveControlsAreAtLeast44dp() {
        val sizeRegex = Regex("""\.(size|height|width|requiredSize|requiredHeight|requiredWidth|heightIn|widthIn|sizeIn)\(\s*(?:min(?:Height|Width)?\s*=\s*)?(\d+(?:\.\d+)?)\.dp""")
        val sizeBothRegex = Regex("""\.size\(\s*(?:width\s*=\s*)?(\d+(?:\.\d+)?)\.dp\s*,\s*(?:height\s*=\s*)?(\d+(?:\.\d+)?)\.dp""")
        val v = mutableListOf<String>()
        for (s in sources) {
            if (s.fileName in touchExceptions) continue
            val interactive = Regex("""\.(clickable|combinedClickable|toggleable|selectable)\b""")
            for (m in interactive.findAll(s.code)) {
                val chain = SourceScan.modifierChain(s.code, m.range.first)
                val small = sizeRegex.findAll(chain).any { it.groupValues[2].toDouble() < touchMinDp } ||
                    sizeBothRegex.findAll(chain).any { it.groupValues[1].toDouble() < touchMinDp || it.groupValues[2].toDouble() < touchMinDp }
                if (small) v += "${s.at(m.range.first)} clicable con size/height/width < 44 dp"
            }
            for (c in SourceScan.calls(s.code, iconActionCalls + controlCalls)) {
                val head = c.text.substringBefore("{")
                if (c.name in surfaceCalls && !Regex("""onClick\s*=""").containsMatchIn(head)) continue
                if (sizeRegex.findAll(head).any { it.groupValues[2].toDouble() < touchMinDp }) {
                    v += "${s.at(c.start)} ${c.name} con size < 44 dp"
                }
            }
        }
        report("Control interactivo con área táctil < 44 dp (DS §6). Envuélvelo en una caja de 44 dp y deja el aspecto visual dentro", v)
    }

    @Test fun minimumInteractiveSizeIsNeverDisabled() {
        val v = mutableListOf<String>()
        for (s in sources) {
            Regex("""LocalMinimumInteractiveComponentSize\s+provides""").findAll(s.code).forEach {
                v += "${s.at(it.range.first)} desactiva LocalMinimumInteractiveComponentSize"
            }
        }
        report("No se desactiva el tamaño mínimo interactivo", v)
    }

    // ------------------------------------------------------------------ 3. Texto

    @Test fun noOperationalTextBelow12sp() {
        val v = mutableListOf<String>()
        val re = Regex("""fontSize\s*=\s*(\d+(?:\.\d+)?)\.sp""")
        for (s in sources) {
            for (m in re.findAll(s.code)) {
                if (m.groupValues[1].toDouble() < 12.0) v += "${s.at(m.range.first)} fontSize ${m.groupValues[1]}.sp"
            }
        }
        report("Piso de texto 12 sp (DS §4)", v)
    }

    @Test fun themeTypographyIsAtLeast12sp() {
        val styles = listOf(
            BendeyTypography.displayLarge, BendeyTypography.displayMedium, BendeyTypography.displaySmall,
            BendeyTypography.headlineLarge, BendeyTypography.headlineMedium, BendeyTypography.headlineSmall,
            BendeyTypography.titleLarge, BendeyTypography.titleMedium, BendeyTypography.titleSmall,
            BendeyTypography.bodyLarge, BendeyTypography.bodyMedium, BendeyTypography.bodySmall,
            BendeyTypography.labelLarge, BendeyTypography.labelMedium, BendeyTypography.labelSmall,
        )
        val low = styles.filter { it.fontSize.value < 12f }
        assertTrue("Estilos del tema por debajo de 12 sp: $low", low.isEmpty())
    }

    // ------------------------------------------------------------------ 4. Formas y sombras

    /**
     * `RoundedCornerShape(` solo vive en BendeyShape.kt (los tokens). Excepción documentada:
     * MenuDigitalTab.kt dibuja la VISTA PREVIA de la carta pública del cliente, con radios que
     * elige el tenant (`cardRadius`, 10/24 dp…); no son formas de la UI de Bendey.
     */
    private val shapeExceptions = mapOf(
        "BendeyShape.kt" to "define los tokens",
        "MenuDigitalTab.kt" to "vista previa de la carta pública con radios del tenant",
    )

    @Test fun shapesComeFromTheDesignSystemTokens() {
        val v = mutableListOf<String>()
        for (s in sources) {
            if (s.fileName in shapeExceptions) continue
            Regex("""RoundedCornerShape\(""").findAll(s.code).forEach {
                v += "${s.at(it.range.first)} RoundedCornerShape suelto: usa BendeyShapeTokens (xs/md/lg/pill)"
            }
        }
        report("Formas del DS", v)
    }

    private val elevationSet = setOf(0.0, 1.0, 4.0, 6.0)

    @Test fun elevationsAreFromTheDesignSystemSet() {
        val v = mutableListOf<String>()
        val re = Regex("""(?:shadowElevation|tonalElevation|defaultElevation|pressedElevation|focusedElevation|hoveredElevation|draggedElevation|disabledElevation)\s*=\s*(\d+(?:\.\d+)?)\.dp""")
        val shadow = Regex("""\.shadow\(\s*(?:elevation\s*=\s*)?(\d+(?:\.\d+)?)\.dp""")
        for (s in sources) {
            for (m in re.findAll(s.code) + shadow.findAll(s.code)) {
                if (m.groupValues[1].toDouble() !in elevationSet) {
                    v += "${s.at(m.range.first)} elevación ${m.groupValues[1]}.dp fuera del conjunto 0/1/4/6 (BendeyElevation)"
                }
            }
        }
        report("Sombras del DS (BendeyElevation 0/1/4/6)", v)
    }

    // ------------------------------------------------------------------ 5. Fechas es-PE

    /** Archivos que pueden usar formateadores/zonas directamente, con motivo. */
    private val dateExceptions = mapOf(
        "PeruDateTime.kt" to "es EL helper",
        "KdsLogic.kt" to "interpreta marcas del servidor SIN zona para medir antigüedad; no es formato visible",
    )

    @Test fun visibleDatesGoThroughPeruDateTime() {
        val v = mutableListOf<String>()
        val re = Regex("""DateTimeFormatter\.ofPattern|SimpleDateFormat|DateFormat\.get|ZoneId\.systemDefault\(\)|TimeZone\.getDefault\(\)|Locale\.getDefault\(\)""")
        for (s in sources) {
            if (s.fileName in dateExceptions) continue
            re.findAll(s.code).forEach {
                v += "${s.at(it.range.first)} ${it.value}: usa PeruDateTime (es-PE, America/Lima)"
            }
        }
        report("Fechas/horas visibles con un único helper", v)
    }

    // ------------------------------------------------------------------ 6. Avisos (snackbar)

    @Test fun snackbarsOnlyThroughBendeySnackbarHost() {
        val v = mutableListOf<String>()
        for (s in sources) {
            if (s.fileName == "BendeySnackbarHost.kt") continue
            Regex("""(?<![A-Za-z])(SnackbarHost|Snackbar)\s*\(""").findAll(s.code).forEach {
                v += "${s.at(it.range.first)} ${it.groupValues[1]} directo: usa BendeySnackbarHost (centrado y con ancho máximo)"
            }
        }
        report("Un solo host de avisos", v)
    }
}
