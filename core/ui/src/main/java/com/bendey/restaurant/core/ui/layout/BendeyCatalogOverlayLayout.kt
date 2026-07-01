package com.bendey.restaurant.core.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.components.BendeyOverlayBanner
import com.bendey.restaurant.core.ui.layout.adaptive.BendeyPosWorkspaceMode

/** Altura visual de la barra flotante POS (chips redondeados). */
val BendeyCompactCartBarHeight = 76.dp

/** Reserva mínima de scroll para la barra compacta de mesa (sin navigation bar). */
val BendeyCompactMesaBarHeight = 88.dp

/**
 * Altura real de [com.bendey.restaurant.feature.mesas.MesaScreen] CompactMesaBar
 * (texto + botón Comanda + padding), para no tapar la última fila del catálogo.
 */
@Composable
fun rememberCompactMesaBarHeight(workspaceMode: BendeyPosWorkspaceMode): Dp {
    val useTabletPortraitBar = workspaceMode == BendeyPosWorkspaceMode.MediumPortrait
    val verticalPad = if (useTabletPortraitBar) BendeySpacing.sm * 2 else 20.dp
    val buttonMin = if (useTabletPortraitBar) 48.dp else 40.dp
    val textBlock = if (useTabletPortraitBar) 52.dp else 44.dp
    return maxOf(buttonMin, textBlock) + verticalPad + BendeySpacing.sm
}

/**
 * Padding inferior para catálogo POS/Mesa con barra compacta superpuesta.
 *
 * @param includeBottomBar true en rutas con bottom navigation (p. ej. POS);
 *   false en detalle de mesa (sin bottom bar, solo navigation bar del sistema).
 */
@Composable
fun rememberBendeyCatalogOverlayBottomInset(includeBottomBar: Boolean): Dp =
    rememberBendeyFloatingActionsBottomOffset(includeBottomBar = includeBottomBar)

@Composable
fun rememberBendeyCatalogGridBottomPadding(
    includeBottomBar: Boolean,
    compactBarHeight: Dp,
    extraScrollPadding: Dp = 0.dp,
): Dp = rememberBendeyCatalogScrollBottomPadding(
    includeBottomBar = includeBottomBar,
    compactBarHeight = compactBarHeight,
    extraScrollPadding = extraScrollPadding,
)

/**
 * Catálogo scrollable + barra compacta anclada al fondo del viewport visible.
 *
 * Evita que la barra quede oculta detrás del bottom navigation, FAB o navigation bar.
 * Usado en POS (teléfono) y detalle de mesa (teléfono).
 */
@Composable
fun BendeyCatalogOverlayLayout(
    modifier: Modifier = Modifier,
    includeBottomBar: Boolean,
    compactBarHeight: Dp,
    bannerMessage: String? = null,
    onBannerDismiss: (() -> Unit)? = null,
    extraCatalogScrollPadding: Dp = 0.dp,
    catalog: @Composable (Modifier, Dp) -> Unit,
    compactBar: @Composable BoxScope.(Modifier) -> Unit,
) {
    val bottomInset = rememberBendeyCatalogOverlayBottomInset(includeBottomBar)
    val gridBottomPadding = rememberBendeyCatalogGridBottomPadding(
        includeBottomBar = includeBottomBar,
        compactBarHeight = compactBarHeight,
        extraScrollPadding = extraCatalogScrollPadding,
    )
    Box(modifier = modifier.fillMaxSize()) {
        catalog(Modifier.fillMaxSize(), gridBottomPadding)
        compactBar(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = bottomInset),
        )
        BendeyOverlayBanner(
            message = bannerMessage,
            onDismiss = onBannerDismiss,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = BendeySpacing.sm,
                    end = BendeySpacing.sm,
                    bottom = bottomInset + compactBarHeight + BendeySpacing.xs,
                ),
        )
    }
}
