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
    /** "PE" o "EC": decide cómo se valida el RUC y la moneda del plan. */
    val country: String = "PE",
)

/** Largo del RUC con el que se registra un negocio: Perú 11, Ecuador 13. */
fun registrationRucLength(country: String): Int = if (country == "EC") 13 else 11

data class RestaurantRegistrationResult(
    val name: String,
)

/**
 * Resultado de validar un RUC para registrarse. Perú lo consulta en SUNAT y trae razón social y dirección.
 * Ecuador no tiene consulta: el backend solo revisa que el RUC sea posible, los datos vienen vacíos y los
 * escribe quien se registra.
 */
data class RucValidation(
    val ruc: String,
    val razonSocial: String,
    val direccion: String = "",
    val ubigeo: String = "",
    /** El RUC es posible pero dudoso (la cédula no pasa el dígito verificador). No bloquea. */
    val warning: String = "",
)

interface TenantRepository {
    suspend fun resolveTenantByRuc(ruc: String): Result<TenantBinding>
    suspend fun validateRuc(ruc: String, country: String): Result<RucValidation>
    suspend fun bindTenant(binding: TenantBinding)
    suspend fun registerRestaurant(input: RestaurantRegistrationInput): Result<RestaurantRegistrationResult>
    suspend fun clearTenant()

    /** Contacto de soporte de Bendey (público). Con valor por defecto para no romper los fakes. */
    suspend fun supportContact(): Result<com.bendey.restaurant.core.domain.subscription.SupportContact> =
        Result.success(com.bendey.restaurant.core.domain.subscription.SupportContact())
}

interface AuthRepository {
    suspend fun loginWithEmail(email: String, password: String): Result<UserSession>
    suspend fun loginWithPin(pin: String, station: PinStation): Result<UserSession>
    suspend fun refreshRestaurantPermissions(): Result<UserSession>
    suspend fun logout()
}
