package com.bendey.restaurant.feature.productos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.ui.components.BendeyCheckboxRow
import com.bendey.restaurant.core.ui.components.BendeyFormDialog

data class MenuChannelForm(
    val productId: Int,
    val productName: String,
    val enabled: Boolean,
)

@Composable
fun MenuChannelDialog(
    form: MenuChannelForm,
    loading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onFormChange: ((MenuChannelForm) -> MenuChannelForm) -> Unit,
    onConfirm: () -> Unit,
) {
    BendeyFormDialog(
        onDismissRequest = onDismiss,
        title = "Menú digital",
        confirmText = if (loading) "Guardando…" else "Guardar",
        loading = loading,
        confirmEnabled = !loading,
        onConfirm = onConfirm,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = form.productName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            BendeyCheckboxRow(
                label = "Visible en menú digital",
                checked = form.enabled,
                onCheckedChange = { checked -> onFormChange { it.copy(enabled = checked) } },
            )
            Text(
                text = if (form.enabled) {
                    "El producto aparecerá en el catálogo web / QR de mesa."
                } else {
                    "No se mostrará en el menú digital aunque esté en POS."
                },
                style = MaterialTheme.typography.bodySmall,
                color = BendeyColors.OnSurfaceVariant,
            )
            error?.let {
                Text(
                    text = it,
                    color = BendeyColors.Error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
