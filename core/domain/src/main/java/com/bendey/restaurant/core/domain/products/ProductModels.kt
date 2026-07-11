package com.bendey.restaurant.core.domain.products

import com.bendey.restaurant.core.domain.catalog.PreparationAreaItem

enum class ProductosTab(val label: String) {
    PRODUCTOS("Productos"),
    CATEGORIAS("Categorías"),
}

enum class CatalogSection(val label: String) {
    PRODUCTOS("Productos"),
    MODIFICADORES("Modificadores"),
    AREAS_PREPARACION("Áreas prep."),
    COMBOS("Combos"),
}

enum class IgvAffectation(val code: String, val label: String) {
    GRAVADO("10", "10 - Gravado IGV"),
    EXONERADO("20", "20 - Exonerado"),
    INAFECTO("30", "30 - Inafecto"),
    EXPORTACION("40", "40 - Exportación"),
    ;

    companion object {
        fun fromCode(code: String?): IgvAffectation =
            entries.firstOrNull { it.code == code?.trim() } ?: GRAVADO

        fun isGravado(code: String): Boolean {
            val c = code.trim()
            return c !in setOf("20", "21", "30", "31", "32", "33", "34", "35", "36", "40")
        }
    }
}

data class ProductItem(
    val id: Int,
    val code: String,
    val name: String,
    val description: String?,
    val salePrice: Double,
    val purchasePrice: Double?,
    val unit: String,
    val categoryId: Int?,
    val categoryName: String?,
    val preparationAreaId: Int?,
    val preparationArea: PreparationAreaItem?,
    val imageUrl: String?,
    val manageStock: Boolean,
    val minStock: Double,
    val hasModifiers: Boolean,
    val hasVariants: Boolean,
    val availableForSale: Boolean,
    val igvAffectationType: String,
    val priceIncludesIgv: Boolean,
    val active: Boolean,
)

data class ProductFormInput(
    val name: String = "",
    val code: String = "",
    val description: String = "",
    val salePrice: String = "",
    val purchasePrice: String = "",
    val categoryId: Int? = null,
    val preparationAreaId: Int? = null,
    val igvAffectation: IgvAffectation = IgvAffectation.GRAVADO,
    val priceIncludesIgv: Boolean = true,
    val availableForSale: Boolean = true,
    val manageStock: Boolean = false,
    val minStock: String = "0",
    val initialStock: String = "",
    val hasModifiers: Boolean = false,
    val hasVariants: Boolean = false,
    val modifierGroupIds: List<Int> = emptyList(),
    val presentations: List<com.bendey.restaurant.core.domain.catalog.ProductPresentation> = emptyList(),
    val imageUrl: String? = null,
    val pendingImageBytes: ByteArray? = null,
    val pendingImageMimeType: String? = null,
)

data class ProductDetail(
    val product: ProductItem,
    val modifierGroupIds: List<Int>,
    val presentations: List<com.bendey.restaurant.core.domain.catalog.ProductPresentation>,
)

data class CategoryItem(
    val id: Int,
    val name: String,
    val description: String?,
)

data class ProductListQuery(
    val query: String = "",
    val categoryId: Int? = null,
    val preparationAreaId: Int? = null,
    val branchId: Int? = null,
    val page: Int = 1,
    val perPage: Int = 25,
)

data class ProductReportBranchStock(
    val branchId: Int,
    val branchName: String,
    val quantity: Double,
)

data class ProductReportItem(
    val id: Int,
    val code: String,
    val name: String,
    val unit: String,
    val salePrice: Double,
    val purchasePrice: Double?,
    val categoryName: String?,
    val manageStock: Boolean,
    val minStock: Double,
    val stockTotal: Double,
    val stockByBranch: List<ProductReportBranchStock>,
    val active: Boolean,
)

data class ProductReportQuery(
    val query: String = "",
    val categoryId: Int? = null,
    val branchId: Int? = null,
    val stockLessThan: Double? = null,
    val page: Int = 1,
    val perPage: Int = 25,
)

fun generateProductCode(): String {
    val raw = "${System.currentTimeMillis()}${(0..999999).random().toString().padStart(6, '0')}"
    val base12 = raw.filter { it.isDigit() }.takeLast(12).padStart(12, '0').take(12)
    var sum = 0
    base12.forEachIndexed { index, char ->
        val digit = char.digitToInt()
        sum += (if (index % 2 == 0) 1 else 3) * digit
    }
    val checkDigit = (10 - (sum % 10)) % 10
    return "$base12$checkDigit"
}

fun ProductItem.toFormInput(
    modifierGroupIds: List<Int> = emptyList(),
    presentations: List<com.bendey.restaurant.core.domain.catalog.ProductPresentation> = emptyList(),
) = ProductFormInput(
    name = name,
    code = code,
    description = description.orEmpty(),
    salePrice = formatProductAmount(salePrice),
    purchasePrice = purchasePrice?.let { formatProductAmount(it) }.orEmpty(),
    categoryId = categoryId,
    preparationAreaId = preparationAreaId ?: preparationArea?.id,
    igvAffectation = IgvAffectation.fromCode(igvAffectationType),
    priceIncludesIgv = priceIncludesIgv,
    availableForSale = availableForSale,
    manageStock = manageStock,
    minStock = if (manageStock) formatProductAmount(minStock) else "0",
    hasModifiers = hasModifiers,
    hasVariants = hasVariants,
    modifierGroupIds = modifierGroupIds,
    presentations = presentations,
    imageUrl = imageUrl,
)

fun ProductDetail.toFormInput() = product.toFormInput(modifierGroupIds, presentations)

private fun formatProductAmount(value: Double): String {
    val rounded = kotlin.math.round(value * 100.0) / 100.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
}
