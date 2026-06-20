package com.bendey.restaurant.feature.repartidores

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.domain.catalog.DeliveryCompany
import com.bendey.restaurant.core.domain.catalog.DeliveryCompanyFormInput
import com.bendey.restaurant.core.domain.catalog.DeliveryDriver
import com.bendey.restaurant.core.domain.catalog.DeliveryDriverFormInput
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepartidoresScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: RepartidoresViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PullToRefreshBox(isRefreshing = state.loading, onRefresh = viewModel::refresh, modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BendeyScreenToolbar(
                title = "Repartidores",
                subtitle = when (state.tab) {
                    RepartidoresTabKind.DRIVERS -> "${state.drivers.size} repartidores"
                    RepartidoresTabKind.COMPANIES -> "${state.companies.size} empresas"
                },
                onBack = onBack,
                actions = {
                    IconButton(onClick = viewModel::refresh) { Icon(Icons.Default.Refresh, contentDescription = null) }
                    IconButton(onClick = viewModel::openCreate) { Icon(Icons.Default.Add, contentDescription = null) }
                },
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.tab == RepartidoresTabKind.DRIVERS, onClick = { viewModel.selectTab(RepartidoresTabKind.DRIVERS.name) }, label = { Text("Repartidores") })
                FilterChip(selected = state.tab == RepartidoresTabKind.COMPANIES, onClick = { viewModel.selectTab(RepartidoresTabKind.COMPANIES.name) }, label = { Text("Empresas") })
            }
            state.error?.let { Text(it, color = BendeyColors.Error, modifier = Modifier.padding(16.dp)) }
            when (state.tab) {
                RepartidoresTabKind.DRIVERS -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.drivers, key = { it.id }) { driver ->
                        DriverRow(driver, { viewModel.openEditDriver(driver.id) }, { viewModel.requestDeleteDriver(driver.id) })
                    }
                }
                RepartidoresTabKind.COMPANIES -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.companies, key = { it.id }) { company ->
                        CompanyRow(company, { viewModel.openEditCompany(company.id) }, { viewModel.requestDeleteCompany(company.id) })
                    }
                }
            }
        }
    }

    if (state.driverFormOpen) {
        DriverFormDialog(state.driverForm, state.actionLoading, state.error, state.editingDriverId != null, state.companies, viewModel::dismissDriverForm, viewModel::updateDriverForm, viewModel::saveDriver)
    }
    if (state.companyFormOpen) {
        CompanyFormDialog(state.companyForm, state.actionLoading, state.error, state.editingCompanyId != null, viewModel::dismissCompanyForm, viewModel::updateCompanyForm, viewModel::saveCompany)
    }
    state.deleteDriverId?.let {
        AlertDialog(onDismissRequest = viewModel::dismissDeleteDriver, title = { Text("Eliminar repartidor") }, text = { Text("¿Eliminar este repartidor?") },
            confirmButton = { BendeyPrimaryButton("Eliminar", viewModel::confirmDeleteDriver) },
            dismissButton = { TextButton(onClick = viewModel::dismissDeleteDriver) { Text("Cancelar") } })
    }
    state.deleteCompanyId?.let {
        AlertDialog(onDismissRequest = viewModel::dismissDeleteCompany, title = { Text("Eliminar empresa") }, text = { Text("¿Eliminar esta empresa?") },
            confirmButton = { BendeyPrimaryButton("Eliminar", viewModel::confirmDeleteCompany) },
            dismissButton = { TextButton(onClick = viewModel::dismissDeleteCompany) { Text("Cancelar") } })
    }
}

@Composable
private fun DriverRow(driver: DeliveryDriver, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(driver.name, fontWeight = FontWeight.SemiBold)
                if (driver.phone.isNotBlank()) Text(driver.phone, style = MaterialTheme.typography.bodySmall)
                driver.deliveryCompanyName?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = BendeyColors.OnSurfaceVariant) }
                if (!driver.active) BendeyStatusChip("Inactivo", BendeyColors.Warning)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = null) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = null, tint = BendeyColors.Error) }
        }
    }
}

@Composable
private fun CompanyRow(company: DeliveryCompany, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(company.name, fontWeight = FontWeight.SemiBold)
                if (!company.active) BendeyStatusChip("Inactiva", BendeyColors.Warning)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = null) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = null, tint = BendeyColors.Error) }
        }
    }
}

@Composable
private fun DriverFormDialog(
    form: DeliveryDriverFormInput,
    loading: Boolean,
    error: String?,
    isEditing: Boolean,
    companies: List<DeliveryCompany>,
    onDismiss: () -> Unit,
    onFormChange: ((DeliveryDriverFormInput) -> DeliveryDriverFormInput) -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Editar repartidor" else "Nuevo repartidor") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BendeyTextField(form.name, { v -> onFormChange { it.copy(name = v) } }, "Nombre *")
                BendeyTextField(form.phone, { v -> onFormChange { it.copy(phone = v) } }, "Teléfono")
                BendeyTextField(form.vehicleType, { v -> onFormChange { it.copy(vehicleType = v) } }, "Vehículo")
                BendeyTextField(form.plate, { v -> onFormChange { it.copy(plate = v) } }, "Placa")
                BendeyTextField(form.notes, { v -> onFormChange { it.copy(notes = v) } }, "Notas", singleLine = false)
                Text("Empresa")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = form.deliveryCompanyId == null, onClick = { onFormChange { it.copy(deliveryCompanyId = null) } }, label = { Text("Ninguna") })
                    companies.forEach { company ->
                        FilterChip(selected = form.deliveryCompanyId == company.id, onClick = { onFormChange { it.copy(deliveryCompanyId = company.id) } }, label = { Text(company.name) })
                    }
                }
                if (isEditing) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Activo"); Switch(form.active, { checked -> onFormChange { it.copy(active = checked) } })
                }
                error?.let { Text(it, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { BendeyPrimaryButton(if (loading) "Guardando…" else "Guardar", onSave, enabled = !loading) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun CompanyFormDialog(
    form: DeliveryCompanyFormInput,
    loading: Boolean,
    error: String?,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onFormChange: ((DeliveryCompanyFormInput) -> DeliveryCompanyFormInput) -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Editar empresa" else "Nueva empresa") },
        text = {
            Column {
                BendeyTextField(form.name, { v -> onFormChange { it.copy(name = v) } }, "Nombre *")
                error?.let { Text(it, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { BendeyPrimaryButton(if (loading) "Guardando…" else "Guardar", onSave, enabled = !loading) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
