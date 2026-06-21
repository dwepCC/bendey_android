package com.bendey.restaurant.core.domain.auth

import com.bendey.restaurant.core.domain.model.PinStation
import com.bendey.restaurant.core.domain.model.TenantBinding
import com.bendey.restaurant.core.domain.model.UserSession

interface TenantRepository {
    suspend fun resolveTenantByRuc(ruc: String): Result<TenantBinding>
    suspend fun bindTenant(binding: TenantBinding)
    suspend fun clearTenant()
}

interface AuthRepository {
    suspend fun loginWithEmail(email: String, password: String): Result<UserSession>
    suspend fun loginWithPin(pin: String, station: PinStation): Result<UserSession>
    suspend fun refreshRestaurantPermissions(): Result<UserSession>
    suspend fun logout()
}
