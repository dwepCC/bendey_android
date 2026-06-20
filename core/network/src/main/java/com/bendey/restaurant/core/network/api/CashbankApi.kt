package com.bendey.restaurant.core.network.api

import com.bendey.restaurant.core.network.dto.AddCashMovementRequestDto
import com.bendey.restaurant.core.network.dto.CashMovementDto
import com.bendey.restaurant.core.network.dto.CashMovementResponseDto
import com.bendey.restaurant.core.network.dto.CashSessionDto
import com.bendey.restaurant.core.network.dto.CashSessionResponseDto
import com.bendey.restaurant.core.network.dto.PaymentMethodDto
import com.bendey.restaurant.core.network.dto.CloseCashSessionRequestDto
import com.bendey.restaurant.core.network.dto.ListResponseDto
import com.bendey.restaurant.core.network.dto.OpenCashSessionRequestDto
import com.bendey.restaurant.core.network.dto.CashSessionReportResponseDto
import com.bendey.restaurant.core.network.dto.SaveArqueoRequestDto
import com.bendey.restaurant.core.network.dto.SaveArqueoResponseDto
import com.bendey.restaurant.core.network.dto.AddBankMovementRequestDto
import com.bendey.restaurant.core.network.dto.BankMovementDto
import com.bendey.restaurant.core.network.dto.BankAccountDto
import com.bendey.restaurant.core.network.dto.BankAccountUpsertRequestDto
import com.bendey.restaurant.core.network.dto.PaymentMethodUpsertRequestDto
import com.bendey.restaurant.core.network.dto.SuccessResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
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

    @GET("/api/cashbank/sessions")
    suspend fun listSessions(
        @Query("branch_id") branchId: Int? = null,
    ): ListResponseDto<CashSessionDto>

    @GET("/api/cashbank/sessions/{sessionId}")
    suspend fun getSession(
        @Path("sessionId") sessionId: Int,
    ): CashSessionResponseDto

    @GET("/api/cashbank/sessions/{sessionId}/report")
    suspend fun getSessionReport(
        @Path("sessionId") sessionId: Int,
    ): CashSessionReportResponseDto

    @POST("/api/cashbank/sessions/{sessionId}/arqueo")
    suspend fun saveArqueo(
        @Path("sessionId") sessionId: Int,
        @Body body: SaveArqueoRequestDto,
    ): SaveArqueoResponseDto

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

    @POST("/api/cashbank/payment-methods")
    suspend fun createPaymentMethod(@Body body: PaymentMethodUpsertRequestDto): SuccessResponseDto

    @PUT("/api/cashbank/payment-methods/{id}")
    suspend fun updatePaymentMethod(
        @Path("id") id: Int,
        @Body body: PaymentMethodUpsertRequestDto,
    ): SuccessResponseDto

    @DELETE("/api/cashbank/payment-methods/{id}")
    suspend fun deletePaymentMethod(@Path("id") id: Int): SuccessResponseDto

    @GET("/api/cashbank/bank-accounts")
    suspend fun listBankAccounts(
        @Query("all") all: String? = "1",
    ): ListResponseDto<BankAccountDto>

    @POST("/api/cashbank/bank-accounts")
    suspend fun createBankAccount(@Body body: BankAccountUpsertRequestDto): SuccessResponseDto

    @PUT("/api/cashbank/bank-accounts/{id}")
    suspend fun updateBankAccount(
        @Path("id") id: Int,
        @Body body: BankAccountUpsertRequestDto,
    ): SuccessResponseDto

    @GET("/api/cashbank/bank-accounts/{id}/movements")
    suspend fun listBankMovements(@Path("id") id: Int): ListResponseDto<BankMovementDto>

    @POST("/api/cashbank/bank-accounts/{id}/movements")
    suspend fun addBankMovement(
        @Path("id") id: Int,
        @Body body: AddBankMovementRequestDto,
    ): SuccessResponseDto
}
