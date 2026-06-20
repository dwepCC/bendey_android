package com.bendey.restaurant.core.domain.session

import com.bendey.restaurant.core.domain.model.CashSessionSnapshot
import com.bendey.restaurant.core.domain.model.TenantBinding
import com.bendey.restaurant.core.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

/** Lectura de sesión para capa de presentación — sin tipos de red. */
interface UserSessionStore {
    val tenantFlow: Flow<TenantBinding?>
    val userSessionFlow: Flow<UserSession?>
    val cashSessionFlow: Flow<CashSessionSnapshot?>
    val isTenantBoundFlow: Flow<Boolean>
    val isAuthenticatedFlow: Flow<Boolean>
}
