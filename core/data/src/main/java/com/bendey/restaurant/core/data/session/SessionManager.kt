package com.bendey.restaurant.core.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bendey.restaurant.core.domain.model.AuthUser
import com.bendey.restaurant.core.domain.model.BranchBrief
import com.bendey.restaurant.core.domain.model.CashSessionSnapshot
import com.bendey.restaurant.core.domain.model.TenantBinding
import com.bendey.restaurant.core.domain.model.UserSession
import com.bendey.restaurant.core.domain.session.UserSessionStore
import com.bendey.restaurant.core.network.session.NetworkSessionProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "bendey_session")

@Serializable
private data class StoredTenant(
    val slug: String,
    val name: String,
    val ruc: String,
    val apiUrl: String,
    val tokenConsultaDatos: String = "",
    val boundAtEpochMs: Long = 0L,
)

@Serializable
private data class StoredUserSession(
    val token: String,
    val user: StoredUser,
    val restaurantPermissions: List<String>,
    val modules: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
    val activeBranch: StoredBranch? = null,
    val canSwitchBranch: Boolean = false,
    val allowedBranches: List<StoredBranch> = emptyList(),
)

@Serializable
private data class StoredUser(
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    val employeeType: String? = null,
    val staffId: Int? = null,
)

@Serializable
private data class StoredBranch(
    val id: Int,
    val name: String,
    val isMain: Boolean = false,
)

@Serializable
private data class StoredCashSession(
    val sessionId: Int,
    val branchId: Int,
    val openedAtEpochMs: Long,
    val openingAmount: Double,
    val label: String? = null,
    val expectedBalance: Double? = null,
)

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenManager: TokenManager,
    private val json: Json,
) : NetworkSessionProvider, UserSessionStore {

    private val keyTenant = stringPreferencesKey("tenant_binding")
    private val keyUserSession = stringPreferencesKey("user_session")
    private val keyCashSession = stringPreferencesKey("cash_session")

    override val tenantFlow: Flow<TenantBinding?> = context.sessionDataStore.data.map { prefs ->
        prefs[keyTenant]?.let { decodeTenant(it) }
    }

    override val userSessionFlow: Flow<UserSession?> = context.sessionDataStore.data.map { prefs ->
        prefs[keyUserSession]?.let { decodeUserSession(it) }
    }

    override val cashSessionFlow: Flow<CashSessionSnapshot?> = context.sessionDataStore.data.map { prefs ->
        prefs[keyCashSession]?.let { decodeCashSession(it) }
    }

    override val isTenantBoundFlow: Flow<Boolean> = tenantFlow.map { it != null && it.apiUrl.isNotBlank() }

    override val isAuthenticatedFlow: Flow<Boolean> = combine(userSessionFlow, tokenManager.tokenFlow) { session, token ->
        session != null && !token.isNullOrBlank()
    }.distinctUntilChanged()

    suspend fun getTenant(): TenantBinding? = tenantFlow.first()

    suspend fun getUserSession(): UserSession? = userSessionFlow.first()

    suspend fun bindTenant(binding: TenantBinding) {
        context.sessionDataStore.edit {
            it[keyTenant] = json.encodeToString(
                StoredTenant(
                    slug = binding.slug,
                    name = binding.name,
                    ruc = binding.ruc,
                    apiUrl = binding.apiUrl,
                    tokenConsultaDatos = binding.tokenConsultaDatos,
                    boundAtEpochMs = binding.boundAtEpochMs,
                ),
            )
        }
    }

    suspend fun clearTenant() {
        context.sessionDataStore.edit {
            it.remove(keyTenant)
        }
        clearUserSession()
    }

    suspend fun applyUserSession(session: UserSession) {
        tokenManager.setToken(session.token)
        context.sessionDataStore.edit {
            it[keyUserSession] = json.encodeToString(session.toStored())
        }
    }

    suspend fun clearUserSession() {
        tokenManager.clearToken()
        context.sessionDataStore.edit {
            it.remove(keyUserSession)
            it.remove(keyCashSession)
        }
    }

    /** Preparado para Fase 2 — caja. */
    suspend fun setCashSession(snapshot: CashSessionSnapshot?) {
        context.sessionDataStore.edit { prefs ->
            if (snapshot == null) {
                prefs.remove(keyCashSession)
            } else {
                prefs[keyCashSession] = json.encodeToString(
                    StoredCashSession(
                        sessionId = snapshot.sessionId,
                        branchId = snapshot.branchId,
                        openedAtEpochMs = snapshot.openedAtEpochMs,
                        openingAmount = snapshot.openingAmount,
                        label = snapshot.label,
                        expectedBalance = snapshot.expectedBalance,
                    ),
                )
            }
        }
    }

    override fun token(): String? = tokenManager.getTokenSync()

    override fun tenantSlug(): String? = runBlocking {
        getTenant()?.slug
    }

    override fun tenantApiBaseUrl(): String? = runBlocking {
        getTenant()?.apiUrl?.let(::normalizeApiUrl)
    }

    private fun decodeTenant(raw: String): TenantBinding? = runCatching {
        val stored = json.decodeFromString<StoredTenant>(raw)
        TenantBinding(
            slug = stored.slug,
            name = stored.name,
            ruc = stored.ruc,
            apiUrl = normalizeApiUrl(stored.apiUrl),
            tokenConsultaDatos = stored.tokenConsultaDatos,
            boundAtEpochMs = stored.boundAtEpochMs,
        )
    }.getOrNull()

    private fun decodeUserSession(raw: String): UserSession? = runCatching {
        json.decodeFromString<StoredUserSession>(raw).toDomain()
    }.getOrNull()

    private fun decodeCashSession(raw: String): CashSessionSnapshot? = runCatching {
        val s = json.decodeFromString<StoredCashSession>(raw)
        CashSessionSnapshot(
            sessionId = s.sessionId,
            branchId = s.branchId,
            openedAtEpochMs = s.openedAtEpochMs,
            openingAmount = s.openingAmount,
            label = s.label,
            expectedBalance = s.expectedBalance,
        )
    }.getOrNull()

    private fun normalizeApiUrl(url: String): String {
        var base = url.trim().trimEnd('/')
        if (base.endsWith("/api")) base = base.removeSuffix("/api")
        return base
    }
}

private fun UserSession.toStored() = StoredUserSession(
    token = token,
    user = StoredUser(
        id = user.id,
        name = user.name,
        email = user.email,
        role = user.role,
        employeeType = user.employeeType,
        staffId = user.staffId,
    ),
    restaurantPermissions = restaurantPermissions,
    modules = modules,
    permissions = permissions,
    activeBranch = activeBranch?.let { StoredBranch(it.id, it.name, it.isMain) },
    canSwitchBranch = canSwitchBranch,
    allowedBranches = allowedBranches.map { StoredBranch(it.id, it.name, it.isMain) },
)

private fun StoredUserSession.toDomain() = UserSession(
    token = token,
    user = AuthUser(
        id = user.id,
        name = user.name,
        email = user.email,
        role = user.role,
        employeeType = user.employeeType,
        staffId = user.staffId,
    ),
    restaurantPermissions = restaurantPermissions,
    modules = modules,
    permissions = permissions,
    activeBranch = activeBranch?.let { BranchBrief(it.id, it.name, it.isMain) },
    canSwitchBranch = canSwitchBranch,
    allowedBranches = allowedBranches.map { BranchBrief(it.id, it.name, it.isMain) },
)
