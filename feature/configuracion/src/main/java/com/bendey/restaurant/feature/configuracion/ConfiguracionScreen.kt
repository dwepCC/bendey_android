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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.bendey.restaurant.core.domain.billing.DocumentSeries
import com.bendey.restaurant.core.domain.catalog.BranchItem
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.components.BendeyTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionScreen(
    onBack: () -> Unit = {},
    onOpenPrinting: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ConfiguracionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PullToRefreshBox(isRefreshing = state.loading, onRefresh = viewModel::refresh, modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BendeyScreenToolbar(
                title = "Configuración",
                subtitle = state.config?.tradeName?.ifBlank { state.config?.businessName }.orEmpty(),
                onBack = onBack,
                actions = {
                    IconButton(onClick = viewModel::refresh) { Icon(Icons.Default.Refresh, contentDescription = null) }
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ConfigTab.entries.forEach { tab ->
                    FilterChip(
                        selected = state.tab == tab,
                        onClick = { viewModel.setTab(tab) },
                        label = { Text(tab.label) },
                    )
                }
            }
            state.error?.let { Text(it, color = BendeyColors.Error, modifier = Modifier.padding(16.dp)) }
            when (state.tab) {
                ConfigTab.GENERAL -> GeneralTab(state, onOpenPrinting, viewModel)
                ConfigTab.OPERACION -> OperacionTab(state, viewModel)
                ConfigTab.BRANCHES -> BranchesTab(state, viewModel)
                ConfigTab.SERIES -> SeriesTab(state, viewModel)
            }
        }
    }

    if (state.configFormOpen) ConfigFormDialog(state, viewModel)
    if (state.sunatFormOpen) SunatFormDialog(state, viewModel)
    if (state.pinDialogOpen) PinDialog(state, viewModel)
    StaffCreateDialog(state, viewModel)
    StaffEditDialog(state, viewModel)
    if (state.branchFormOpen) BranchFormDialog(state, viewModel)
    if (state.seriesFormOpen) SeriesFormDialog(state, viewModel)
    state.deleteBranchId?.let {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteBranch,
            title = { Text("Eliminar sucursal") },
            text = { Text("¿Eliminar esta sucursal?") },
            confirmButton = { BendeyPrimaryButton("Eliminar", viewModel::confirmDeleteBranch, enabled = !state.actionLoading) },
            dismissButton = { TextButton(onClick = viewModel::dismissDeleteBranch) { Text("Cancelar") } },
        )
    }
    state.deleteSeriesId?.let {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteSeries,
            title = { Text("Eliminar serie") },
            text = { Text("¿Eliminar esta serie de comprobante?") },
            confirmButton = { BendeyPrimaryButton("Eliminar", viewModel::confirmDeleteSeries, enabled = !state.actionLoading) },
            dismissButton = { TextButton(onClick = viewModel::dismissDeleteSeries) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun GeneralTab(state: ConfiguracionUiState, onOpenPrinting: () -> Unit, viewModel: ConfiguracionViewModel) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Empresa", fontWeight = FontWeight.SemiBold)
                    state.config?.let { config ->
                        Text("RUC: ${config.ruc}", style = MaterialTheme.typography.bodySmall)
                        Text(config.businessName, style = MaterialTheme.typography.bodyMedium)
                        if (config.address.isNotBlank()) Text(config.address, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                    }
                    BendeyPrimaryButton("Editar datos de contacto", viewModel::openEditConfig, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("SUNAT / IGV", fontWeight = FontWeight.SemiBold)
                    state.sunat?.let { sunat ->
                        Text("IGV: ${sunat.taxRate}%", style = MaterialTheme.typography.bodySmall)
                        Text(if (sunat.sunatEnabled) "Facturación electrónica activa" else "Facturación electrónica desactivada", style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.sunat?.sunatEnabled == true) {
                        BendeyPrimaryButton("Editar configuración IGV", viewModel::openEditSunat, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Impresoras", fontWeight = FontWeight.SemiBold)
                    BendeyPrimaryButton("Abrir impresoras", onOpenPrinting, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun BranchesTab(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
            BendeyPrimaryButton("Nueva sucursal", viewModel::openCreateBranch, fillWidth = false)
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.branches, key = { it.id }) { branch ->
                BranchCard(branch, viewModel)
            }
        }
    }
}

@Composable
private fun BranchCard(branch: BranchItem, viewModel: ConfiguracionViewModel) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(branch.name, fontWeight = FontWeight.SemiBold)
                if (branch.address.isNotBlank()) Text(branch.address, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (branch.isMain) BendeyStatusChip("Principal", BendeyColors.Primary)
                    BendeyStatusChip(if (branch.active) "Activa" else "Inactiva", if (branch.active) BendeyColors.Success else BendeyColors.OnSurfaceVariant)
                }
            }
            IconButton(onClick = { viewModel.openEditBranch(branch) }) { Icon(Icons.Default.Edit, contentDescription = null) }
            IconButton(onClick = { viewModel.requestDeleteBranch(branch.id) }) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = BendeyColors.Error)
            }
        }
    }
}

@Composable
private fun SeriesTab(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            state.branches.forEach { branch ->
                FilterChip(
                    selected = state.selectedBranchId == branch.id,
                    onClick = { viewModel.selectBranch(branch.id) },
                    label = { Text(branch.name) },
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
            BendeyPrimaryButton("Nueva serie", viewModel::openCreateSeries, fillWidth = false, enabled = state.selectedBranchId != null)
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.series, key = { it.id }) { series ->
                SeriesCard(series, viewModel)
            }
        }
    }
}

@Composable
private fun SeriesCard(series: DocumentSeries, viewModel: ConfiguracionViewModel) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${series.docType} · ${series.series}", fontWeight = FontWeight.SemiBold)
                Text("SUNAT ${series.sunatCode ?: "—"}", style = MaterialTheme.typography.bodySmall)
                BendeyStatusChip(if (series.active) "Activa" else "Inactiva", if (series.active) BendeyColors.Success else BendeyColors.OnSurfaceVariant)
            }
            IconButton(onClick = { viewModel.openEditSeries(series) }) { Icon(Icons.Default.Edit, contentDescription = null) }
            IconButton(onClick = { viewModel.requestDeleteSeries(series.id) }) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = BendeyColors.Error)
            }
        }
    }
}

@Composable private fun ConfigFormDialog(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::dismissEditConfig,
        title = { Text("Datos de contacto") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BendeyTextField(state.configForm.tradeName, { v -> viewModel.updateConfigForm { it.copy(tradeName = v) } }, "Nombre comercial")
                BendeyTextField(state.configForm.businessName, { v -> viewModel.updateConfigForm { it.copy(businessName = v) } }, "Razón social")
                BendeyTextField(state.configForm.address, { v -> viewModel.updateConfigForm { it.copy(address = v) } }, "Dirección", singleLine = false)
                BendeyTextField(state.configForm.phone, { v -> viewModel.updateConfigForm { it.copy(phone = v) } }, "Teléfono")
                BendeyTextField(state.configForm.email, { v -> viewModel.updateConfigForm { it.copy(email = v) } }, "Email")
            }
        },
        confirmButton = { BendeyPrimaryButton(if (state.actionLoading) "Guardando…" else "Guardar", viewModel::saveConfig, enabled = !state.actionLoading) },
        dismissButton = { TextButton(onClick = viewModel::dismissEditConfig) { Text("Cancelar") } },
    )
}

@Composable private fun SunatFormDialog(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::dismissEditSunat,
        title = { Text("Configuración IGV") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BendeyTextField(state.sunatForm.taxRate, { v -> viewModel.updateSunatForm { it.copy(taxRate = v) } }, "Tasa IGV (%)")
                BendeyTextField(state.sunatForm.igvRegime, { v -> viewModel.updateSunatForm { it.copy(igvRegime = v) } }, "Régimen IGV")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Zona de beneficio tributario")
                    Switch(state.sunatForm.taxBenefitZone, { c -> viewModel.updateSunatForm { it.copy(taxBenefitZone = c) } })
                }
            }
        },
        confirmButton = { BendeyPrimaryButton(if (state.actionLoading) "Guardando…" else "Guardar", viewModel::saveSunat, enabled = !state.actionLoading) },
        dismissButton = { TextButton(onClick = viewModel::dismissEditSunat) { Text("Cancelar") } },
    )
}

@Composable private fun PinDialog(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::dismissPinDialog,
        title = { Text("PIN de anulación") },
        text = { BendeyTextField(state.pinValue, viewModel::setPinValue, "Nuevo PIN (4+ dígitos)") },
        confirmButton = { BendeyPrimaryButton(if (state.actionLoading) "Guardando…" else "Guardar", viewModel::savePin, enabled = !state.actionLoading) },
        dismissButton = { TextButton(onClick = viewModel::dismissPinDialog) { Text("Cancelar") } },
    )
}

@Composable private fun BranchFormDialog(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::dismissBranchForm,
        title = { Text(if (state.branchForm.id == null) "Nueva sucursal" else "Editar sucursal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BendeyTextField(state.branchForm.name, { v -> viewModel.updateBranchForm { it.copy(name = v) } }, "Nombre *")
                BendeyTextField(state.branchForm.address, { v -> viewModel.updateBranchForm { it.copy(address = v) } }, "Dirección")
                BendeyTextField(state.branchForm.phone, { v -> viewModel.updateBranchForm { it.copy(phone = v) } }, "Teléfono")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Sucursal principal")
                    Switch(state.branchForm.isMain, { c -> viewModel.updateBranchForm { it.copy(isMain = c) } })
                }
                if (state.branchForm.id != null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Activa")
                        Switch(state.branchForm.active, { c -> viewModel.updateBranchForm { it.copy(active = c) } })
                    }
                }
            }
        },
        confirmButton = { BendeyPrimaryButton(if (state.actionLoading) "Guardando…" else "Guardar", viewModel::saveBranch, enabled = !state.actionLoading) },
        dismissButton = { TextButton(onClick = viewModel::dismissBranchForm) { Text("Cancelar") } },
    )
}

@Composable private fun SeriesFormDialog(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::dismissSeriesForm,
        title = { Text(if (state.seriesForm.id == null) "Nueva serie" else "Editar serie") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BendeyTextField(state.seriesForm.docType, { v -> viewModel.updateSeriesForm { it.copy(docType = v) } }, "Tipo documento")
                BendeyTextField(state.seriesForm.series, { v -> viewModel.updateSeriesForm { it.copy(series = v) } }, "Serie *")
                BendeyTextField(state.seriesForm.sunatCode, { v -> viewModel.updateSeriesForm { it.copy(sunatCode = v) } }, "Código SUNAT")
                if (state.seriesForm.id != null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Activa")
                        Switch(state.seriesForm.active, { c -> viewModel.updateSeriesForm { it.copy(active = c) } })
                    }
                }
            }
        },
        confirmButton = { BendeyPrimaryButton(if (state.actionLoading) "Guardando…" else "Guardar", viewModel::saveSeries, enabled = !state.actionLoading) },
        dismissButton = { TextButton(onClick = viewModel::dismissSeriesForm) { Text("Cancelar") } },
    )
}
