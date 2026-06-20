package com.bendey.restaurant.core.data.printer

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bendey.restaurant.platform.printing.escpos.PaperWidthMm
import com.bendey.restaurant.platform.printing.transport.PrinterConnectionType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.printerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "bendey_printer_prefs",
)

@Singleton
class PrinterPreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.printerDataStore

    val settings: Flow<PrinterSettings> = dataStore.data.map { prefs -> prefs.toSettings() }

    /** Comandas slot — compatibilidad con servicios existentes. */
    val config: Flow<SavedPrinterConfig> = settings.map { it.toLegacyConfig() }

    suspend fun save(settings: PrinterSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.AUTO_PRINT] = settings.autoPrintComandas
            prefs[Keys.AUTO_PRINT_DOCS] = settings.autoPrintDocuments
            writeSlot(prefs, PrinterSlot.COMANDAS, settings.comandas)
            writeSlot(prefs, PrinterSlot.PRECUENTA, settings.precuenta)
            writeSlot(prefs, PrinterSlot.DOCUMENTOS, settings.documentos)
        }
    }

    suspend fun saveSlot(slot: PrinterSlot, config: PrinterSlotConfig) {
        dataStore.edit { prefs ->
            writeSlot(prefs, slot, config)
        }
    }

    suspend fun save(settings: SavedPrinterConfig) {
        val slot = settings.toSlotConfig()
        save(
            PrinterSettings(
                comandas = slot,
                precuenta = slot,
                documentos = slot,
                autoPrintComandas = settings.autoPrintComandas,
                autoPrintDocuments = settings.autoPrintDocuments,
            ),
        )
    }

    private fun writeSlot(prefs: androidx.datastore.preferences.core.MutablePreferences, slot: PrinterSlot, config: PrinterSlotConfig) {
        val prefix = slot.keyPrefix
        prefs[stringPreferencesKey("${prefix}_connection")] = when (config.connectionType) {
            PrinterConnectionType.TCP -> "tcp"
            PrinterConnectionType.BLUETOOTH -> "bluetooth"
        }
        prefs[stringPreferencesKey("${prefix}_bt")] = config.bluetoothAddress
        prefs[stringPreferencesKey("${prefix}_tcp_host")] = config.tcpHost
        prefs[intPreferencesKey("${prefix}_tcp_port")] = config.tcpPort
        prefs[intPreferencesKey("${prefix}_paper_mm")] = config.paperWidth.mm
    }

    private fun Preferences.toSettings(): PrinterSettings {
        val legacy = readLegacySingleSlot()
        val hasMultiSlot = hasKey(stringPreferencesKey("${PrinterSlot.COMANDAS.keyPrefix}_connection")) ||
            hasKey(stringPreferencesKey("${PrinterSlot.PRECUENTA.keyPrefix}_connection")) ||
            hasKey(stringPreferencesKey("${PrinterSlot.DOCUMENTOS.keyPrefix}_connection"))

        val fallback = legacy ?: PrinterSlotConfig()
        return PrinterSettings(
            comandas = if (hasMultiSlot) readSlot(PrinterSlot.COMANDAS) else fallback,
            precuenta = if (hasMultiSlot) readSlot(PrinterSlot.PRECUENTA) else fallback,
            documentos = if (hasMultiSlot) readSlot(PrinterSlot.DOCUMENTOS) else fallback,
            autoPrintComandas = this[Keys.AUTO_PRINT] ?: true,
            autoPrintDocuments = this[Keys.AUTO_PRINT_DOCS] ?: true,
        )
    }

    private fun Preferences.readSlot(slot: PrinterSlot): PrinterSlotConfig {
        val prefix = slot.keyPrefix
        return PrinterSlotConfig(
            connectionType = when (this[stringPreferencesKey("${prefix}_connection")]) {
                "tcp" -> PrinterConnectionType.TCP
                else -> PrinterConnectionType.BLUETOOTH
            },
            bluetoothAddress = this[stringPreferencesKey("${prefix}_bt")] ?: "",
            tcpHost = this[stringPreferencesKey("${prefix}_tcp_host")] ?: "",
            tcpPort = this[intPreferencesKey("${prefix}_tcp_port")] ?: 9100,
            paperWidth = PaperWidthMm.fromMm(this[intPreferencesKey("${prefix}_paper_mm")] ?: 80),
        )
    }

    private fun Preferences.readLegacySingleSlot(): PrinterSlotConfig? {
        val hasLegacy = hasKey(Keys.CONNECTION_TYPE) || hasKey(Keys.BT_ADDRESS)
        if (!hasLegacy) return null
        return PrinterSlotConfig(
            connectionType = when (this[Keys.CONNECTION_TYPE]) {
                "tcp" -> PrinterConnectionType.TCP
                else -> PrinterConnectionType.BLUETOOTH
            },
            bluetoothAddress = this[Keys.BT_ADDRESS] ?: "",
            tcpHost = this[Keys.TCP_HOST] ?: "",
            tcpPort = this[Keys.TCP_PORT] ?: 9100,
            paperWidth = PaperWidthMm.fromMm(this[Keys.PAPER_MM] ?: 80),
        )
    }

    private fun PrinterSettings.toLegacyConfig() = SavedPrinterConfig(
        connectionType = comandas.connectionType,
        bluetoothAddress = comandas.bluetoothAddress,
        tcpHost = comandas.tcpHost,
        tcpPort = comandas.tcpPort,
        paperWidth = comandas.paperWidth,
        autoPrintComandas = autoPrintComandas,
        autoPrintDocuments = autoPrintDocuments,
    )

    private val PrinterSlot.keyPrefix: String
        get() = when (this) {
            PrinterSlot.COMANDAS -> "comandas"
            PrinterSlot.PRECUENTA -> "precuenta"
            PrinterSlot.DOCUMENTOS -> "documentos"
        }

    private fun Preferences.hasKey(key: Preferences.Key<*>): Boolean = asMap().containsKey(key)

    private object Keys {
        val CONNECTION_TYPE = stringPreferencesKey("connection_type")
        val BT_ADDRESS = stringPreferencesKey("bt_address")
        val TCP_HOST = stringPreferencesKey("tcp_host")
        val TCP_PORT = intPreferencesKey("tcp_port")
        val PAPER_MM = intPreferencesKey("paper_mm")
        val AUTO_PRINT = booleanPreferencesKey("auto_print_comandas")
        val AUTO_PRINT_DOCS = booleanPreferencesKey("auto_print_documents")
    }
}
