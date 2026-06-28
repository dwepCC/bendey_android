package com.bendey.restaurant.core.data.cache

import com.bendey.restaurant.core.domain.billing.BillingRepository
import com.bendey.restaurant.core.domain.catalog.SettingsRepository
import com.bendey.restaurant.core.domain.restaurant.PosRepository
import com.bendey.restaurant.core.domain.session.UserSessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Precarga series/contactos/pagos y categorías al iniciar sesión (como React al montar POS). */
@Singleton
class OperationalDataPreloader @Inject constructor(
    private val billingRepository: BillingRepository,
    private val posRepository: PosRepository,
    private val settingsRepository: SettingsRepository,
    private val sessionStore: UserSessionStore,
    private val cache: OperationalDataCache,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            sessionStore.userSessionFlow
                .map { it?.activeBranch?.id }
                .distinctUntilChanged()
                .collect { branchId ->
                    if (branchId == null) {
                        cache.clearAll()
                    } else {
                        preload(branchId)
                    }
                }
        }
    }

    fun preloadActiveBranch() {
        scope.launch {
            val branchId = sessionStore.userSessionFlow.first()?.activeBranch?.id ?: return@launch
            preload(branchId)
        }
    }

    private suspend fun preload(branchId: Int) {
        if (cache.getCheckoutMeta(branchId) == null) {
            billingRepository.loadCheckoutMeta(branchId)
        }
        if (cache.getCategories().isNullOrEmpty()) {
            posRepository.loadCategories()
        }
        settingsRepository.preloadTenantSettings()
    }
}
