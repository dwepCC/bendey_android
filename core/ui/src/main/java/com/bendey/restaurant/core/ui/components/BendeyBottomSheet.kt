package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.previews.BendeyPhonePreview
import com.bendey.restaurant.core.designsystem.previews.BendeyPreviewSurface
import com.bendey.restaurant.core.designsystem.previews.BendeyTabletPreview
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyElevation
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.ui.layout.BendeyNavigationBarScrim
import com.bendey.restaurant.core.ui.layout.rememberNavigationBarInset

/**
 * Asa de arrastre estándar Bendey — rayita tomate que indica cerrar deslizando hacia abajo.
 */
@Composable
fun BendeyBottomSheetDragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(BendeyColors.Rest900),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BendeyBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    dragHandle: @Composable (() -> Unit)? = { BendeyBottomSheetDragHandle() },
    content: @Composable ColumnScope.() -> Unit,
) {
    val navigationBarInset = rememberNavigationBarInset()
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = BendeyColors.Surface,
        tonalElevation = BendeyElevation.none,
        dragHandle = dragHandle,
        shape = BendeyShapeTokens.sheet,
        contentWindowInsets = { WindowInsets(0) },
        content = {
            CompositionLocalProvider(LocalBendeyScrollHintsEnabled provides false) {
                Box(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = navigationBarInset),
                    ) {
                        content()
                    }
                    BendeyNavigationBarScrim(
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        },
    )
}

@BendeyPhonePreview
@Composable
private fun BendeyBottomSheetDragHandlePreview() {
    BendeyPreviewSurface {
        BendeyBottomSheetDragHandle()
    }
}

@BendeyTabletPreview
@Composable
private fun BendeyBottomSheetTabletPreview() {
    BendeyBottomSheetDragHandlePreview()
}
