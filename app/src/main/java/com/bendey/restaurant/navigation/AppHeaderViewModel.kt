package com.bendey.restaurant.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.session.UserSessionStore
import com.bendey.restaurant.core.ui.components.BendeyAppHeaderState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AppHeaderViewModel @Inject constructor(
    sessionStore: UserSessionStore,
) : ViewModel() {

    val headerState: StateFlow<BendeyAppHeaderState> = combine(
        sessionStore.tenantFlow,
        sessionStore.userSessionFlow,
        sessionStore.cashSessionFlow,
    ) { tenant, session, cash ->
        val user = session?.user
        val name = user?.name.orEmpty()
        val initials = name.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "?" }
        val currency = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
        BendeyAppHeaderState(
            restaurantName = tenant?.name ?: tenant?.slug.orEmpty(),
            branchName = session?.activeBranch?.name.orEmpty(),
            userName = name,
            userInitials = initials,
            isCashOpen = cash != null,
            cashLabel = cash?.let {
                val balance = it.expectedBalance ?: it.openingAmount
                "Caja ${currency.format(balance)}"
            },
            isOnline = true,
            notificationCount = 0,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BendeyAppHeaderState())
}
