package com.bendey.restaurant.feature.repartidores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.catalog.DeliveryRepository
import com.bendey.restaurant.core.domain.delivery.DeliveryCard
import com.bendey.restaurant.core.domain.delivery.DeliverySection
import com.bendey.restaurant.core.domain.session.UserSessionStore
import com.bendey.restaurant.core.realtime.delivery.DeliveryBoardState
import com.bendey.restaurant.core.realtime.delivery.DeliveryBoardStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pegamento Hilt de la vista Delivery: el tablero es de [DeliveryBoardStore] (el mismo que alimenta el badge de
 * la barra); la lógica de las acciones, de [DeliveryPresenter].
 */
@HiltViewModel
class DeliveryViewModel @Inject constructor(
    store: DeliveryBoardStore,
    repository: DeliveryRepository,
    sessionStore: UserSessionStore,
) : ViewModel() {

    private val presenter = DeliveryPresenter(viewModelScope, store, repository)

    val board: StateFlow<DeliveryBoardState> = store.state
    val ui: StateFlow<DeliveryUiState> = presenter.state
    val messages: SharedFlow<String> = presenter.messages

    init {
        viewModelScope.launch {
            sessionStore.userSessionFlow.collect { presenter.setPermissions(it?.restaurantPermissions.orEmpty(), it?.user?.employeeType) }
        }
        // Entrar a la vista siempre pide el tablero fresco (coalescido con cualquier consulta en curso).
        presenter.refresh()
    }

    fun refresh() = presenter.refresh()
    fun selectSection(section: DeliverySection) = presenter.selectSection(section)
    fun selectDrivers() = presenter.selectDrivers()
    fun openAssign(card: DeliveryCard) = presenter.openAssign(card)
    fun openCancel(card: DeliveryCard) = presenter.openCancel(card)
    fun openFailed(card: DeliveryCard) = presenter.openFailed(card)
    fun openDelivered(card: DeliveryCard) = presenter.openDelivered(card)
    fun dismissDialog() = presenter.dismissDialog()
    fun chooseReason(reason: String?) = presenter.chooseReason(reason)
    fun setReasonText(text: String) = presenter.setReasonText(text)
    fun assign(driverId: Int) = presenter.assign(driverId)
    fun confirmCancel() = presenter.confirmCancel()
    fun confirmFailed() = presenter.confirmFailed()
    fun confirmDelivered() = presenter.confirmDelivered()
}
