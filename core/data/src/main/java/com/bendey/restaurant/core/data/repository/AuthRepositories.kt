package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.data.mapper.toDomain
import com.bendey.restaurant.core.data.cache.OperationalDataCache
import com.bendey.restaurant.core.data.session.SessionManager
import com.bendey.restaurant.core.domain.auth.AuthRepository
import com.bendey.restaurant.core.domain.auth.RestaurantRegistrationInput
import com.bendey.restaurant.core.domain.auth.RestaurantRegistrationResult
import com.bendey.restaurant.core.domain.auth.SunatRucValidation
import com.bendey.restaurant.core.domain.auth.TenantRepository
import com.bendey.restaurant.core.network.dto.PublicRegisterRequestDto
import com.bendey.restaurant.core.network.dto.ValidateRucRequestDto
import com.bendey.restaurant.core.domain.model.PinStation
import com.bendey.restaurant.core.domain.model.TenantBinding
import com.bendey.restaurant.core.domain.model.UserSession
import com.bendey.restaurant.core.network.api.AuthApi
import com.bendey.restaurant.core.network.api.PublicApi
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.dto.EmailLoginRequestDto
import com.bendey.restaurant.core.network.dto.PinLoginRequestDto
import com.bendey.restaurant.core.network.error.NetworkErrorMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TenantRepositoryImpl @Inject constructor(
    private val publicApi: PublicApi,
    private val sessionManager: SessionManager,
    private val tenantRetrofitProvider: TenantRetrofitProvider,
    private val operationalDataCache: OperationalDataCache,
) : TenantRepository {

    override suspend fun resolveTenantByRuc(ruc: String): Result<TenantBinding> = runCatching {
        val normalized = ruc.replace(Regex("\\D"), "").trim()
        require(normalized.length >= 8) { "Ingrese un RUC válido" }
        val dto = publicApi.getTenantByRuc(normalized)
        val binding = dto.toDomain(normalized)
        require(binding.slug.isNotBlank()) { "Tenant no encontrado" }
        require(binding.apiUrl.isNotBlank()) { "URL de API no disponible para este tenant" }
        binding.copy(apiUrl = normalizeApiUrl(binding.apiUrl))
    }.recoverCatching { error ->
        throw NetworkErrorMapper.map(error)
    }

    override suspend fun bindTenant(binding: TenantBinding) {
        sessionManager.bindTenant(binding)
        tenantRetrofitProvider.invalidate()
    }

    override suspend fun validateRucWithSunat(ruc: String): Result<SunatRucValidation> = runCatching {
        val normalized = ruc.replace(Regex("\\D"), "").trim()
        require(normalized.length == 11) { "El RUC debe tener 11 dígitos" }
        val dto = publicApi.validateRuc(ValidateRucRequestDto(ruc = normalized))
        require(dto.success) { "RUC no encontrado en SUNAT" }
        require(dto.razonSocial.isNotBlank()) { "No se obtuvo la razón social del RUC" }
        SunatRucValidation(
            ruc = dto.ruc.ifBlank { normalized },
            razonSocial = dto.razonSocial,
            direccion = dto.direccion,
            ubigeo = dto.ubigeo,
        )
    }.recoverCatching { error ->
        throw NetworkErrorMapper.map(error)
    }

    override suspend fun registerRestaurant(
        input: RestaurantRegistrationInput,
    ): Result<RestaurantRegistrationResult> = runCatching {
        val normalizedRuc = input.ruc.replace(Regex("\\D"), "").trim()
        require(normalizedRuc.length == 11) { "El RUC debe tener 11 dígitos" }
        val response = publicApi.registerTenant(
            PublicRegisterRequestDto(
                name = input.name.trim(),
                razonSocial = input.razonSocial.trim(),
                ruc = normalizedRuc,
                email = input.email.trim(),
                phone = input.phone.trim(),
                address = input.address.trim(),
                ubigeo = input.ubigeo.trim(),
                password = input.password,
                rubro = RESTAURANT_RUBRO,
            ),
        )
        val binding = resolveTenantByRuc(normalizedRuc).getOrThrow()
        operationalDataCache.clearAll()
        sessionManager.clearUserSession()
        tenantRetrofitProvider.invalidate()
        bindTenant(binding)
        RestaurantRegistrationResult(name = response.name.ifBlank { binding.name })
    }.recoverCatching { error ->
        throw NetworkErrorMapper.map(error)
    }

    override suspend fun clearTenant() {
        operationalDataCache.clearAll()
        sessionManager.clearTenant()
        tenantRetrofitProvider.invalidate()
    }

    private fun normalizeApiUrl(url: String): String {
        var base = url.trim().trimEnd('/')
        if (base.endsWith("/api")) base = base.removeSuffix("/api")
        return base
    }

    private companion object {
        const val RESTAURANT_RUBRO = "gastronomico"
    }
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
    private val sessionManager: SessionManager,
    private val operationalDataCache: OperationalDataCache,
) : AuthRepository {

    override suspend fun loginWithEmail(email: String, password: String): Result<UserSession> = runCatching {
        operationalDataCache.clearAll()
        tenantRetrofitProvider.invalidate()
        sessionManager.clearUserSession()
        val tenant = sessionManager.getTenant()
            ?: error("Vincule el RUC antes de iniciar sesión")
        val api = tenantRetrofitProvider.create<AuthApi>()
        val response = api.login(
            EmailLoginRequestDto(
                email = email.trim(),
                password = password,
                slug = tenant.slug,
            ),
        )
        val session = response.toDomain()
        sessionManager.applyUserSession(session)
        val enriched = enrichSession(session)
        if (enriched != session) {
            sessionManager.applyUserSession(enriched)
        }
        enriched
    }.recoverCatching { throw NetworkErrorMapper.map(it) }

    override suspend fun loginWithPin(pin: String, station: PinStation): Result<UserSession> = runCatching {
        operationalDataCache.clearAll()
        tenantRetrofitProvider.invalidate()
        sessionManager.clearUserSession()
        require(pin.length >= 4) { "Ingrese al menos 4 dígitos" }
        val api = tenantRetrofitProvider.create<AuthApi>()
        val response = api.pinLogin(
            PinLoginRequestDto(pin = pin, station = station.routeKey),
        )
        val session = response.toDomain()
        sessionManager.applyUserSession(session)
        val enriched = enrichSession(session)
        if (enriched != session) {
            sessionManager.applyUserSession(enriched)
        }
        enriched
    }.recoverCatching { throw NetworkErrorMapper.map(it) }

    override suspend fun refreshRestaurantPermissions(): Result<UserSession> = runCatching {
        val session = sessionManager.getUserSession() ?: error("Sin sesión activa")
        val enriched = enrichSession(session)
        sessionManager.applyUserSession(enriched)
        enriched
    }.recoverCatching { throw NetworkErrorMapper.map(it) }

    /** Refresca permisos restaurante (paridad Capacitor `getSessionPermissions`). */
    private suspend fun enrichSession(session: UserSession): UserSession {
        return try {
            val api = tenantRetrofitProvider.create<AuthApi>()
            val dto = api.getSessionPermissions()
            val perms = dto.permissions.orEmpty()
            session.copy(
                restaurantPermissions = perms.ifEmpty { session.restaurantPermissions },
                user = session.user.copy(
                    employeeType = dto.employeeType?.takeIf { it.isNotBlank() } ?: session.user.employeeType,
                    staffId = dto.staffId ?: session.user.staffId,
                ),
            )
        } catch (_: Exception) {
            session
        }
    }

    override suspend fun logout() {
        operationalDataCache.clearAll()
        sessionManager.clearUserSession()
        tenantRetrofitProvider.invalidate()
    }
}
