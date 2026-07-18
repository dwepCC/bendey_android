package com.bendey.restaurant.core.domain.subscription

data class TenantSubscriptionView(
    val hasSubscription: Boolean = false,
    val planId: Int = 0,
    val planName: String = "",
    val billingCycle: String = "",
    val status: String = "",
    val tenantStatus: String = "",
    val daysUntilExpiry: Int = 0,
    val inGracePeriod: Boolean = false,
    val isOverdue: Boolean = false,
    val isSuspended: Boolean = false,
    val isBlocked: Boolean = false,
    val canSubmitPayment: Boolean = false,
    val pendingAmount: Double = 0.0,
    val reconnectionFee: Double = 0.0,
    val canOperate: Boolean = true,
    val nextBillingDate: String? = null,
    val supportMessage: String? = null,
    val hasPendingPaymentReview: Boolean = false,
    val provisionalHoursLeft: Int = 0,
)

data class BillingContext(
    val urgencyTier: String = "normal",
    val planAmount: Double = 0.0,
    val currentPaymentLabel: String = "",
    val currentPaymentTone: String = "success",
    val hasRealDebt: Boolean = false,
)

data class PaymentMethodOption(
    val key: String,
    val label: String,
    val enabled: Boolean,
)

data class BankAccountOption(
    val bank: String,
    val accountNumber: String,
    val cci: String,
    val holder: String,
    val currency: String,
)

data class PaymentConfig(
    val methods: List<PaymentMethodOption> = emptyList(),
    val bankAccounts: List<BankAccountOption> = emptyList(),
    val yapeQrUrl: String = "",
    val plinQrUrl: String = "",
)

data class SupportContact(
    val whatsapp: String = "",
    val email: String = "",
    val phone: String = "",
)

data class StatusBanner(
    val variant: String = "success",
    val message: String = "",
)

data class DocumentUsage(
    val isUnlimited: Boolean = true,
    val planLimit: Int = 0,
    val planUsed: Int = 0,
    val totalAvailable: Int = 0,
    val totalConsumed: Int = 0,
    val usagePercent: Int = 0,
    val warningLevel: String = "none",
    val warningMessage: String? = null,
    val canEmit: Boolean = true,
)

data class SubscriptionPayment(
    val id: Int,
    val amount: Double,
    val paymentMethod: String,
    val status: String,
    val purpose: String,
    val newPlanName: String?,
    val reference: String?,
    val rejectReason: String?,
    val createdAt: String,
)

data class SubscriptionTimelineEvent(
    val id: Int,
    val label: String,
    val reason: String?,
    val createdAt: String,
)

data class BillingHub(
    val subscription: TenantSubscriptionView = TenantSubscriptionView(),
    val billingContext: BillingContext = BillingContext(),
    val paymentConfig: PaymentConfig = PaymentConfig(),
    val support: SupportContact = SupportContact(),
    val statusBanner: StatusBanner = StatusBanner(),
    val documents: DocumentUsage? = null,
    val payments: List<SubscriptionPayment> = emptyList(),
    val events: List<SubscriptionTimelineEvent> = emptyList(),
)

data class AvailablePlan(
    val id: Int,
    val name: String,
    val description: String,
    val price: Double,
    val annualPrice: Double,
    val featured: Boolean,
)

data class SubscriptionActionResult(
    val success: Boolean,
    val message: String?,
    val hub: BillingHub?,
)

class SubmitPaymentInput(
    val billingCycleId: Int?,
    val amount: Double,
    val paymentMethod: String,
    val reference: String,
    val notes: String,
    val paymentDate: String?,
    val receiptBytes: ByteArray,
    val receiptMimeType: String,
    val receiptFileName: String,
)

class PlanChangeInput(
    val planId: Int,
    val amount: Double,
    val paymentMethod: String,
    val reference: String,
    val notes: String,
    val paymentDate: String?,
    val receiptBytes: ByteArray,
    val receiptMimeType: String,
    val receiptFileName: String,
)
