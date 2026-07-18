package com.bendey.restaurant.core.domain.billing

data class CheckoutDocTypeGroup(
    val key: String,
    val label: String,
    val locked: Boolean = false,
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

fun groupCheckoutDocTypes(series: List<DocumentSeries>): List<CheckoutDocTypeGroup> =
    buildDocTypeGroups(unlockedSeries = series, lockedSeries = emptyList())

/**
 * Igual que [groupCheckoutDocTypes], pero además incluye los tipos de documento presentes
 * únicamente en [lockedSeries] (p. ej. boleta/factura sin el módulo `billing` en el plan),
 * marcados con `locked = true` en vez de ocultarlos.
 */
fun groupCheckoutDocTypesWithLocked(
    unlockedSeries: List<DocumentSeries>,
    lockedSeries: List<DocumentSeries>,
): List<CheckoutDocTypeGroup> = buildDocTypeGroups(unlockedSeries, lockedSeries)

private data class DocTypeGroupSeed(
    val key: String,
    val docType: String,
    val sunatCode: String?,
    val locked: Boolean,
)

private fun buildDocTypeGroups(
    unlockedSeries: List<DocumentSeries>,
    lockedSeries: List<DocumentSeries>,
): List<CheckoutDocTypeGroup> {
    val byKey = linkedMapOf<String, DocTypeGroupSeed>()
    unlockedSeries.forEach { item ->
        val key = normalizeDocTypeKey(item.docType)
        if (!byKey.containsKey(key)) {
            byKey[key] = DocTypeGroupSeed(key, item.docType, item.sunatCode, locked = false)
        }
    }
    lockedSeries.forEach { item ->
        val key = normalizeDocTypeKey(item.docType)
        if (!byKey.containsKey(key)) {
            byKey[key] = DocTypeGroupSeed(key, item.docType, item.sunatCode, locked = true)
        }
    }
    return byKey.values
        .sortedWith(compareBy({ docTypeSortOrder(it.docType, it.sunatCode) }, { it.docType }))
        .map { seed ->
            CheckoutDocTypeGroup(
                key = seed.key,
                label = docTypeShortLabel(seed.docType, seed.sunatCode),
                locked = seed.locked,
            )
        }
}

fun seriesForDocType(series: List<DocumentSeries>, docType: String): List<DocumentSeries> {
    val key = normalizeDocTypeKey(docType)
    return series.filter { normalizeDocTypeKey(it.docType) == key }
}

fun firstSeriesForDocTypeKey(series: List<DocumentSeries>, docTypeKey: String): DocumentSeries? =
    series.firstOrNull { normalizeDocTypeKey(it.docType) == docTypeKey }
