package com.bendey.restaurant.feature.repartidores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.delivery.DeliveryEarningsRepository
import com.bendey.restaurant.core.domain.delivery.PendingEarning
import com.bendey.restaurant.core.domain.delivery.canPayDriverEarnings
import com.bendey.restaurant.core.domain.session.UserSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Pegamento Hilt de la pestaña Ganancias; la lógica está en [GananciasPresenter]. */
@HiltViewModel
class GananciasViewModel @Inject constructor(
    repository: DeliveryEarningsRepository,
    sessionStore: UserSessionStore,
) : ViewModel() {

    private val presenter = GananciasPresenter(viewModelScope, repository)

    val state: StateFlow<GananciasUiState> = presenter.state
    val messages: SharedFlow<String> = presenter.messages

    init {
        viewModelScope.launch {
            sessionStore.userSessionFlow.collect {
                presenter.setCanPay(canPayDriverEarnings(it?.restaurantPermissions.orEmpty(), it?.user?.employeeType))
            }
        }
    }

    fun refresh() = presenter.refresh()
    fun askPay(item: PendingEarning) = presenter.askPay(item)
    fun dismissPay() = presenter.dismissPay()
    fun confirmPay() = presenter.confirmPay()
}
