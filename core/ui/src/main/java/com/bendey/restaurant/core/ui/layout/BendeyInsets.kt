package com.bendey.restaurant.core.ui.layout

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Helpers de insets siguiendo
 * [WindowInsets in Compose](https://developer.android.com/develop/ui/compose/layouts/insets).
 *
 * Preferir modifiers oficiales (`statusBarsPadding`, `navigationBarsPadding`, `safeDrawingPadding`)
 * antes de composiciones custom.
 */

fun Modifier.bendeySafeDrawingPadding(): Modifier = safeDrawingPadding()

fun Modifier.bendeyStatusBarsPadding(): Modifier = statusBarsPadding()

fun Modifier.bendeyNavigationBarsPadding(): Modifier = navigationBarsPadding()

fun Modifier.bendeyDisplayCutoutPadding(): Modifier = displayCutoutPadding()

fun Modifier.bendeyImePadding(): Modifier = imePadding()

/** Status bar + display cutout superior (edge-to-edge). */
fun Modifier.bendeyTopSystemInsetsPadding(): Modifier = this
    .statusBarsPadding()
    .displayCutoutPadding()

/**
 * Insets horizontales de [WindowInsets.safeDrawing] (API oficial, requiere composable).
 * Sustituye intentos con `safeDrawing.only()` fuera de [WindowInsets.Companion].
 */
@Composable
fun Modifier.bendeyHorizontalSafeDrawingPadding(): Modifier {
    val horizontalInsets = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Horizontal,
    )
    return windowInsetsPadding(horizontalInsets)
}

/** Alias interno — mantener compatibilidad con call sites existentes. */
@Composable
fun Modifier.bendeyHorizontalSafeInsetsPadding(): Modifier = bendeyHorizontalSafeDrawingPadding()

/**
 * Padding inferior para [androidx.compose.foundation.lazy.LazyColumn] /
 * [androidx.compose.foundation.lazy.grid.LazyVerticalGrid] bajo [BendeyBottomNavigationBar].
 *
 * El shell ya no aplica este padding al contenedor raíz (evita franja blanca fija).
 */
@Composable
fun rememberBendeyBottomBarScrollPadding(includeBottomBar: Boolean = true): Dp {
    val navigationBarPadding = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    return if (includeBottomBar) {
        BendeyBottomBarInset + navigationBarPadding
    } else {
        navigationBarPadding
    }
}

fun Modifier.bendeyFullscreenSafePadding(): Modifier = safeDrawingPadding()
