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

/** Orden UI: Nota de venta → Boleta → Factura → resto. */
fun docTypeSortOrder(docType: String, sunatCode: String? = null): Int {
    when (sunatCode?.trim().orEmpty()) {
        "00" -> return 0
        "03" -> return 1
        "01" -> return 2
    }
    val normalized = normalizeDocTypeKey(docType)
    return when {
        (normalized.contains("nota") && normalized.contains("venta")) || normalized == "notadeventa" -> 0
        normalized == "boleta" -> 1
        normalized == "factura" -> 2
        else -> 99
    }
}

fun groupCheckoutDocTypes(series: List<DocumentSeries>): List<CheckoutDocTypeGroup> {
    val byKey = linkedMapOf<String, DocumentSeries>()
    series.forEach { item ->
        val key = normalizeDocTypeKey(item.docType)
        if (!byKey.containsKey(key)) byKey[key] = item
    }
    return byKey.values
        .sortedWith(compareBy({ docTypeSortOrder(it.docType, it.sunatCode) }, { it.docType }))
        .map { item ->
            CheckoutDocTypeGroup(
                key = normalizeDocTypeKey(item.docType),
                label = docTypeShortLabel(item.docType, item.sunatCode),
            )
        }
}

fun seriesForDocType(series: List<DocumentSeries>, docType: String): List<DocumentSeries> {
    val key = normalizeDocTypeKey(docType)
    return series.filter { normalizeDocTypeKey(it.docType) == key }
}

fun firstSeriesForDocTypeKey(series: List<DocumentSeries>, docTypeKey: String): DocumentSeries? =
    series.firstOrNull { normalizeDocTypeKey(it.docType) == docTypeKey }
