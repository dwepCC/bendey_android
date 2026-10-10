package com.bendey.restaurant.core.domain.delivery

import com.bendey.restaurant.core.domain.billing.SalePrintData
import com.bendey.restaurant.core.domain.catalog.resolvePublicAssetUrl
import com.bendey.restaurant.core.domain.model.AppResult

/** Medio de pago que el restaurante ofrece para pagar con captura (Yape, Plin, transferencia...). */
data class PayMethod(
    val id: Int,
    val methodCode: String,
    val label: String,
    val holder: String = "",
    val accountNumber: String = "",
    val cci: String = "",
    val instructions: String = "",
    /** Ruta relativa (/storage/...) o URL de la imagen QR; vacío = sin QR. */
    val qrUrl: String = "",
    val active: Boolean = true,
)

data class PayMethodInput(
    val methodCode: String,
    val label: String,
    val holder: String = "",
    val accountNumber: String = "",
    val cci: String = "",
    val instructions: String = "",
    val active: Boolean = true,
)

/** Un pago con captura esperando que el personal lo valide. */
data class PaymentReview(
    val paymentId: Int,
    val sessionId: Int,
    val orderCode: String,
    val orderType: String,
    val customerName: String,
    val customerPhone: String,
    val amount: Double,
    val methodCode: String,
    val methodLabel: String,
    /** Ruta relativa (/storage/...) o URL de la captura. */
    val proofUrl: String,
    val operationNumber: String,
    /** "boleta", "factura" o vacío (nota de venta). */
    val docKind: String,
    val clientDocNumber: String,
    val clientName: String,
    val attempts: Int,
    /** Epoch ms en que vence el plazo; null = sin dato. */
    val expiresAtMs: Long?,
) {
    val wantsDocument: Boolean get() = docKind == "boleta" || docKind == "factura"
    val docLabel: String get() = if (docKind == "factura") "Factura" else "Boleta"
}

/** Resultado de validar un pago: la venta ya existe y el pedido está en cocina. */
data class ApprovedPayment(
    val orderCode: String,
    val sessionId: Int,
    val customerPhone: String,
    val saleNumber: String,
    val printData: SalePrintData?,
)

interface ManualPaymentRepository {
    suspend fun payMethods(): AppResult<List<PayMethod>>
    suspend fun createPayMethod(input: PayMethodInput): AppResult<PayMethod>
    suspend fun updatePayMethod(id: Int, input: PayMethodInput): AppResult<PayMethod>
    suspend fun deletePayMethod(id: Int): AppResult<Unit>

    /** Sube la imagen QR del medio; devuelve la URL guardada. */
    suspend fun uploadQr(id: Int, bytes: ByteArray, mimeType: String): AppResult<String>

    suspend fun paymentsReview(): AppResult<List<PaymentReview>>
    suspend fun approve(paymentId: Int): AppResult<ApprovedPayment>
    suspend fun reject(paymentId: Int, reason: String): AppResult<Unit>
}

object ManualPaymentRules {
    const val MIN_REVIEW_MINUTES = 5
    const val MAX_REVIEW_MINUTES = 120
    const val DEFAULT_REVIEW_MINUTES = 15
    const val MIN_REJECT_REASON = 3

    /** Minutos válidos del plazo para pagar, o null si el texto no sirve. */
    fun parseReviewMinutes(text: String): Int? =
        text.trim().toIntOrNull()?.takeIf { it in MIN_REVIEW_MINUTES..MAX_REVIEW_MINUTES }

    fun isRejectReasonValid(reason: String): Boolean = reason.trim().length >= MIN_REJECT_REASON

    /** Medios de Caja y Bancos que sirven para pagar con captura: activos y que no sean efectivo. */
    fun isOfferableCashbankMethod(code: String, destinationType: String, active: Boolean): Boolean =
        active && code.isNotBlank() && !code.equals("cash", ignoreCase = true) && !destinationType.equals("cash", ignoreCase = true)

    /** Mensaje de la validación del formulario de un medio, o null si está bien. */
    fun validateMethod(input: PayMethodInput): String? = when {
        input.methodCode.isBlank() -> "Elige el medio de Caja y Bancos donde entra la plata."
        input.label.trim().isEmpty() -> "Escribe el nombre del medio de pago."
        else -> null
    }

    /** Minutos que le quedan al plazo (nunca negativo); null si no hay vencimiento. */
    fun minutesLeft(expiresAtMs: Long?, nowMs: Long): Int? {
        if (expiresAtMs == null) return null
        val left = expiresAtMs - nowMs
        return if (left <= 0) 0 else ((left + 59_999) / 60_000).toInt()
    }

    /** Resuelve una ruta /storage/... contra el origen público de archivos. */
    fun resolveAssetUrl(base: String?, url: String): String = resolvePublicAssetUrl(base, url)
}
