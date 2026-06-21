package com.bendey.restaurant.feature.printing

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.data.printer.PrinterSlot
import com.bendey.restaurant.core.data.printer.PrinterSlotConfig
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.domain.products.PreparationArea
import com.bendey.restaurant.core.ui.components.BendeyLoadingOverlay
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.layout.bendeySafeDrawingPadding
import com.bendey.restaurant.platform.printing.escpos.ComandaTextSize
import com.bendey.restaurant.platform.printing.escpos.PaperWidthMm
import com.bendey.restaurant.platform.printing.transport.BluetoothDeviceInfo
import com.bendey.restaurant.platform.printing.transport.PrinterConnectionType

private val preparationAreasForPrint =
    PreparationArea.entries.filter { it != PreparationArea.NONE }

@Composable
fun PrinterTestScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PrinterTestViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var areasExpanded by remember { mutableStateOf(false) }

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
            subtitle = when (state.selectedSlot) {
                PrinterSlot.COMANDAS -> if (state.editingAreaKey != null) {
                    "Comandas · ${PreparationArea.fromApi(state.editingAreaKey).label}"
                } else {
                    "Comandas · impresora por defecto"
                }
                PrinterSlot.PRECUENTA -> "Precuenta"
                PrinterSlot.DOCUMENTOS -> "Documentos"
            },
            onBack = onBack,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrinterSlot.entries.forEach { slot ->
                    FilterChip(
                        selected = state.selectedSlot == slot,
                        onClick = {
                            areasExpanded = false
                            viewModel.selectSlot(slot)
                        },
                        label = {
                            Text(
                                when (slot) {
                                    PrinterSlot.COMANDAS -> "Comandas"
                                    PrinterSlot.PRECUENTA -> "Precuenta"
                                    PrinterSlot.DOCUMENTOS -> "Documentos"
                                },
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                    )
                }
            }

            if (state.selectedSlot == PrinterSlot.COMANDAS && state.editingAreaKey != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BendeyColors.PrimaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Configurando área",
                                style = MaterialTheme.typography.labelSmall,
                                color = BendeyColors.OnSurfaceVariant,
                            )
                            Text(
                                PreparationArea.fromApi(state.editingAreaKey).label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        TextButton(onClick = viewModel::backToDefaultComandaPrinter) {
                            Text("Volver al default")
                        }
                    }
                }
            }

            PrinterConfigCard(
                title = when {
                    state.selectedSlot == PrinterSlot.COMANDAS && state.editingAreaKey == null ->
                        "Impresora por defecto"
                    state.selectedSlot == PrinterSlot.COMANDAS ->
                        "Impresora del área"
                    state.selectedSlot == PrinterSlot.PRECUENTA -> "Impresora de precuenta"
                    else -> "Impresora de documentos"
                },
                subtitle = when (state.selectedSlot) {
                    PrinterSlot.COMANDAS -> if (state.editingAreaKey == null) {
                        "Productos sin área o áreas sin impresora dedicada"
                    } else {
                        "Solo comandas de ${PreparationArea.fromApi(state.editingAreaKey).label}"
                    }
                    PrinterSlot.PRECUENTA -> "Tickets de precuenta antes de cobrar"
                    PrinterSlot.DOCUMENTOS -> "Boletas, facturas y notas de venta"
                },
                connectionType = state.connectionType,
                onConnectionType = viewModel::setConnectionType,
                pairedDevices = state.pairedDevices,
                bluetoothAddress = state.bluetoothAddress,
                onSelectBluetooth = viewModel::setBluetoothAddress,
                tcpHost = state.tcpHost,
                tcpPort = state.tcpPort,
                onTcpHost = viewModel::setTcpHost,
                onTcpPort = viewModel::setTcpPort,
                onConnectBluetooth = viewModel::connectBluetooth,
                paperWidth = state.paperWidth,
                onPaperWidth = viewModel::setPaperWidth,
            )

            if (state.selectedSlot == PrinterSlot.COMANDAS && state.editingAreaKey == null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Tamaño texto comanda", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = state.comandaTextSize == ComandaTextSize.DEFAULT,
                                onClick = { viewModel.setComandaTextSize(ComandaTextSize.DEFAULT) },
                                label = { Text("Grande", style = MaterialTheme.typography.labelMedium) },
                            )
                            FilterChip(
                                selected = state.comandaTextSize == ComandaTextSize.MEDIANO,
                                onClick = { viewModel.setComandaTextSize(ComandaTextSize.MEDIANO) },
                                label = { Text("Mediano", style = MaterialTheme.typography.labelMedium) },
                            )
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Auto-impresión", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.autoPrintComandas,
                            onClick = { viewModel.setAutoPrintComandas(!state.autoPrintComandas) },
                            label = { Text("Comandas", style = MaterialTheme.typography.labelMedium) },
                        )
                        FilterChip(
                            selected = state.autoPrintDocuments,
                            onClick = { viewModel.setAutoPrintDocuments(!state.autoPrintDocuments) },
                            label = { Text("Documentos", style = MaterialTheme.typography.labelMedium) },
                        )
                    }
                }
            }

            if (state.selectedSlot == PrinterSlot.COMANDAS && state.editingAreaKey == null) {
                ComandaAreasCard(
                    expanded = areasExpanded,
                    onToggleExpanded = { areasExpanded = !areasExpanded },
                    comandasByArea = state.comandasByArea,
                    defaultConfig = state.let {
                        PrinterSlotConfig(
                            connectionType = it.connectionType,
                            bluetoothAddress = it.bluetoothAddress,
                            tcpHost = it.tcpHost,
                            tcpPort = it.tcpPort.toIntOrNull() ?: 9100,
                            paperWidth = it.paperWidth,
                        )
                    },
                    onConfigureArea = viewModel::editComandaArea,
                    onClearArea = viewModel::clearComandaArea,
                    onTestArea = viewModel::printComandaAreaSample,
                )
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Impresión de prueba", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        when (state.selectedSlot) {
                            PrinterSlot.COMANDAS -> {
                                CompactTestButton("Comanda", viewModel::printComandaSample, Modifier.weight(1f))
                            }
                            PrinterSlot.PRECUENTA -> {
                                CompactTestButton("Precuenta", viewModel::printPrecuentaSample, Modifier.weight(1f))
                            }
                            PrinterSlot.DOCUMENTOS -> {
                                CompactTestButton("Documento", viewModel::printDocumentSample, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = state.statusMessage != null, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = state.statusMessage.orEmpty(),
                    color = BendeyColors.Success,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            AnimatedVisibility(visible = state.error != null, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = state.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
    BendeyLoadingOverlay(visible = state.loading)
}

@Composable
private fun PrinterConfigCard(
    title: String,
    subtitle: String,
    connectionType: PrinterConnectionType,
    onConnectionType: (PrinterConnectionType) -> Unit,
    pairedDevices: List<BluetoothDeviceInfo>,
    bluetoothAddress: String,
    onSelectBluetooth: (String) -> Unit,
    tcpHost: String,
    tcpPort: String,
    onTcpHost: (String) -> Unit,
    onTcpPort: (String) -> Unit,
    onConnectBluetooth: () -> Unit,
    paperWidth: PaperWidthMm,
    onPaperWidth: (PaperWidthMm) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text("Conexión", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = connectionType == PrinterConnectionType.BLUETOOTH,
                    onClick = { onConnectionType(PrinterConnectionType.BLUETOOTH) },
                    label = { Text("Bluetooth", style = MaterialTheme.typography.labelMedium) },
                )
                FilterChip(
                    selected = connectionType == PrinterConnectionType.TCP,
                    onClick = { onConnectionType(PrinterConnectionType.TCP) },
                    label = { Text("Red / IP", style = MaterialTheme.typography.labelMedium) },
                )
            }

            when (connectionType) {
                PrinterConnectionType.BLUETOOTH -> {
                    Text("Dispositivos emparejados", style = MaterialTheme.typography.labelMedium)
                    if (pairedDevices.isEmpty()) {
                        Text(
                            "Empareja la impresora en Ajustes de Android.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        pairedDevices.forEach { device ->
                            DeviceRow(
                                device = device,
                                selected = bluetoothAddress == device.address,
                                onSelect = { onSelectBluetooth(device.address) },
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onConnectBluetooth,
                        enabled = bluetoothAddress.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Conectar Bluetooth", style = MaterialTheme.typography.labelMedium)
                    }
                }
                PrinterConnectionType.TCP -> {
                    BendeyTextField(
                        value = tcpHost,
                        onValueChange = onTcpHost,
                        label = "Host / IP",
                    )
                    BendeyTextField(
                        value = tcpPort,
                        onValueChange = onTcpPort,
                        label = "Puerto (9100)",
                    )
                }
            }

            HorizontalDivider()

            Text("Papel", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = paperWidth == PaperWidthMm.W58,
                    onClick = { onPaperWidth(PaperWidthMm.W58) },
                    label = { Text("58 mm", style = MaterialTheme.typography.labelMedium) },
                )
                FilterChip(
                    selected = paperWidth == PaperWidthMm.W80,
                    onClick = { onPaperWidth(PaperWidthMm.W80) },
                    label = { Text("80 mm", style = MaterialTheme.typography.labelMedium) },
                )
            }
        }
    }
}

@Composable
private fun ComandaAreasCard(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    comandasByArea: Map<String, PrinterSlotConfig>,
    defaultConfig: PrinterSlotConfig,
    onConfigureArea: (String) -> Unit,
    onClearArea: (String) -> Unit,
    onTestArea: (String) -> Unit,
) {
    val configuredCount = preparationAreasForPrint.count { area ->
        comandasByArea[area.apiValue]?.isConfigured == true
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Impresión por área",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (configuredCount > 0) {
                            "$configuredCount área(s) con impresora propia · ${preparationAreasForPrint.size} disponibles"
                        } else {
                            "Opcional — cocina, bar, postres… Si no configuras, usa la impresora por defecto"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Ocultar áreas" else "Ver áreas",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider()
                    Text(
                        "Toca un área para asignar impresora. Sin configurar → impresora por defecto.",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    preparationAreasForPrint.forEachIndexed { index, area ->
                        ComandaAreaRow(
                            area = area,
                            customConfig = comandasByArea[area.apiValue],
                            defaultConfigured = defaultConfig.isConfigured,
                            onConfigure = { onConfigureArea(area.apiValue) },
                            onClear = { onClearArea(area.apiValue) },
                            onTest = { onTestArea(area.apiValue) },
                        )
                        if (index < preparationAreasForPrint.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComandaAreaRow(
    area: PreparationArea,
    customConfig: PrinterSlotConfig?,
    defaultConfigured: Boolean,
    onConfigure: () -> Unit,
    onClear: () -> Unit,
    onTest: () -> Unit,
) {
    val hasCustom = customConfig?.isConfigured == true
    val statusLabel = when {
        hasCustom -> "Impresora propia"
        defaultConfigured -> "Usa default"
        else -> "Sin impresora"
    }
    val statusColor = when {
        hasCustom -> BendeyColors.AccentTeal
        defaultConfigured -> BendeyColors.OnSurfaceVariant
        else -> MaterialTheme.colorScheme.error
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            Modifier
                .weight(1f)
                .clickable(onClick = onConfigure),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(area.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            BendeyStatusChip(label = statusLabel, accentColor = statusColor)
            if (hasCustom) {
                Text(
                    printerSummary(customConfig),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (hasCustom) {
            TextButton(onClick = onClear) {
                Text("Quitar", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(onClick = onTest) {
                Text("Probar", style = MaterialTheme.typography.labelMedium)
            }
        } else {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun printerSummary(config: PrinterSlotConfig?): String {
    if (config == null || !config.isConfigured) return "Sin configurar"
    return when (config.connectionType) {
        PrinterConnectionType.BLUETOOTH -> "BT · ${config.bluetoothAddress.takeLast(8)}"
        PrinterConnectionType.TCP -> "IP · ${config.tcpHost}:${config.tcpPort}"
    }
}

@Composable
private fun CompactTestButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
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
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onSelect)
            .background(
                if (selected) BendeyColors.PrimaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surface,
            )
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column {
            Text(device.name, style = MaterialTheme.typography.bodyMedium)
            Text(device.address, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
