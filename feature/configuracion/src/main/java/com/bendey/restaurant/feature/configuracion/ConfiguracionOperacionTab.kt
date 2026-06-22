package com.bendey.restaurant.feature.configuracion

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.bendey.restaurant.core.ui.components.BendeySwitchRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyChipDefaults
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.catalog.RestaurantEmployeeType
import com.bendey.restaurant.core.domain.catalog.RestaurantStaffManagementRow
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyOption
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeySimpleSelect
import com.bendey.restaurant.core.ui.components.BendeyTextField

@Composable
fun OperacionTab(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(BendeySpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Usuarios del restaurante", fontWeight = FontWeight.SemiBold)
            if (state.canManageRestaurantSettings) {
                BendeyPrimaryButton("+ Usuario", viewModel::openCreateStaff, fillWidth = false)
            }
        }
        BendeyManagementCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xxs),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                Text("PIN de anulación", fontWeight = FontWeight.SemiBold)
                Text(
                    if (state.settings?.hasDeletionPin == true) "Configurado" else "Sin configurar",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
                if (state.canManageRestaurantSettings) {
                    BendeyPrimaryButton("Configurar PIN", viewModel::openPinDialog, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        if (state.staffLoading && state.staffRows.isEmpty()) {
            Text("Cargando usuarios…", modifier = Modifier.padding(BendeySpacing.md), color = BendeyColors.OnSurfaceVariant)
        } else if (state.staffRows.isEmpty()) {
            Text("Sin usuarios registrados", modifier = Modifier.padding(BendeySpacing.md), color = BendeyColors.OnSurfaceVariant)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(BendeySpacing.md),
                verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                modifier = Modifier.weight(1f),
            ) {
                items(state.staffRows, key = { it.userId }) { row ->
                    StaffManagementCard(row, onEdit = { viewModel.openEditStaff(row) }, canEdit = state.canManageRestaurantSettings)
                }
            }
        }
    }
}

@Composable
private fun StaffManagementCard(row: RestaurantStaffManagementRow, onEdit: () -> Unit, canEdit: Boolean) {
    BendeyManagementCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs)) {
                Text(row.name.ifBlank { row.email }, fontWeight = FontWeight.SemiBold)
                Text(row.email, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                    BendeyStatusChip(
                        label = RestaurantEmployeeType.fromApi(row.employeeType).label,
                        accentColor = if (row.profileComplete) BendeyColors.Primary else BendeyColors.Warning,
                    )
                    if (row.hasPin) BendeyStatusChip("PIN", BendeyColors.AccentTeal)
                    if (!row.active) BendeyStatusChip("Inactivo", BendeyColors.Error)
                }
                if (row.branchNames.isNotEmpty()) {
                    Text(
                        row.branchNames.joinToString(", "),
                        style = MaterialTheme.typography.labelSmall,
                        color = BendeyColors.OnSurfaceVariant,
                    )
                }
            }
            if (canEdit) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
            }
        }
    }
}

@Composable
fun StaffCreateDialog(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    if (!state.staffCreateOpen) return
    val form = state.staffCreateForm
    BendeyFormDialog(
        onDismissRequest = viewModel::dismissCreateStaff,
        title = "Nuevo usuario",
        confirmText = if (state.actionLoading) "Guardando…" else "Crear",
        onConfirm = viewModel::confirmCreateStaff,
        onDismiss = viewModel::dismissCreateStaff,
        confirmEnabled = !state.actionLoading,
        loading = state.actionLoading,
    ) {
        BendeyTextField(form.name, { v -> viewModel.updateStaffCreateForm { it.copy(name = v) } }, "Nombre")
        BendeyTextField(form.email, { v -> viewModel.updateStaffCreateForm { it.copy(email = v) } }, "Email")
        BendeyTextField(form.phone, { v -> viewModel.updateStaffCreateForm { it.copy(phone = v) } }, "Teléfono (opcional)")
        EmployeeTypePicker(form.employeeType) { type ->
            viewModel.updateStaffCreateForm { it.copy(employeeType = type) }
        }
        BendeyTextField(form.pin, { v ->
            viewModel.updateStaffCreateForm { it.copy(pin = v.filter { c -> c.isDigit() }.take(6)) }
        }, "PIN acceso (4-6 dígitos)")
        BranchMultiSelect(state.branches, form.branchIds, viewModel::toggleStaffCreateBranch)
    }
}

@Composable
fun StaffEditDialog(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    if (!state.staffEditOpen) return
    val form = state.staffEditForm
    BendeyFormDialog(
        onDismissRequest = viewModel::dismissEditStaff,
        title = "Editar usuario",
        confirmText = if (state.actionLoading) "Guardando…" else "Guardar",
        onConfirm = viewModel::confirmEditStaff,
        onDismiss = viewModel::dismissEditStaff,
        confirmEnabled = !state.actionLoading,
        loading = state.actionLoading,
    ) {
        Text(form.name, fontWeight = FontWeight.SemiBold)
        Text(form.email, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
        EmployeeTypePicker(form.employeeType) { type ->
            viewModel.updateStaffEditForm { it.copy(employeeType = type) }
        }
        BendeyTextField(form.pin, { v ->
            viewModel.updateStaffEditForm { it.copy(pin = v.filter { c -> c.isDigit() }.take(6), clearPin = false) }
        }, "Nuevo PIN (opcional)")
        BendeySwitchRow(
            label = "Quitar PIN",
            checked = form.clearPin,
            onCheckedChange = { checked ->
                viewModel.updateStaffEditForm { it.copy(clearPin = checked, pin = if (checked) "" else it.pin) }
            },
        )
        if (form.employeeType.isNotBlank()) {
            BranchMultiSelect(state.branches, form.branchIds, viewModel::toggleStaffEditBranch)
        }
    }
}

@Composable
private fun EmployeeTypePicker(selected: String, onSelect: (String) -> Unit) {
    val options = RestaurantEmployeeType.entries
        .filter { it != RestaurantEmployeeType.NONE }
        .map { BendeyOption(it.apiValue, it.label) } +
        listOf(BendeyOption("", "Sin acceso"))
    BendeySimpleSelect(
        options = options,
        selectedValue = selected,
        onSelect = onSelect,
        label = "Rol",
    )
}

@Composable
private fun BranchMultiSelect(
    branches: List<com.bendey.restaurant.core.domain.catalog.BranchItem>,
    selectedIds: List<Int>,
    onToggle: (Int) -> Unit,
) {
    Text("Sucursales", style = MaterialTheme.typography.labelMedium)
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
    ) {
        branches.filter { it.active }.forEach { branch ->
            FilterChip(
                selected = selectedIds.contains(branch.id),
                onClick = { onToggle(branch.id) },
                label = { Text(branch.name) },
                colors = BendeyChipDefaults.filterChipColors(),
                shape = BendeyShapeTokens.chip,
                border = null,
            )
        }
    }
}
