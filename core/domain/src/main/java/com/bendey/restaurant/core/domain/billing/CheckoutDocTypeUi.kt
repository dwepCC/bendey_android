package com.bendey.restaurant.core.domain.billing

data class CheckoutDocTypeGroup(
    val key: String,
    val label: String,
)

fun normalizeDocTypeKey(docType: String): String =
    docType.lowercase().replace("\\s+".toRegex(), "")

fun docTypeShortLabel(docType: String, sunatCode: String? = null): String {
    val code = sunatCode?.trim().orEmpty()
    when (code) {
        "00" -> return "Nota de venta"
        "03" -> return "Boleta"
        "01" -> return "Factura"
    }
    val normalized = normalizeDocTypeKey(docType)
    return when {
        normalized.contains("credito") || normalized.contains("crédito") -> "N. Crédito"
        normalized.contains("debito") || normalized.contains("débito") -> "N. Débito"
        (normalized.contains("nota") && normalized.contains("venta")) || normalized == "notadeventa" -> "Nota de venta"
        normalized == "boleta" -> "Boleta"
        normalized == "factura" -> "Factura"
        else -> {
            val trimmed = docType.trim()
            if (trimmed.length > 14) "${trimmed.take(12)}…" else trimmed
        }
    }
}

fun groupCheckoutDocTypes(series: List<DocumentSeries>): List<CheckoutDocTypeGroup> {
    val seen = linkedSetOf<String>()
    val groups = mutableListOf<CheckoutDocTypeGroup>()
    series.forEach { item ->
        val key = normalizeDocTypeKey(item.docType)
        if (!seen.add(key)) return@forEach
        groups.add(CheckoutDocTypeGroup(key = key, label = docTypeShortLabel(item.docType, item.sunatCode)))
    }
    return groups
}

fun seriesForDocType(series: List<DocumentSeries>, docType: String): List<DocumentSeries> {
    val key = normalizeDocTypeKey(docType)
    return series.filter { normalizeDocTypeKey(it.docType) == key }
}

fun firstSeriesForDocTypeKey(series: List<DocumentSeries>, docTypeKey: String): DocumentSeries? =
    series.firstOrNull { normalizeDocTypeKey(it.docType) == docTypeKey }
