package com.bendey.restaurant.core.network.api

import com.bendey.restaurant.core.network.dto.PrintDataDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

@Serializable
data class PayMethodDto(
    val id: Int,
    @SerialName("method_code") val methodCode: String = "",
    val label: String = "",
    val holder: String = "",
    @SerialName("account_number") val accountNumber: String = "",
    val cci: String = "",
    val instructions: String = "",
    @SerialName("qr_url") val qrUrl: String = "",
    val active: Boolean = true,
    @SerialName("sort_order") val sortOrder: Int = 0,
)

@Serializable
data class PayMethodsEnvelopeDto(val data: List<PayMethodDto> = emptyList())

@Serializable
data class PayMethodEnvelopeDto(val data: PayMethodDto? = null)

@Serializable
data class PayMethodRequestDto(
    @SerialName("method_code") val methodCode: String,
    val label: String,
    val holder: String = "",
    @SerialName("account_number") val accountNumber: String = "",
    val cci: String = "",
    val instructions: String = "",
    val active: Boolean = true,
)

@Serializable
data class PayMethodQrResponseDto(@SerialName("qr_url") val qrUrl: String = "")

@Serializable
data class PaymentReviewDto(
    @SerialName("payment_id") val paymentId: Int,
    @SerialName("session_id") val sessionId: Int = 0,
    @SerialName("order_code") val orderCode: String = "",
    @SerialName("order_type") val orderType: String = "",
    @SerialName("customer_name") val customerName: String = "",
    @SerialName("customer_phone") val customerPhone: String = "",
    val amount: Double = 0.0,
    @SerialName("method_code") val methodCode: String = "",
    @SerialName("method_label") val methodLabel: String = "",
    @SerialName("proof_url") val proofUrl: String = "",
    @SerialName("operation_number") val operationNumber: String = "",
    @SerialName("doc_kind") val docKind: String = "",
    @SerialName("client_doc_number") val clientDocNumber: String = "",
    @SerialName("client_name") val clientName: String = "",
    val attempts: Int = 1,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class PaymentsReviewEnvelopeDto(val data: List<PaymentReviewDto> = emptyList())

@Serializable
data class ApprovePaymentSaleDto(val number: String = "", val total: Double = 0.0)

@Serializable
data class ApprovePaymentResponseDto(
    val success: Boolean = false,
    val sale: ApprovePaymentSaleDto? = null,
    @SerialName("print_data") val printData: PrintDataDto? = null,
    @SerialName("customer_phone") val customerPhone: String = "",
    @SerialName("session_id") val sessionId: Int = 0,
    @SerialName("order_code") val orderCode: String = "",
)

@Serializable
data class RejectPaymentRequestDto(val reason: String)

/** Pago manual con captura (D2a). Medios: `s.m`; bandeja y validar/rechazar: `o.ch`. */
interface ManualPaymentApi {
    @GET("/api/restaurant/delivery/pay-methods")
    suspend fun payMethods(): PayMethodsEnvelopeDto

    @POST("/api/restaurant/delivery/pay-methods")
    suspend fun createPayMethod(@Body body: PayMethodRequestDto): PayMethodEnvelopeDto

    @PUT("/api/restaurant/delivery/pay-methods/{id}")
    suspend fun updatePayMethod(@Path("id") id: Int, @Body body: PayMethodRequestDto): PayMethodEnvelopeDto

    @DELETE("/api/restaurant/delivery/pay-methods/{id}")
    suspend fun deletePayMethod(@Path("id") id: Int): SuccessOnlyDto

    @Multipart
    @POST("/api/restaurant/delivery/pay-methods/{id}/qr")
    suspend fun uploadQr(@Path("id") id: Int, @Part photo: MultipartBody.Part): PayMethodQrResponseDto

    @GET("/api/restaurant/delivery/payments/review")
    suspend fun paymentsReview(): PaymentsReviewEnvelopeDto

    @POST("/api/restaurant/delivery/payments/{id}/approve")
    suspend fun approve(@Path("id") paymentId: Int, @Body body: Map<String, String> = emptyMap()): ApprovePaymentResponseDto

    @POST("/api/restaurant/delivery/payments/{id}/reject")
    suspend fun reject(@Path("id") paymentId: Int, @Body body: RejectPaymentRequestDto): SuccessOnlyDto
}
