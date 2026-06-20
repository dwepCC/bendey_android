package com.bendey.restaurant.feature.configuracion

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar

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
            state.error?.let { Text(it, color = BendeyColors.Error, modifier = Modifier.padding(16.dp)) }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Empresa", fontWeight = FontWeight.SemiBold)
                            state.config?.let { config ->
                                Text("RUC: ${config.ruc}", style = MaterialTheme.typography.bodySmall)
                                Text(config.businessName, style = MaterialTheme.typography.bodyMedium)
                                if (config.address.isNotBlank()) Text(config.address, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                                Text("${config.phone} · ${config.email}", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
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
                                Text("Régimen: ${sunat.igvRegime.ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    if (sunat.taxBenefitZone) "Zona de beneficio tributario: sí" else "Zona de beneficio tributario: no",
                                    style = MaterialTheme.typography.bodySmall,
                                )
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
                            Text("Operación restaurante", fontWeight = FontWeight.SemiBold)
                            Text(
                                if (state.settings?.hasDeletionPin == true) "PIN de anulación configurado" else "Sin PIN de anulación",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            BendeyPrimaryButton("Configurar PIN de anulación", viewModel::openPinDialog, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                item {
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Impresoras", fontWeight = FontWeight.SemiBold)
                            Text("Configuración local de impresoras térmicas", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                            BendeyPrimaryButton("Abrir impresoras", onOpenPrinting, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                if (state.branches.isNotEmpty()) {
                    item { Text("Sucursales", fontWeight = FontWeight.SemiBold) }
                    items(state.branches, key = { it.id }) { branch ->
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface)) {
                            Column(Modifier.padding(14.dp)) {
                                Text(branch.name, fontWeight = FontWeight.SemiBold)
                                if (branch.address.isNotBlank()) Text(branch.address, style = MaterialTheme.typography.bodySmall)
                                if (branch.isMain) Text("Principal", style = MaterialTheme.typography.labelSmall, color = BendeyColors.Primary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.configFormOpen) {
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
                    state.error?.let { Text(it, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = { BendeyPrimaryButton(if (state.actionLoading) "Guardando…" else "Guardar", viewModel::saveConfig, enabled = !state.actionLoading) },
            dismissButton = { TextButton(onClick = viewModel::dismissEditConfig) { Text("Cancelar") } },
        )
    }

    if (state.sunatFormOpen) {
        AlertDialog(
            onDismissRequest = viewModel::dismissEditSunat,
            title = { Text("Configuración IGV") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BendeyTextField(state.sunatForm.taxRate, { v -> viewModel.updateSunatForm { it.copy(taxRate = v) } }, "Tasa IGV (%)")
                    BendeyTextField(state.sunatForm.igvRegime, { v -> viewModel.updateSunatForm { it.copy(igvRegime = v) } }, "Régimen IGV")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Zona de beneficio tributario")
                        Switch(state.sunatForm.taxBenefitZone, { checked -> viewModel.updateSunatForm { it.copy(taxBenefitZone = checked) } })
                    }
                    state.error?.let { Text(it, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = { BendeyPrimaryButton(if (state.actionLoading) "Guardando…" else "Guardar", viewModel::saveSunat, enabled = !state.actionLoading) },
            dismissButton = { TextButton(onClick = viewModel::dismissEditSunat) { Text("Cancelar") } },
        )
    }

    if (state.pinDialogOpen) {
        AlertDialog(
            onDismissRequest = viewModel::dismissPinDialog,
            title = { Text("PIN de anulación") },
            text = {
                Column {
                    BendeyTextField(state.pinValue, viewModel::setPinValue, "Nuevo PIN (4+ dígitos)", singleLine = true)
                    state.error?.let { Text(it, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = { BendeyPrimaryButton(if (state.actionLoading) "Guardando…" else "Guardar", viewModel::savePin, enabled = !state.actionLoading) },
            dismissButton = { TextButton(onClick = viewModel::dismissPinDialog) { Text("Cancelar") } },
        )
    }
}
