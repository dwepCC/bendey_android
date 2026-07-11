package com.bendey.restaurant.feature.productos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.domain.catalog.BranchItem
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyOption
import com.bendey.restaurant.core.ui.components.BendeySimpleSelect
import com.bendey.restaurant.core.ui.components.BendeyTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

data class StockAdjustmentForm(
    val productId: Int,
    val productName: String,
    val branchId: Int? = null,
    val isIncrease: Boolean = true,
    val quantity: String = "1",
    val notes: String = "",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StockAdjustmentDialog(
    form: StockAdjustmentForm,
    branches: List<BranchItem>,
    loading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onFormChange: ((StockAdjustmentForm) -> StockAdjustmentForm) -> Unit,
    onConfirm: () -> Unit,
) {
    val branchOptions = branches.map { BendeyOption(it.id.toString(), it.name) }
    BendeyFormDialog(
        onDismissRequest = onDismiss,
        title = "Ajuste de stock",
        confirmText = if (loading) "Guardando…" else "Guardar ajuste",
        loading = loading,
        confirmEnabled = !loading && form.notes.trim().isNotEmpty(),
        onConfirm = onConfirm,
        enableContentScroll = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = form.productName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (branchOptions.isNotEmpty()) {
                BendeySimpleSelect(
                    options = branchOptions,
                    selectedValue = form.branchId?.toString().orEmpty(),
                    onSelect = { value ->
                        onFormChange { it.copy(branchId = value.toIntOrNull()) }
                    },
                    label = "Sucursal",
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tipo de ajuste",
                    style = MaterialTheme.typography.labelMedium,
                    color = BendeyColors.OnSurfaceVariant,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BendeyFilterChip(
                        selected = form.isIncrease,
                        onClick = { onFormChange { it.copy(isIncrease = true) } },
                        text = "Aumentar stock",
                    )
                    BendeyFilterChip(
                        selected = !form.isIncrease,
                        onClick = { onFormChange { it.copy(isIncrease = false) } },
                        text = "Disminuir stock",
                    )
                }
            }
            BendeyTextField(
                value = form.quantity,
                onValueChange = { value -> onFormChange { it.copy(quantity = value) } },
                label = "Cantidad",
            )
            BendeyTextField(
                value = form.notes,
                onValueChange = { value -> onFormChange { it.copy(notes = value) } },
                label = "Motivo del ajuste *",
                singleLine = false,
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
