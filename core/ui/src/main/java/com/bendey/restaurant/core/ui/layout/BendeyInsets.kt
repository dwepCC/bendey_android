package com.bendey.restaurant.core.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

/**
 * Helpers de insets siguiendo
 * [WindowInsets in Compose](https://developer.android.com/develop/ui/compose/layouts/insets).
 *
 * Preferir modifiers oficiales (`statusBarsPadding`, `navigationBarsPadding`, `safeDrawingPadding`)
 * antes de composiciones custom.
 */

fun Modifier.bendeySafeDrawingPadding(): Modifier = safeDrawingPadding()

fun Modifier.bendeyStatusBarsPadding(): Modifier = statusBarsPadding()

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

/** Altura del inset inferior del sistema (gesture / navigation bar). */
@Composable
fun rememberNavigationBarInset(): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/**
 * Franja tomate únicamente en el área real del sistema (gesture/navigation bar).
 * Usar al pie de BottomSheets y contenedores edge-to-edge.
 */
@Composable
fun BendeyNavigationBarScrim(modifier: Modifier = Modifier) {
    val inset = rememberNavigationBarInset()
    if (inset > 0.dp) {
        Spacer(
            modifier = modifier
                .fillMaxWidth()
                .height(inset)
                .background(BendeyColors.Rest900),
        )
    }
}

/**
 * Offset inferior para barras flotantes POS/Mesa: encima del Bottom Navigation
 * con un margen para que no queden pegados al menú (sin reservar overlap del FAB central).
 *
 * En detalle de mesa ([includeBottomBar] = false) devuelve 0: la barra va al fondo
 * y aplica [navigationBarsPadding] internamente.
 */
@Composable
fun rememberBendeyFloatingActionsBottomOffset(includeBottomBar: Boolean): Dp {
    val navBar = rememberNavigationBarInset()
    val gapAboveBottomBar = BendeySpacing.sm
    return if (includeBottomBar) {
        BendeyBottomBarHeight + navBar + gapAboveBottomBar
    } else {
        0.dp
    }
}

/** Espacio bajo el catálogo cuando hay barra compacta superpuesta (scroll). */
@Composable
fun rememberBendeyCatalogScrollBottomPadding(
    includeBottomBar: Boolean,
    compactBarHeight: Dp,
): Dp {
    val navBar = rememberNavigationBarInset()
    return if (includeBottomBar) {
        compactBarHeight + rememberBendeyFloatingActionsBottomOffset(includeBottomBar = true)
    } else {
        compactBarHeight + navBar
    }
}

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
        BendeyBottomBarInset + navigationBarPadding + BendeySpacing.md
    } else {
        navigationBarPadding + BendeySpacing.sm
    }
}

/**
 * [PaddingValues] estándar para listas bajo [BendeyBottomNavigationBar] superpuesta.
 */
@Composable
fun rememberBendeyLazyListContentPadding(
    includeBottomBar: Boolean = true,
    horizontal: Dp = BendeySpacing.screenHorizontal,
    top: Dp = BendeySpacing.sm,
    extraBottom: Dp = 0.dp,
): PaddingValues {
    val bottom = rememberBendeyBottomBarScrollPadding(includeBottomBar) + extraBottom
    return PaddingValues(
        start = horizontal,
        end = horizontal,
        top = top,
        bottom = bottom,
    )
}

/**
 * Offset inferior del [com.bendey.restaurant.core.ui.components.BendeySnackbarHost]
 * para que no quede oculto bajo bottom navigation ni barra compacta POS/Mesa.
 */
@Composable
fun rememberBendeySnackbarBottomPadding(
    currentRoute: String?,
    showBottomBar: Boolean,
    isCompactWidth: Boolean,
): Dp {
    val navigationBarPadding = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    val bottomBarPadding = if (showBottomBar && isCompactWidth) BendeyBottomBarInset else 0.dp
    val catalogOverlayPadding = if (isCompactWidth) {
        when {
            currentRoute == "pos" -> BendeyCompactCartBarHeight + 4.dp
            currentRoute?.startsWith("mesa/") == true -> BendeyCompactMesaBarHeight + 4.dp
            else -> 0.dp
        }
    } else {
        0.dp
    }
    return navigationBarPadding + bottomBarPadding + catalogOverlayPadding
}
