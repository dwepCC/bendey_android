package com.bendey.restaurant.core.network.api

import com.bendey.restaurant.core.network.dto.BranchDto
import com.bendey.restaurant.core.network.dto.ComboDataResponseDto
import com.bendey.restaurant.core.network.dto.ComboResolveRequestDto
import com.bendey.restaurant.core.network.dto.ComboResolveResponseDto
import com.bendey.restaurant.core.network.dto.ComboUpsertRequestDto
import com.bendey.restaurant.core.network.dto.CompanyConfigDto
import com.bendey.restaurant.core.network.dto.CompanyConfigResponseDto
import com.bendey.restaurant.core.network.dto.DeliveryCompanyDto
import com.bendey.restaurant.core.network.dto.DeliveryCompanyUpsertRequestDto
import com.bendey.restaurant.core.network.dto.DeliveryDriverDto
import com.bendey.restaurant.core.network.dto.DeliveryDriverUpsertRequestDto
import com.bendey.restaurant.core.network.dto.ListResponseDto
import com.bendey.restaurant.core.network.dto.ModifierGroupDto
import com.bendey.restaurant.core.network.dto.ModifierGroupResponseDto
import com.bendey.restaurant.core.network.dto.ModifierGroupUpsertRequestDto
import com.bendey.restaurant.core.network.dto.RestaurantSettingsDto
import com.bendey.restaurant.core.network.dto.RestaurantSettingsUpdateRequestDto
import com.bendey.restaurant.core.network.dto.SuccessResponseDto
import com.bendey.restaurant.core.network.dto.SunatConfigDto
import com.bendey.restaurant.core.network.dto.ComboDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
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
}
