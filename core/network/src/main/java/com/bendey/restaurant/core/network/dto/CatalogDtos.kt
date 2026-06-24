package com.bendey.restaurant.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PreparationAreaDto(
    val id: Int,
    val name: String,
    val description: String = "",
    val color: String = "",
    @SerialName("estimated_minutes") val estimatedMinutes: Int = 0,
    @SerialName("sort_order") val sortOrder: Int = 0,
    val active: Boolean = true,
)

@Serializable
data class PreparationAreaDataResponseDto(
    val data: PreparationAreaDto,
)

@Serializable
data class PreparationAreaUpsertRequestDto(
    val name: String,
    val description: String = "",
    val color: String = "",
    @SerialName("estimated_minutes") val estimatedMinutes: Int = 0,
    @SerialName("sort_order") val sortOrder: Int = 0,
    val active: Boolean = true,
)

@Serializable
data class PreparationAreaStatusRequestDto(
    val active: Boolean,
)

@Serializable
data class ProductPresentationDto(
    val id: Int? = null,
    val name: String,
    @SerialName("sale_price") val salePrice: Double,
    val description: String? = null,
    @SerialName("sort_order") val sortOrder: Int? = null,
    val active: Boolean? = true,
)

@Serializable
data class ModifierOptionDto(
    val id: Int? = null,
    val name: String,
    @SerialName("extra_price") val extraPrice: Double = 0.0,
)

@Serializable
data class ModifierGroupDto(
    val id: Int,
    val name: String,
    val required: Boolean = false,
    @SerialName("multi_select") val multiSelect: Boolean? = null,
    @SerialName("min_select") val minSelect: Int? = null,
    @SerialName("max_select") val maxSelect: Int? = null,
    @SerialName("selection_mode") val selectionMode: String? = null,
    val options: List<ModifierOptionDto> = emptyList(),
)

@Serializable
data class ModifierGroupResponseDto(
    val group: ModifierGroupDto,
)

@Serializable
data class ModifierGroupUpsertRequestDto(
    val name: String,
    val required: Boolean = false,
    @SerialName("multi_select") val multiSelect: Boolean = false,
    @SerialName("min_select") val minSelect: Int = 0,
    @SerialName("max_select") val maxSelect: Int = 0,
    @SerialName("selection_mode") val selectionMode: String? = null,
    val options: List<ModifierOptionDto> = emptyList(),
)

@Serializable
data class BulkImportItemDto(
    @SerialName("row_number") val rowNumber: Int,
    val name: String,
    val code: String? = null,
    val description: String? = null,
    @SerialName("sale_price") val salePrice: Double,
    val unit: String? = "NIU",
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("igv_affectation_type") val igvAffectationType: String? = "10",
    @SerialName("price_includes_igv") val priceIncludesIgv: Boolean? = true,
    @SerialName("manage_stock") val manageStock: Boolean? = false,
    @SerialName("initial_stock") val initialStock: Double? = null,
    @SerialName("preparation_area") val preparationArea: String? = null,
)

@Serializable
data class BulkImportRequestDto(
    val items: List<BulkImportItemDto>,
)

@Serializable
data class BulkImportFailedRowDto(
    val row: Int,
    val name: String,
    val error: String,
)

@Serializable
data class BulkImportResultDto(
    val created: Int = 0,
    @SerialName("stock_registered") val stockRegistered: Int = 0,
    val failed: List<BulkImportFailedRowDto> = emptyList(),
)

@Serializable
data class BulkImportResponseDto(
    val success: Boolean = true,
    val data: BulkImportResultDto,
)

@Serializable
data class ProductImageUploadResponseDto(
    @SerialName("image_url") val imageUrl: String,
)

@Serializable
data class ComboFixedItemDto(
    val id: Int? = null,
    @SerialName("product_id") val productId: Int,
    @SerialName("presentation_id") val presentationId: Int? = null,
    val quantity: Double = 1.0,
    @SerialName("sort_order") val sortOrder: Int? = null,
)

@Serializable
data class ComboSlotOptionDto(
    val id: Int? = null,
    @SerialName("product_id") val productId: Int,
    @SerialName("presentation_id") val presentationId: Int? = null,
    val quantity: Double = 1.0,
    @SerialName("upgrade_price") val upgradePrice: Double = 0.0,
    @SerialName("sort_order") val sortOrder: Int? = null,
)

@Serializable
data class ComboSlotDto(
    val id: Int? = null,
    val name: String,
    @SerialName("min_pick") val minPick: Int = 1,
    @SerialName("max_pick") val maxPick: Int = 1,
    @SerialName("selection_mode") val selectionMode: String? = null,
    @SerialName("sort_order") val sortOrder: Int? = null,
    val options: List<ComboSlotOptionDto> = emptyList(),
)

@Serializable
data class ComboDto(
    val id: Int,
    val name: String,
    val description: String? = null,
    @SerialName("combo_type") val comboType: String = "fixed",
    @SerialName("base_price") val basePrice: Double = 0.0,
    val active: Boolean = true,
    @SerialName("valid_from") val validFrom: String? = null,
    @SerialName("valid_to") val validTo: String? = null,
    @SerialName("sort_order") val sortOrder: Int? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("fixed_items") val fixedItems: List<ComboFixedItemDto> = emptyList(),
    val slots: List<ComboSlotDto> = emptyList(),
    @SerialName("branch_settings") val branchSettings: List<ComboBranchSettingDto> = emptyList(),
)

@Serializable
data class ComboBranchSettingDto(
    @SerialName("branch_id") val branchId: Int,
    val active: Boolean = true,
    @SerialName("price_override") val priceOverride: Double? = null,
)

@Serializable
data class ComboDataResponseDto(
    val data: ComboDto,
)

@Serializable
data class ComboResolveRequestDto(
    @SerialName("branch_id") val branchId: Int,
    @SerialName("combo_config_json") val comboConfigJson: String = "{}",
)

@Serializable
data class ComboResolveResponseDto(
    val data: ResolvedComboSnapshotDto? = null,
    @SerialName("unit_price") val unitPrice: Double? = null,
)

@Serializable
data class ResolvedComboSnapshotDto(
    @SerialName("combo_id") val comboId: Int? = null,
    @SerialName("combo_name") val comboName: String? = null,
    @SerialName("combo_type") val comboType: String? = null,
    @SerialName("unit_price") val unitPrice: Double? = null,
    val components: List<ResolvedComboComponentDto> = emptyList(),
)

@Serializable
data class ResolvedComboComponentDto(
    @SerialName("product_id") val productId: Int? = null,
    @SerialName("product_name") val productName: String = "",
    @SerialName("presentation_name") val presentationName: String? = null,
    val quantity: Double = 1.0,
    @SerialName("modifiers_json") val modifiersJson: String? = null,
)

@Serializable
data class ComboUpsertRequestDto(
    val name: String,
    val description: String? = null,
    @SerialName("combo_type") val comboType: String = "fixed",
    @SerialName("base_price") val basePrice: Double,
    val active: Boolean = true,
    @SerialName("valid_from") val validFrom: String? = null,
    @SerialName("valid_to") val validTo: String? = null,
    @SerialName("fixed_items") val fixedItems: List<ComboFixedItemDto> = emptyList(),
    val slots: List<ComboSlotDto> = emptyList(),
    @SerialName("branch_settings") val branchSettings: List<ComboBranchSettingDto> = emptyList(),
)

@Serializable
data class DeliveryCompanyDto(
    val id: Int,
    val name: String,
    @SerialName("sort_order") val sortOrder: Int? = null,
    val active: Boolean = true,
)

@Serializable
data class DeliveryDriverDto(
    val id: Int,
    val name: String,
    val phone: String? = null,
    @SerialName("vehicle_type") val vehicleType: String? = null,
    val plate: String? = null,
    val active: Boolean = true,
    val notes: String? = null,
    @SerialName("delivery_company_id") val deliveryCompanyId: Int? = null,
    @SerialName("delivery_company") val deliveryCompany: DeliveryCompanyDto? = null,
)

@Serializable
data class DeliveryCompanyUpsertRequestDto(
    val name: String,
    val active: Boolean? = null,
    @SerialName("sort_order") val sortOrder: Int? = null,
)

@Serializable
data class DeliveryDriverUpsertRequestDto(
    val name: String,
    val phone: String? = null,
    @SerialName("vehicle_type") val vehicleType: String? = null,
    val plate: String? = null,
    val notes: String? = null,
    val active: Boolean? = null,
    @SerialName("delivery_company_id") val deliveryCompanyId: Int? = null,
)

@Serializable
data class CompanyConfigDto(
    @SerialName("business_name") val businessName: String = "",
    @SerialName("trade_name") val tradeName: String = "",
    val ruc: String = "",
    val address: String = "",
    val ubigeo: String? = null,
    val phone: String = "",
    val email: String = "",
    val currency: String = "PEN",
    @SerialName("tax_rate") val taxRate: Double? = null,
    @SerialName("logo_url") val logoUrl: String = "",
    @SerialName("color_theme") val colorTheme: String? = null,
    @SerialName("additional_notes") val additionalNotes: String? = null,
)

@Serializable
data class CompanyConfigResponseDto(
    val success: Boolean = true,
    val data: CompanyConfigDto,
)

@Serializable
data class SunatConfigDto(
    @SerialName("sunat_enabled") val sunatEnabled: Boolean = false,
    @SerialName("tax_rate") val taxRate: Double = 18.0,
    @SerialName("igv_regime") val igvRegime: String = "",
    @SerialName("tax_benefit_zone") val taxBenefitZone: Boolean = false,
)

@Serializable
data class BranchDto(
    val id: Int,
    val name: String,
    val address: String = "",
    val phone: String = "",
    @SerialName("fiscal_domicile_code") val fiscalDomicileCode: String? = null,
    @SerialName("is_main") val isMain: Boolean = false,
    val active: Boolean? = true,
)

@Serializable
data class BranchUpsertRequestDto(
    val name: String,
    val address: String = "",
    val phone: String = "",
    @SerialName("fiscal_domicile_code") val fiscalDomicileCode: String? = null,
    @SerialName("is_main") val isMain: Boolean = false,
    val active: Boolean? = null,
)

@Serializable
data class RestaurantSettingsDto(
    @SerialName("has_deletion_pin") val hasDeletionPin: Boolean = false,
)

@Serializable
data class RestaurantSettingsUpdateRequestDto(
    @SerialName("deletion_pin") val deletionPin: String,
)
