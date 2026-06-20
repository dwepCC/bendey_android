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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.domain.catalog.RestaurantEmployeeType
import com.bendey.restaurant.core.domain.catalog.RestaurantStaffManagementRow
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextField

@Composable
fun OperacionTab(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Usuarios del restaurante", fontWeight = FontWeight.SemiBold)
            BendeyPrimaryButton("+ Usuario", viewModel::openCreateStaff, fillWidth = false)
        }
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("PIN de anulación", fontWeight = FontWeight.SemiBold)
                Text(
                    if (state.settings?.hasDeletionPin == true) "Configurado" else "Sin configurar",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
                BendeyPrimaryButton("Configurar PIN", viewModel::openPinDialog, modifier = Modifier.fillMaxWidth())
            }
        }
        if (state.staffLoading && state.staffRows.isEmpty()) {
            Text("Cargando usuarios…", modifier = Modifier.padding(16.dp), color = BendeyColors.OnSurfaceVariant)
        } else if (state.staffRows.isEmpty()) {
            Text("Sin usuarios registrados", modifier = Modifier.padding(16.dp), color = BendeyColors.OnSurfaceVariant)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(state.staffRows, key = { it.userId }) { row ->
                    StaffManagementCard(row, onEdit = { viewModel.openEditStaff(row) })
                }
            }
        }
    }
}

@Composable
private fun StaffManagementCard(row: RestaurantStaffManagementRow, onEdit: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(row.name.ifBlank { row.email }, fontWeight = FontWeight.SemiBold)
                Text(row.email, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar")
            }
        }
    }
}

@Composable
fun StaffCreateDialog(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    if (!state.staffCreateOpen) return
    val form = state.staffCreateForm
    AlertDialog(
        onDismissRequest = viewModel::dismissCreateStaff,
        title = { Text("Nuevo usuario") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
        },
        confirmButton = {
            BendeyPrimaryButton(
                if (state.actionLoading) "Guardando…" else "Crear",
                viewModel::confirmCreateStaff,
                enabled = !state.actionLoading,
            )
        },
        dismissButton = { TextButton(onClick = viewModel::dismissCreateStaff) { Text("Cancelar") } },
    )
}

@Composable
fun StaffEditDialog(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    if (!state.staffEditOpen) return
    val form = state.staffEditForm
    AlertDialog(
        onDismissRequest = viewModel::dismissEditStaff,
        title = { Text("Editar usuario") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(form.name, fontWeight = FontWeight.SemiBold)
                Text(form.email, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                EmployeeTypePicker(form.employeeType) { type ->
                    viewModel.updateStaffEditForm { it.copy(employeeType = type) }
                }
                BendeyTextField(form.pin, { v ->
                    viewModel.updateStaffEditForm { it.copy(pin = v.filter { c -> c.isDigit() }.take(6), clearPin = false) }
                }, "Nuevo PIN (opcional)")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Quitar PIN")
                    Switch(
                        checked = form.clearPin,
                        onCheckedChange = { checked ->
                            viewModel.updateStaffEditForm { it.copy(clearPin = checked, pin = if (checked) "" else it.pin) }
                        },
                    )
                }
                if (form.employeeType.isNotBlank()) {
                    BranchMultiSelect(state.branches, form.branchIds, viewModel::toggleStaffEditBranch)
                }
            }
        },
        confirmButton = {
            BendeyPrimaryButton(
                if (state.actionLoading) "Guardando…" else "Guardar",
                viewModel::confirmEditStaff,
                enabled = !state.actionLoading,
            )
        },
        dismissButton = { TextButton(onClick = viewModel::dismissEditStaff) { Text("Cancelar") } },
    )
}

@Composable
private fun EmployeeTypePicker(selected: String, onSelect: (String) -> Unit) {
    Text("Rol", style = MaterialTheme.typography.labelMedium)
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        RestaurantEmployeeType.entries.filter { it != RestaurantEmployeeType.NONE }.forEach { type ->
            FilterChip(
                selected = selected == type.apiValue,
                onClick = { onSelect(type.apiValue) },
                label = { Text(type.label) },
            )
        }
        FilterChip(
            selected = selected.isBlank(),
            onClick = { onSelect("") },
            label = { Text("Sin acceso") },
        )
    }
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
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        branches.filter { it.active }.forEach { branch ->
            FilterChip(
                selected = selectedIds.contains(branch.id),
                onClick = { onToggle(branch.id) },
                label = { Text(branch.name) },
            )
        }
    }
}
