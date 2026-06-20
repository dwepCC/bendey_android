package com.bendey.restaurant.core.network.api

import com.bendey.restaurant.core.network.dto.BulkImportRequestDto
import com.bendey.restaurant.core.network.dto.BulkImportResponseDto
import com.bendey.restaurant.core.network.dto.BillSessionRequestDto
import com.bendey.restaurant.core.network.dto.BillSessionResponseDto
import com.bendey.restaurant.core.network.dto.AddOrderRequestDto
import com.bendey.restaurant.core.network.dto.AddOrderResponseDto
import com.bendey.restaurant.core.network.dto.CategoryDataResponseDto
import com.bendey.restaurant.core.network.dto.CategoryDto
import com.bendey.restaurant.core.network.dto.CategoryUpsertRequestDto
import com.bendey.restaurant.core.network.dto.CreateProductRequestDto
import com.bendey.restaurant.core.network.dto.ProductDataResponseDto
import com.bendey.restaurant.core.network.dto.ProductDetailResponseDto
import com.bendey.restaurant.core.network.dto.DashboardResponseDto
import com.bendey.restaurant.core.network.dto.EmailLoginRequestDto
import com.bendey.restaurant.core.network.dto.FloorDto
import com.bendey.restaurant.core.network.dto.FloorUpsertRequestDto
import com.bendey.restaurant.core.network.dto.TableUpsertRequestDto
import com.bendey.restaurant.core.network.dto.KitchenComandaDto
import com.bendey.restaurant.core.network.dto.ListResponseDto
import com.bendey.restaurant.core.network.dto.LoginResponseDto
import com.bendey.restaurant.core.network.dto.OpenSessionRequestDto
import com.bendey.restaurant.core.network.dto.OpenSessionResponseDto
import com.bendey.restaurant.core.network.dto.PinLoginRequestDto
import com.bendey.restaurant.core.network.dto.PrecuentaResponseDto
import com.bendey.restaurant.core.network.dto.SessionDetailResponseDto
import com.bendey.restaurant.core.network.dto.ProductListResponseDto
import com.bendey.restaurant.core.network.dto.RestaurantTableDto
import com.bendey.restaurant.core.network.dto.StaffOptionDto
import com.bendey.restaurant.core.network.dto.SuccessResponseDto
import com.bendey.restaurant.core.network.dto.TenantByRucDto
import com.bendey.restaurant.core.network.dto.UpdateComandaStatusRequestDto
import com.bendey.restaurant.core.network.dto.UpdateProductRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface PublicApi {
    @GET("/api/public/tenant-by-ruc")
    suspend fun getTenantByRuc(@Query("ruc") ruc: String): TenantByRucDto
}

interface AuthApi {
    @POST("/api/login")
    suspend fun login(@Body body: EmailLoginRequestDto): LoginResponseDto

    @POST("/api/restaurant/auth/pin")
    suspend fun pinLogin(@Body body: PinLoginRequestDto): LoginResponseDto
}

interface RestaurantApi {
    @GET("/api/restaurant/dashboard")
    suspend fun getDashboard(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): DashboardResponseDto

    @GET("/api/restaurant/floors")
    suspend fun listFloors(): ListResponseDto<FloorDto>

    @GET("/api/restaurant/tables")
    suspend fun listTables(
        @Query("floor_id") floorId: Int? = null,
    ): ListResponseDto<RestaurantTableDto>

    @GET("/api/restaurant/staff")
    suspend fun listStaff(): ListResponseDto<StaffOptionDto>

    @POST("/api/restaurant/sessions")
    suspend fun openSession(@Body body: OpenSessionRequestDto): OpenSessionResponseDto

    @POST("/api/restaurant/sessions/{sessionId}/orders")
    suspend fun addOrder(
        @retrofit2.http.Path("sessionId") sessionId: Int,
        @Body body: AddOrderRequestDto,
    ): AddOrderResponseDto

    @GET("/api/restaurant/kitchen")
    suspend fun getKitchen(): ListResponseDto<KitchenComandaDto>

    @PUT("/api/restaurant/comandas/{comandaId}/status")
    suspend fun updateComandaStatus(
        @retrofit2.http.Path("comandaId") comandaId: Int,
        @Body body: UpdateComandaStatusRequestDto,
    ): SuccessResponseDto

    @GET("/api/restaurant/sessions/{sessionId}")
    suspend fun getSession(
        @retrofit2.http.Path("sessionId") sessionId: Int,
    ): SessionDetailResponseDto

    @GET("/api/restaurant/sessions/{sessionId}/precuenta")
    suspend fun getPrecuenta(
        @retrofit2.http.Path("sessionId") sessionId: Int,
    ): PrecuentaResponseDto

    @POST("/api/restaurant/sessions/{sessionId}/bill")
    suspend fun billSession(
        @retrofit2.http.Path("sessionId") sessionId: Int,
        @Body body: BillSessionRequestDto,
    ): BillSessionResponseDto

    @POST("/api/restaurant/floors")
    suspend fun createFloor(@Body body: FloorUpsertRequestDto): SuccessResponseDto

    @PUT("/api/restaurant/floors/{id}")
    suspend fun updateFloor(
        @Path("id") id: Int,
        @Body body: FloorUpsertRequestDto,
    ): SuccessResponseDto

    @DELETE("/api/restaurant/floors/{id}")
    suspend fun deleteFloor(@Path("id") id: Int): SuccessResponseDto

    @POST("/api/restaurant/tables")
    suspend fun createTable(@Body body: TableUpsertRequestDto): SuccessResponseDto

    @PUT("/api/restaurant/tables/{id}")
    suspend fun updateTable(
        @Path("id") id: Int,
        @Body body: TableUpsertRequestDto,
    ): SuccessResponseDto

    @DELETE("/api/restaurant/tables/{id}")
    suspend fun deleteTable(@Path("id") id: Int): SuccessResponseDto
}

interface ProductsApi {
    @GET("/api/products")
    suspend fun listProducts(
        @Query("q") query: String = "",
        @Query("restaurant_only") restaurantOnly: String = "true",
        @Query("active_only") activeOnly: String = "true",
        @Query("catalog_only") catalogOnly: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 40,
        @Query("category_id") categoryId: Int? = null,
        @Query("preparation_area") preparationArea: String? = null,
        @Query("branch_id") branchId: Int? = null,
    ): ProductListResponseDto

    @GET("/api/products/{id}")
    suspend fun getProduct(@Path("id") id: Int): ProductDetailResponseDto

    @POST("/api/products")
    suspend fun createProduct(@Body body: CreateProductRequestDto): ProductDataResponseDto

    @PUT("/api/products/{id}")
    suspend fun updateProduct(
        @Path("id") id: Int,
        @Body body: UpdateProductRequestDto,
    ): SuccessResponseDto

    @DELETE("/api/products/{id}")
    suspend fun deleteProduct(@Path("id") id: Int): SuccessResponseDto

    @GET("/api/categories")
    suspend fun listCategories(): ListResponseDto<CategoryDto>

    @POST("/api/categories")
    suspend fun createCategory(@Body body: CategoryUpsertRequestDto): CategoryDataResponseDto

    @PUT("/api/categories/{id}")
    suspend fun updateCategory(
        @Path("id") id: Int,
        @Body body: CategoryUpsertRequestDto,
    ): CategoryDataResponseDto

    @DELETE("/api/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Int): SuccessResponseDto

    @POST("/api/products/bulk-import/restaurant")
    suspend fun bulkImportRestaurant(@Body body: BulkImportRequestDto): BulkImportResponseDto
}
