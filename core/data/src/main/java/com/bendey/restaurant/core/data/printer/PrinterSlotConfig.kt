package com.bendey.restaurant.core.data.printer

import com.bendey.restaurant.platform.printing.escpos.PaperWidthMm
import com.bendey.restaurant.platform.printing.transport.PrinterConnectionType
import com.bendey.restaurant.platform.printing.transport.PrinterTarget

enum class PrinterSlot {
    COMANDAS,
    PRECUENTA,
    DOCUMENTOS,
}

data class PrinterSlotConfig(
    val connectionType: PrinterConnectionType = PrinterConnectionType.BLUETOOTH,
    val bluetoothAddress: String = "",
    val tcpHost: String = "",
    val tcpPort: Int = 9100,
    val paperWidth: PaperWidthMm = PaperWidthMm.W80,
) {
    fun toTarget(): PrinterTarget? = when (connectionType) {
        PrinterConnectionType.BLUETOOTH -> {
            if (bluetoothAddress.isBlank()) null
            else PrinterTarget(
                type = PrinterConnectionType.BLUETOOTH,
                bluetoothAddress = bluetoothAddress,
                paperWidth = paperWidth,
            )
        }
        PrinterConnectionType.TCP -> {
            if (tcpHost.isBlank()) null
            else PrinterTarget(
                type = PrinterConnectionType.TCP,
                tcpHost = tcpHost,
                tcpPort = tcpPort,
                paperWidth = paperWidth,
            )
        }
    }

    val isConfigured: Boolean get() = toTarget() != null
}

data class PrinterSettings(
    val comandas: PrinterSlotConfig = PrinterSlotConfig(),
    val precuenta: PrinterSlotConfig = PrinterSlotConfig(),
    val documentos: PrinterSlotConfig = PrinterSlotConfig(),
    val autoPrintComandas: Boolean = true,
    val autoPrintDocuments: Boolean = true,
) {
    fun targetFor(slot: PrinterSlot): PrinterTarget? = when (slot) {
        PrinterSlot.COMANDAS -> comandas.toTarget()
        PrinterSlot.PRECUENTA -> precuenta.toTarget()
        PrinterSlot.DOCUMENTOS -> documentos.toTarget()
    }

    fun configFor(slot: PrinterSlot): PrinterSlotConfig = when (slot) {
        PrinterSlot.COMANDAS -> comandas
        PrinterSlot.PRECUENTA -> precuenta
        PrinterSlot.DOCUMENTOS -> documentos
    }

    fun withSlot(slot: PrinterSlot, config: PrinterSlotConfig): PrinterSettings = when (slot) {
        PrinterSlot.COMANDAS -> copy(comandas = config)
        PrinterSlot.PRECUENTA -> copy(precuenta = config)
        PrinterSlot.DOCUMENTOS -> copy(documentos = config)
    }
}

/** @deprecated Usar [PrinterSettings]; conservado para migración interna. */
data class SavedPrinterConfig(
    val connectionType: PrinterConnectionType = PrinterConnectionType.BLUETOOTH,
    val bluetoothAddress: String = "",
    val tcpHost: String = "",
    val tcpPort: Int = 9100,
    val paperWidth: PaperWidthMm = PaperWidthMm.W80,
    val autoPrintComandas: Boolean = true,
    val autoPrintDocuments: Boolean = true,
) {
    fun toSlotConfig() = PrinterSlotConfig(
        connectionType = connectionType,
        bluetoothAddress = bluetoothAddress,
        tcpHost = tcpHost,
        tcpPort = tcpPort,
        paperWidth = paperWidth,
    )

    fun toTarget(): PrinterTarget? = toSlotConfig().toTarget()
}
