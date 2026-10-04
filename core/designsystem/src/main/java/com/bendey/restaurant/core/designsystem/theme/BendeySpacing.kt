package com.bendey.restaurant.core.designsystem.theme

import androidx.compose.ui.unit.dp

/**
 * Escala de espaciado Bendey — nombrada POR VALOR (DESIGN-SYSTEM §5): s4 · s8 · s12 · s16 · s24 · s32 · s48.
 * Los nombres por talla (`xxs`…`xl`) siguen funcionando pero están deprecados.
 */
object BendeySpacing {
    val s4 = 4.dp
    val s8 = 8.dp
    val s12 = 12.dp
    val s16 = 16.dp
    val s24 = 24.dp
    val s32 = 32.dp
    val s48 = 48.dp

    @Deprecated("Usar s4", ReplaceWith("s4"))
    val xxs get() = s4
    @Deprecated("Usar s8", ReplaceWith("s8"))
    val xs get() = s8
    @Deprecated("Usar s12", ReplaceWith("s12"))
    val sm get() = s12
    @Deprecated("Usar s16", ReplaceWith("s16"))
    val md get() = s16
    @Deprecated("Usar s24", ReplaceWith("s24"))
    val lg get() = s24
    @Deprecated("Usar s32", ReplaceWith("s32"))
    val xl get() = s32

    /** Alias semánticos (documentación / APIs externas). */
    val ExtraExtraSmall get() = s4
    val ExtraSmall get() = s8
    val Small get() = s12
    val Medium get() = s16
    val Large get() = s24
    val ExtraLarge get() = s32

    val screenHorizontal = s16
    val screenVertical = s12
    val cardPadding = s16
    val sectionGap = s12
    val formFieldGap = s12

    // Tamaños táctiles (DESIGN-SYSTEM §6). El piso no se reduce en ningún breakpoint.
    /** Cualquier control interactivo (botón, chip, fila, icono). */
    val touchMin = 44.dp
    /** Acción principal (Cobrar, Enviar a cocina, Confirmar). */
    val touchPrimary = 48.dp
    /** Cocina, teclado de PIN, uso con guantes / manos mojadas. */
    val touchKds = 56.dp
    /** Separación mínima entre controles. */
    val touchGap = 8.dp

    val buttonHeight = touchMin
    val touchTarget = touchPrimary
}
