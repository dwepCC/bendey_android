package com.bendey.restaurant.feature.caja.reparto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.rcshare.RcShareRepository
import com.bendey.restaurant.core.domain.rcshare.RcSharePresenter
import com.bendey.restaurant.core.domain.rcshare.RcShareUiState
import com.bendey.restaurant.core.domain.rcshare.RcPeriodPreset
import com.bendey.restaurant.core.domain.rcshare.RcStaff
import com.bendey.restaurant.core.domain.rcshare.canDistributeRc
import com.bendey.restaurant.core.domain.restaurant.MesasRepository
import com.bendey.restaurant.core.domain.session.UserSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Pegamento Hilt del reparto del recargo al consumo; la lógica está en [RcSharePresenter]. */
@HiltViewModel
class RcShareViewModel @Inject constructor(
    repository: RcShareRepository,
    mesasRepository: MesasRepository,
    private val sessionStore: UserSessionStore,
) : ViewModel() {
    private val presenter = RcSharePresenter(
        scope = viewModelScope,
        repository = repository,
        loadStaff = {
            when (val r = mesasRepository.loadStaff()) {
                is AppResult.Success -> AppResult.Success(r.data.map { RcStaff(it.id, it.displayName) })
                is AppResult.Error -> r
                AppResult.Loading -> AppResult.Loading
            }
        },
    )
    val state: StateFlow<RcShareUiState> = presenter.state
    private var started = false

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            val perms = sessionStore.userSessionFlow.first()?.restaurantPermissions
            presenter.start(canDistributeRc(perms))
        }
    }

    fun refresh() = presenter.refresh()
    fun selectPreset(p: RcPeriodPreset) = presenter.selectPreset(p)
    fun setFrom(iso: String) = presenter.setFrom(iso)
    fun setTo(iso: String) = presenter.setTo(iso)
    fun applyCustom() = presenter.applyCustom()
    fun openDistribute() = presenter.openDistribute()
    fun closeDistribute() = presenter.closeDistribute()
    fun setAmount(staffId: Int, text: String) = presenter.setAmount(staffId, text)
    fun splitRemainder() = presenter.splitRemainder()
    fun setNotes(text: String) = presenter.setNotes(text)
    fun setPayFromCash(v: Boolean) = presenter.setPayFromCash(v)
    fun submit() = presenter.submit()
    fun consumeSnack() = presenter.consumeSnack()
}
