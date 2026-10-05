package com.bendey.restaurant.core.network.api

import com.bendey.restaurant.core.network.dto.BillingActionResponseDto
import com.bendey.restaurant.core.network.dto.ListResponseDto
import com.bendey.restaurant.core.network.dto.StuckVoidCreditNoteDto
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

    // Anulaciones que quedaron a medias (NC rechazada o colgada): el aviso de Ventas (R10.9, solo lectura).
    @retrofit2.http.GET("/api/billing/stuck-void-credit-notes")
    suspend fun listStuckVoidCreditNotes(): ListResponseDto<StuckVoidCreditNoteDto>

    @retrofit2.http.GET("/api/billing/invoice/{saleId}/document/{kind}")
    suspend fun downloadDocument(
        @Path("saleId") saleId: Int,
        @Path("kind") kind: String,
    ): okhttp3.ResponseBody
}
