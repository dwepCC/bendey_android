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

/** Altura visual de la barra flotante POS (chips redondeados). */
val BendeyCompactCartBarHeight = 76.dp

/** Altura visual de la barra compacta en detalle de mesa (padding + botón). */
val BendeyCompactMesaBarHeight = 76.dp

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
): Dp = rememberBendeyCatalogScrollBottomPadding(
    includeBottomBar = includeBottomBar,
    compactBarHeight = compactBarHeight,
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
    catalog: @Composable (Modifier, Dp) -> Unit,
    compactBar: @Composable BoxScope.(Modifier) -> Unit,
) {
    val bottomInset = rememberBendeyCatalogOverlayBottomInset(includeBottomBar)
    val gridBottomPadding = rememberBendeyCatalogGridBottomPadding(
        includeBottomBar = includeBottomBar,
        compactBarHeight = compactBarHeight,
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
