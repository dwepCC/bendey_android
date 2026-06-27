package com.bendey.restaurant.core.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bendey.restaurant.core.designsystem.previews.BendeyPhonePreview
import com.bendey.restaurant.core.designsystem.previews.BendeyPreviewSurface
import com.bendey.restaurant.core.designsystem.previews.BendeyTabletPreview
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyElevation
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens

@Composable
fun BendeyAlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    text: @Composable () -> Unit = {},
    confirmButton: @Composable () -> Unit = {},
    dismissButton: @Composable () -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        containerColor = BendeyColors.Surface,
        tonalElevation = BendeyElevation.none,
        shape = BendeyShapeTokens.xl,
        title = title,
        text = text,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
    )
}

@Composable
fun BendeyAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "Aceptar",
    dismissText: String = "Cancelar",
    onDismiss: () -> Unit = onDismissRequest,
    confirmEnabled: Boolean = true,
    confirmLoading: Boolean = false,
) {
    BendeyAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = BendeyColors.OnSurface,
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = BendeyColors.OnSurfaceVariant,
            )
        },
        confirmButton = {
            BendeyPrimaryButton(
                text = confirmText,
                onClick = onConfirm,
                enabled = confirmEnabled,
                loading = confirmLoading,
                fillWidth = false,
            )
        },
        dismissButton = {
            BendeyTextButton(text = dismissText, onClick = onDismiss)
        },
    )
}

@BendeyPhonePreview
@Composable
private fun BendeyAlertDialogPhonePreview() {
    BendeyPreviewSurface {
        BendeyAlertDialog(
            onDismissRequest = {},
            title = "Eliminar sucursal",
            message = "¿Eliminar esta sucursal?",
            onConfirm = {},
        )
    }
}

@BendeyTabletPreview
@Composable
private fun BendeyAlertDialogTabletPreview() {
    BendeyPreviewSurface {
        BendeyAlertDialog(
            onDismissRequest = {},
            title = { Text("Confirmación") },
            text = { Text("Mensaje de ejemplo.") },
            confirmButton = { BendeyPrimaryButton(text = "Aceptar", onClick = {}, fillWidth = false) },
            dismissButton = { BendeyTextButton(text = "Cancelar", onClick = {}) },
        )
    }
}
