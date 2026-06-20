package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.data.mapper.toDomain
import com.bendey.restaurant.core.data.session.SessionManager
import com.bendey.restaurant.core.domain.auth.AuthRepository
import com.bendey.restaurant.core.domain.auth.TenantRepository
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

    override suspend fun clearTenant() {
        sessionManager.clearTenant()
        tenantRetrofitProvider.invalidate()
    }

    private fun normalizeApiUrl(url: String): String {
        var base = url.trim().trimEnd('/')
        if (base.endsWith("/api")) base = base.removeSuffix("/api")
        return base
    }
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
    private val sessionManager: SessionManager,
) : AuthRepository {

    override suspend fun loginWithEmail(email: String, password: String): Result<UserSession> = runCatching {
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
        session
    }.recoverCatching { throw NetworkErrorMapper.map(it) }

    override suspend fun loginWithPin(pin: String, station: PinStation): Result<UserSession> = runCatching {
        require(pin.length >= 4) { "Ingrese al menos 4 dígitos" }
        val api = tenantRetrofitProvider.create<AuthApi>()
        val response = api.pinLogin(
            PinLoginRequestDto(pin = pin, station = station.routeKey),
        )
        val session = response.toDomain()
        sessionManager.applyUserSession(session)
        session
    }.recoverCatching { throw NetworkErrorMapper.map(it) }

    override suspend fun logout() {
        sessionManager.clearUserSession()
    }
}
