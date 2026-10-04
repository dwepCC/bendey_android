package com.bendey.restaurant.core.domain.onboarding.wizard

import com.bendey.restaurant.core.domain.catalog.BulkImportRow

/**
 * Fila de la vista previa de "Pegar mi lista". Se puede editar (nombre/precio/categoría) y quitar;
 * NUNCA se importa sin que el usuario pulse "Crear mi carta".
 */
data class PastePreviewRow(
    val id: Int,
    val line: Int,
    val category: String,
    val name: String,
    /** Texto del precio tal como se muestra/edita ("28.5"); vacío si no hubo precio. */
    val priceText: String,
    val doubtful: Boolean,
    val error: String?,
    val warning: String?,
    /** Marcada para importar. Las dudosas arrancan desmarcadas; las inválidas no se pueden marcar. */
    val selected: Boolean,
    /** Ya hay un producto con ese nombre en la carta: se excluye para no actualizarlo sin querer. */
    val alreadyExists: Boolean = false,
) {
    val price: Double? get() = parsePrice(priceText)

    /** Se puede importar: sin error, marcada y que no exista ya. */
    val importable: Boolean get() = error == null && selected && !alreadyExists
}

/** "28,50" / "28.5" / "S/ 28.5" -> 28.5; null si no hay número. */
fun parsePrice(text: String): Double? {
    val cleaned = text.trim().replace(Regex("(?i)s/\\.?"), "").replace(",", ".").trim()
    if (cleaned.isEmpty()) return null
    return cleaned.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
}

private fun formatPrice(price: Double?): String = when {
    price == null -> ""
    price == price.toLong().toDouble() -> price.toLong().toString()
    else -> price.toString()
}

private fun normalizedKey(name: String) = name.trim().lowercase()

/** Convierte lo parseado en filas de vista previa y marca las que ya existen en la carta. */
fun buildPastePreview(result: PasteParseResult, existingNames: Set<String>): List<PastePreviewRow> {
    val existing = existingNames.map(::normalizedKey).toSet()
    return result.rows.mapIndexed { index, row ->
        PastePreviewRow(
            id = index,
            line = row.line,
            category = row.category,
            name = row.name,
            priceText = formatPrice(row.price),
            doubtful = row.doubtful,
            error = row.error,
            warning = row.warning,
            selected = row.error == null && !row.doubtful,
            alreadyExists = row.name.isNotEmpty() && normalizedKey(row.name) in existing,
        )
    }
}

/**
 * Aplica una edición y vuelve a clasificar la fila. Una fila que pasa a ser válida y no dudosa se
 * marca sola; una dudosa conserva la marca que el usuario le puso.
 */
fun PastePreviewRow.edited(
    name: String = this.name,
    priceText: String = this.priceText,
    category: String = this.category,
    existingNames: Set<String> = emptySet(),
): PastePreviewRow {
    val cleanName = normalizeProductName(name)
    val price = parsePrice(priceText)
    val cls = classifyPasteRow(cleanName, price)
    val exists = cleanName.isNotEmpty() && normalizedKey(cleanName) in existingNames.map(::normalizedKey)
    return copy(
        name = name,
        priceText = priceText,
        category = category.trim().ifEmpty { PasteMenu.DEFAULT_CATEGORY },
        doubtful = cls.doubtful,
        error = cls.error,
        warning = cls.error ?: cls.warning,
        selected = if (cls.error != null) false else if (cls.doubtful) selected else true,
        alreadyExists = exists,
    )
}

/** Filas que se mandan a `bulk-import/restaurant`: solo las importables, sin stock, sin canal MENU. */
fun pasteImportRows(rows: List<PastePreviewRow>): List<BulkImportRow> =
    rows.filter { it.importable }.mapNotNull { row ->
        val price = row.price ?: return@mapNotNull null
        BulkImportRow(
            rowNumber = row.line,
            name = normalizeProductName(row.name),
            code = "",
            description = "",
            salePrice = price,
            unit = "NIU",
            categoryName = row.category.trim().ifEmpty { PasteMenu.DEFAULT_CATEGORY },
            // Vacía: el motor la resuelve como "Cocina" para platos.
            preparationArea = "",
            igvAffectationType = "10",
            priceIncludesIgv = true,
            manageStock = false,
            initialStock = 0.0,
        )
    }

/** Mensaje final: "Creamos N platos. M no se pudieron crear." */
fun importResultMessage(created: Int, failed: Int): String = buildString {
    append(WizardCopy.RESULT_CREATED.format(created))
    if (failed > 0) append(' ').append(WizardCopy.RESULT_FAILED.format(failed))
}
