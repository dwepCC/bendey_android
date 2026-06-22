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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyChipDefaults
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.billing.DocumentSeries
import com.bendey.restaurant.core.domain.catalog.BranchItem
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.components.BendeySwitchRow
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
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.xxs),
                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
            ) {
                ConfigTab.entries.forEach { tab ->
                    FilterChip(
                        selected = state.tab == tab,
                        onClick = { viewModel.setTab(tab) },
                        label = { Text(tab.label) },
                        colors = BendeyChipDefaults.filterChipColors(),
                        shape = BendeyShapeTokens.chip,
                        border = null,
                    )
                }
            }
            state.error?.let { Text(it, color = BendeyColors.Error, modifier = Modifier.padding(BendeySpacing.md)) }
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
    LazyColumn(
        contentPadding = PaddingValues(BendeySpacing.md),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
    ) {
        item {
            BendeyManagementCard {
                Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                    Text("Empresa", fontWeight = FontWeight.SemiBold)
                    state.config?.let { config ->
                        Text("RUC: ${config.ruc}", style = MaterialTheme.typography.bodySmall)
                        Text(config.businessName, style = MaterialTheme.typography.bodyMedium)
                        if (config.address.isNotBlank()) Text(config.address, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                    }
                    BendeyPrimaryButton("Editar datos de contacto", viewModel::openEditConfig, modifier = Modifier.fillMaxWidth(), enabled = state.canManageRestaurantSettings)
                }
            }
        }
        item {
            BendeyManagementCard {
                Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                    Text("SUNAT / IGV", fontWeight = FontWeight.SemiBold)
                    state.sunat?.let { sunat ->
                        Text("IGV: ${sunat.taxRate}%", style = MaterialTheme.typography.bodySmall)
                        Text(if (sunat.sunatEnabled) "Facturación electrónica activa" else "Facturación electrónica desactivada", style = MaterialTheme.typography.bodySmall)
                    }
                    BendeyPrimaryButton("Editar configuración IGV", viewModel::openEditSunat, modifier = Modifier.fillMaxWidth(), enabled = state.canManageRestaurantSettings)
                }
            }
        }
        item {
            BendeyManagementCard {
                Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
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
        Row(Modifier.fillMaxWidth().padding(BendeySpacing.md), horizontalArrangement = Arrangement.End) {
            if (state.canManageRestaurantSettings) {
                BendeyPrimaryButton("Nueva sucursal", viewModel::openCreateBranch, fillWidth = false)
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = BendeySpacing.md),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
        ) {
            items(state.branches, key = { it.id }) { branch ->
                BranchCard(branch, viewModel, state.canManageRestaurantSettings)
            }
        }
    }
}

@Composable
private fun BranchCard(branch: BranchItem, viewModel: ConfiguracionViewModel, canManage: Boolean) {
    BendeyManagementCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(branch.name, fontWeight = FontWeight.SemiBold)
                if (branch.address.isNotBlank()) Text(branch.address, style = MaterialTheme.typography.bodySmall)
                if (branch.fiscalDomicileCode.isNotBlank()) {
                    Text("Domicilio fiscal: ${branch.fiscalDomicileCode}", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                    if (branch.isMain) BendeyStatusChip("Principal", BendeyColors.Primary)
                    BendeyStatusChip(if (branch.active) "Activa" else "Inactiva", if (branch.active) BendeyColors.Success else BendeyColors.OnSurfaceVariant)
                }
            }
            if (canManage) {
                IconButton(onClick = { viewModel.openEditBranch(branch) }) { Icon(Icons.Default.Edit, contentDescription = null) }
                if (!branch.isMain) {
                    IconButton(onClick = { viewModel.requestDeleteBranch(branch.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = BendeyColors.Error)
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesTab(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
        ) {
            state.branches.forEach { branch ->
                FilterChip(
                    selected = state.selectedBranchId == branch.id,
                    onClick = { viewModel.selectBranch(branch.id) },
                    label = { Text(branch.name) },
                    colors = BendeyChipDefaults.filterChipColors(),
                    shape = BendeyShapeTokens.chip,
                    border = null,
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = BendeySpacing.md), horizontalArrangement = Arrangement.End) {
            if (state.canManageRestaurantSettings) {
                BendeyPrimaryButton("Nueva serie", viewModel::openCreateSeries, fillWidth = false, enabled = state.selectedBranchId != null)
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(BendeySpacing.md),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
        ) {
            items(state.series, key = { it.id }) { series ->
                SeriesCard(series, viewModel, state.canManageRestaurantSettings, state.sunat?.sunatEnabled == true)
            }
        }
    }
}

@Composable
private fun SeriesCard(series: DocumentSeries, viewModel: ConfiguracionViewModel, canManage: Boolean, sunatEnabled: Boolean) {
    BendeyManagementCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${series.docType} · ${series.series}", fontWeight = FontWeight.SemiBold)
                Text("SUNAT ${series.sunatCode ?: "—"} · Corr. ${series.currentNumber}", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                    BendeyStatusChip(if (series.active) "Activa" else "Inactiva", if (series.active) BendeyColors.Success else BendeyColors.OnSurfaceVariant)
                    if (series.locked) BendeyStatusChip("En uso", BendeyColors.Warning)
                }
            }
            if (canManage) {
                IconButton(onClick = { viewModel.openEditSeries(series) }) { Icon(Icons.Default.Edit, contentDescription = null) }
                if (series.canDelete) {
                    IconButton(onClick = { viewModel.requestDeleteSeries(series.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = BendeyColors.Error)
                    }
                }
            }
        }
    }
}

@Composable private fun ConfigFormDialog(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    BendeyFormDialog(
        onDismissRequest = viewModel::dismissEditConfig,
        title = "Datos de contacto",
        confirmText = if (state.actionLoading) "Guardando…" else "Guardar",
        onConfirm = viewModel::saveConfig,
        onDismiss = viewModel::dismissEditConfig,
        confirmEnabled = !state.actionLoading,
        loading = state.actionLoading,
    ) {
        BendeyTextField(state.configForm.tradeName, { v -> viewModel.updateConfigForm { it.copy(tradeName = v) } }, "Nombre comercial")
        BendeyTextField(state.configForm.address, { v -> viewModel.updateConfigForm { it.copy(address = v) } }, "Dirección", singleLine = false)
        BendeyTextField(state.configForm.phone, { v -> viewModel.updateConfigForm { it.copy(phone = v) } }, "Teléfono")
        BendeyTextField(state.configForm.email, { v -> viewModel.updateConfigForm { it.copy(email = v) } }, "Email")
    }
}

@Composable private fun SunatFormDialog(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    BendeyFormDialog(
        onDismissRequest = viewModel::dismissEditSunat,
        title = "Configuración IGV",
        confirmText = if (state.actionLoading) "Guardando…" else "Guardar",
        onConfirm = viewModel::saveSunat,
        onDismiss = viewModel::dismissEditSunat,
        confirmEnabled = !state.actionLoading,
        loading = state.actionLoading,
    ) {
        BendeyTextField(state.sunatForm.taxRate, { v -> viewModel.updateSunatForm { it.copy(taxRate = v) } }, "Tasa IGV (%)")
        BendeyTextField(state.sunatForm.igvRegime, { v -> viewModel.updateSunatForm { it.copy(igvRegime = v) } }, "Régimen IGV")
        BendeySwitchRow(
            label = "Zona de beneficio tributario",
            checked = state.sunatForm.taxBenefitZone,
            onCheckedChange = { checked -> viewModel.updateSunatForm { it.copy(taxBenefitZone = checked) } },
        )
    }
}

@Composable private fun PinDialog(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    BendeyFormDialog(
        onDismissRequest = viewModel::dismissPinDialog,
        title = "PIN de anulación",
        confirmText = if (state.actionLoading) "Guardando…" else "Guardar",
        onConfirm = viewModel::savePin,
        onDismiss = viewModel::dismissPinDialog,
        confirmEnabled = !state.actionLoading,
        loading = state.actionLoading,
    ) {
        BendeyTextField(state.pinValue, { v -> viewModel.setPinValue(v.filter { it.isDigit() }.take(6)) }, "Nuevo PIN (4-6 dígitos)")
    }
}

@Composable private fun BranchFormDialog(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    BendeyFormDialog(
        onDismissRequest = viewModel::dismissBranchForm,
        title = if (state.branchForm.id == null) "Nueva sucursal" else "Editar sucursal",
        confirmText = if (state.actionLoading) "Guardando…" else "Guardar",
        onConfirm = viewModel::saveBranch,
        onDismiss = viewModel::dismissBranchForm,
        confirmEnabled = !state.actionLoading,
        loading = state.actionLoading,
    ) {
        BendeyTextField(state.branchForm.name, { v -> viewModel.updateBranchForm { it.copy(name = v) } }, "Nombre *")
        BendeyTextField(state.branchForm.address, { v -> viewModel.updateBranchForm { it.copy(address = v) } }, "Dirección")
        BendeyTextField(state.branchForm.phone, { v -> viewModel.updateBranchForm { it.copy(phone = v) } }, "Teléfono")
        BendeyTextField(state.branchForm.fiscalDomicileCode, { v -> viewModel.updateBranchForm { it.copy(fiscalDomicileCode = v) } }, "Código domicilio fiscal")
        BendeySwitchRow(
            label = "Sucursal principal",
            checked = state.branchForm.isMain,
            onCheckedChange = { checked -> viewModel.updateBranchForm { it.copy(isMain = checked) } },
        )
        if (state.branchForm.id != null) {
            BendeySwitchRow(
                label = "Activa",
                checked = state.branchForm.active,
                onCheckedChange = { checked -> viewModel.updateBranchForm { it.copy(active = checked) } },
            )
        }
    }
}

@Composable private fun SeriesFormDialog(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    val form = state.seriesForm
    val sunatEnabled = state.sunat?.sunatEnabled == true
    val fieldsLocked = form.locked && form.id != null
    BendeyFormDialog(
        onDismissRequest = viewModel::dismissSeriesForm,
        title = if (form.id == null) "Nueva serie" else "Editar serie",
        confirmText = if (state.actionLoading) "Guardando…" else "Guardar",
        onConfirm = viewModel::saveSeries,
        onDismiss = viewModel::dismissSeriesForm,
        confirmEnabled = !state.actionLoading,
        loading = state.actionLoading,
    ) {
        if (!sunatEnabled) {
            Text("Sin FE: solo series SUNAT 00", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
        }
        BendeyTextField(form.docType, { v -> viewModel.updateSeriesForm { it.copy(docType = v) } }, "Tipo documento", enabled = !fieldsLocked)
        BendeyTextField(form.series, { v -> viewModel.updateSeriesForm { it.copy(series = v) } }, "Serie *", enabled = !fieldsLocked)
        BendeyTextField(
            form.sunatCode,
            { v -> viewModel.updateSeriesForm { it.copy(sunatCode = v) } },
            "Código SUNAT",
            enabled = !fieldsLocked && sunatEnabled,
        )
        if (form.id != null) {
            BendeyTextField(
                form.currentNumber.toString(),
                { v -> viewModel.updateSeriesForm { it.copy(currentNumber = v.toIntOrNull() ?: 0) } },
                "Correlativo",
                enabled = !fieldsLocked,
            )
            BendeySwitchRow(
                label = "Activa",
                checked = form.active,
                onCheckedChange = { checked -> viewModel.updateSeriesForm { it.copy(active = checked) } },
            )
        }
    }
}
