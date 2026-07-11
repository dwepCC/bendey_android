package com.bendey.restaurant.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductDto(
    val id: Int,
    val code: String = "",
    val name: String,
    val description: String? = null,
    @SerialName("sale_price") val salePrice: Double = 0.0,
    @SerialName("purchase_price") val purchasePrice: Double? = null,
    val unit: String = "NIU",
    @SerialName("category_id") val categoryId: Int? = null,
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("preparation_area_id") val preparationAreaId: Int? = null,
    @SerialName("preparation_area") val preparationArea: PreparationAreaDto? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("is_restaurant") val isRestaurant: Boolean = true,
    @SerialName("has_modifiers") val hasModifiers: Boolean = false,
    @SerialName("has_variants") val hasVariants: Boolean = false,
    @SerialName("manage_stock") val manageStock: Boolean = false,
    @SerialName("min_stock") val minStock: Double = 0.0,
    @SerialName("stock_total") val stockTotal: Double? = null,
    @SerialName("stock_by_branch") val stockByBranch: List<ProductReportBranchStockDto>? = null,
    val active: Boolean = true,
    @SerialName("available_for_sale") val availableForSale: Boolean = true,
    @SerialName("igv_affectation_type") val igvAffectationType: String? = null,
    @SerialName("price_includes_igv") val priceIncludesIgv: Boolean? = null,
)

@Serializable
data class ProductListResponseDto(
    val data: List<ProductDto> = emptyList(),
    val total: Int? = null,
)

@Serializable
data class CategoryDto(
    val id: Int,
    val name: String,
    val description: String? = null,
    @SerialName("parent_id") val parentId: Int? = null,
    val active: Boolean? = null,
)

@Serializable
data class ProductDataResponseDto(
    val data: ProductDto,
)

@Serializable
data class ProductDetailResponseDto(
    val data: ProductDto,
    @SerialName("modifier_group_ids") val modifierGroupIds: List<Int> = emptyList(),
    val presentations: List<ProductPresentationDto> = emptyList(),
)

@Serializable
data class CategoryDataResponseDto(
    val data: CategoryDto,
)

@Serializable
data class CreateProductRequestDto(
    val name: String,
    val code: String = "",
    val description: String = "",
    @SerialName("image_url") val imageUrl: String = "",
    val unit: String = "NIU",
    @SerialName("sale_price") val salePrice: Double,
    @SerialName("purchase_price") val purchasePrice: Double = 0.0,
    @SerialName("category_id") val categoryId: Int? = null,
    @SerialName("preparation_area_id") val preparationAreaId: Int? = null,
    @SerialName("igv_affectation_type") val igvAffectationType: String = "10",
    @SerialName("price_includes_igv") val priceIncludesIgv: Boolean = true,
    @SerialName("manage_stock") val manageStock: Boolean = false,
    @SerialName("min_stock") val minStock: Double = 0.0,
    @SerialName("initial_stock") val initialStock: Double? = null,
    @SerialName("has_modifiers") val hasModifiers: Boolean = false,
    @SerialName("has_variants") val hasVariants: Boolean = false,
    @SerialName("is_restaurant") val isRestaurant: Boolean = true,
    @SerialName("modifier_group_ids") val modifierGroupIds: List<Int> = emptyList(),
    val presentations: List<ProductPresentationDto> = emptyList(),
    @SerialName("available_for_sale") val availableForSale: Boolean = true,
)

@Serializable
data class UpdateProductRequestDto(
    val name: String? = null,
    val code: String? = null,
    val description: String? = null,
    @SerialName("sale_price") val salePrice: Double? = null,
    @SerialName("purchase_price") val purchasePrice: Double? = null,
    @SerialName("category_id") val categoryId: Int? = null,
    @SerialName("preparation_area_id") val preparationAreaId: Int? = null,
    @SerialName("igv_affectation_type") val igvAffectationType: String? = null,
    @SerialName("price_includes_igv") val priceIncludesIgv: Boolean? = null,
    @SerialName("manage_stock") val manageStock: Boolean? = null,
    @SerialName("min_stock") val minStock: Double? = null,
    @SerialName("available_for_sale") val availableForSale: Boolean? = null,
    @SerialName("is_restaurant") val isRestaurant: Boolean? = null,
    @SerialName("has_modifiers") val hasModifiers: Boolean? = null,
    @SerialName("has_variants") val hasVariants: Boolean? = null,
    @SerialName("modifier_group_ids") val modifierGroupIds: List<Int>? = null,
    val presentations: List<ProductPresentationDto>? = null,
)

@Serializable
data class CategoryUpsertRequestDto(
    val name: String,
    val description: String = "",
)

@Serializable
data class StockSummaryResponseDto(
    val data: Map<String, Double> = emptyMap(),
)

@Serializable
data class ProductReportBranchStockDto(
    @SerialName("branch_id") val branchId: Int = 0,
    @SerialName("branch_name") val branchName: String = "",
    val quantity: Double = 0.0,
)

@Serializable
data class ProductReportDto(
    val id: Int,
    val code: String = "",
    val name: String,
    val unit: String = "NIU",
    @SerialName("sale_price") val salePrice: Double = 0.0,
    @SerialName("purchase_price") val purchasePrice: Double? = null,
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("manage_stock") val manageStock: Boolean = false,
    @SerialName("min_stock") val minStock: Double = 0.0,
    @SerialName("stock_total") val stockTotal: Double = 0.0,
    @SerialName("stock_by_branch") val stockByBranch: List<ProductReportBranchStockDto> = emptyList(),
    val active: Boolean = true,
)
