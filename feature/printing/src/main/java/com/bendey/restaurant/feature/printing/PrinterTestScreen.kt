package com.bendey.restaurant.feature.printing

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.data.printer.PrinterSlot
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.domain.products.PreparationArea
import com.bendey.restaurant.core.ui.components.BendeyLoadingOverlay
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.layout.bendeySafeDrawingPadding
import com.bendey.restaurant.platform.printing.transport.BluetoothDeviceInfo
import com.bendey.restaurant.platform.printing.transport.PrinterConnectionType

@Composable
fun PrinterTestScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PrinterTestViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ ->
        viewModel.refreshPairedDevices()
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                ),
            )
        }
    }

    Column(modifier = modifier.fillMaxSize().bendeySafeDrawingPadding()) {
        BendeyScreenToolbar(
            title = "Impresoras",
            subtitle = "Comandas · Precuenta · Documentos",
            onBack = onBack,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Impresora a configurar", style = MaterialTheme.typography.titleMedium)
            if (state.selectedSlot == PrinterSlot.COMANDAS && state.editingAreaKey != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Área: ${PreparationArea.fromApi(state.editingAreaKey).label}",
                        style = MaterialTheme.typography.labelLarge,
                        color = BendeyColors.Primary,
                    )
                    BendeyPrimaryButton("Volver a default", viewModel::backToDefaultComandaPrinter)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrinterSlot.entries.forEach { slot ->
                    FilterChip(
                        selected = state.selectedSlot == slot,
                        onClick = { viewModel.selectSlot(slot) },
                        label = {
                            Text(
                                when (slot) {
                                    PrinterSlot.COMANDAS -> "Comandas"
                                    PrinterSlot.PRECUENTA -> "Precuenta"
                                    PrinterSlot.DOCUMENTOS -> "Documentos"
                                },
                            )
                        },
                    )
                }
            }
            Text("Conexión", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.connectionType == PrinterConnectionType.BLUETOOTH,
                    onClick = { viewModel.setConnectionType(PrinterConnectionType.BLUETOOTH) },
                    label = { Text("Bluetooth") },
                )
                FilterChip(
                    selected = state.connectionType == PrinterConnectionType.TCP,
                    onClick = { viewModel.setConnectionType(PrinterConnectionType.TCP) },
                    label = { Text("TCP/IP") },
                )
            }

            when (state.connectionType) {
                PrinterConnectionType.BLUETOOTH -> {
                    Text("Impresoras emparejadas", style = MaterialTheme.typography.labelLarge)
                    if (state.pairedDevices.isEmpty()) {
                        Text(
                            "No hay dispositivos. Empareje la impresora en Ajustes Android.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    state.pairedDevices.forEach { device ->
                        DeviceRow(
                            device = device,
                            selected = state.bluetoothAddress == device.address,
                            onSelect = { viewModel.setBluetoothAddress(device.address) },
                        )
                    }
                    BendeyPrimaryButton(
                        text = "CONECTAR BLUETOOTH",
                        onClick = viewModel::connectBluetooth,
                        enabled = state.bluetoothAddress.isNotBlank(),
                    )
                }
                PrinterConnectionType.TCP -> {
                    BendeyTextField(
                        value = state.tcpHost,
                        onValueChange = viewModel::setTcpHost,
                        label = "Host / IP",
                    )
                    BendeyTextField(
                        value = state.tcpPort,
                        onValueChange = viewModel::setTcpPort,
                        label = "Puerto (9100)",
                    )
                }
            }

            Text("Auto-impresión", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.autoPrintComandas,
                    onClick = { viewModel.setAutoPrintComandas(!state.autoPrintComandas) },
                    label = { Text("Comandas") },
                )
                FilterChip(
                    selected = state.autoPrintDocuments,
                    onClick = { viewModel.setAutoPrintDocuments(!state.autoPrintDocuments) },
                    label = { Text("Documentos") },
                )
            }

            if (state.selectedSlot == PrinterSlot.COMANDAS && state.editingAreaKey == null) {
                Text("Impresión por área", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Opcional: asigne una impresora distinta por cocina, bar, etc. Si no configura un área, usa la impresora por defecto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PreparationArea.entries.filter { it != PreparationArea.NONE }.forEach { area ->
                    val custom = state.comandasByArea[area.apiValue]
                    val hasCustom = custom?.isConfigured == true
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(area.label, style = MaterialTheme.typography.bodyMedium)
                            BendeyStatusChip(
                                label = if (hasCustom) "Impresora propia" else "Por defecto",
                                accentColor = if (hasCustom) BendeyColors.AccentTeal else BendeyColors.OnSurfaceVariant,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            BendeyPrimaryButton("Config", { viewModel.editComandaArea(area.apiValue) })
                            if (hasCustom) {
                                BendeyPrimaryButton("Quitar", { viewModel.clearComandaArea(area.apiValue) })
                            }
                            BendeyPrimaryButton("Probar", { viewModel.printComandaAreaSample(area.apiValue) })
                        }
                    }
                }
            }

            Text("Impresión de prueba", style = MaterialTheme.typography.titleLarge)
            BendeyPrimaryButton(
                text = "IMPRIMIR COMANDA DE PRUEBA",
                onClick = viewModel::printComandaSample,
            )
            BendeyPrimaryButton(
                text = "IMPRIMIR PRECUENTA DE PRUEBA",
                onClick = viewModel::printPrecuentaSample,
            )
            BendeyPrimaryButton(
                text = "IMPRIMIR DOCUMENTO DE PRUEBA",
                onClick = viewModel::printDocumentSample,
            )

            AnimatedVisibility(visible = state.statusMessage != null, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = state.statusMessage.orEmpty(),
                    color = BendeyColors.Success,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            AnimatedVisibility(visible = state.error != null, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = state.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
    BendeyLoadingOverlay(visible = state.loading)
}

@Composable
private fun DeviceRow(
    device: BluetoothDeviceInfo,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column {
            Text(device.name, style = MaterialTheme.typography.bodyLarge)
            Text(device.address, style = MaterialTheme.typography.labelSmall)
        }
    }
}
