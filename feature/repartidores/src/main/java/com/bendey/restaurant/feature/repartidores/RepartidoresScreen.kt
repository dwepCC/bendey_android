package com.bendey.restaurant.feature.repartidores

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.bendey.restaurant.core.ui.components.BendeyVerticalScrollColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import com.bendey.restaurant.core.ui.components.BendeyAlertDialog
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyIconButton
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import com.bendey.restaurant.core.ui.components.BendeySwitchRow
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyCard
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.catalog.DeliveryCompany
import com.bendey.restaurant.core.domain.catalog.DeliveryCompanyFormInput
import com.bendey.restaurant.core.domain.catalog.DeliveryDriver
import com.bendey.restaurant.core.domain.catalog.DeliveryDriverFormInput
import com.bendey.restaurant.core.ui.components.BendeyEmptyState
import com.bendey.restaurant.core.ui.components.BendeyLazyColumn
import com.bendey.restaurant.core.ui.components.BendeyVerticalScrollColumn
import com.bendey.restaurant.core.ui.components.BendeyOption
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeySimpleSelect
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.layout.rememberUseAdaptiveTwoPane

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepartidoresScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: RepartidoresViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val useTwoPane = rememberUseAdaptiveTwoPane()

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
                    BendeyIconButton(
                        onClick = viewModel::refresh,
                        icon = Icons.Default.Refresh,
                        contentDescription = "Actualizar",
                    )
                    BendeyIconButton(
                        onClick = viewModel::openCreate,
                        icon = Icons.Default.Add,
                        contentDescription = "Nuevo",
                    )
                },
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xs), horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                BendeyFilterChip(
                    selected = state.tab == RepartidoresTabKind.DRIVERS,
                    onClick = { viewModel.selectTab(RepartidoresTabKind.DRIVERS.name) },
                    text = "Repartidores",
                )
                BendeyFilterChip(
                    selected = state.tab == RepartidoresTabKind.COMPANIES,
                    onClick = { viewModel.selectTab(RepartidoresTabKind.COMPANIES.name) },
                    text = "Empresas",
                )
            }
            state.error?.let { Text(it, color = BendeyColors.Error, modifier = Modifier.padding(BendeySpacing.md)) }
            when (state.tab) {
                RepartidoresTabKind.DRIVERS -> {
                    if (useTwoPane) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        ) {
                            BendeyLazyColumn(state = rememberLazyListState(),
                                contentPadding = PaddingValues(BendeySpacing.md),
                                verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                                modifier = Modifier
                                    .weight(0.42f)
                                    .fillMaxHeight(),
                            ) {
                                items(state.drivers, key = { it.id }) { driver ->
                                    DriverRow(
                                        driver = driver,
                                        selected = state.driverFormOpen && state.editingDriverId == driver.id,
                                        onEdit = { viewModel.openEditDriver(driver.id) },
                                        onDelete = { viewModel.requestDeleteDriver(driver.id) },
                                    )
                                }
                            }
                            VerticalDivider()
                            Column(
                                modifier = Modifier
                                    .weight(0.58f)
                                    .fillMaxHeight()
                                    .padding(BendeySpacing.md),
                            ) {
                                if (state.driverFormOpen) {
                                    DriverFormPane(
                                        form = state.driverForm,
                                        loading = state.actionLoading,
                                        error = state.error,
                                        isEditing = state.editingDriverId != null,
                                        companies = state.companies,
                                        onDismiss = viewModel::dismissDriverForm,
                                        onFormChange = viewModel::updateDriverForm,
                                        onSave = viewModel::saveDriver,
                                    )
                                } else {
                                    BendeyEmptyState(
                                        title = "Selecciona o crea un repartidor",
                                        description = "Consulta y edita datos sin ocultar la lista.",
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        }
                    } else {
                        BendeyLazyColumn(state = rememberLazyListState(),
                            contentPadding = PaddingValues(BendeySpacing.md),
                            verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        ) {
                            items(state.drivers, key = { it.id }) { driver ->
                                DriverRow(
                                    driver = driver,
                                    onEdit = { viewModel.openEditDriver(driver.id) },
                                    onDelete = { viewModel.requestDeleteDriver(driver.id) },
                                )
                            }
                        }
                    }
                }
                RepartidoresTabKind.COMPANIES -> BendeyLazyColumn(state = rememberLazyListState(),
                    contentPadding = PaddingValues(BendeySpacing.md),
                    verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    items(state.companies, key = { it.id }) { company ->
                        CompanyRow(company, { viewModel.openEditCompany(company.id) }, { viewModel.requestDeleteCompany(company.id) })
                    }
                }
            }
        }
    }

    if (state.driverFormOpen && (!useTwoPane || state.tab != RepartidoresTabKind.DRIVERS)) {
        DriverFormDialog(state.driverForm, state.actionLoading, state.error, state.editingDriverId != null, state.companies, viewModel::dismissDriverForm, viewModel::updateDriverForm, viewModel::saveDriver)
    }
    if (state.companyFormOpen) {
        CompanyFormDialog(state.companyForm, state.actionLoading, state.error, state.editingCompanyId != null, viewModel::dismissCompanyForm, viewModel::updateCompanyForm, viewModel::saveCompany)
    }
    state.deleteDriverId?.let {
        BendeyAlertDialog(
            onDismissRequest = viewModel::dismissDeleteDriver,
            title = "Eliminar repartidor",
            message = "¿Eliminar este repartidor?",
            onConfirm = viewModel::confirmDeleteDriver,
            confirmText = "Eliminar",
        )
    }
    state.deleteCompanyId?.let {
        BendeyAlertDialog(
            onDismissRequest = viewModel::dismissDeleteCompany,
            title = "Eliminar empresa",
            message = "¿Eliminar esta empresa?",
            onConfirm = viewModel::confirmDeleteCompany,
            confirmText = "Eliminar",
        )
    }
}

@Composable
private fun DriverRow(
    driver: DeliveryDriver,
    selected: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    BendeyCard(
        containerColor = if (selected) BendeyColors.PrimaryContainer else BendeyColors.Surface,
        contentPadding = PaddingValues(BendeySpacing.cardPadding),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(driver.name, fontWeight = FontWeight.SemiBold)
                if (driver.phone.isNotBlank()) Text(driver.phone, style = MaterialTheme.typography.bodySmall)
                driver.deliveryCompanyName?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = BendeyColors.OnSurfaceVariant) }
                if (!driver.active) BendeyStatusChip("Inactivo", BendeyColors.Warning)
            }
            BendeyIconButton(
                onClick = onEdit,
                icon = Icons.Default.Edit,
                contentDescription = "Editar repartidor",
            )
            BendeyIconButton(
                onClick = onDelete,
                icon = Icons.Default.Delete,
                contentDescription = "Eliminar repartidor",
                tint = BendeyColors.Error,
            )
        }
    }
}

@Composable
private fun CompanyRow(company: DeliveryCompany, onEdit: () -> Unit, onDelete: () -> Unit) {
    BendeyCard(contentPadding = PaddingValues(BendeySpacing.cardPadding)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(company.name, fontWeight = FontWeight.SemiBold)
                if (!company.active) BendeyStatusChip("Inactiva", BendeyColors.Warning)
            }
            BendeyIconButton(
                onClick = onEdit,
                icon = Icons.Default.Edit,
                contentDescription = "Editar empresa",
            )
            BendeyIconButton(
                onClick = onDelete,
                icon = Icons.Default.Delete,
                contentDescription = "Eliminar empresa",
                tint = BendeyColors.Error,
            )
        }
    }
}

@Composable
private fun DriverFormFields(
    form: DeliveryDriverFormInput,
    loading: Boolean,
    error: String?,
    isEditing: Boolean,
    companies: List<DeliveryCompany>,
    onFormChange: ((DeliveryDriverFormInput) -> DeliveryDriverFormInput) -> Unit,
) {
    val companyOptions = listOf(BendeyOption("", "Ninguna")) +
        companies.map { BendeyOption(it.id.toString(), it.name) }
    BendeyTextField(form.name, { v -> onFormChange { it.copy(name = v) } }, "Nombre *")
    BendeyTextField(form.phone, { v -> onFormChange { it.copy(phone = v) } }, "Teléfono")
    BendeyTextField(form.vehicleType, { v -> onFormChange { it.copy(vehicleType = v) } }, "Vehículo")
    BendeyTextField(form.plate, { v -> onFormChange { it.copy(plate = v) } }, "Placa")
    BendeyTextField(form.notes, { v -> onFormChange { it.copy(notes = v) } }, "Notas", singleLine = false)
    BendeySimpleSelect(
        options = companyOptions,
        selectedValue = form.deliveryCompanyId?.toString().orEmpty(),
        onSelect = { value ->
            val id = value.toIntOrNull()
            onFormChange { it.copy(deliveryCompanyId = id) }
        },
        label = "Empresa",
    )
    if (isEditing) {
        BendeySwitchRow(
            label = "Activo",
            checked = form.active,
            onCheckedChange = { checked -> onFormChange { it.copy(active = checked) } },
        )
    }
    error?.let { Text(it, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun DriverFormPane(
    form: DeliveryDriverFormInput,
    loading: Boolean,
    error: String?,
    isEditing: Boolean,
    companies: List<DeliveryCompany>,
    onDismiss: () -> Unit,
    onFormChange: ((DeliveryDriverFormInput) -> DeliveryDriverFormInput) -> Unit,
    onSave: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Text(
            if (isEditing) "Editar repartidor" else "Nuevo repartidor",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = BendeySpacing.sm),
        )
        BendeyVerticalScrollColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
        ) {
            DriverFormFields(form, loading, error, isEditing, companies, onFormChange)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = BendeySpacing.sm))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {
            BendeyTextButton(text = "Cancelar", onClick = onDismiss, enabled = !loading, modifier = Modifier.weight(1f))
            BendeyPrimaryButton(
                text = if (loading) "Guardando…" else "Guardar",
                onClick = onSave,
                enabled = !loading,
                modifier = Modifier.weight(1f),
            )
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
    BendeyFormDialog(
        onDismissRequest = onDismiss,
        title = if (isEditing) "Editar repartidor" else "Nuevo repartidor",
        confirmText = if (loading) "Guardando…" else "Guardar",
        onConfirm = onSave,
        onDismiss = onDismiss,
        confirmEnabled = !loading,
        loading = loading,
    ) {
        DriverFormFields(form, loading, error, isEditing, companies, onFormChange)
    }
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
    BendeyFormDialog(
        onDismissRequest = onDismiss,
        title = if (isEditing) "Editar empresa" else "Nueva empresa",
        confirmText = if (loading) "Guardando…" else "Guardar",
        onConfirm = onSave,
        onDismiss = onDismiss,
        confirmEnabled = !loading,
        loading = loading,
    ) {
        BendeyTextField(form.name, { v -> onFormChange { it.copy(name = v) } }, "Nombre *")
        error?.let { Text(it, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall) }
    }
}
