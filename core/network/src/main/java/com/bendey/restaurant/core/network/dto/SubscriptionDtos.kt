package com.bendey.restaurant.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TenantSubscriptionViewDto(
    @SerialName("has_subscription") val hasSubscription: Boolean = false,
    @SerialName("plan_id") val planId: Int = 0,
    @SerialName("plan_name") val planName: String = "",
    @SerialName("billing_cycle") val billingCycle: String = "",
    val status: String = "",
    @SerialName("tenant_status") val tenantStatus: String = "",
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("days_until_expiry") val daysUntilExpiry: Int = 0,
    @SerialName("in_grace_period") val inGracePeriod: Boolean = false,
    @SerialName("is_overdue") val isOverdue: Boolean = false,
    @SerialName("is_suspended") val isSuspended: Boolean = false,
    @SerialName("is_blocked") val isBlocked: Boolean = false,
    @SerialName("strike_count") val strikeCount: Int = 0,
    @SerialName("can_submit_payment") val canSubmitPayment: Boolean = false,
    @SerialName("provisional_until") val provisionalUntil: String? = null,
    @SerialName("pending_amount") val pendingAmount: Double = 0.0,
    @SerialName("reconnection_fee") val reconnectionFee: Double = 0.0,
    @SerialName("show_renewal_banner") val showRenewalBanner: Boolean = false,
    @SerialName("show_suspended_banner") val showSuspendedBanner: Boolean = false,
    @SerialName("can_operate") val canOperate: Boolean = true,
    @SerialName("portal_url") val portalUrl: String? = null,
    @SerialName("next_billing_date") val nextBillingDate: String? = null,
    @SerialName("pending_invoice_id") val pendingInvoiceId: Int = 0,
    @SerialName("support_message") val supportMessage: String? = null,
    @SerialName("has_pending_payment_review") val hasPendingPaymentReview: Boolean = false,
    @SerialName("provisional_hours_left") val provisionalHoursLeft: Int = 0,
)

@Serializable
data class BillingContextViewDto(
    @SerialName("reminder_days") val reminderDays: List<Int> = emptyList(),
    @SerialName("max_reminder_days") val maxReminderDays: Int = 0,
    @SerialName("urgency_tier") val urgencyTier: String = "normal",
    @SerialName("plan_amount") val planAmount: Double = 0.0,
    @SerialName("current_payment_label") val currentPaymentLabel: String = "",
    @SerialName("current_payment_tone") val currentPaymentTone: String = "success",
    @SerialName("has_real_debt") val hasRealDebt: Boolean = false,
    @SerialName("display_debt_amount") val displayDebtAmount: Double? = null,
    @SerialName("show_status_banner") val showStatusBanner: Boolean = false,
    @SerialName("status_banner_variant") val statusBannerVariant: String? = null,
    @SerialName("status_banner_message") val statusBannerMessage: String? = null,
)

@Serializable
data class PaymentMethodConfigDto(
    val key: String = "",
    val label: String = "",
    val enabled: Boolean = false,
)

@Serializable
data class BankAccountConfigDto(
    val bank: String = "",
    @SerialName("account_number") val accountNumber: String = "",
    val cci: String = "",
    val holder: String = "",
    val currency: String = "",
    val enabled: Boolean = true,
)

@Serializable
data class PaymentConfigViewDto(
    val methods: List<PaymentMethodConfigDto> = emptyList(),
    @SerialName("bank_accounts") val bankAccounts: List<BankAccountConfigDto> = emptyList(),
    @SerialName("yape_qr_url") val yapeQrUrl: String = "",
    @SerialName("plin_qr_url") val plinQrUrl: String = "",
    @SerialName("use_internal_hub") val useInternalHub: Boolean = true,
)

@Serializable
data class SupportConfigDto(
    val whatsapp: String = "",
    val email: String = "",
    val phone: String = "",
)

@Serializable
data class StatusBannerViewDto(
    val variant: String = "success",
    val message: String = "",
)

@Serializable
data class DocumentUsageHubViewDto(
    @SerialName("is_unlimited") val isUnlimited: Boolean = true,
    @SerialName("plan_limit") val planLimit: Int = 0,
    @SerialName("plan_used") val planUsed: Int = 0,
    @SerialName("plan_remaining") val planRemaining: Int = 0,
    @SerialName("package_bonus") val packageBonus: Int = 0,
    @SerialName("package_used") val packageUsed: Int = 0,
    @SerialName("package_remaining") val packageRemaining: Int = 0,
    @SerialName("total_available") val totalAvailable: Int = 0,
    @SerialName("total_consumed") val totalConsumed: Int = 0,
    @SerialName("usage_percent") val usagePercent: Int = 0,
    @SerialName("warning_level") val warningLevel: String = "none",
    @SerialName("warning_message") val warningMessage: String? = null,
    @SerialName("can_emit") val canEmit: Boolean = true,
)

@Serializable
data class InvoiceViewDto(
    val id: Int = 0,
    val amount: Double = 0.0,
    @SerialName("reconnection_fee") val reconnectionFee: Double = 0.0,
    val currency: String = "PEN",
    val status: String = "",
    @SerialName("due_date") val dueDate: String = "",
    @SerialName("period_start") val periodStart: String = "",
    @SerialName("period_end") val periodEnd: String = "",
)

@Serializable
data class PaymentViewDto(
    val id: Int = 0,
    val amount: Double = 0.0,
    @SerialName("payment_method") val paymentMethod: String = "",
    val status: String = "",
    val purpose: String = "renewal",
    @SerialName("new_plan_id") val newPlanId: Int? = null,
    @SerialName("new_plan_name") val newPlanName: String? = null,
    val reference: String? = null,
    @SerialName("payment_date") val paymentDate: String? = null,
    @SerialName("reject_reason") val rejectReason: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class TimelineEventViewDto(
    val id: Int = 0,
    @SerialName("event_type") val eventType: String = "",
    val label: String = "",
    val reason: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class BillingHubDto(
    val subscription: TenantSubscriptionViewDto = TenantSubscriptionViewDto(),
    @SerialName("billing_context") val billingContext: BillingContextViewDto = BillingContextViewDto(),
    @SerialName("payment_config") val paymentConfig: PaymentConfigViewDto = PaymentConfigViewDto(),
    val support: SupportConfigDto = SupportConfigDto(),
    @SerialName("status_banner") val statusBanner: StatusBannerViewDto = StatusBannerViewDto(),
    val documents: DocumentUsageHubViewDto? = null,
    val invoices: List<InvoiceViewDto> = emptyList(),
    val payments: List<PaymentViewDto> = emptyList(),
    val events: List<TimelineEventViewDto> = emptyList(),
)

@Serializable
data class AvailablePlanItemDto(
    val id: Int = 0,
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    @SerialName("annual_price") val annualPrice: Double = 0.0,
    val featured: Boolean = false,
)

@Serializable
data class AvailablePlansResponseDto(
    val data: List<AvailablePlanItemDto> = emptyList(),
)

@Serializable
data class SubscriptionActionResponseDto(
    val success: Boolean = false,
    val message: String? = null,
    val hub: BillingHubDto? = null,
)
