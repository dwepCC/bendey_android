package com.bendey.restaurant.feature.configuracion

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.components.BendeySectionTitle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import com.bendey.restaurant.core.designsystem.components.BendeyCard
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.billing.DocumentSeries
import com.bendey.restaurant.core.domain.catalog.BranchItem
import com.bendey.restaurant.core.ui.components.BendeyAlertDialog
import com.bendey.restaurant.core.ui.components.BendeyLazyColumn
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyHorizontalScrollRow
import com.bendey.restaurant.core.ui.components.BendeyIconButton
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.components.BendeySwitchRow
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import com.bendey.restaurant.core.ui.layout.rememberUseAdaptiveTwoPane

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionScreen(
    onBack: () -> Unit = {},
    onOpenPrinting: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ConfiguracionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val useTwoPane = rememberUseAdaptiveTwoPane()

    PullToRefreshBox(isRefreshing = state.loading, onRefresh = viewModel::refresh, modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BendeyScreenToolbar(
                title = "Configuración",
                subtitle = state.config?.tradeName?.ifBlank { state.config?.businessName }.orEmpty(),
                onBack = onBack,
                actions = {
                    BendeyIconButton(
                        onClick = viewModel::refresh,
                        icon = Icons.Default.Refresh,
                        contentDescription = "Actualizar",
                    )
                },
            )
            if (useTwoPane) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = BendeySpacing.sm, horizontal = BendeySpacing.xs),
                        verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
                    ) {
                        ConfigTab.entries.forEach { tab ->
                            BendeyFilterChip(
                                selected = state.tab == tab,
                                onClick = { viewModel.setTab(tab) },
                                text = tab.label,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    VerticalDivider()
                    Column(Modifier.weight(1f)) {
                        state.error?.let { Text(it, color = BendeyColors.Error, modifier = Modifier.padding(BendeySpacing.md)) }
                        ConfigTabContent(state, viewModel, onOpenPrinting, useTwoPane)
                    }
                }
            } else {
                BendeyHorizontalScrollRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        horizontal = BendeySpacing.sm,
                        vertical = BendeySpacing.xxs,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                ) {
                    ConfigTab.entries.forEach { tab ->
                        BendeyFilterChip(
                            selected = state.tab == tab,
                            onClick = { viewModel.setTab(tab) },
                            text = tab.label,
                        )
                    }
                }
                state.error?.let { Text(it, color = BendeyColors.Error, modifier = Modifier.padding(BendeySpacing.md)) }
                ConfigTabContent(
                    state = state,
                    viewModel = viewModel,
                    onOpenPrinting = onOpenPrinting,
                    useTwoPane = useTwoPane,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            }
        }
    }

    if (state.configFormOpen) ConfigFormDialog(state, viewModel)
    if (state.sunatFormOpen) SunatFormDialog(state, viewModel)
    if (state.pinDialogOpen) PinDialog(state, viewModel)
    if (!useTwoPane || state.tab != ConfigTab.OPERACION) {
        StaffCreateDialog(state, viewModel)
        StaffEditDialog(state, viewModel)
    }
    if (state.branchFormOpen && (!useTwoPane || state.tab != ConfigTab.BRANCHES)) {
        BranchFormDialog(state, viewModel)
    }
    if (state.seriesFormOpen && (!useTwoPane || state.tab != ConfigTab.SERIES)) {
        SeriesFormDialog(state, viewModel)
    }
    state.deleteBranchId?.let {
        BendeyAlertDialog(
            onDismissRequest = viewModel::dismissDeleteBranch,
            title = "Eliminar sucursal",
            message = "¿Eliminar esta sucursal?",
            confirmText = "Eliminar",
            onConfirm = viewModel::confirmDeleteBranch,
            confirmEnabled = !state.actionLoading,
            onDismiss = viewModel::dismissDeleteBranch,
        )
    }
    state.deleteSeriesId?.let {
        BendeyAlertDialog(
            onDismissRequest = viewModel::dismissDeleteSeries,
            title = "Eliminar serie",
            message = "¿Eliminar esta serie de comprobante?",
            confirmText = "Eliminar",
            onConfirm = viewModel::confirmDeleteSeries,
            confirmEnabled = !state.actionLoading,
            onDismiss = viewModel::dismissDeleteSeries,
        )
    }
}

@Composable
private fun ConfigTabContent(
    state: ConfiguracionUiState,
    viewModel: ConfiguracionViewModel,
    onOpenPrinting: () -> Unit,
    useTwoPane: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state.tab) {
        ConfigTab.GENERAL -> GeneralTab(state, onOpenPrinting, viewModel, modifier)
        ConfigTab.OPERACION -> OperacionTab(state, viewModel, useTwoPane, modifier)
        ConfigTab.BRANCHES -> BranchesTab(state, viewModel, useTwoPane, modifier)
        ConfigTab.SERIES -> SeriesTab(state, viewModel, useTwoPane, modifier)
    }
}

@Composable
private fun GeneralTab(
    state: ConfiguracionUiState,
    onOpenPrinting: () -> Unit,
    viewModel: ConfiguracionViewModel,
    modifier: Modifier = Modifier,
) {
    BendeyLazyColumn(state = rememberLazyListState(),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(BendeySpacing.md),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
    ) {
        item {
            BendeyManagementCard {
                Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                    BendeySectionTitle(text = "Empresa")
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
                    BendeySectionTitle(text = "SUNAT / IGV")
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
                    BendeySectionTitle(text = "Impresoras")
                    BendeyPrimaryButton("Abrir impresoras", onOpenPrinting, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun BranchesTab(
    state: ConfiguracionUiState,
    viewModel: ConfiguracionViewModel,
    useTwoPane: Boolean,
    modifier: Modifier = Modifier,
) {
    if (useTwoPane) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = BendeySpacing.xs),
        ) {
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight(),
            ) {
                Row(Modifier.fillMaxWidth().padding(BendeySpacing.md), horizontalArrangement = Arrangement.End) {
                    if (state.canManageRestaurantSettings) {
                        BendeyPrimaryButton("Nueva sucursal", viewModel::openCreateBranch, fillWidth = false)
                    }
                }
                BendeyLazyColumn(state = rememberLazyListState(),
                    contentPadding = PaddingValues(horizontal = BendeySpacing.md),
                    verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                    modifier = Modifier.weight(1f),
                ) {
                    items(state.branches, key = { it.id }) { branch ->
                        BranchCard(
                            branch = branch,
                            viewModel = viewModel,
                            canManage = state.canManageRestaurantSettings,
                            selected = state.branchFormOpen && state.branchForm.id == branch.id,
                        )
                    }
                }
            }
            VerticalDivider()
            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
                    .padding(BendeySpacing.md),
            ) {
                if (state.branchFormOpen) {
                    BranchFormPane(state, viewModel)
                } else {
                    com.bendey.restaurant.core.ui.components.BendeyEmptyState(
                        title = "Selecciona o crea una sucursal",
                        description = "Edita datos sin ocultar la lista de sucursales.",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(BendeySpacing.md), horizontalArrangement = Arrangement.End) {
                if (state.canManageRestaurantSettings) {
                    BendeyPrimaryButton("Nueva sucursal", viewModel::openCreateBranch, fillWidth = false)
                }
            }
            BendeyLazyColumn(state = rememberLazyListState(),
                contentPadding = PaddingValues(horizontal = BendeySpacing.md),
                verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                items(state.branches, key = { it.id }) { branch ->
                    BranchCard(branch, viewModel, state.canManageRestaurantSettings)
                }
            }
        }
    }
}

@Composable
private fun BranchCard(
    branch: BranchItem,
    viewModel: ConfiguracionViewModel,
    canManage: Boolean,
    selected: Boolean = false,
) {
    BendeyCard(
        containerColor = if (selected) BendeyColors.PrimaryContainer else BendeyColors.Surface,
        contentPadding = PaddingValues(BendeySpacing.cardPadding),
    ) {
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
                BendeyIconButton(
                    onClick = { viewModel.openEditBranch(branch) },
                    icon = Icons.Default.Edit,
                    contentDescription = "Editar sucursal",
                )
                if (!branch.isMain) {
                    BendeyIconButton(
                        onClick = { viewModel.requestDeleteBranch(branch.id) },
                        icon = Icons.Default.Delete,
                        contentDescription = "Eliminar sucursal",
                        tint = BendeyColors.Error,
                    )
                }
            }
        }
    }
}

@Composable
private fun SeriesTab(
    state: ConfiguracionUiState,
    viewModel: ConfiguracionViewModel,
    useTwoPane: Boolean,
    modifier: Modifier = Modifier,
) {
    if (useTwoPane) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = BendeySpacing.xs),
        ) {
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight(),
            ) {
                BendeyHorizontalScrollRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        horizontal = BendeySpacing.md,
                        vertical = BendeySpacing.xs,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                ) {
                    state.branches.forEach { branch ->
                        BendeyFilterChip(
                            selected = state.selectedBranchId == branch.id,
                            onClick = { viewModel.selectBranch(branch.id) },
                            label = { Text(branch.name) },
                        )
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = BendeySpacing.md), horizontalArrangement = Arrangement.End) {
                    if (state.canManageRestaurantSettings) {
                        BendeyPrimaryButton(
                            "Nueva serie",
                            viewModel::openCreateSeries,
                            fillWidth = false,
                            enabled = state.selectedBranchId != null,
                        )
                    }
                }
                BendeyLazyColumn(state = rememberLazyListState(),
                    contentPadding = PaddingValues(BendeySpacing.md),
                    verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                    modifier = Modifier.weight(1f),
                ) {
                    items(state.series, key = { it.id }) { series ->
                        SeriesCard(
                            series = series,
                            viewModel = viewModel,
                            canManage = state.canManageRestaurantSettings,
                            sunatEnabled = state.sunat?.sunatEnabled == true,
                            selected = state.seriesFormOpen && state.seriesForm.id == series.id,
                        )
                    }
                }
            }
            VerticalDivider()
            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
                    .padding(BendeySpacing.md),
            ) {
                if (state.seriesFormOpen) {
                    SeriesFormPane(state, viewModel)
                } else {
                    com.bendey.restaurant.core.ui.components.BendeyEmptyState(
                        title = "Selecciona o crea una serie",
                        description = "Configura series de comprobantes sin ocultar la lista.",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            BendeyHorizontalScrollRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = BendeySpacing.md,
                    vertical = BendeySpacing.xs,
                ),
                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
            ) {
                state.branches.forEach { branch ->
                    BendeyFilterChip(
                        selected = state.selectedBranchId == branch.id,
                        onClick = { viewModel.selectBranch(branch.id) },
                        label = { Text(branch.name) },
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = BendeySpacing.md), horizontalArrangement = Arrangement.End) {
                if (state.canManageRestaurantSettings) {
                    BendeyPrimaryButton("Nueva serie", viewModel::openCreateSeries, fillWidth = false, enabled = state.selectedBranchId != null)
                }
            }
            BendeyLazyColumn(state = rememberLazyListState(),
                contentPadding = PaddingValues(BendeySpacing.md),
                verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                items(state.series, key = { it.id }) { series ->
                    SeriesCard(series, viewModel, state.canManageRestaurantSettings, state.sunat?.sunatEnabled == true)
                }
            }
        }
    }
}

@Composable
private fun SeriesCard(
    series: DocumentSeries,
    viewModel: ConfiguracionViewModel,
    canManage: Boolean,
    sunatEnabled: Boolean,
    selected: Boolean = false,
) {
    BendeyCard(
        containerColor = if (selected) BendeyColors.PrimaryContainer else BendeyColors.Surface,
        contentPadding = PaddingValues(BendeySpacing.cardPadding),
    ) {
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
                BendeyIconButton(
                    onClick = { viewModel.openEditSeries(series) },
                    icon = Icons.Default.Edit,
                    contentDescription = "Editar serie",
                )
                if (series.canDelete) {
                    BendeyIconButton(
                        onClick = { viewModel.requestDeleteSeries(series.id) },
                        icon = Icons.Default.Delete,
                        contentDescription = "Eliminar serie",
                        tint = BendeyColors.Error,
                    )
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

@Composable private fun BranchFormFields(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
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

@Composable private fun BranchFormPane(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    Column(Modifier.fillMaxSize()) {
        Text(
            if (state.branchForm.id == null) "Nueva sucursal" else "Editar sucursal",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = BendeySpacing.sm),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
        ) {
            BranchFormFields(state, viewModel)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = BendeySpacing.sm))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {
            BendeyTextButton(
                text = "Cancelar",
                onClick = viewModel::dismissBranchForm,
                enabled = !state.actionLoading,
                modifier = Modifier.weight(1f),
            )
            BendeyPrimaryButton(
                text = if (state.actionLoading) "Guardando…" else "Guardar",
                onClick = viewModel::saveBranch,
                enabled = !state.actionLoading,
                modifier = Modifier.weight(1f),
            )
        }
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
        BranchFormFields(state, viewModel)
    }
}

@Composable private fun SeriesFormFields(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    val form = state.seriesForm
    val sunatEnabled = state.sunat?.sunatEnabled == true
    val fieldsLocked = form.locked && form.id != null
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

@Composable private fun SeriesFormPane(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    Column(Modifier.fillMaxSize()) {
        Text(
            if (state.seriesForm.id == null) "Nueva serie" else "Editar serie",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = BendeySpacing.sm),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
        ) {
            SeriesFormFields(state, viewModel)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = BendeySpacing.sm))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {
            BendeyTextButton(
                text = "Cancelar",
                onClick = viewModel::dismissSeriesForm,
                enabled = !state.actionLoading,
                modifier = Modifier.weight(1f),
            )
            BendeyPrimaryButton(
                text = if (state.actionLoading) "Guardando…" else "Guardar",
                onClick = viewModel::saveSeries,
                enabled = !state.actionLoading,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable private fun SeriesFormDialog(state: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    BendeyFormDialog(
        onDismissRequest = viewModel::dismissSeriesForm,
        title = if (state.seriesForm.id == null) "Nueva serie" else "Editar serie",
        confirmText = if (state.actionLoading) "Guardando…" else "Guardar",
        onConfirm = viewModel::saveSeries,
        onDismiss = viewModel::dismissSeriesForm,
        confirmEnabled = !state.actionLoading,
        loading = state.actionLoading,
    ) {
        SeriesFormFields(state, viewModel)
    }
}
