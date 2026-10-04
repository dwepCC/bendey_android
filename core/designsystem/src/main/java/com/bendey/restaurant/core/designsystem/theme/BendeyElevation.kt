package com.bendey.restaurant.core.designsystem.theme

import androidx.compose.ui.unit.dp

/** Elevaciones estándar Bendey — conjunto 0 / 1 / 4 / 6 dp (DESIGN-SYSTEM §5). Superficies planas por defecto. */
object BendeyElevation {
    val none = 0.dp
    val pressed = 1.dp
    /** Banners y elementos flotantes (antes 4 dp suelto en `BendeyOverlayBanner`). */
    val floating = 4.dp
    val dialogShadow = 6.dp
}
