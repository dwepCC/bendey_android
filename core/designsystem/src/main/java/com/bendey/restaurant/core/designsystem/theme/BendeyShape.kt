package com.bendey.restaurant.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object BendeyShapeTokens {
    val xs = RoundedCornerShape(8.dp)
    // Escala de radios 8 / 12 / 16 (DESIGN-SYSTEM §5). Se conservan los 5 nombres; `sm` (antes 10 dp)
    // pasa a 12 y `lg` (antes 14 dp) pasa a 16, así que sm==md y lg==xl en valor.
    val sm = RoundedCornerShape(12.dp)
    val md = RoundedCornerShape(12.dp)
    val lg = RoundedCornerShape(16.dp)
    val xl = RoundedCornerShape(16.dp)

    /** Alias semánticos — equivalen a la escala Material3 mapeada en [BendeyShapes]. */
    val ExtraSmall get() = xs
    val Small get() = sm
    val Medium get() = md
    val Large get() = lg
    val ExtraLarge get() = xl

    val sheet = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    /** Pill real (50 %): un chip alto ya no se ve ovalado. */
    val pill: androidx.compose.ui.graphics.Shape = CircleShape

    /** Alias de [pill] — mismo shape, dos nombres para el mismo shape. Preferir `pill`
     * en código nuevo; se mantiene por compatibilidad con los sitios que ya llaman `chip`. */
    @Deprecated("Usar BendeyShapeTokens.pill — mismo valor, un solo nombre.", ReplaceWith("pill"))
    val chip get() = pill
    /** Barras de progreso / mini indicadores */
    val bar = RoundedCornerShape(4.dp)
    /** Punto de estado (mesas, badges) */
    val dot = RoundedCornerShape(2.dp)
}

val BendeyShapes = Shapes(
    extraSmall = BendeyShapeTokens.xs,
    small = BendeyShapeTokens.sm,
    medium = BendeyShapeTokens.md,
    large = BendeyShapeTokens.lg,
    extraLarge = BendeyShapeTokens.xl,
)
