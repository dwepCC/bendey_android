package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.delivery.ApprovedPayment
import com.bendey.restaurant.core.domain.delivery.ManualPaymentRepository
import com.bendey.restaurant.core.domain.delivery.PayMethod
import com.bendey.restaurant.core.domain.delivery.PayMethodInput
import com.bendey.restaurant.core.domain.delivery.PaymentReview
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.network.api.ManualPaymentApi
import com.bendey.restaurant.core.network.api.PayMethodDto
import com.bendey.restaurant.core.network.api.PayMethodRequestDto
import com.bendey.restaurant.core.network.api.PaymentReviewDto
import com.bendey.restaurant.core.network.api.RejectPaymentRequestDto
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.error.apiCall
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class ManualPaymentRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : ManualPaymentRepository {

    private val api: ManualPaymentApi
        get() = tenantRetrofitProvider.create()

    override suspend fun payMethods(): AppResult<List<PayMethod>> = apiCall {
        api.payMethods().data.map { it.toDomain() }
    }

    override suspend fun createPayMethod(input: PayMethodInput): AppResult<PayMethod> = apiCall {
        api.createPayMethod(input.toDto()).data?.toDomain() ?: error("Medio no guardado")
    }

    override suspend fun updatePayMethod(id: Int, input: PayMethodInput): AppResult<PayMethod> = apiCall {
        api.updatePayMethod(id, input.toDto()).data?.toDomain() ?: error("Medio no guardado")
    }

    override suspend fun deletePayMethod(id: Int): AppResult<Unit> = apiCall {
        api.deletePayMethod(id)
        Unit
    }

    override suspend fun uploadQr(id: Int, bytes: ByteArray, mimeType: String): AppResult<String> = apiCall {
        val ext = when {
            mimeType.contains("png") -> "png"
            mimeType.contains("webp") -> "webp"
            else -> "jpg"
        }
        val part = MultipartBody.Part.createFormData("photo", "qr.$ext", bytes.toRequestBody(mimeType.toMediaType()))
        api.uploadQr(id, part).qrUrl
    }

    override suspend fun paymentsReview(): AppResult<List<PaymentReview>> = apiCall {
        api.paymentsReview().data.map { it.toDomain() }
    }

    override suspend fun approve(paymentId: Int): AppResult<ApprovedPayment> = apiCall {
        val r = api.approve(paymentId)
        ApprovedPayment(
            orderCode = r.orderCode,
            sessionId = r.sessionId,
            customerPhone = r.customerPhone,
            saleNumber = r.sale?.number.orEmpty(),
            printData = r.printData?.toDomain(),
        )
    }

    override suspend fun reject(paymentId: Int, reason: String): AppResult<Unit> = apiCall {
        api.reject(paymentId, RejectPaymentRequestDto(reason.trim()))
        Unit
    }
}

private fun PayMethodInput.toDto() = PayMethodRequestDto(
    methodCode = methodCode,
    label = label.trim(),
    holder = holder.trim(),
    accountNumber = accountNumber.trim(),
    cci = cci.trim(),
    instructions = instructions.trim(),
    active = active,
)

private fun PayMethodDto.toDomain() = PayMethod(id, methodCode, label, holder, accountNumber, cci, instructions, qrUrl, active)

internal fun PaymentReviewDto.toDomain() = PaymentReview(
    paymentId = paymentId,
    sessionId = sessionId,
    orderCode = orderCode,
    orderType = orderType,
    customerName = customerName,
    customerPhone = customerPhone,
    amount = amount,
    methodCode = methodCode,
    methodLabel = methodLabel.ifBlank { methodCode },
    proofUrl = proofUrl,
    operationNumber = operationNumber,
    docKind = docKind,
    clientDocNumber = clientDocNumber,
    clientName = clientName,
    attempts = attempts,
    expiresAtMs = expiresAt?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() },
)
