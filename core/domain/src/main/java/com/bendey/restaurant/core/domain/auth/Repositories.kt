package com.bendey.restaurant.core.domain.auth

import com.bendey.restaurant.core.domain.model.PinStation
import com.bendey.restaurant.core.domain.model.TenantBinding
import com.bendey.restaurant.core.domain.model.UserSession

data class RestaurantRegistrationInput(
    val name: String,
    val razonSocial: String,
    val ruc: String,
    val email: String,
    val phone: String,
    val password: String,
    val address: String = "",
    val ubigeo: String = "",
)

data class RestaurantRegistrationResult(
    val name: String,
)

data class SunatRucValidation(
    val ruc: String,
    val razonSocial: String,
    val direccion: String = "",
    val ubigeo: String = "",
)

interface TenantRepository {
    suspend fun resolveTenantByRuc(ruc: String): Result<TenantBinding>
    suspend fun validateRucWithSunat(ruc: String): Result<SunatRucValidation>
    suspend fun bindTenant(binding: TenantBinding)
    suspend fun registerRestaurant(input: RestaurantRegistrationInput): Result<RestaurantRegistrationResult>
    suspend fun clearTenant()
}

interface AuthRepository {
    suspend fun loginWithEmail(email: String, password: String): Result<UserSession>
    suspend fun loginWithPin(pin: String, station: PinStation): Result<UserSession>
    suspend fun refreshRestaurantPermissions(): Result<UserSession>
    suspend fun logout()
}
