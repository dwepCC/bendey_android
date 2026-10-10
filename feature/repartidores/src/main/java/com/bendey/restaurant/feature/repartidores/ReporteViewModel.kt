package com.bendey.restaurant.feature.repartidores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.delivery.DeliveryReportRepository
import com.bendey.restaurant.core.domain.delivery.ReportPreset
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** Pegamento Hilt del reporte de delivery (D5); la lógica está en [ReportePresenter]. */
@HiltViewModel
class ReporteViewModel @Inject constructor(repository: DeliveryReportRepository) : ViewModel() {
    private val presenter = ReportePresenter(viewModelScope, repository)
    val state: StateFlow<ReporteUiState> = presenter.state

    fun start() = presenter.start()
    fun refresh() = presenter.refresh()
    fun selectPreset(preset: ReportPreset) = presenter.selectPreset(preset)
    fun setFrom(iso: String) = presenter.setFrom(iso)
    fun setTo(iso: String) = presenter.setTo(iso)
    fun applyCustom() = presenter.applyCustom()
}
