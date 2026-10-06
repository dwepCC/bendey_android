package com.bendey.restaurant.core.network.api

import com.bendey.restaurant.core.network.dto.AssignDeliveryDriverRequestDto
import com.bendey.restaurant.core.network.dto.BranchDto
import com.bendey.restaurant.core.network.dto.ComboDataResponseDto
import com.bendey.restaurant.core.network.dto.ComboResolveRequestDto
import com.bendey.restaurant.core.network.dto.ComboResolveResponseDto
import com.bendey.restaurant.core.network.dto.ComboUpsertRequestDto
import com.bendey.restaurant.core.network.dto.CompanyConfigDto
import com.bendey.restaurant.core.network.dto.CompanyConfigResponseDto
import com.bendey.restaurant.core.network.dto.UbiItemDto
import com.bendey.restaurant.core.network.dto.DeliveryBoardDto
import com.bendey.restaurant.core.network.dto.DeliveryCancelRequestDto
import com.bendey.restaurant.core.network.dto.DeliveryStatusRequestDto
import com.bendey.restaurant.core.network.dto.DeliveryCompanyDto
import com.bendey.restaurant.core.network.dto.DeliveryCompanyUpsertRequestDto
import com.bendey.restaurant.core.network.dto.DeliveryEarningSettingsUpdateRequestDto
import com.bendey.restaurant.core.network.dto.DeliveryDriverDto
import com.bendey.restaurant.core.network.dto.DeliveryDriverUpsertRequestDto
import com.bendey.restaurant.core.network.dto.ListResponseDto
import com.bendey.restaurant.core.network.dto.ModifierGroupDto
import com.bendey.restaurant.core.network.dto.ModifierGroupResponseDto
import com.bendey.restaurant.core.network.dto.ModifierGroupUpsertRequestDto
import com.bendey.restaurant.core.network.dto.PreparationAreaDataResponseDto
import com.bendey.restaurant.core.network.dto.PreparationAreaDto
import com.bendey.restaurant.core.network.dto.PreparationAreaStatusRequestDto
import com.bendey.restaurant.core.network.dto.PreparationAreaUpsertRequestDto
import com.bendey.restaurant.core.network.dto.RestaurantSettingsDto
import com.bendey.restaurant.core.network.dto.RestaurantSettingsUpdateRequestDto
import com.bendey.restaurant.core.network.dto.SuccessResponseDto
import com.bendey.restaurant.core.network.dto.SunatConfigDto
import com.bendey.restaurant.core.network.dto.ComboDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ModifierGroupsApi {
    @GET("/api/modifier-groups")
    suspend fun listModifierGroups(): ListResponseDto<ModifierGroupDto>

    @POST("/api/modifier-groups")
    suspend fun createModifierGroup(@Body body: ModifierGroupUpsertRequestDto): ModifierGroupResponseDto

    @PUT("/api/modifier-groups/{id}")
    suspend fun updateModifierGroup(
        @Path("id") id: Int,
        @Body body: ModifierGroupUpsertRequestDto,
    ): ModifierGroupResponseDto

    @DELETE("/api/modifier-groups/{id}")
    suspend fun deleteModifierGroup(@Path("id") id: Int): SuccessResponseDto
}

interface PreparationAreasApi {
    @GET("/api/restaurant/preparation-areas")
    suspend fun listPreparationAreas(
        @Query("active_only") activeOnly: String = "true",
    ): ListResponseDto<PreparationAreaDto>

    @POST("/api/restaurant/preparation-areas")
    suspend fun createPreparationArea(
        @Body body: PreparationAreaUpsertRequestDto,
    ): PreparationAreaDataResponseDto

    @PUT("/api/restaurant/preparation-areas/{id}")
    suspend fun updatePreparationArea(
        @Path("id") id: Int,
        @Body body: PreparationAreaUpsertRequestDto,
    ): SuccessResponseDto

    @PATCH("/api/restaurant/preparation-areas/{id}/status")
    suspend fun setPreparationAreaStatus(
        @Path("id") id: Int,
        @Body body: PreparationAreaStatusRequestDto,
    ): SuccessResponseDto
}

interface CombosApi {
    @GET("/api/combos")
    suspend fun listCombos(
        @Query("branch_id") branchId: Int? = null,
        @Query("active_only") activeOnly: String = "true",
    ): ListResponseDto<ComboDto>

    @GET("/api/combos/{id}")
    suspend fun getCombo(@Path("id") id: Int): ComboDataResponseDto

    @POST("/api/combos")
    suspend fun createCombo(@Body body: ComboUpsertRequestDto): ComboDataResponseDto

    @PUT("/api/combos/{id}")
    suspend fun updateCombo(
        @Path("id") id: Int,
        @Body body: ComboUpsertRequestDto,
    ): ComboDataResponseDto

    @DELETE("/api/combos/{id}")
    suspend fun deleteCombo(@Path("id") id: Int): SuccessResponseDto

    @POST("/api/combos/{id}/resolve")
    suspend fun resolveCombo(
        @Path("id") id: Int,
        @Body body: ComboResolveRequestDto,
    ): ComboResolveResponseDto
}

interface DeliveryApi {
    @GET("/api/restaurant/delivery-drivers")
    suspend fun listDeliveryDrivers(
        @Query("active_only") activeOnly: String = "true",
    ): ListResponseDto<DeliveryDriverDto>

    @POST("/api/restaurant/delivery-drivers")
    suspend fun createDeliveryDriver(@Body body: DeliveryDriverUpsertRequestDto): SuccessResponseDto

    @PUT("/api/restaurant/delivery-drivers/{id}")
    suspend fun updateDeliveryDriver(
        @Path("id") id: Int,
        @Body body: DeliveryDriverUpsertRequestDto,
    ): SuccessResponseDto

    @DELETE("/api/restaurant/delivery-drivers/{id}")
    suspend fun deleteDeliveryDriver(@Path("id") id: Int): SuccessResponseDto

    // Crea/reasigna la asignación real del pedido -- antes elegir un repartidor en el diálogo de
    // Delivery del POS solo guardaba delivery_driver_id como referencia en la sesión, sin llamar
    // a este endpoint: nunca aparecía en "Entregas activas" ni en Bendey Delivery.
    @POST("/api/restaurant/sessions/{id}/delivery/assign")
    suspend fun assignDeliveryDriver(
        @Path("id") sessionId: Int,
        @Body body: AssignDeliveryDriverRequestDto,
    ): SuccessResponseDto

    // Tablero de Delivery (D1): 5 secciones + repartidores de la sucursal activa (permiso d.v).
    @GET("/api/restaurant/delivery/board")
    suspend fun getDeliveryBoard(): DeliveryBoardDto

    // Cancelar un pedido delivery con motivo (D1, permiso d.u). Idempotente en el backend.
    @POST("/api/restaurant/sessions/{id}/delivery/cancel")
    suspend fun cancelDeliveryOrder(
        @Path("id") sessionId: Int,
        @Body body: DeliveryCancelRequestDto,
    ): SuccessResponseDto

    // Cambio de estado de una asignación por staff (D1): delivered | failed (el staff no necesita foto).
    @PUT("/api/delivery/assignments/{id}/status")
    suspend fun updateDeliveryAssignmentStatus(
        @Path("id") assignmentId: Int,
        @Body body: DeliveryStatusRequestDto,
    ): SuccessResponseDto

    @GET("/api/restaurant/delivery-companies")
    suspend fun listDeliveryCompanies(
        @Query("active_only") activeOnly: String = "true",
    ): ListResponseDto<DeliveryCompanyDto>

    @POST("/api/restaurant/delivery-companies")
    suspend fun createDeliveryCompany(@Body body: DeliveryCompanyUpsertRequestDto): SuccessResponseDto

    @PUT("/api/restaurant/delivery-companies/{id}")
    suspend fun updateDeliveryCompany(
        @Path("id") id: Int,
        @Body body: DeliveryCompanyUpsertRequestDto,
    ): SuccessResponseDto

    @DELETE("/api/restaurant/delivery-companies/{id}")
    suspend fun deleteDeliveryCompany(@Path("id") id: Int): SuccessResponseDto
}

interface SettingsApi {
    @GET("/api/company/config")
    suspend fun getCompanyConfig(): CompanyConfigDto

    @PUT("/api/company/config")
    suspend fun updateCompanyConfig(@Body body: CompanyConfigDto): CompanyConfigResponseDto

    @GET("/api/ubigeo/regiones")
    suspend fun getUbigeoRegiones(): ListResponseDto<UbiItemDto>

    @GET("/api/ubigeo/provincias")
    suspend fun getUbigeoProvincias(@Query("region_id") regionId: String): ListResponseDto<UbiItemDto>

    @GET("/api/ubigeo/distritos")
    suspend fun getUbigeoDistritos(@Query("provincia_id") provinciaId: String): ListResponseDto<UbiItemDto>

    @GET("/api/company/sunat")
    suspend fun getSunatConfig(): SunatConfigDto

    @PUT("/api/company/sunat")
    suspend fun updateSunatConfig(@Body body: SunatConfigDto): SuccessResponseDto

    @GET("/api/company/branches")
    suspend fun listBranches(): ListResponseDto<BranchDto>

    @GET("/api/restaurant/settings")
    suspend fun getRestaurantSettings(): RestaurantSettingsDto

    @PUT("/api/restaurant/settings")
    suspend fun updateRestaurantSettings(@Body body: RestaurantSettingsUpdateRequestDto): SuccessResponseDto

    @PUT("/api/restaurant/settings/delivery-earning")
    suspend fun updateDeliveryEarningSettings(@Body body: DeliveryEarningSettingsUpdateRequestDto): SuccessResponseDto
}
