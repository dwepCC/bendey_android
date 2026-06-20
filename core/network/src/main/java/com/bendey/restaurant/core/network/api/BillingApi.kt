package com.bendey.restaurant.core.network.api

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
}
