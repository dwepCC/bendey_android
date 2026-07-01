package com.bendey.restaurant.feature.configuracion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.components.BendeySectionTitle
import com.bendey.restaurant.core.ui.components.BendeyIconButton
import androidx.compose.material3.MaterialTheme
import com.bendey.restaurant.core.ui.components.BendeySwitchRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.bendey.restaurant.core.designsystem.components.BendeyCard
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.catalog.RestaurantEmployeeType
import com.bendey.restaurant.core.domain.catalog.RestaurantStaffManagementRow
import com.bendey.restaurant.core.ui.components.BendeyLazyColumn
import com.bendey.restaurant.core.ui.components.BendeyEmptyState
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyHorizontalScrollRow
import com.bendey.restaurant.core.ui.components.BendeyOption
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeySimpleSelect
import com.bendey.restaurant.core.ui.components.BendeyTextField

@Composable
fun OperacionTab(
    state: ConfiguracionUiState,
    viewModel: ConfiguracionViewModel,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        OperacionStaffHeader(state, viewModel)
        BendeyManagementCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xxs),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                BendeySectionTitle(text = "PIN de anulación")
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
        OperacionStaffList(state, viewModel, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun OperacionStaffHeader(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    Row(
        Modifier.fillMaxWidth().padding(BendeySpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BendeySectionTitle(text = "Usuarios del restaurante")
        if (state.canManageRestaurantSettings) {
            BendeyPrimaryButton("+ Usuario", viewModel::openCreateStaff, fillWidth = false)
        }
    }
}

@Composable
private fun OperacionStaffList(
    state: ConfiguracionUiState,
    viewModel: ConfiguracionViewModel,
    modifier: Modifier = Modifier,
) {
    if (state.staffLoading && state.staffRows.isEmpty()) {
        Text("Cargando usuarios…", modifier = Modifier.padding(BendeySpacing.md), color = BendeyColors.OnSurfaceVariant)
    } else if (state.staffRows.isEmpty()) {
        BendeyEmptyState(title = "Sin usuarios registrados", inline = true, modifier = modifier)
    } else {
        BendeyLazyColumn(
            state = rememberLazyListState(),
            contentPadding = PaddingValues(BendeySpacing.md),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
            modifier = modifier,
        ) {
            items(state.staffRows, key = { it.userId }) { row ->
                StaffManagementCard(
                    row = row,
                    onEdit = { viewModel.openEditStaff(row) },
                    canEdit = state.canManageRestaurantSettings,
                )
            }
        }
    }
}

@Composable
private fun StaffManagementCard(
    row: RestaurantStaffManagementRow,
    onEdit: () -> Unit,
    canEdit: Boolean,
) {
    BendeyCard(
        containerColor = BendeyColors.Surface,
        contentPadding = PaddingValues(BendeySpacing.cardPadding),
    ) {
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
                BendeyIconButton(
                    onClick = onEdit,
                    icon = Icons.Default.Edit,
                    contentDescription = "Editar",
                )
            }
        }
    }
}

@Composable
fun StaffCreateDialog(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    if (!state.staffCreateOpen) return
    BendeyFormDialog(
        onDismissRequest = viewModel::dismissCreateStaff,
        title = "Nuevo usuario",
        confirmText = if (state.actionLoading) "Guardando…" else "Crear",
        onConfirm = viewModel::confirmCreateStaff,
        onDismiss = viewModel::dismissCreateStaff,
        confirmEnabled = !state.actionLoading,
        loading = state.actionLoading,
        enableContentScroll = true,
    ) {
        StaffCreateFields(state, viewModel)
    }
}

@Composable
fun StaffEditDialog(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    if (!state.staffEditOpen) return
    BendeyFormDialog(
        onDismissRequest = viewModel::dismissEditStaff,
        title = "Editar usuario",
        confirmText = if (state.actionLoading) "Guardando…" else "Guardar",
        onConfirm = viewModel::confirmEditStaff,
        onDismiss = viewModel::dismissEditStaff,
        confirmEnabled = !state.actionLoading,
        loading = state.actionLoading,
        enableContentScroll = true,
    ) {
        StaffEditFields(state, viewModel)
    }
}

@Composable
private fun StaffCreateFields(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    val form = state.staffCreateForm
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

@Composable
private fun StaffEditFields(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    val form = state.staffEditForm
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
    BendeyHorizontalScrollRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
    ) {
        branches.filter { it.active }.forEach { branch ->
            BendeyFilterChip(
                selected = selectedIds.contains(branch.id),
                onClick = { onToggle(branch.id) },
                label = { Text(branch.name) },
            )
        }
    }
}
