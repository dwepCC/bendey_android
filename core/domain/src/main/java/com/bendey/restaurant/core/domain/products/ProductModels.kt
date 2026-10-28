package com.bendey.restaurant.core.domain.products

import com.bendey.restaurant.core.domain.catalog.PreparationAreaItem
import com.bendey.restaurant.core.domain.locale.LabelKey
import com.bendey.restaurant.core.domain.locale.TenantLocaleHolder

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

/** comercial: se compra y vende, sin receta · insumo: no aparece en POS, ingrediente de recetas ·
 * elaborado: tiene receta, costo calculado, nunca stock propio — ver feature/produccion. */
enum class ProductType(val code: String, val label: String) {
    COMERCIAL("comercial", "Comercial — se compra y se vende"),
    INSUMO("insumo", "Insumo — ingrediente de recetas, no aparece en POS"),
    ELABORADO("elaborado", "Elaborado — tiene receta, costo calculado"),
    ;

    companion object {
        fun fromCode(code: String?): ProductType =
            entries.firstOrNull { it.code == code?.trim() } ?: COMERCIAL
    }
}

/** Subconjunto práctico del catálogo SUNAT 03 (unidades de medida) para negocios de restaurante. */
enum class UnitOfMeasure(val code: String, val label: String) {
    NIU("NIU", "NIU - Unidad"),
    KGM("KGM", "KGM - Kilogramo"),
    GRM("GRM", "GRM - Gramo"),
    LTR("LTR", "LTR - Litro"),
    MLT("MLT", "MLT - Mililitro"),
    BX("BX", "BX - Caja"),
    PK("PK", "PK - Paquete"),
    DZN("DZN", "DZN - Docena"),
    BG("BG", "BG - Bolsa"),
    ;

    companion object {
        fun fromCode(code: String?): UnitOfMeasure =
            entries.firstOrNull { it.code == code?.trim()?.uppercase() } ?: NIU
    }
}

enum class IgvAffectation(val code: String) {
    GRAVADO("10"),
    EXONERADO("20"),
    INAFECTO("30"),
    EXPORTACION("40"),
    ;

    /** Texto visible, según el país de la sesión (I5). En Perú: «10 - Gravado IGV», «20 - Exonerado»… */
    val label: String
        get() = when (this) {
            GRAVADO -> "10 - ${LabelKey.TAX_TAXED.text} ${LabelKey.TAX_NAME.text}"
            EXONERADO -> "20 - ${LabelKey.TAX_EXEMPT.text}"
            INAFECTO -> "30 - ${LabelKey.TAX_UNAFFECTED.text}"
            EXPORTACION -> "40 - Exportación"
        }

    companion object {
        fun fromCode(code: String?): IgvAffectation =
            entries.firstOrNull { it.code == code?.trim() } ?: GRAVADO

        /**
         * Opciones (código, texto) del selector de impuesto de un producto, según el país: en Perú el
         * catálogo SUNAT 07 de siempre ([entries]); en Ecuador las tarifas del IVA del locale
         * (`tax_rates`: «4» IVA 15 %, «5» 5 %, «0» 0 %, «6» no objeto, «7» exento).
         */
        fun formOptions(): List<Pair<String, String>> =
            TenantLocaleHolder.current.countryTaxRates?.map { it.code to it.label }
                ?: entries.map { it.code to it.label }

        /**
         * Código que muestra el formulario para lo que trae el producto. Perú: igual que antes
         * ([fromCode], un código fuera de 10/20/30/40 se ve como «10»). Ecuador: el código si está en el
         * catálogo del país; si no, la tarifa por defecto («4»).
         */
        fun formCode(code: String?): String {
            val locale = TenantLocaleHolder.current
            val rates = locale.countryTaxRates ?: return fromCode(code).code
            val c = code?.trim().orEmpty()
            return if (rates.any { it.code == c }) c else locale.defaultTaxCode ?: c
        }

        /** Código con que nace un producto nuevo: «10» en Perú, la tarifa por defecto del país en Ecuador. */
        fun defaultFormCode(): String = TenantLocaleHolder.current.defaultTaxCode ?: GRAVADO.code

        fun isGravado(code: String): Boolean {
            // Ecuador: grava el código con tarifa mayor que 0 («0», «6» y «7» no llevan IVA).
            val locale = TenantLocaleHolder.current
            if (locale.countryTaxRates != null) return (locale.taxRateOf(code) ?: 0.0) > 0.0
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
    /** Stock separado por presentación; el del producto es la suma. */
    val stockByPresentation: Boolean = false,
    val hasModifiers: Boolean,
    val hasVariants: Boolean,
    val availableForSale: Boolean,
    val igvAffectationType: String,
    val priceIncludesIgv: Boolean,
    val active: Boolean,
    val productType: ProductType = ProductType.COMERCIAL,
)

data class ProductFormInput(
    val name: String = "",
    val code: String = "",
    val description: String = "",
    val salePrice: String = "",
    val purchasePrice: String = "",
    val unit: String = "NIU",
    val categoryId: Int? = null,
    val preparationAreaId: Int? = null,
    /** Código de impuesto de la línea: catálogo SUNAT 07 en Perú («10»), codigoPorcentaje del SRI en Ecuador («4»). */
    val igvAffectationCode: String = IgvAffectation.defaultFormCode(),
    val priceIncludesIgv: Boolean = true,
    val availableForSale: Boolean = true,
    val menuChannelEnabled: Boolean = true,
    val productType: ProductType = ProductType.COMERCIAL,
    val manageStock: Boolean = false,
    val minStock: String = "0",
    val stockByPresentation: Boolean = false,
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
    /** 'non_insumo' excluye insumos; 'insumo'|'comercial'|'elaborado' filtra a ese tipo exacto. */
    val productTypeFilter: String? = null,
    /** Mostrar SOLO productos desactivados (active=false). Por defecto se listan los activos. */
    val inactiveOnly: Boolean = false,
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
    unit = unit,
    categoryId = categoryId,
    preparationAreaId = preparationAreaId ?: preparationArea?.id,
    igvAffectationCode = IgvAffectation.formCode(igvAffectationType),
    priceIncludesIgv = priceIncludesIgv,
    availableForSale = availableForSale,
    productType = productType,
    manageStock = manageStock,
    minStock = if (manageStock) formatProductAmount(minStock) else "0",
    stockByPresentation = stockByPresentation,
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
