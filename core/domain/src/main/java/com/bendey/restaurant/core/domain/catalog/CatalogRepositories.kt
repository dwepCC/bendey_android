package com.bendey.restaurant.core.domain.catalog

import com.bendey.restaurant.core.domain.model.AppResult

interface ModifiersRepository {
    suspend fun listModifierGroups(): AppResult<List<ModifierGroup>>
    suspend fun createModifierGroup(input: ModifierGroupFormInput): AppResult<ModifierGroup>
    suspend fun updateModifierGroup(id: Int, input: ModifierGroupFormInput): AppResult<ModifierGroup>
    suspend fun deleteModifierGroup(id: Int): AppResult<Unit>
}

interface CombosRepository {
    suspend fun listCombos(): AppResult<List<ComboItem>>
    suspend fun getCombo(id: Int): AppResult<ComboFormInput>
    suspend fun createCombo(input: ComboFormInput): AppResult<ComboItem>
    suspend fun updateCombo(id: Int, input: ComboFormInput): AppResult<ComboItem>
    suspend fun deleteCombo(id: Int): AppResult<Unit>
}

interface DeliveryRepository {
    suspend fun listDrivers(): AppResult<List<DeliveryDriver>>
    suspend fun createDriver(input: DeliveryDriverFormInput): AppResult<Unit>
    suspend fun updateDriver(id: Int, input: DeliveryDriverFormInput): AppResult<Unit>
    suspend fun deleteDriver(id: Int): AppResult<Unit>

    suspend fun listCompanies(): AppResult<List<DeliveryCompany>>
    suspend fun createCompany(input: DeliveryCompanyFormInput): AppResult<Unit>
    suspend fun updateCompany(id: Int, name: String, active: Boolean): AppResult<Unit>
    suspend fun deleteCompany(id: Int): AppResult<Unit>
}

interface SettingsRepository {
    suspend fun getCompanyConfig(): AppResult<CompanyConfig>
    suspend fun updateCompanyConfig(input: CompanyConfigFormInput): AppResult<CompanyConfig>
    suspend fun getSunatConfig(): AppResult<SunatConfig>
    suspend fun updateSunatConfig(input: SunatConfigFormInput): AppResult<SunatConfig>
    suspend fun listBranches(): AppResult<List<BranchItem>>
    suspend fun getRestaurantSettings(): AppResult<RestaurantSettings>
    suspend fun updateDeletionPin(pin: String): AppResult<Unit>
}

interface ProductImportRepository {
    suspend fun validateExcel(bytes: ByteArray): BulkImportValidationResult
    suspend fun importRows(rows: List<BulkImportRow>, categories: Map<String, Int>): AppResult<BulkImportProgress>
    fun generateTemplateBytes(): ByteArray
}

interface ProductImageRepository {
    fun tenantAssetsBaseUrl(): String?
    suspend fun uploadProductImage(productId: Int, bytes: ByteArray, mimeType: String): AppResult<String>
}
