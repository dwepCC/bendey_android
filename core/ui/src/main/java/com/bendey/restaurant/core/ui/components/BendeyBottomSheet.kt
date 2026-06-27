package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bendey.restaurant.core.designsystem.previews.BendeyPhonePreview
import com.bendey.restaurant.core.designsystem.previews.BendeyPreviewSurface
import com.bendey.restaurant.core.designsystem.previews.BendeyTabletPreview
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyElevation
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BendeyBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = BendeyColors.Surface,
        tonalElevation = BendeyElevation.none,
        dragHandle = dragHandle,
        shape = BendeyShapeTokens.sheet,
        content = content,
    )
}

@BendeyPhonePreview
@Composable
private fun BendeyBottomSheetPhonePreview() {
    BendeyPreviewSurface {
        androidx.compose.material3.Text("BendeyBottomSheet — contenedor Surface + sheet shape")
    }
}

@BendeyTabletPreview
@Composable
private fun BendeyBottomSheetTabletPreview() {
    BendeyBottomSheetPhonePreview()
}
