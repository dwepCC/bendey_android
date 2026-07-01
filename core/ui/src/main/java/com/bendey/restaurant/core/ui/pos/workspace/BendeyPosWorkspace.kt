package com.bendey.restaurant.core.ui.pos.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.bendey.restaurant.core.ui.layout.adaptive.AdaptiveDimensions
import com.bendey.restaurant.core.ui.layout.adaptive.AdaptivePos
import com.bendey.restaurant.core.ui.layout.adaptive.BendeyAdaptiveProfile
import com.bendey.restaurant.core.ui.layout.adaptive.BendeyPosWorkspaceMode
import com.bendey.restaurant.core.ui.layout.adaptive.rememberBendeyAdaptiveProfile
import com.bendey.restaurant.core.ui.layout.adaptive.rememberPosWorkspaceMode
import com.bendey.restaurant.core.ui.layout.adaptive.toPosWorkspaceMode

/**
 * Layout adaptive del espacio de trabajo POS.
 *
 * - [BendeyPosWorkspaceMode.Compact]: delega en [compactContent] sin cambios.
 * - Medium Portrait: evolución móvil vía [portraitMobileContent] (catálogo full + carrito flotante).
 * - Medium Landscape / Expanded: catálogo + carrito permanente.
 */
@Composable
fun BendeyPosWorkspace(
    modifier: Modifier = Modifier,
    profile: BendeyAdaptiveProfile = rememberBendeyAdaptiveProfile(),
    workspaceMode: BendeyPosWorkspaceMode = rememberPosWorkspaceMode(),
    sessionChrome: @Composable () -> Unit = {},
    compactContent: @Composable () -> Unit,
    portraitMobileContent: @Composable () -> Unit = {},
    catalog: @Composable (Modifier, Dp) -> Unit,
    cartPanel: @Composable (Modifier) -> Unit,
    cartPeekBar: @Composable (
        expanded: Boolean,
        onExpandToggle: () -> Unit,
        Modifier,
    ) -> Unit = { _, _, _ -> },
    cartExpandedOverlay: @Composable (
        visible: Boolean,
        onDismiss: () -> Unit,
        peekHeight: Dp,
        content: @Composable () -> Unit,
    ) -> Unit = { _, _, _, _ -> },
) {
    when (workspaceMode) {
        BendeyPosWorkspaceMode.Compact -> {
            compactContent()
        }
        BendeyPosWorkspaceMode.MediumPortrait -> {
            Column(modifier = modifier.fillMaxSize()) {
                sessionChrome()
                portraitMobileContent()
            }
        }
        BendeyPosWorkspaceMode.MediumLandscape,
        BendeyPosWorkspaceMode.Expanded,
        -> {
            val gap = AdaptivePos.panelGap(profile)
            val outerPad = AdaptivePos.workspacePadding(profile)
            val cartWidth = AdaptiveDimensions.cartPanelWidth(profile)

            Column(modifier = modifier.fillMaxSize()) {
                sessionChrome()
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(horizontal = outerPad, vertical = gap),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        catalog(Modifier.fillMaxSize(), gap)
                    }
                    Box(
                        modifier = Modifier
                            .width(cartWidth)
                            .fillMaxHeight(),
                    ) {
                        cartPanel(Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}
