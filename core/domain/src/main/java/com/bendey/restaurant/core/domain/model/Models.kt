package com.bendey.restaurant.core.domain.model

data class TenantBinding(
    val slug: String,
    val name: String,
    val ruc: String,
    val apiUrl: String,
    val tokenConsultaDatos: String = "",
    val boundAtEpochMs: Long = System.currentTimeMillis(),
)

data class BranchBrief(
    val id: Int,
    val name: String,
    val isMain: Boolean = false,
)

data class AuthUser(
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    val employeeType: String? = null,
    val staffId: Int? = null,
)

data class UserSession(
    val token: String,
    val user: AuthUser,
    val restaurantPermissions: List<String>,
    val modules: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
    val activeBranch: BranchBrief? = null,
    val canSwitchBranch: Boolean = false,
    val allowedBranches: List<BranchBrief> = emptyList(),
)

/** Snapshot de sesión de caja — persistido localmente. */
data class CashSessionSnapshot(
    val sessionId: Int,
    val branchId: Int,
    val openedAtEpochMs: Long,
    val openingAmount: Double,
    val label: String? = null,
    val expectedBalance: Double? = null,
)

enum class PinStation(val routeKey: String, val label: String) {
    WAITER("waiter", "Mozo"),
    CASHIER("cashier", "Cajero"),
    KITCHEN("kitchen", "Cocina"),
    DELIVERY("delivery", "Delivery"),
    ADMIN("admin", "Administración"),
    ;

    companion object {
        fun fromRouteKey(key: String): PinStation? =
            entries.firstOrNull { it.routeKey.equals(key, ignoreCase = true) }
    }
}

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(val message: String, val cause: Throwable? = null) : AppResult<Nothing>
    data object Loading : AppResult<Nothing>
}
