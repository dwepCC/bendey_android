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

/** Altura visual de la barra compacta de carrito POS (card + márgenes). */
val BendeyCompactCartBarHeight = 68.dp

/** Altura visual de la barra compacta en detalle de mesa. */
val BendeyCompactMesaBarHeight = 72.dp

/**
 * Padding inferior para catálogo POS/Mesa con barra compacta superpuesta.
 *
 * @param includeBottomBar true en rutas con bottom navigation (p. ej. POS);
 *   false en detalle de mesa (sin bottom bar, solo navigation bar del sistema).
 */
@Composable
fun rememberBendeyCatalogOverlayBottomInset(includeBottomBar: Boolean): Dp =
    rememberBendeyBottomBarScrollPadding(includeBottomBar = includeBottomBar)

@Composable
fun rememberBendeyCatalogGridBottomPadding(
    includeBottomBar: Boolean,
    compactBarHeight: Dp,
): Dp = compactBarHeight + rememberBendeyCatalogOverlayBottomInset(includeBottomBar) + 4.dp

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
    }
}
