package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bendey.restaurant.core.designsystem.previews.BendeyPhonePreview
import com.bendey.restaurant.core.designsystem.previews.BendeyPreviewSurface
import com.bendey.restaurant.core.designsystem.previews.BendeyTabletPreview
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens

@Composable
fun BendeySnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
    ) { data ->
        // Tablet: centrado abajo y con ancho legible (no pegado a una esquina ni de borde a borde).
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Snackbar(
                snackbarData = data,
                modifier = Modifier.widthIn(max = BendeySnackbarMaxWidth),
                containerColor = BendeyColors.OnSurface,
                contentColor = BendeyColors.Surface,
                shape = BendeyShapeTokens.md,
                actionColor = BendeyColors.Primary,
                actionContentColor = BendeyColors.Primary,
            )
        }
    }
}

/** Ancho máximo legible del aviso; en teléfono ocupa el ancho disponible. */
val BendeySnackbarMaxWidth: Dp = 560.dp

@BendeyPhonePreview
@Composable
private fun BendeySnackbarHostPhonePreview() {
    BendeyPreviewSurface {
        Snackbar(
            action = {},
            shape = BendeyShapeTokens.md,
            containerColor = BendeyColors.OnSurface,
            contentColor = BendeyColors.Surface,
        ) {
            androidx.compose.material3.Text(
                text = "Operación completada",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@BendeyTabletPreview
@Composable
private fun BendeySnackbarHostTabletPreview() {
    BendeySnackbarHostPhonePreview()
}
