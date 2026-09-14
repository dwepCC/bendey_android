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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.bendey.restaurant.core.ui.components.BendeyVerticalScrollColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
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
import com.bendey.restaurant.core.data.printer.printserver.PrintDeliveryMode
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.components.BendeySectionTitle
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyCardDefaults
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.catalog.PreparationAreaItem
import com.bendey.restaurant.core.domain.catalog.normalizedName
import com.bendey.restaurant.core.domain.catalog.preparationAreaDisplayLabel
import com.bendey.restaurant.core.ui.components.BendeyLoadingOverlay
import com.bendey.restaurant.core.ui.components.BendeyOutlinedButton
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.layout.bendeySafeDrawingPadding
import com.bendey.restaurant.platform.printing.escpos.ComandaComboDisplay
import com.bendey.restaurant.platform.printing.escpos.ComandaTextSize
import com.bendey.restaurant.platform.printing.escpos.LogoSize
import com.bendey.restaurant.platform.printing.escpos.PaperWidthMm
import com.bendey.restaurant.platform.printing.transport.BluetoothDeviceInfo
import com.bendey.restaurant.platform.printing.transport.PrinterConnectionType

/** Mismas tres opciones, mismo orden y mismas etiquetas que Bendey Resto (Tauri). */
private val COMBO_DISPLAY_OPTIONS = listOf(
    ComandaComboDisplay.DETAILED to "Detallada",
    ComandaComboDisplay.GROUPED to "Agrupada",
    ComandaComboDisplay.PRODUCTS to "Solo productos",
)

private val COMBO_DISPLAY_HINTS = mapOf(
    ComandaComboDisplay.DETAILED to
        "Cada combo con sus componentes, uno debajo del otro. Es lo clásico.",
    ComandaComboDisplay.GROUPED to
        "Resume los combos iguales arriba y suma los componentes repetidos " +
        "(ej. 2 combos con papa → 2x Papa frita).",
    ComandaComboDisplay.PRODUCTS to
        "Solo los platos a preparar, sin nombre de combo. Los platos iguales " +
        "van juntos en una línea aunque vengan de combos distintos.",
)

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

    val nearbyWifiLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ -> viewModel.scanPrintServers() }

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
                    "Comandas · ${preparationAreaDisplayLabel(state.editingAreaKey!!)}"
                } else {
                    "Comandas · impresora por defecto"
                }
                PrinterSlot.PRECUENTA -> "Precuenta"
                PrinterSlot.DOCUMENTOS -> "Documentos"
            },
            onBack = onBack,
        )
        BendeyVerticalScrollColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.sm),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
        ) {
            // Nivel 1 — qué se está configurando (comandas / precuenta / documentos).
            Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                PrinterSlot.entries.forEach { slot ->
                    BendeyFilterChip(
                        selected = state.selectedSlot == slot,
                        onClick = {
                            areasExpanded = false
                            viewModel.selectSlot(slot)
                        },
                        text = when (slot) {
                            PrinterSlot.COMANDAS -> "Comandas"
                            PrinterSlot.PRECUENTA -> "Precuenta"
                            PrinterSlot.DOCUMENTOS -> "Documentos"
                        },
                    )
                }
            }

            if (state.selectedSlot == PrinterSlot.COMANDAS && state.editingAreaKey != null) {
                BendeyManagementCard(contentPadding = PaddingValues(horizontal = BendeySpacing.sm, vertical = BendeySpacing.xs)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                preparationAreaDisplayLabel(state.editingAreaKey!!),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        BendeyTextButton(text = "Volver al default", onClick = viewModel::backToDefaultComandaPrinter)
                    }
                }
            }

            // Nivel 2 — dónde imprime (local / servidor de impresión).
            PrintServerModeCard(
                deliveryMode = state.deliveryMode,
                onDeliveryMode = viewModel::setDeliveryMode,
                scanning = state.scanningServers,
                discoveredServers = state.discoveredServers,
                selectedServer = state.selectedPrintServer,
                manualHost = state.manualServerHost,
                showAdvanced = state.showAdvancedServerHost,
                onScan = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        nearbyWifiLauncher.launch(Manifest.permission.NEARBY_WIFI_DEVICES)
                    } else {
                        viewModel.scanPrintServers()
                    }
                },
                onSelectServer = viewModel::selectPrintServer,
                onManualHost = viewModel::setManualServerHost,
                onToggleAdvanced = viewModel::toggleAdvancedServerHost,
                onTestServer = viewModel::printServerTest,
            )

            if (state.deliveryMode == PrintDeliveryMode.LOCAL) {
            // Nivel 2 (modo local) — cómo conecta (Bluetooth / Red) + qué papel usa.
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
                        "Solo comandas de ${preparationAreaDisplayLabel(state.editingAreaKey!!)}"
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

            // Nivel 3 (solo comandas, impresora por defecto) — cómo se ve: tamaño de texto.
            if (state.selectedSlot == PrinterSlot.COMANDAS && state.editingAreaKey == null) {
                BendeyManagementCard {
                    Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                        BendeySectionTitle(text = "Tamaño texto comanda", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                            BendeyFilterChip(
                                selected = state.comandaTextSize == ComandaTextSize.DEFAULT,
                                onClick = { viewModel.setComandaTextSize(ComandaTextSize.DEFAULT) },
                                text = "Grande",
                            )
                            BendeyFilterChip(
                                selected = state.comandaTextSize == ComandaTextSize.MEDIANO,
                                onClick = { viewModel.setComandaTextSize(ComandaTextSize.MEDIANO) },
                                text = "Mediano",
                            )
                        }
                    }
                }
            }

            // Nivel 3 (solo comandas, impresora por defecto) — cómo se ve: combos en comanda.
            if (state.selectedSlot == PrinterSlot.COMANDAS && state.editingAreaKey == null) {
                BendeyManagementCard {
                    Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                        BendeySectionTitle(text = "Combos en la comanda", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Cómo se imprimen los combos en cocina. Solo productos muestra únicamente " +
                                "los platos a preparar, sin el nombre del combo. La pantalla de cocina " +
                                "y el carrito no cambian.",
                            style = MaterialTheme.typography.bodySmall,
                            color = BendeyColors.OnSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                            COMBO_DISPLAY_OPTIONS.forEach { (value, label) ->
                                BendeyFilterChip(
                                    selected = state.comandaComboDisplay == value,
                                    onClick = { viewModel.setComandaComboDisplay(value) },
                                    text = label,
                                )
                            }
                        }
                        Text(
                            COMBO_DISPLAY_HINTS.getValue(state.comandaComboDisplay),
                            style = MaterialTheme.typography.bodySmall,
                            color = BendeyColors.OnSurfaceVariant,
                        )
                    }
                }
            }

            // Nivel 2 (modo local, comandas por defecto) — impresora dedicada por área.
            if (state.selectedSlot == PrinterSlot.COMANDAS && state.editingAreaKey == null) {
                ComandaAreasCard(
                    expanded = areasExpanded,
                    onToggleExpanded = { areasExpanded = !areasExpanded },
                    preparationAreas = state.preparationAreas,
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
            }

            // Nivel 2 — qué imprime automáticamente.
            BendeyManagementCard {
                Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                    BendeySectionTitle(text = "Auto-impresión", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                        BendeyFilterChip(
                            selected = state.autoPrintComandas,
                            onClick = { viewModel.setAutoPrintComandas(!state.autoPrintComandas) },
                            text = "Comandas",
                        )
                        BendeyFilterChip(
                            selected = state.autoPrintDocuments,
                            onClick = { viewModel.setAutoPrintDocuments(!state.autoPrintDocuments) },
                            text = "Documentos",
                        )
                    }
                }
            }

            // Nivel 2 — cómo se ve: tamaño del logo en comprobantes.
            BendeyManagementCard {
                Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                    BendeySectionTitle(text = "Tamaño del logo en comprobantes", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                        BendeyFilterChip(
                            selected = state.documentLogoSize == LogoSize.SMALL,
                            onClick = { viewModel.setDocumentLogoSize(LogoSize.SMALL) },
                            text = "Pequeño",
                        )
                        BendeyFilterChip(
                            selected = state.documentLogoSize == LogoSize.MEDIUM,
                            onClick = { viewModel.setDocumentLogoSize(LogoSize.MEDIUM) },
                            text = "Mediano",
                        )
                        BendeyFilterChip(
                            selected = state.documentLogoSize == LogoSize.LARGE,
                            onClick = { viewModel.setDocumentLogoSize(LogoSize.LARGE) },
                            text = "Grande",
                        )
                    }
                }
            }

            BendeyManagementCard {
                Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                    BendeySectionTitle(text = "Gaveta de caja", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Abre el cajón de dinero al imprimir el comprobante (solo documentos, no comandas ni precuenta). Requiere una gaveta conectada a la impresora.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    BendeyFilterChip(
                        selected = state.openCashDrawerOnDocument,
                        onClick = { viewModel.setOpenCashDrawerOnDocument(!state.openCashDrawerOnDocument) },
                        text = "Abrir gaveta al imprimir",
                    )
                }
            }

            BendeyManagementCard {
                Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                    BendeySectionTitle(text = "Impresión de prueba", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
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
                    color = BendeyColors.Error,
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
    BendeyManagementCard {
        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {
            Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
            }

            BendeySectionTitle(text = "Conexión")
            Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                BendeyFilterChip(
                    selected = connectionType == PrinterConnectionType.BLUETOOTH,
                    onClick = { onConnectionType(PrinterConnectionType.BLUETOOTH) },
                    text = "Bluetooth",
                )
                BendeyFilterChip(
                    selected = connectionType == PrinterConnectionType.TCP,
                    onClick = { onConnectionType(PrinterConnectionType.TCP) },
                    text = "Red / IP",
                )
            }

            when (connectionType) {
                PrinterConnectionType.BLUETOOTH -> {
                    Text("Dispositivos emparejados", style = MaterialTheme.typography.labelMedium)
                    if (pairedDevices.isEmpty()) {
                        Text(
                            "Empareja la impresora en Ajustes de Android.",
                            style = MaterialTheme.typography.bodySmall,
                            color = BendeyColors.OnSurfaceVariant,
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
                    BendeyOutlinedButton(
                        text = "Conectar Bluetooth",
                        onClick = onConnectBluetooth,
                        enabled = bluetoothAddress.isNotBlank(),
                        fillWidth = true,
                    )
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

            BendeySectionTitle(text = "Papel")
            Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                BendeyFilterChip(
                    selected = paperWidth == PaperWidthMm.W58,
                    onClick = { onPaperWidth(PaperWidthMm.W58) },
                    text = "58 mm",
                )
                BendeyFilterChip(
                    selected = paperWidth == PaperWidthMm.W80,
                    onClick = { onPaperWidth(PaperWidthMm.W80) },
                    text = "80 mm",
                )
            }
        }
    }
}

@Composable
private fun ComandaAreasCard(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    preparationAreas: List<PreparationAreaItem>,
    comandasByArea: Map<String, PrinterSlotConfig>,
    defaultConfig: PrinterSlotConfig,
    onConfigureArea: (String) -> Unit,
    onClearArea: (String) -> Unit,
    onTestArea: (String) -> Unit,
) {
    val configuredCount = preparationAreas.count { area ->
        comandasByArea[area.normalizedName()]?.isConfigured == true
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = BendeyShapeTokens.lg,
        colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface),
        border = BendeyCardDefaults.border,
        elevation = BendeyCardDefaults.elevation(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs)) {
                    Text(
                        "Impresión por área",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (configuredCount > 0) {
                            "$configuredCount área(s) con impresora propia · ${preparationAreas.size} disponibles"
                        } else {
                            "Opcional — cocina, bar, postres… Si no configuras, usa la impresora por defecto"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.OnSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Ocultar áreas" else "Ver áreas",
                    tint = BendeyColors.OnSurfaceVariant,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider()
                    Text(
                        "Toca un área para asignar impresora. Sin configurar → impresora por defecto.",
                        modifier = Modifier.padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.xs),
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.OnSurfaceVariant,
                    )
                    preparationAreas.forEachIndexed { index, area ->
                        val areaKey = area.normalizedName()
                        ComandaAreaRow(
                            area = area,
                            customConfig = comandasByArea[areaKey],
                            defaultConfigured = defaultConfig.isConfigured,
                            onConfigure = { onConfigureArea(areaKey) },
                            onClear = { onClearArea(areaKey) },
                            onTest = { onTestArea(areaKey) },
                        )
                        if (index < preparationAreas.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = BendeySpacing.sm))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComandaAreaRow(
    area: PreparationAreaItem,
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
        else -> BendeyColors.Error
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
    ) {
        Column(
            Modifier
                .weight(1f)
                .clickable(onClick = onConfigure),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
        ) {
            Text(area.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            BendeyStatusChip(label = statusLabel, accentColor = statusColor)
            if (hasCustom) {
                Text(
                    printerSummary(customConfig),
                    style = MaterialTheme.typography.labelSmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
        }
        if (hasCustom) {
            BendeyTextButton(text = "Quitar", onClick = onClear)
            BendeyOutlinedButton(text = "Probar", onClick = onTest)
        } else {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = BendeyColors.OnSurfaceVariant,
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
    BendeyOutlinedButton(text = label, onClick = onClick, modifier = modifier)
}

@Composable
private fun PrintServerModeCard(
    deliveryMode: PrintDeliveryMode,
    onDeliveryMode: (PrintDeliveryMode) -> Unit,
    scanning: Boolean,
    discoveredServers: List<com.bendey.restaurant.core.data.printer.printserver.DiscoveredPrintServer>,
    selectedServer: com.bendey.restaurant.core.data.printer.printserver.PrintServerSelection?,
    manualHost: String,
    showAdvanced: Boolean,
    onScan: () -> Unit,
    onSelectServer: (com.bendey.restaurant.core.data.printer.printserver.DiscoveredPrintServer) -> Unit,
    onManualHost: (String) -> Unit,
    onToggleAdvanced: () -> Unit,
    onTestServer: () -> Unit,
) {
    BendeyManagementCard {
        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
            BendeySectionTitle(text = "Modo de impresión", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                BendeyFilterChip(
                    selected = deliveryMode == PrintDeliveryMode.LOCAL,
                    onClick = { onDeliveryMode(PrintDeliveryMode.LOCAL) },
                    text = "Local",
                )
                BendeyFilterChip(
                    selected = deliveryMode == PrintDeliveryMode.SERVER,
                    onClick = { onDeliveryMode(PrintDeliveryMode.SERVER) },
                    text = "Servidor de impresión",
                )
            }

            if (deliveryMode == PrintDeliveryMode.SERVER) {
                Text(
                    "Las tablets envían trabajos a la PC Windows configurada en la red. La configuración local se conserva al volver al modo local.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
                BendeyOutlinedButton(
                    text = if (scanning) "Buscando en la red…" else "Buscar servidores en la red",
                    onClick = onScan,
                    enabled = !scanning,
                    fillWidth = true,
                )
                Text(
                    "Si no aparece, use IP manual (misma IP que funciona en Chrome). El escaneo puede tardar ~15 s.",
                    style = MaterialTheme.typography.labelSmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
                selectedServer?.let { server ->
                    Text(
                        "Servidor seleccionado: ${server.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "${server.branchName.ifBlank { "Sucursal" }} · ${server.resolvedHost()}:${server.port}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                discoveredServers.forEach { server ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectServer(server) },
                        shape = BendeyShapeTokens.md,
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedServer?.serverId == server.serverId) {
                                BendeyColors.PrimaryContainer.copy(alpha = 0.45f)
                            } else {
                                BendeyColors.Surface
                            },
                        ),
                    ) {
                        Column(Modifier.padding(BendeySpacing.sm)) {
                            Text(server.displayName, fontWeight = FontWeight.SemiBold)
                            Text(server.branchName.ifBlank { "Sucursal" }, style = MaterialTheme.typography.bodySmall)
                            Text("${server.host} · ${server.latencyMs} ms", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                BendeyTextButton(
                    text = if (showAdvanced) "Ocultar IP manual" else "IP manual (avanzado)",
                    onClick = onToggleAdvanced,
                )
                if (showAdvanced) {
                    BendeyTextField(
                        value = manualHost,
                        onValueChange = onManualHost,
                        label = "Host / IP manual",
                        placeholder = "192.168.1.20",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Solo la IP (ej. 192.168.1.20). Puerto por defecto: 19280.",
                        style = MaterialTheme.typography.labelSmall,
                        color = BendeyColors.OnSurfaceVariant,
                    )
                }
                BendeyOutlinedButton(text = "Probar servidor", onClick = onTestServer, fillWidth = true)
            }
        }
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
            .clip(BendeyShapeTokens.xs)
            .clickable(onClick = onSelect)
            .background(
                if (selected) BendeyColors.PrimaryContainer.copy(alpha = 0.5f)
                else BendeyColors.Surface,
            )
            .padding(vertical = BendeySpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // RadioButton (no BendeyFilterChip) a propósito: esto es una lista de dispositivos
        // emparejados con selección única real, no un ajuste on/off — el control semánticamente
        // correcto de Material sigue siendo el radio, solo se tiñe con el color de marca.
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = BendeyColors.Primary),
        )
        Column {
            Text(device.name, style = MaterialTheme.typography.bodyMedium)
            Text(device.address, style = MaterialTheme.typography.labelSmall, color = BendeyColors.OnSurfaceVariant)
        }
    }
}
