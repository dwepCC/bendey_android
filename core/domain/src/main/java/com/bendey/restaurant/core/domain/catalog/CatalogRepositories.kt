package com.bendey.restaurant.core.domain.catalog

import com.bendey.restaurant.core.domain.model.AppResult

interface ModifiersRepository {
    suspend fun listModifierGroups(): AppResult<List<ModifierGroup>>
    suspend fun createModifierGroup(input: ModifierGroupFormInput): AppResult<ModifierGroup>
    suspend fun updateModifierGroup(id: Int, input: ModifierGroupFormInput): AppResult<ModifierGroup>
    suspend fun deleteModifierGroup(id: Int): AppResult<Unit>
}

interface PreparationAreasRepository {
    suspend fun listPreparationAreas(activeOnly: Boolean = false): AppResult<List<PreparationAreaItem>>
    suspend fun createPreparationArea(input: PreparationAreaFormInput): AppResult<PreparationAreaItem>
    suspend fun updatePreparationArea(id: Int, input: PreparationAreaFormInput): AppResult<Unit>
    suspend fun setPreparationAreaStatus(id: Int, active: Boolean): AppResult<Unit>
}

data class ComboResolveResult(
    val unitPrice: Double,
    val summaryLines: List<String> = emptyList(),
)

interface CombosRepository {
    /** includeInactive = true trae también los combos desactivados (para poder reactivarlos). */
    suspend fun listCombos(includeInactive: Boolean = false): AppResult<List<ComboItem>>
    suspend fun listPosCombos(branchId: Int?): AppResult<List<com.bendey.restaurant.core.domain.pos.PosComboItem>>
    suspend fun getCombo(id: Int): AppResult<ComboFormInput>
    suspend fun resolveCombo(id: Int, branchId: Int, comboConfigJson: String): AppResult<ComboResolveResult>
    suspend fun createCombo(input: ComboFormInput): AppResult<ComboItem>
    suspend fun updateCombo(id: Int, input: ComboFormInput): AppResult<ComboItem>
    suspend fun deleteCombo(id: Int): AppResult<Unit>
}

interface DeliveryRepository {
    suspend fun listDrivers(): AppResult<List<DeliveryDriver>>
    suspend fun createDriver(input: DeliveryDriverFormInput): AppResult<Unit>
    suspend fun updateDriver(id: Int, input: DeliveryDriverFormInput): AppResult<Unit>
    suspend fun deleteDriver(id: Int): AppResult<Unit>

    /** Tablero de Delivery (D1): 5 secciones + repartidores. `GET /api/restaurant/delivery/board`. */
    suspend fun getDeliveryBoard(): AppResult<com.bendey.restaurant.core.domain.delivery.DeliveryBoardData>

    /** Asignar / reasignar. `POST /api/restaurant/sessions/:id/delivery/assign` (permiso d.u). */
    suspend fun assignDriver(sessionId: Int, driverId: Int): AppResult<Unit>

    /** Cancelar un pedido delivery con motivo (3-255). `POST /api/restaurant/sessions/:id/delivery/cancel` (d.u). */
    suspend fun cancelDeliveryOrder(sessionId: Int, reason: String): AppResult<Unit>

    /** Cambio de estado por staff (delivered | failed). `PUT /api/delivery/assignments/:id/status` (d.u). */
    suspend fun updateAssignmentStatus(assignmentId: Int, status: String, failedReason: String? = null): AppResult<Unit>

    suspend fun listCompanies(): AppResult<List<DeliveryCompany>>
    suspend fun createCompany(input: DeliveryCompanyFormInput): AppResult<Unit>
    suspend fun updateCompany(id: Int, name: String, active: Boolean): AppResult<Unit>
    suspend fun deleteCompany(id: Int): AppResult<Unit>
}

interface SettingsRepository {
    suspend fun preloadTenantSettings()
    fun invalidateTenantSettingsCache()
    fun peekTenantSettings(): TenantSettingsSnapshot?
    suspend fun getCompanyConfig(): AppResult<CompanyConfig>
    suspend fun updateCompanyConfig(input: CompanyConfigFormInput): AppResult<CompanyConfig>
    suspend fun getUbigeoRegiones(): AppResult<List<UbiItem>>
    suspend fun getUbigeoProvincias(regionId: String): AppResult<List<UbiItem>>
    suspend fun getUbigeoDistritos(provinciaId: String): AppResult<List<UbiItem>>
    suspend fun getSunatConfig(): AppResult<SunatConfig>
    suspend fun updateSunatConfig(input: SunatConfigFormInput): AppResult<SunatConfig>
    suspend fun listBranches(): AppResult<List<BranchItem>>
    suspend fun createBranch(input: BranchFormInput): AppResult<Unit>
    suspend fun updateBranch(id: Int, input: BranchFormInput): AppResult<Unit>
    suspend fun deleteBranch(id: Int): AppResult<Unit>
    suspend fun getSaleDetailConfig(branchId: Int): AppResult<SaleDetailConfig>
    suspend fun updateSaleDetailConfig(branchId: Int, enabled: Boolean, defaultText: String): AppResult<SaleDetailConfig>
    suspend fun getServiceChargeConfig(branchId: Int): AppResult<ServiceChargeConfig>
    suspend fun updateServiceChargeConfig(branchId: Int, enabled: Boolean, rate: Double): AppResult<ServiceChargeConfig>
    suspend fun listSeries(branchId: Int?): AppResult<List<com.bendey.restaurant.core.domain.billing.DocumentSeries>>
    suspend fun createSeries(input: SeriesFormInput): AppResult<Unit>
    suspend fun updateSeries(id: Int, input: SeriesFormInput): AppResult<Unit>
    suspend fun deleteSeries(id: Int): AppResult<Unit>
    suspend fun getRestaurantSettings(): AppResult<RestaurantSettings>
    suspend fun updateDeletionPin(pin: String): AppResult<Unit>
    suspend fun updateDeliveryEarningPerOrder(amount: Double): AppResult<Unit>
    suspend fun listStaffManagement(): AppResult<List<RestaurantStaffManagementRow>>
    suspend fun createStaffUser(input: StaffCreateFormInput): AppResult<Unit>
    suspend fun updateStaffUser(input: StaffEditFormInput): AppResult<Unit>
}

interface ProductImportRepository {
    /**
     * @param areaNames nombres de las áreas de preparación del restaurante; si no está vacío, la
     * columna de área se valida contra ellas (y un área vacía se resuelve como "cocina").
     */
    suspend fun validateExcel(bytes: ByteArray, areaNames: List<String> = emptyList()): BulkImportValidationResult
    suspend fun importRows(rows: List<BulkImportRow>, categories: Map<String, Int>): AppResult<BulkImportProgress>

    /** Plantilla AVANZADA (11 columnas). */
    fun generateTemplateBytes(): ByteArray

    /** Plantilla SIMPLE (R4): nombre, categoría, precio, área. */
    fun generateSimpleTemplateBytes(): ByteArray
}

interface ProductImageRepository {
    fun tenantAssetsBaseUrl(): String?
    suspend fun uploadProductImage(productId: Int, bytes: ByteArray, mimeType: String): AppResult<String>
    suspend fun uploadComboImage(comboId: Int, bytes: ByteArray, mimeType: String): AppResult<String>
}
