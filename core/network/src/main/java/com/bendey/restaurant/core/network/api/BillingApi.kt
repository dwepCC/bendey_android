package com.bendey.restaurant.core.network.api

import com.bendey.restaurant.core.network.dto.BillingActionResponseDto
import com.bendey.restaurant.core.network.dto.VoidCreditNoteRequestDto
import com.bendey.restaurant.core.network.dto.VoidCreditNoteResponseDto
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface BillingApi {
    @POST("/api/billing/void-with-credit-note/{saleId}")
    suspend fun voidWithCreditNote(
        @Path("saleId") saleId: Int,
        @Body body: VoidCreditNoteRequestDto,
    ): VoidCreditNoteResponseDto

    @POST("/api/billing/send/{saleId}")
    suspend fun sendToSunat(
        @Path("saleId") saleId: Int,
    ): BillingActionResponseDto

    @POST("/api/billing/resend/{saleId}")
    suspend fun resendToSunat(
        @Path("saleId") saleId: Int,
    ): BillingActionResponseDto

    @retrofit2.http.GET("/api/billing/invoice/{saleId}/document/pdf")
    suspend fun downloadOfficialPdf(
        @Path("saleId") saleId: Int,
    ): okhttp3.ResponseBody
}
