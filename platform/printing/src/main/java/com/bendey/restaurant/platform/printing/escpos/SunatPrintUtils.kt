package com.bendey.restaurant.platform.printing.escpos

import com.bendey.restaurant.core.domain.locale.LabelKey

object SunatPrintUtils {

    // Títulos del ticket por código de comprobante. Se arman con las etiquetas del país (I5); en Perú
    // salen idénticos a los de siempre: «NOTA DE VENTA», «FACTURA ELECTRONICA»,
    // «BOLETA DE VENTA ELECTRONICA», «NOTA DE CREDITO», «NOTA DE DEBITO» (sin tildes, por la ticketera).
    private val facturaElectronica: String
        get() = "${LabelKey.DOC_INVOICE.upper} ELECTRONICA"
    private val boletaElectronica: String
        get() = "${LabelKey.DOC_RECEIPT.peruAs("BOLETA DE VENTA").uppercase()} ELECTRONICA"
    private val notaDeVenta: String
        get() = LabelKey.DOC_SALES_NOTE.upper

    private fun tipoComprobante(code: String): String? = when (code) {
        "00" -> notaDeVenta
        "01" -> facturaElectronica
        "03" -> boletaElectronica
        "07" -> LabelKey.DOC_CREDIT_NOTE.peruAs("NOTA DE CREDITO").uppercase()
        "08" -> LabelKey.DOC_DEBIT_NOTE.peruAs("NOTA DE DEBITO").uppercase()
        else -> null
    }

    fun isElectronicSunatCode(code: String): Boolean {
        val c = code.trim()
        return c == "01" || c == "03" || c == "07" || c == "08"
    }

    fun tipoComprobanteLabel(sunatCode: String, docType: String): String {
        val c = sunatCode.trim()
        tipoComprobante(c)?.let { return it }
        val dt = docType.lowercase()
        return when {
            dt.contains("factura") -> facturaElectronica
            dt.contains("boleta") -> boletaElectronica
            dt.contains("nota") && dt.contains("venta") -> notaDeVenta
            docType.isNotBlank() -> docType.uppercase()
            else -> "COMPROBANTE"
        }
    }
}
