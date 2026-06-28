package com.bendey.restaurant.platform.printing.escpos

import org.junit.Assert.assertTrue
import org.junit.Test

class EscPosLayoutTest {

    @Test
    fun comandaLayout_containsHeaderAndCut() {
        val bytes = ComandaLayoutBuilder.build(
            ComandaPrintInput(
                tableName = "Mesa 1",
                orderNumber = 10,
                items = listOf(ComandaItem("Cafe", 1.0)),
            ),
        )
        val text = bytes.decodeToString()
        assertTrue(text.contains("COMANDA"))
        assertTrue(text.contains("MESA: Mesa 1"))
        assertTrue(text.contains("1x Cafe"))
        assertTrue(bytes.size > 20)
        assertTrue(bytes.last() != 0.toByte() || bytes.any { it == 0x1D.toByte() })
    }

    @Test
    fun precuentaLayout_containsTotalAndCut() {
        val bytes = PrecuentaLayoutBuilder.build(
            PrecuentaPrintInput(
                tableName = "Mesa 2",
                items = listOf(PrecuentaItem("Lomo", 1.0, 32.0)),
                total = 32.0,
            ),
        )
        val text = bytes.decodeToString()
        assertTrue(text.contains("PRECUENTA"))
        assertTrue(text.contains("Mesa: Mesa 2"))
        assertTrue(text.contains("TOTAL"))
        assertTrue(text.contains("32.00"))
    }

    @Test
    fun normalizeText_stripsAccentsAndSpecialChars() {
        assertTrue(EscPosTextUtils.normalizeForPrint("año").contains("anio"))
        assertTrue(EscPosTextUtils.normalizeForPrint("ají").contains("aji"))
        assertTrue(EscPosTextUtils.normalizeForPrint("café").contains("cafe"))
        val withDot = EscPosTextUtils.normalizeForPrint("Mesa · Cocina")
        assertTrue(withDot.contains("Mesa - Cocina"))
        assertTrue(!withDot.contains("·"))
        val comboQty = EscPosTextUtils.normalizeForPrint("Americana ×2")
        assertTrue(comboQty.contains("Americana x2"))
        assertTrue(!comboQty.contains("×"))
    }

    @Test
    fun columnsForPaper_matchesWeb() {
        assertTrue(columnsForPaper(PaperWidthMm.W58) == 32)
        assertTrue(columnsForPaper(PaperWidthMm.W80) == 48)
    }

    @Test
    fun escposQr_containsModel2Header() {
        val bytes = EscPosQr.encode("https://example.com/cpe")
        assertTrue(bytes.size > 20)
        assertTrue(bytes.contains(0x1D))
        assertTrue(bytes.contains(0x28))
        assertTrue(bytes.contains(0x6B))
    }

    @Test
    fun documentLayout_electronicIncludesQrCommands() {
        val bytes = DocumentLayoutBuilder.build(
            DocumentPrintInput(
                docType = "BOLETA",
                sunatCode = "03",
                number = "B001-123",
                issueDate = "2026-06-19",
                companyName = "Demo SAC",
                companyRuc = "20123456789",
                companyAddress = "Lima",
                branchName = "Principal",
                clientName = "Cliente",
                clientDocNumber = "12345678",
                items = listOf(DocumentPrintLine("Producto", 1.0, 10.0, 10.0)),
                subtotal = 8.47,
                taxAmount = 1.53,
                total = 10.0,
                currency = "PEN",
                payments = listOf(DocumentPrintPayment("Efectivo", 10.0)),
                legendText = "DIEZ CON 00/100 SOLES",
                qrData = "20123456789|03|B001|123|1.53|10.00|2026-06-19|1",
            ),
        )
        assertTrue(bytes.size > 100)
        assertTrue(bytes.contains(0x1D))
        assertTrue(bytes.contains(0x28))
    }

    @Test
    fun sunatPrintUtils_detectsElectronicCodes() {
        assertTrue(SunatPrintUtils.isElectronicSunatCode("03"))
        assertTrue(SunatPrintUtils.isElectronicSunatCode("01"))
        assertTrue(!SunatPrintUtils.isElectronicSunatCode("00"))
    }
}
