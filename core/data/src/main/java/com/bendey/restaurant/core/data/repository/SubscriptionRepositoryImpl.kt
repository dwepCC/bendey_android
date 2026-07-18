package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.subscription.AvailablePlan
import com.bendey.restaurant.core.domain.subscription.BankAccountOption
import com.bendey.restaurant.core.domain.subscription.BillingContext
import com.bendey.restaurant.core.domain.subscription.BillingHub
import com.bendey.restaurant.core.domain.subscription.DocumentUsage
import com.bendey.restaurant.core.domain.subscription.PaymentConfig
import com.bendey.restaurant.core.domain.subscription.PaymentMethodOption
import com.bendey.restaurant.core.domain.subscription.PlanChangeInput
import com.bendey.restaurant.core.domain.subscription.StatusBanner
import com.bendey.restaurant.core.domain.subscription.SubmitPaymentInput
import com.bendey.restaurant.core.domain.subscription.SubscriptionActionResult
import com.bendey.restaurant.core.domain.subscription.SubscriptionPayment
import com.bendey.restaurant.core.domain.subscription.SubscriptionRepository
import com.bendey.restaurant.core.domain.subscription.SubscriptionTimelineEvent
import com.bendey.restaurant.core.domain.subscription.SupportContact
import com.bendey.restaurant.core.domain.subscription.TenantSubscriptionView
import com.bendey.restaurant.core.network.api.SubscriptionApi
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.dto.BillingHubDto
import com.bendey.restaurant.core.network.dto.SubscriptionActionResponseDto
import com.bendey.restaurant.core.network.error.NetworkErrorMapper
import com.bendey.restaurant.core.network.serialization.ApiJson
import com.bendey.restaurant.core.network.session.NetworkSessionProvider
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
    private val client: OkHttpClient,
    private val sessionProvider: NetworkSessionProvider,
) : SubscriptionRepository {

    private val api: SubscriptionApi
        get() = tenantRetrofitProvider.create()

    override suspend fun getHub(): AppResult<BillingHub> = apiCall {
        api.getSummary().toDomain()
    }

    override suspend fun getPlans(): AppResult<List<AvailablePlan>> = apiCall {
        api.getPlans().data.map {
            AvailablePlan(
                id = it.id,
                name = it.name,
                description = it.description,
                price = it.price,
                annualPrice = it.annualPrice,
                featured = it.featured,
            )
        }
    }

    override suspend fun submitPayment(input: SubmitPaymentInput): AppResult<SubscriptionActionResult> =
        postMultipart(
            path = "/api/subscription/payments",
            fields = buildMap {
                input.billingCycleId?.let { put("billing_cycle_id", it.toString()) }
                put("amount", input.amount.toString())
                put("payment_method", input.paymentMethod)
                put("reference", input.reference)
                put("notes", input.notes)
                input.paymentDate?.let { put("payment_date", it) }
            },
            receiptBytes = input.receiptBytes,
            receiptMimeType = input.receiptMimeType,
            receiptFileName = input.receiptFileName,
        )

    override suspend fun requestPlanChange(input: PlanChangeInput): AppResult<SubscriptionActionResult> =
        postMultipart(
            path = "/api/subscription/plan-change",
            fields = buildMap {
                put("plan_id", input.planId.toString())
                put("amount", input.amount.toString())
                put("payment_method", input.paymentMethod)
                put("reference", input.reference)
                put("notes", input.notes)
                input.paymentDate?.let { put("payment_date", it) }
            },
            receiptBytes = input.receiptBytes,
            receiptMimeType = input.receiptMimeType,
            receiptFileName = input.receiptFileName,
        )

    private suspend fun postMultipart(
        path: String,
        fields: Map<String, String>,
        receiptBytes: ByteArray,
        receiptMimeType: String,
        receiptFileName: String,
    ): AppResult<SubscriptionActionResult> {
        return try {
            val baseUrl = sessionProvider.tenantApiBaseUrl()?.trim()?.trimEnd('/')
                ?: return AppResult.Error("Tenant API URL no configurada")
            val normalizedBase = baseUrl.removeSuffix("/api")
            val url = "$normalizedBase$path"
            val bodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
            fields.forEach { (key, value) -> bodyBuilder.addFormDataPart(key, value) }
            bodyBuilder.addFormDataPart(
                "receipt",
                receiptFileName,
                receiptBytes.toRequestBody(receiptMimeType.toMediaType()),
            )
            val requestBuilder = Request.Builder().url(url).post(bodyBuilder.build())
            sessionProvider.token()?.let { requestBuilder.header("Authorization", "Bearer $it") }
            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val mapped = NetworkErrorMapper.map(
                    IllegalStateException(responseBody.ifBlank { "Error al enviar el comprobante" }),
                )
                return AppResult.Error(mapped.message ?: "Error al enviar el comprobante", mapped)
            }
            val parsed = ApiJson.decodeFromString(SubscriptionActionResponseDto.serializer(), responseBody)
            AppResult.Success(
                SubscriptionActionResult(
                    success = parsed.success,
                    message = parsed.message,
                    hub = parsed.hub?.toDomain(),
                ),
            )
        } catch (e: Exception) {
            val mapped = NetworkErrorMapper.map(e)
            AppResult.Error(mapped.message ?: "Error al enviar el comprobante", mapped)
        }
    }
}

private fun BillingHubDto.toDomain(): BillingHub = BillingHub(
    subscription = TenantSubscriptionView(
        hasSubscription = subscription.hasSubscription,
        planId = subscription.planId,
        planName = subscription.planName,
        billingCycle = subscription.billingCycle,
        status = subscription.status,
        tenantStatus = subscription.tenantStatus,
        daysUntilExpiry = subscription.daysUntilExpiry,
        inGracePeriod = subscription.inGracePeriod,
        isOverdue = subscription.isOverdue,
        isSuspended = subscription.isSuspended,
        isBlocked = subscription.isBlocked,
        canSubmitPayment = subscription.canSubmitPayment,
        pendingAmount = subscription.pendingAmount,
        reconnectionFee = subscription.reconnectionFee,
        canOperate = subscription.canOperate,
        nextBillingDate = subscription.nextBillingDate,
        supportMessage = subscription.supportMessage,
        hasPendingPaymentReview = subscription.hasPendingPaymentReview,
        provisionalHoursLeft = subscription.provisionalHoursLeft,
    ),
    billingContext = BillingContext(
        urgencyTier = billingContext.urgencyTier,
        planAmount = billingContext.planAmount,
        currentPaymentLabel = billingContext.currentPaymentLabel,
        currentPaymentTone = billingContext.currentPaymentTone,
        hasRealDebt = billingContext.hasRealDebt,
    ),
    paymentConfig = PaymentConfig(
        methods = paymentConfig.methods.map { PaymentMethodOption(it.key, it.label, it.enabled) },
        bankAccounts = paymentConfig.bankAccounts.map {
            BankAccountOption(it.bank, it.accountNumber, it.cci, it.holder, it.currency)
        },
        yapeQrUrl = paymentConfig.yapeQrUrl,
        plinQrUrl = paymentConfig.plinQrUrl,
    ),
    support = SupportContact(support.whatsapp, support.email, support.phone),
    statusBanner = StatusBanner(statusBanner.variant, statusBanner.message),
    documents = documents?.let {
        DocumentUsage(
            isUnlimited = it.isUnlimited,
            planLimit = it.planLimit,
            planUsed = it.planUsed,
            totalAvailable = it.totalAvailable,
            totalConsumed = it.totalConsumed,
            usagePercent = it.usagePercent,
            warningLevel = it.warningLevel,
            warningMessage = it.warningMessage,
            canEmit = it.canEmit,
        )
    },
    payments = payments.map {
        SubscriptionPayment(
            id = it.id,
            amount = it.amount,
            paymentMethod = it.paymentMethod,
            status = it.status,
            purpose = it.purpose,
            newPlanName = it.newPlanName,
            reference = it.reference,
            rejectReason = it.rejectReason,
            createdAt = it.createdAt,
        )
    },
    events = events.map { SubscriptionTimelineEvent(it.id, it.label, it.reason, it.createdAt) },
)

private inline fun <T> apiCall(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: Exception) {
    val mapped = NetworkErrorMapper.map(e)
    AppResult.Error(mapped.message ?: "Error de conexión", mapped)
}
