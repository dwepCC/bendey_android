package com.bendey.restaurant.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TenantByRucDto(
    val slug: String = "",
    @SerialName("tenant_slug") val tenantSlug: String? = null,
    val name: String = "",
    val subdomain: String? = null,
    @SerialName("api_url") val apiUrl: String? = null,
    @SerialName("token_consulta_datos") val tokenConsultaDatos: String = "",
)

@Serializable
data class ValidateRucRequestDto(
    val ruc: String,
)

@Serializable
data class ValidateRucResponseDto(
    val success: Boolean = false,
    val ruc: String = "",
    @SerialName("razon_social") val razonSocial: String = "",
    val direccion: String = "",
    val ubigeo: String = "",
    val estado: String = "",
    val condicion: String = "",
)

@Serializable
data class PublicRegisterRequestDto(
    val name: String,
    @SerialName("razon_social") val razonSocial: String = "",
    @SerialName("business_name") val businessName: String = "",
    val ruc: String,
    val email: String,
    val phone: String = "",
    val address: String = "",
    val ubigeo: String = "",
    val rubro: String = "gastronomico",
    val password: String,
    @SerialName("plan_id") val planId: Int = 0,
    @SerialName("referral_code") val referralCode: String = "",
)

@Serializable
data class PublicRegisterResponseDto(
    val slug: String = "",
    val name: String = "",
    val subdomain: String? = null,
    @SerialName("tenant_url") val tenantUrl: String? = null,
    val email: String? = null,
    val message: String? = null,
)

@Serializable
data class PublicApplicationDto(
    val name: String = "",
    val code: String? = null,
    val description: String? = null,
    @SerialName("windows_store_url") val windowsStoreUrl: String = "",
    @SerialName("android_store_url") val androidStoreUrl: String = "",
    val downloads: Map<String, PublicApplicationDownloadDto>? = null,
)

@Serializable
data class PublicApplicationDownloadDto(
    val platform: String = "",
    val version: String = "",
    @SerialName("download_url") val downloadUrl: String = "",
    @SerialName("file_name") val fileName: String = "",
    @SerialName("file_size") val fileSize: Long = 0,
)

@Serializable
data class PublicApplicationsResponseDto(
    val data: List<PublicApplicationDto> = emptyList(),
)

@Serializable
data class AuthUserDto(
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    @SerialName("employee_type") val employeeType: String? = null,
    @SerialName("staff_id") val staffId: Int? = null,
)

@Serializable
data class BranchBriefDto(
    val id: Int,
    val name: String,
    @SerialName("is_main") val isMain: Boolean? = null,
)

@Serializable
data class LoginResponseDto(
    val token: String,
    val user: AuthUserDto,
    val modules: List<String>? = null,
    val permissions: List<String>? = null,
    @SerialName("restaurant_permissions") val restaurantPermissions: List<String>? = null,
    @SerialName("active_branch") val activeBranch: BranchBriefDto? = null,
    @SerialName("can_switch_branch") val canSwitchBranch: Boolean? = null,
    @SerialName("allowed_branches") val allowedBranches: List<BranchBriefDto>? = null,
)

@Serializable
data class PinLoginRequestDto(
    val pin: String,
    val station: String,
)

@Serializable
data class SessionPermissionsDto(
    val permissions: List<String>? = null,
    @SerialName("employee_type") val employeeType: String? = null,
    @SerialName("staff_id") val staffId: Int? = null,
    @SerialName("auth_method") val authMethod: String? = null,
)

@Serializable
data class EmailLoginRequestDto(
    val email: String,
    val password: String,
    val slug: String? = null,
)

@Serializable
data class ApiErrorDto(
    val error: String? = null,
    val code: String? = null,
    val message: String? = null,
)

@Serializable
data class DashboardResponseDto(
    val period: DashboardPeriodDto? = null,
    val summary: DashboardSummaryDto? = null,
    @SerialName("by_order_type") val byOrderType: List<DashboardOrderTypeDto> = emptyList(),
    @SerialName("by_payment_method") val byPaymentMethod: List<DashboardPaymentMethodDto> = emptyList(),
    @SerialName("top_products") val topProducts: List<DashboardTopProductDto> = emptyList(),
    val hourly: List<DashboardHourlyDto> = emptyList(),
    @SerialName("daily_30") val daily30: List<DashboardDailyDto> = emptyList(),
    @SerialName("recent_sessions") val recentSessions: List<DashboardSessionDto> = emptyList(),
    @SerialName("table_summary") val tableSummary: DashboardTableSummaryDto? = null,
    // Legacy flat fields (tolerated)
    val sales_today: Double? = null,
    val ticket_average: Double? = null,
    val active_tables: Int? = null,
    val total_tables: Int? = null,
    val pending_comandas: Int? = null,
    val data: DashboardDataDto? = null,
)

@Serializable
data class DashboardPeriodDto(
    val from: String = "",
    val to: String = "",
    @SerialName("duration_days") val durationDays: Int = 1,
    @SerialName("prev_from") val prevFrom: String = "",
    @SerialName("prev_to") val prevTo: String = "",
)

@Serializable
data class DashboardSummaryDto(
    @SerialName("total_revenue") val totalRevenue: Double = 0.0,
    @SerialName("total_sessions") val totalSessions: Int = 0,
    @SerialName("paid_sessions") val paidSessions: Int = 0,
    @SerialName("open_sessions") val openSessions: Int = 0,
    @SerialName("cancelled_sessions") val cancelledSessions: Int = 0,
    @SerialName("avg_ticket") val avgTicket: Double = 0.0,
    @SerialName("total_guests") val totalGuests: Int = 0,
    @SerialName("prev_revenue") val prevRevenue: Double = 0.0,
    @SerialName("revenue_change_pct") val revenueChangePct: Double = 0.0,
    @SerialName("prev_sessions") val prevSessions: Int = 0,
    @SerialName("sessions_change_pct") val sessionsChangePct: Double = 0.0,
)

@Serializable
data class DashboardOrderTypeDto(
    val type: String = "",
    val count: Int = 0,
    val revenue: Double = 0.0,
)

@Serializable
data class DashboardPaymentMethodDto(
    val method: String = "",
    val count: Int = 0,
    val amount: Double = 0.0,
)

@Serializable
data class DashboardTopProductDto(
    @SerialName("product_id") val productId: Int = 0,
    val name: String = "",
    val quantity: Double = 0.0,
    val revenue: Double = 0.0,
    val category: String? = null,
)

@Serializable
data class DashboardHourlyDto(
    val hour: Int = 0,
    val sessions: Int = 0,
    val revenue: Double = 0.0,
)

@Serializable
data class DashboardDailyDto(
    val date: String = "",
    val sessions: Int = 0,
    val revenue: Double = 0.0,
)

@Serializable
data class DashboardSessionDto(
    val id: Int = 0,
    @SerialName("order_code") val orderCode: String = "",
    @SerialName("order_type") val orderType: String = "",
    @SerialName("order_status") val orderStatus: String = "",
    val status: String = "",
    @SerialName("table_name") val tableName: String = "",
    @SerialName("customer_name") val customerName: String = "",
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    val guests: Int = 0,
    @SerialName("opened_at") val openedAt: String = "",
    @SerialName("closed_at") val closedAt: String? = null,
)

@Serializable
data class DashboardTableSummaryDto(
    val total: Int = 0,
    val libre: Int = 0,
    val ocupada: Int = 0,
    @SerialName("en_consumo") val enConsumo: Int = 0,
    val reservada: Int = 0,
)

@Serializable
data class DashboardDataDto(
    val sales_today: Double? = null,
    val ticket_average: Double? = null,
    val active_tables: Int? = null,
    val total_tables: Int? = null,
    val pending_comandas: Int? = null,
)
