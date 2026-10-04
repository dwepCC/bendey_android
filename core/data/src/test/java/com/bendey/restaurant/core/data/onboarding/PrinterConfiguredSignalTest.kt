package com.bendey.restaurant.core.data.onboarding

import com.bendey.restaurant.core.data.printer.PrinterSettings
import com.bendey.restaurant.core.data.printer.PrinterSlotConfig
import com.bendey.restaurant.core.data.printer.printserver.PrintDeliveryMode
import com.bendey.restaurant.core.data.printer.printserver.PrintServerSelection
import com.bendey.restaurant.platform.printing.transport.PrinterConnectionType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** La señal del paso "Conecta tu impresora (este equipo)": ¿esta tablet tiene una impresora lista? */
class PrinterConfiguredSignalTest {

    @Test
    fun `sin nada configurado no hay impresora`() {
        assertFalse(PrinterSettings().hasAnyPrinterConfigured())
    }

    @Test
    fun `bluetooth con direccion cuenta`() {
        val s = PrinterSettings(comandas = PrinterSlotConfig(bluetoothAddress = "AA:BB:CC:DD:EE:FF"))
        assertTrue(s.hasAnyPrinterConfigured())
    }

    @Test
    fun `tcp con host cuenta, tcp sin host no`() {
        val tcp = PrinterSlotConfig(connectionType = PrinterConnectionType.TCP, tcpHost = "192.168.1.50")
        assertTrue(PrinterSettings(documentos = tcp).hasAnyPrinterConfigured())
        assertFalse(
            PrinterSettings(comandas = PrinterSlotConfig(connectionType = PrinterConnectionType.TCP)).hasAnyPrinterConfigured(),
        )
    }

    @Test
    fun `solo la precuenta configurada tambien cuenta`() {
        val s = PrinterSettings(precuenta = PrinterSlotConfig(bluetoothAddress = "AA:BB"))
        assertTrue(s.hasAnyPrinterConfigured())
    }

    @Test
    fun `servidor de impresion listo cuenta aunque no haya impresora local`() {
        val s = PrinterSettings(
            deliveryMode = PrintDeliveryMode.SERVER,
            printServer = PrintServerSelection(host = "192.168.1.10", port = 19_280),
        )
        assertTrue(s.hasAnyPrinterConfigured())
    }

    @Test
    fun `modo servidor sin servidor elegido no cuenta`() {
        assertFalse(PrinterSettings(deliveryMode = PrintDeliveryMode.SERVER).hasAnyPrinterConfigured())
    }
}
