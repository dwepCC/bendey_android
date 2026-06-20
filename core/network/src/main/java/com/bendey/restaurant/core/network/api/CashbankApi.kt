package com.bendey.restaurant.core.network.api

import com.bendey.restaurant.core.network.dto.AddCashMovementRequestDto
import com.bendey.restaurant.core.network.dto.CashMovementDto
import com.bendey.restaurant.core.network.dto.CashMovementResponseDto
import com.bendey.restaurant.core.network.dto.CashSessionResponseDto
import com.bendey.restaurant.core.network.dto.CloseCashSessionRequestDto
import com.bendey.restaurant.core.network.dto.ListResponseDto
import com.bendey.restaurant.core.network.dto.OpenCashSessionRequestDto
import com.bendey.restaurant.core.network.dto.PaymentMethodDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CashbankApi {
    @GET("/api/cashbank/sessions/open")
    suspend fun getOpenSession(
        @Query("branch_id") branchId: Int? = null,
    ): CashSessionResponseDto

    @POST("/api/cashbank/sessions")
    suspend fun openSession(
        @Body body: OpenCashSessionRequestDto,
    ): CashSessionResponseDto

    @POST("/api/cashbank/sessions/{sessionId}/close")
    suspend fun closeSession(
        @Path("sessionId") sessionId: Int,
        @Body body: CloseCashSessionRequestDto = CloseCashSessionRequestDto(),
    ): CashSessionResponseDto

    @GET("/api/cashbank/sessions/{sessionId}/movements")
    suspend fun listMovements(
        @Path("sessionId") sessionId: Int,
    ): ListResponseDto<CashMovementDto>

    @POST("/api/cashbank/sessions/{sessionId}/movements")
    suspend fun addMovement(
        @Path("sessionId") sessionId: Int,
        @Body body: AddCashMovementRequestDto,
    ): CashMovementResponseDto

    @GET("/api/cashbank/payment-methods")
    suspend fun listPaymentMethods(
        @Query("all") all: String? = "1",
    ): ListResponseDto<PaymentMethodDto>
}
