package com.bendey.restaurant.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

/** Marca composables dentro de la Experience Layer (Material 3 Expressive). */
val LocalBendeyExpressive = compositionLocalOf { false }

/**
 * Alcance Experience Layer sobre BendeyTheme.
 * Conserva identidad Bendey; activa motion tokens expresivos vía [BendeyMotion].
 */
@Composable
fun BendeyExpressiveScope(
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalBendeyExpressive provides true, content = content)
}
