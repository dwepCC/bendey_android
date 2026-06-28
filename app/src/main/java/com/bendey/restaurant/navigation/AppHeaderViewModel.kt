package com.bendey.restaurant.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.catalog.SettingsRepository
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.session.UserSessionStore
import com.bendey.restaurant.core.ui.components.BendeyAppHeaderState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Paridad con `resolveHeaderDisplayName` en RestaurantHeader.tsx */
private fun resolveHeaderDisplayName(tradeName: String, businessName: String, tenantName: String): String {
    tradeName.trim().takeIf { it.isNotEmpty() }?.let { return it }
    businessName.trim().takeIf { it.isNotEmpty() }?.let { return it }
    return tenantName.trim().ifBlank { "Restaurante" }
}

@HiltViewModel
class AppHeaderViewModel @Inject constructor(
    sessionStore: UserSessionStore,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val companyTradeName = MutableStateFlow("")
    private val companyBusinessName = MutableStateFlow("")

    init {
        viewModelScope.launch {
            sessionStore.userSessionFlow.collect { session ->
                if (session == null) {
                    companyTradeName.value = ""
                    companyBusinessName.value = ""
                    return@collect
                }
                when (val result = settingsRepository.getCompanyConfig()) {
                    is AppResult.Success -> {
                        companyTradeName.value = result.data.tradeName
                        companyBusinessName.value = result.data.businessName
                    }
                    else -> {
                        companyTradeName.value = ""
                        companyBusinessName.value = ""
                    }
                }
            }
        }
    }

    val headerState: StateFlow<BendeyAppHeaderState> = combine(
        sessionStore.tenantFlow,
        sessionStore.userSessionFlow,
        companyTradeName,
        companyBusinessName,
    ) { tenant, session, tradeName, businessName ->
        val user = session?.user
        val name = user?.name.orEmpty()
        val initials = name.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "?" }
        BendeyAppHeaderState(
            restaurantName = resolveHeaderDisplayName(
                tradeName = tradeName,
                businessName = businessName,
                tenantName = tenant?.name.orEmpty(),
            ),
            branchName = session?.activeBranch?.name.orEmpty(),
            userName = name,
            userInitials = initials,
            isOnline = true,
            notificationCount = 0,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BendeyAppHeaderState())
}
