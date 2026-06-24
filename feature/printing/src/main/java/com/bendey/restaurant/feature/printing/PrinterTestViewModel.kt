package com.bendey.restaurant.feature.printing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.data.printer.PrinterPreferencesStore
import com.bendey.restaurant.core.data.printer.PrinterSettings
import com.bendey.restaurant.core.data.printer.PrinterSlot
import com.bendey.restaurant.core.data.printer.PrinterSlotConfig
import com.bendey.restaurant.core.data.kitchen.areaTicketLabel
import com.bendey.restaurant.core.domain.catalog.PreparationAreaItem
import com.bendey.restaurant.core.domain.catalog.PreparationAreasRepository
import com.bendey.restaurant.core.domain.catalog.normalizedName
import com.bendey.restaurant.core.domain.catalog.preparationAreaDisplayLabel
import com.bendey.restaurant.platform.printing.escpos.ComandaItem
import com.bendey.restaurant.platform.printing.escpos.ComandaPrintInput
import com.bendey.restaurant.platform.printing.escpos.DocumentPrintInput
import com.bendey.restaurant.platform.printing.escpos.DocumentPrintLine
import com.bendey.restaurant.platform.printing.escpos.DocumentPrintPayment
import com.bendey.restaurant.platform.printing.escpos.ComandaTextSize
import com.bendey.restaurant.platform.printing.escpos.PaperWidthMm
import com.bendey.restaurant.platform.printing.escpos.PrecuentaItem
import com.bendey.restaurant.platform.printing.escpos.PrecuentaPrintInput
import com.bendey.restaurant.platform.printing.transport.BluetoothDeviceInfo
import com.bendey.restaurant.platform.printing.transport.PrintResult
import com.bendey.restaurant.platform.printing.transport.PrinterConnectionType
import com.bendey.restaurant.platform.printing.transport.PrinterRepository
import com.bendey.restaurant.platform.printing.transport.PrinterTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrinterTestUiState(
    val selectedSlot: PrinterSlot = PrinterSlot.COMANDAS,
    val connectionType: PrinterConnectionType = PrinterConnectionType.BLUETOOTH,
    val bluetoothAddress: String = "",
    val tcpHost: String = "192.168.1.100",
    val tcpPort: String = "9100",
    val paperWidth: PaperWidthMm = PaperWidthMm.W80,
    val comandaTextSize: ComandaTextSize = ComandaTextSize.DEFAULT,
    val autoPrintComandas: Boolean = true,
    val autoPrintDocuments: Boolean = true,
    val comandasByArea: Map<String, PrinterSlotConfig> = emptyMap(),
    val preparationAreas: List<PreparationAreaItem> = emptyList(),
    val editingAreaKey: String? = null,
    val pairedDevices: List<BluetoothDeviceInfo> = emptyList(),
    val loading: Boolean = false,
    val statusMessage: String? = null,
    val error: String? = null,
)

@HiltViewModel
class PrinterTestViewModel @Inject constructor(
    private val printerRepository: PrinterRepository,
    private val printerPreferencesStore: PrinterPreferencesStore,
    private val preparationAreasRepository: PreparationAreasRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrinterTestUiState())
    val uiState: StateFlow<PrinterTestUiState> = _uiState.asStateFlow()

    private var cachedSettings = PrinterSettings()

    init {
        refreshPairedDevices()
        viewModelScope.launch {
            cachedSettings = printerPreferencesStore.settings.first()
            applySlotToUi(cachedSettings, PrinterSlot.COMANDAS, editingAreaKey = null)
            when (val result = preparationAreasRepository.listPreparationAreas(activeOnly = true)) {
                is com.bendey.restaurant.core.domain.model.AppResult.Success ->
                    _uiState.update { it.copy(preparationAreas = result.data) }
                else -> Unit
            }
        }
    }

    fun selectSlot(slot: PrinterSlot) {
        persistCurrentSlotToCache()
        cachedSettings = cachedSettings.withSlot(_uiState.value.selectedSlot, readSlotFromUi())
        applySlotToUi(cachedSettings, slot, editingAreaKey = null)
    }

    fun editComandaArea(areaKey: String) {
        if (_uiState.value.selectedSlot != PrinterSlot.COMANDAS) {
            selectSlot(PrinterSlot.COMANDAS)
        }
        persistCurrentSlotToCache()
        val config = cachedSettings.comandasByArea[areaKey] ?: cachedSettings.comandas
        applyAreaConfigToUi(areaKey, config)
    }

    fun clearComandaArea(areaKey: String) {
        cachedSettings = cachedSettings.withComandaArea(areaKey, null)
        _uiState.update { it.copy(comandasByArea = cachedSettings.comandasByArea) }
        if (_uiState.value.editingAreaKey == areaKey) {
            applySlotToUi(cachedSettings, PrinterSlot.COMANDAS, editingAreaKey = null)
        }
        viewModelScope.launch { printerPreferencesStore.save(cachedSettings) }
    }

    fun backToDefaultComandaPrinter() {
        persistCurrentSlotToCache()
        applySlotToUi(cachedSettings, PrinterSlot.COMANDAS, editingAreaKey = null)
    }

    fun setConnectionType(type: PrinterConnectionType) {
        _uiState.update { it.copy(connectionType = type, error = null) }
    }

    fun setBluetoothAddress(address: String) {
        _uiState.update { it.copy(bluetoothAddress = address) }
    }

    fun setTcpHost(host: String) {
        _uiState.update { it.copy(tcpHost = host) }
    }

    fun setTcpPort(port: String) {
        _uiState.update { it.copy(tcpPort = port.filter { c -> c.isDigit() }) }
    }

    fun setPaperWidth(width: PaperWidthMm) {
        _uiState.update { it.copy(paperWidth = width) }
        persistCurrentSlotToCache()
        viewModelScope.launch {
            val state = _uiState.value
            printerPreferencesStore.save(
                cachedSettings.copy(
                    autoPrintComandas = state.autoPrintComandas,
                    autoPrintDocuments = state.autoPrintDocuments,
                    comandaTextSize = state.comandaTextSize,
                ),
            )
        }
    }

    fun setComandaTextSize(size: ComandaTextSize) {
        _uiState.update { it.copy(comandaTextSize = size) }
        cachedSettings = cachedSettings.copy(comandaTextSize = size)
        viewModelScope.launch { printerPreferencesStore.save(cachedSettings) }
    }

    fun setAutoPrintComandas(enabled: Boolean) {
        _uiState.update { it.copy(autoPrintComandas = enabled) }
    }

    fun setAutoPrintDocuments(enabled: Boolean) {
        _uiState.update { it.copy(autoPrintDocuments = enabled) }
    }

    fun refreshPairedDevices() {
        viewModelScope.launch {
            val devices = printerRepository.getPairedDevices()
            _uiState.update { it.copy(pairedDevices = devices) }
        }
    }

    fun connectBluetooth() {
        val address = _uiState.value.bluetoothAddress
        if (address.isBlank()) {
            _uiState.update { it.copy(error = "Seleccione una impresora Bluetooth") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            when (val result = printerRepository.connectBluetooth(address)) {
                is PrintResult.Success -> {
                    persistAll()
                    _uiState.update { it.copy(loading = false, statusMessage = "Bluetooth conectado") }
                }
                is PrintResult.Error -> _uiState.update {
                    it.copy(loading = false, error = result.message)
                }
            }
        }
    }

    fun printComandaSample() = printSample(SampleKind.COMANDA)

    fun printComandaAreaSample(areaKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, statusMessage = null) }
            persistAll()
            val settings = cachedSettings
            val target = settings.targetForComandaArea(areaKey) ?: run {
                _uiState.update { it.copy(loading = false, error = "Configure impresora para esta área") }
                return@launch
            }
            val label = areaTicketLabel("Mesa 05", areaKey)
            when (val result = printerRepository.printComanda(
                target,
                ComandaPrintInput(
                    tableName = label,
                    orderNumber = 42,
                    waiterName = "Maria Lopez",
                    items = listOf(
                        ComandaItem(
                            productName = preparationAreaDisplayLabel(areaKey),
                            quantity = 1.0,
                            notes = "Prueba área",
                        ),
                    ),
                ),
            )) {
                is PrintResult.Success -> _uiState.update {
                    it.copy(loading = false, statusMessage = "Comanda de prueba ($areaKey) enviada")
                }
                is PrintResult.Error -> _uiState.update {
                    it.copy(loading = false, error = result.message)
                }
            }
        }
    }

    fun printPrecuentaSample() = printSample(SampleKind.PRECUENTA)

    fun printDocumentSample() = printSample(SampleKind.DOCUMENTO)

    private enum class SampleKind { COMANDA, PRECUENTA, DOCUMENTO }

    private fun printSample(kind: SampleKind) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, statusMessage = null) }
            persistAll()
            val target = buildTarget()
            val result = when (kind) {
                SampleKind.COMANDA -> printerRepository.printComanda(
                    target,
                    ComandaPrintInput(
                        tableName = "Mesa 05",
                        orderNumber = 42,
                        waiterName = "Maria Lopez",
                        items = listOf(
                            ComandaItem(
                                productName = "Pollo a la brasa",
                                quantity = 2.0,
                                modifierLines = listOf("+ Papas extra"),
                                notes = "Sin ají",
                            ),
                            ComandaItem(productName = "Chicha morada", quantity = 1.0),
                        ),
                    ),
                )
                SampleKind.PRECUENTA -> printerRepository.printPrecuenta(
                    target,
                    PrecuentaPrintInput(
                        tableName = "Mesa 05",
                        items = listOf(
                            PrecuentaItem("Pollo a la brasa", 2.0, 38.0),
                            PrecuentaItem("Chicha morada", 1.0, 8.0),
                        ),
                        total = 84.0,
                    ),
                )
                SampleKind.DOCUMENTO -> printerRepository.printDocument(
                    target,
                    DocumentPrintInput(
                        docType = "NOTA DE VENTA",
                        number = "NV001-00000001",
                        issueDate = "2026-06-19",
                        companyName = "Restaurante Demo",
                        companyRuc = "20123456789",
                        companyAddress = "Av. Principal 123",
                        branchName = "Local principal",
                        clientName = "CLIENTES VARIOS",
                        clientDocNumber = "00000000",
                        items = listOf(
                            DocumentPrintLine("Pollo a la brasa", 2.0, 38.0, 76.0),
                            DocumentPrintLine("Chicha morada", 1.0, 8.0, 8.0),
                        ),
                        subtotal = 71.19,
                        taxAmount = 12.81,
                        total = 84.0,
                        currency = "PEN",
                        payments = listOf(DocumentPrintPayment("Efectivo", 84.0)),
                        legendText = null,
                    ),
                )
            }
            when (result) {
                is PrintResult.Success -> _uiState.update {
                    it.copy(
                        loading = false,
                        statusMessage = when (kind) {
                            SampleKind.COMANDA -> "Comanda enviada"
                            SampleKind.PRECUENTA -> "Precuenta enviada"
                            SampleKind.DOCUMENTO -> "Documento enviado"
                        },
                    )
                }
                is PrintResult.Error -> _uiState.update {
                    it.copy(loading = false, error = result.message)
                }
            }
        }
    }

    private fun buildTarget(): PrinterTarget {
        val state = _uiState.value
        return when (state.connectionType) {
            PrinterConnectionType.BLUETOOTH -> PrinterTarget(
                type = PrinterConnectionType.BLUETOOTH,
                bluetoothAddress = state.bluetoothAddress,
                paperWidth = state.paperWidth,
            )
            PrinterConnectionType.TCP -> PrinterTarget(
                type = PrinterConnectionType.TCP,
                tcpHost = state.tcpHost.trim(),
                tcpPort = state.tcpPort.toIntOrNull() ?: 9100,
                paperWidth = state.paperWidth,
            )
        }
    }

    private fun readSlotFromUi(): PrinterSlotConfig {
        val state = _uiState.value
        return PrinterSlotConfig(
            connectionType = state.connectionType,
            bluetoothAddress = state.bluetoothAddress,
            tcpHost = state.tcpHost.trim(),
            tcpPort = state.tcpPort.toIntOrNull() ?: 9100,
            paperWidth = state.paperWidth,
        )
    }

    private fun persistCurrentSlotToCache() {
        val state = _uiState.value
        val config = readSlotFromUi()
        cachedSettings = if (state.selectedSlot == PrinterSlot.COMANDAS && state.editingAreaKey != null) {
            cachedSettings.withComandaArea(state.editingAreaKey, config)
        } else {
            cachedSettings.withSlot(state.selectedSlot, config)
        }
        _uiState.update { it.copy(comandasByArea = cachedSettings.comandasByArea) }
    }

    private fun persistAll() {
        persistCurrentSlotToCache()
        val state = _uiState.value
        viewModelScope.launch {
            printerPreferencesStore.save(
                cachedSettings.copy(
                    autoPrintComandas = state.autoPrintComandas,
                    autoPrintDocuments = state.autoPrintDocuments,
                    comandaTextSize = state.comandaTextSize,
                ),
            )
        }
    }

    private fun applySlotToUi(settings: PrinterSettings, slot: PrinterSlot, editingAreaKey: String?) {
        val config = if (slot == PrinterSlot.COMANDAS && editingAreaKey != null) {
            settings.comandasByArea[editingAreaKey] ?: settings.comandas
        } else {
            settings.configFor(slot)
        }
        _uiState.update {
            it.copy(
                selectedSlot = slot,
                editingAreaKey = if (slot == PrinterSlot.COMANDAS) editingAreaKey else null,
                connectionType = config.connectionType,
                bluetoothAddress = config.bluetoothAddress,
                tcpHost = config.tcpHost.ifBlank { "192.168.1.100" },
                tcpPort = config.tcpPort.toString(),
                paperWidth = config.paperWidth,
                comandaTextSize = settings.comandaTextSize,
                autoPrintComandas = settings.autoPrintComandas,
                autoPrintDocuments = settings.autoPrintDocuments,
                comandasByArea = settings.comandasByArea,
            )
        }
    }

    private fun applyAreaConfigToUi(areaKey: String, config: PrinterSlotConfig) {
        _uiState.update {
            it.copy(
                selectedSlot = PrinterSlot.COMANDAS,
                editingAreaKey = areaKey,
                connectionType = config.connectionType,
                bluetoothAddress = config.bluetoothAddress,
                tcpHost = config.tcpHost.ifBlank { "192.168.1.100" },
                tcpPort = config.tcpPort.toString(),
                paperWidth = config.paperWidth,
                comandasByArea = cachedSettings.comandasByArea,
            )
        }
    }
}
