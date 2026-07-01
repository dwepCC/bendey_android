package com.bendey.restaurant.core.ui.pos.workspace

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.bendey.restaurant.core.ui.layout.BendeyCatalogOverlayLayout
import com.bendey.restaurant.core.ui.layout.BendeyFlexibleContentSlot
import com.bendey.restaurant.core.ui.layout.adaptive.AdaptivePos

/**
 * POS tablet portrait — evolución del layout móvil.
 *
 * Catálogo a ancho completo + barra flotante (pedidos / carrito) + bottom sheet al abrir carrito.
 * Solo [com.bendey.restaurant.core.ui.layout.adaptive.BendeyAdaptiveProfile.MediumPortrait].
 */
@Composable
fun BendeyPosPortraitMobileLayout(
    orderTypeContent: @Composable () -> Unit,
    catalog: @Composable (Modifier, Dp) -> Unit,
    floatingBar: @Composable BoxScope.(Modifier) -> Unit,
    modifier: Modifier = Modifier,
    bannerMessage: String? = null,
    onBannerDismiss: (() -> Unit)? = null,
) {
    val barHeight = AdaptivePos.portraitFloatingActionChipSize()

    Column(modifier = modifier.fillMaxSize()) {
        orderTypeContent()
        BendeyFlexibleContentSlot { inner ->
            BendeyCatalogOverlayLayout(
                modifier = inner,
                includeBottomBar = true,
                compactBarHeight = barHeight,
                bannerMessage = bannerMessage,
                onBannerDismiss = onBannerDismiss,
                catalog = catalog,
                compactBar = floatingBar,
            )
        }
    }
}
