package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors

/** Tamaños oficiales del spinner. */
enum class BendeySpinnerSize(val diameter: Dp, val stroke: Dp) {
    /** Dentro de botones y campos (16 dp). */
    Small(16.dp, 2.dp),
    /** Por defecto (24 dp). */
    Medium(24.dp, 2.5.dp),
    /** Bloques de carga de pantalla (40 dp). */
    Large(40.dp, 4.dp),
}

/**
 * Spinner único de Bendey. Usarlo SOLO dentro de botones o para cargas breves; para listas y
 * tarjetas, preferir [BendeySkeletonListRow] / [BendeySkeletonCard].
 * `color` por defecto = tomate; dentro de un botón primario pasar `BendeyColors.OnPrimary`.
 */
@Composable
fun BendeySpinner(
    modifier: Modifier = Modifier,
    size: BendeySpinnerSize = BendeySpinnerSize.Medium,
    color: Color = BendeyColors.Primary,
) {
    CircularProgressIndicator(
        modifier = modifier
            .size(size.diameter)
            .semantics { contentDescription = "Cargando" },
        color = color,
        strokeWidth = size.stroke,
    )
}
