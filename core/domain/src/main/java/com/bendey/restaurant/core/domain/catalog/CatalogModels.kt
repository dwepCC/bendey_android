package com.bendey.restaurant.core.domain.catalog

data class ProductPresentation(
    val id: Int? = null,
    val name: String,
    val salePrice: Double,
    val description: String = "",
    val sortOrder: Int = 0,
    val active: Boolean = true,
)

data class ModifierOption(
    val id: Int? = null,
    val name: String,
    val extraPrice: Double = 0.0,
)

enum class ModifierSelectionMode(val apiValue: String, val label: String) {
    SINGLE("single", "Una opción"),
    MULTIPLE("multiple", "Varias opciones"),
    QUANTITY("quantity", "Cantidad"),
    ;

    companion object {
        fun fromApi(value: String?): ModifierSelectionMode =
            entries.firstOrNull { it.apiValue == value?.trim() } ?: SINGLE
    }
}

data class ModifierGroup(
    val id: Int,
    val name: String,
    val required: Boolean,
    val selectionMode: ModifierSelectionMode,
    val minSelect: Int,
    val maxSelect: Int,
    val options: List<ModifierOption>,
)

data class ModifierGroupFormInput(
    val name: String = "",
    val required: Boolean = false,
    val selectionMode: ModifierSelectionMode = ModifierSelectionMode.SINGLE,
    val minSelect: String = "0",
    val maxSelect: String = "0",
    val options: List<ModifierOption> = listOf(ModifierOption(name = "")),
)

enum class ComboType(val apiValue: String, val label: String) {
    FIXED("fixed", "Combo fijo"),
    CONFIGURABLE("configurable", "Con opciones"),
    PROMO("promo", "Promoción"),
    FAMILY("family", "Familiar"),
    BUILD_YOUR_OWN("build_your_own", "Arma tu combo"),
    ;

    companion object {
        fun fromApi(value: String?): ComboType =
            entries.firstOrNull { it.apiValue == value?.trim() } ?: FIXED
    }
}

data class ComboSlotOption(
    val id: Int? = null,
    val productId: Int,
    val productName: String? = null,
    val presentationId: Int? = null,
    val quantity: Double = 1.0,
    val upgradePrice: Double = 0.0,
)

data class ComboSlot(
    val id: Int? = null,
    val name: String,
    val minPick: Int = 1,
    val maxPick: Int = 1,
    val options: List<ComboSlotOption> = emptyList(),
)

data class ComboBranchSetting(
    val branchId: Int,
    val branchName: String? = null,
    val active: Boolean = true,
    val priceOverride: String = "",
)

enum class ComboEditorTab(val label: String) {
    GENERAL("Datos"),
    FIXED("Fijos"),
    SLOTS("Opciones"),
    BRANCHES("Sucursales"),
}

data class ComboFixedItem(
    val id: Int? = null,
    val productId: Int,
    val productName: String? = null,
    val presentationId: Int? = null,
    val quantity: Double = 1.0,
)

data class ComboItem(
    val id: Int,
    val name: String,
    val description: String?,
    val comboType: ComboType,
    val basePrice: Double,
    val active: Boolean,
    val fixedItemsCount: Int,
    val slotsCount: Int = 0,
    val validFrom: String? = null,
    val validTo: String? = null,
)

data class ComboFormInput(
    val name: String = "",
    val description: String = "",
    val comboType: ComboType = ComboType.FIXED,
    val basePrice: String = "",
    val active: Boolean = true,
    val validFrom: String = "",
    val validTo: String = "",
    val fixedItems: List<ComboFixedItem> = emptyList(),
    val slots: List<ComboSlot> = emptyList(),
    val branchSettings: List<ComboBranchSetting> = emptyList(),
)

data class DeliveryCompany(
    val id: Int,
    val name: String,
    val active: Boolean,
)

data class DeliveryDriver(
    val id: Int,
    val name: String,
    val phone: String,
    val vehicleType: String,
    val plate: String,
    val active: Boolean,
    val notes: String,
    val deliveryCompanyId: Int?,
    val deliveryCompanyName: String?,
)

data class DeliveryCompanyFormInput(val name: String = "")

data class DeliveryDriverFormInput(
    val name: String = "",
    val phone: String = "",
    val vehicleType: String = "",
    val plate: String = "",
    val notes: String = "",
    val deliveryCompanyId: Int? = null,
    val active: Boolean = true,
)

data class CompanyConfig(
    val businessName: String,
    val tradeName: String,
    val ruc: String,
    val address: String,
    val phone: String,
    val email: String,
    val currency: String,
    val logoUrl: String,
)

data class CompanyConfigFormInput(
    val businessName: String = "",
    val tradeName: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
)

data class SunatConfig(
    val sunatEnabled: Boolean,
    val taxRate: Double,
    val igvRegime: String,
    val taxBenefitZone: Boolean,
)

data class SunatConfigFormInput(
    val taxRate: String = "",
    val igvRegime: String = "",
    val taxBenefitZone: Boolean = false,
)

data class BranchItem(
    val id: Int,
    val name: String,
    val address: String,
    val phone: String,
    val isMain: Boolean,
    val active: Boolean,
)

data class BranchFormInput(
    val id: Int? = null,
    val name: String = "",
    val address: String = "",
    val phone: String = "",
    val isMain: Boolean = false,
    val active: Boolean = true,
)

data class SeriesFormInput(
    val id: Int? = null,
    val branchId: Int? = null,
    val docType: String = "NOTA DE VENTA",
    val series: String = "",
    val category: String = "venta",
    val sunatCode: String = "00",
    val active: Boolean = true,
)

data class RestaurantSettings(
    val hasDeletionPin: Boolean,
)

data class RestaurantStaffManagementRow(
    val userId: Int,
    val name: String,
    val email: String,
    val active: Boolean,
    val staffId: Int?,
    val employeeType: String,
    val displayName: String,
    val staffCode: String,
    val hasPin: Boolean,
    val staffActive: Boolean,
    val profileComplete: Boolean,
    val branchIds: List<Int>,
    val branchNames: List<String>,
)

data class StaffCreateFormInput(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val employeeType: String = "waiter",
    val pin: String = "",
    val branchIds: List<Int> = emptyList(),
)

data class StaffEditFormInput(
    val userId: Int = 0,
    val name: String = "",
    val email: String = "",
    val employeeType: String = "",
    val pin: String = "",
    val clearPin: Boolean = false,
    val branchIds: List<Int> = emptyList(),
    val hasPin: Boolean = false,
)

enum class RestaurantEmployeeType(val apiValue: String, val label: String) {
    NONE("", "Sin acceso restaurante"),
    ADMIN("admin", "Administrador"),
    SUPERVISOR("supervisor", "Supervisor"),
    CASHIER("cashier", "Cajero"),
    WAITER("waiter", "Moz@"),
    COOK("cook", "Cocina"),
    DRIVER("driver", "Repartidor"),
    ;

    companion object {
        fun fromApi(value: String?): RestaurantEmployeeType =
            entries.firstOrNull { it.apiValue == value.orEmpty().trim() } ?: NONE
    }
}

fun ModifierGroup.toFormInput() = ModifierGroupFormInput(
    name = name,
    required = required,
    selectionMode = selectionMode,
    minSelect = minSelect.toString(),
    maxSelect = maxSelect.toString(),
    options = options.ifEmpty { listOf(ModifierOption(name = "")) },
)

data class BulkImportRow(
    val rowNumber: Int,
    val name: String,
    val code: String,
    val description: String,
    val salePrice: Double,
    val unit: String,
    val categoryName: String,
    val preparationArea: String,
    val igvAffectationType: String,
    val priceIncludesIgv: Boolean,
    val manageStock: Boolean,
    val initialStock: Double,
)

data class BulkImportRowError(
    val row: Int,
    val column: String,
    val message: String,
)

data class BulkImportValidationResult(
    val rows: List<BulkImportRow>,
    val errors: List<BulkImportRowError>,
)

data class BulkImportProgress(
    val totalRows: Int,
    val processedRows: Int,
    val created: Int,
    val failed: List<BulkImportRowError>,
)

fun resolvePublicAssetUrl(baseUrl: String?, url: String?): String {
    if (url.isNullOrBlank()) return ""
    val trimmed = url.trim()
    if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
        return trimmed
    }
    val base = baseUrl?.trim()?.trimEnd('/')?.removeSuffix("/api").orEmpty()
    if (base.isEmpty()) return trimmed
    return "$base${if (trimmed.startsWith("/")) trimmed else "/$trimmed"}"
}
