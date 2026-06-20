package com.bendey.restaurant.platform.printing.escpos

object SunatPrintUtils {

    private val TIPO_COMPROBANTE = mapOf(
        "00" to "NOTA DE VENTA",
        "01" to "FACTURA ELECTRONICA",
        "03" to "BOLETA DE VENTA ELECTRONICA",
        "07" to "NOTA DE CREDITO",
        "08" to "NOTA DE DEBITO",
    )

    fun isElectronicSunatCode(code: String): Boolean {
        val c = code.trim()
        return c == "01" || c == "03" || c == "07" || c == "08"
    }

    fun tipoComprobanteLabel(sunatCode: String, docType: String): String {
        val c = sunatCode.trim()
        TIPO_COMPROBANTE[c]?.let { return it }
        val dt = docType.lowercase()
        return when {
            dt.contains("factura") -> "FACTURA ELECTRONICA"
            dt.contains("boleta") -> "BOLETA DE VENTA ELECTRONICA"
            dt.contains("nota") && dt.contains("venta") -> "NOTA DE VENTA"
            docType.isNotBlank() -> docType.uppercase()
            else -> "COMPROBANTE"
        }
    }
}
