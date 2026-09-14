package com.bendey.restaurant.core.ui.components

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Colores de estado `disabled` compartidos por los 6 wrappers de botón de Bendey.
 *
 * Antes de esto cada botón definía su propia fórmula: `BendeyPrimaryButton` atenuaba solo el
 * contenedor (alpha 0.38), `BendeyFilledCartButton` atenuaba contenedor a 0.38 y contenido a
 * 0.55 (un coeficiente distinto para el mismo concepto), `BendeyOutlinedButton`/`BendeyIconButton`
 * sustituían el color por un gris sólido en vez de atenuar, y `BendeySecondaryButton` no cambiaba
 * el fondo/borde en absoluto al deshabilitarse — un botón "apagado" seguía viéndose activo.
 *
 * Un solo alpha (0.38, el mismo que usa Material3 para su propio `disabledContentColor` por
 * defecto) para contenedor, contenido y borde en los seis — "deshabilitado" ahora se ve igual
 * sin importar qué variante de botón sea.
 */
object BendeyButtonDefaults {
    const val DisabledContainerAlpha = 0.38f
    const val DisabledContentAlpha = 0.38f
    const val DisabledBorderAlpha = 0.38f

    /** Para botones con contenedor sólido (Primary, FilledCartButton y sus variantes). */
    @Composable
    fun filledColors(
        containerColor: Color,
        contentColor: Color = contentColorFor(containerColor),
    ): ButtonColors = ButtonDefaults.buttonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = containerColor.copy(alpha = DisabledContainerAlpha),
        disabledContentColor = contentColor.copy(alpha = DisabledContentAlpha),
    )

    /** Para botones tonal-filled (FilledTonalButton) — mismo criterio de alpha sobre los
     * colores tonales por defecto de Material3. */
    @Composable
    fun filledTonalColors(): ButtonColors {
        val base = ButtonDefaults.filledTonalButtonColors()
        return ButtonDefaults.filledTonalButtonColors(
            disabledContainerColor = base.containerColor.copy(alpha = DisabledContainerAlpha),
            disabledContentColor = base.contentColor.copy(alpha = DisabledContentAlpha),
        )
    }

    /** Para botones de solo borde/texto (Outlined, Icon, Text). */
    @Composable
    fun outlinedColors(
        contentColor: Color,
    ): ButtonColors = ButtonDefaults.outlinedButtonColors(
        contentColor = contentColor,
        disabledContentColor = contentColor.copy(alpha = DisabledContentAlpha),
    )
}
