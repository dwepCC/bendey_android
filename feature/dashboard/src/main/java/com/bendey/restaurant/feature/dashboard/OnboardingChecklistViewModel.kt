package com.bendey.restaurant.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.data.printer.PrinterPreferencesStore
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.onboarding.OnboardingChecklist
import com.bendey.restaurant.core.domain.onboarding.OnboardingPreferencesUpdate
import com.bendey.restaurant.core.domain.onboarding.OnboardingRefreshPolicy
import com.bendey.restaurant.core.domain.onboarding.OnboardingRepository
import com.bendey.restaurant.core.domain.onboarding.OnboardingState
import com.bendey.restaurant.core.domain.onboarding.OnboardingStepKey
import com.bendey.restaurant.core.domain.onboarding.OnboardingUnavailableException
import com.bendey.restaurant.core.domain.onboarding.SunatRequestStatus
import com.bendey.restaurant.core.domain.onboarding.buildOnboardingChecklist
import com.bendey.restaurant.core.domain.onboarding.canSeeOnboarding
import com.bendey.restaurant.core.domain.session.UserSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Confirmación pendiente (los diálogos los pinta la UI). */
enum class OnboardingConfirm { SAMPLE_MENU, SUNAT_REQUEST }

data class OnboardingFeedback(val message: String, val isError: Boolean)

data class OnboardingCardUiState(
    /** Primera carga en curso y todavía sin datos: se pinta un esqueleto. */
    val loading: Boolean = false,
    /** Falló la primera carga y no hay nada que mostrar: aviso discreto con "Reintentar". */
    val error: String? = null,
    /** Null = no hay tarjeta (no eres administrador, ocultada, todo completo o no disponible). */
    val checklist: OnboardingChecklist? = null,
    /** Hay una acción en curso (omitir, ocultar, cargar ejemplo…): se bloquean los botones. */
    val busy: Boolean = false,
    val feedback: OnboardingFeedback? = null,
    val confirm: OnboardingConfirm? = null,
) {
    val visible: Boolean get() = loading || error != null || checklist != null
}

/**
 * Checklist de activación del Dashboard. Es OPCIONAL: ningún fallo de aquí puede romper el
 * Dashboard. Solo se activa para el administrador de sesión completa; caché ~60 s y relectura al
 * volver a primer plano (sin polling).
 */
@HiltViewModel
class OnboardingChecklistViewModel @Inject constructor(
    private val repository: OnboardingRepository,
    sessionStore: UserSessionStore,
    printerStore: PrinterPreferencesStore,
) : ViewModel() {

    private data class Raw(
        val canSee: Boolean = false,
        val state: OnboardingState? = null,
        val sunat: SunatRequestStatus? = null,
        val unavailable: Boolean = false,
        val loading: Boolean = false,
        val error: String? = null,
        val busy: Boolean = false,
        val feedback: OnboardingFeedback? = null,
        val confirm: OnboardingConfirm? = null,
    )

    private val raw = MutableStateFlow(Raw())
    private var lastLoadedAtMs: Long? = null
    private var loadJob: Job? = null

    val uiState: StateFlow<OnboardingCardUiState> = combine(raw, printerStore.hasConfiguredPrinter) { r, printer ->
        if (!r.canSee || r.unavailable) {
            OnboardingCardUiState()
        } else {
            OnboardingCardUiState(
                loading = r.loading && r.state == null,
                error = r.error.takeIf { r.state == null },
                checklist = r.state?.let { buildOnboardingChecklist(it, printer, r.sunat) },
                busy = r.busy,
                feedback = r.feedback,
                confirm = r.confirm,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OnboardingCardUiState())

    init {
        viewModelScope.launch {
            sessionStore.userSessionFlow
                .map { it.canSeeOnboarding() }
                .distinctUntilChanged()
                .collect { canSee ->
                    loadJob?.cancel()
                    lastLoadedAtMs = null
                    raw.value = Raw(canSee = canSee)
                    if (canSee) refresh(force = true)
                }
        }
    }

    /** Al volver a primer plano o al tirar para refrescar. Respeta la caché salvo que se fuerce. */
    fun refresh(force: Boolean = false) {
        if (!raw.value.canSee) return
        if (loadJob?.isActive == true) return
        if (!OnboardingRefreshPolicy.shouldRefresh(lastLoadedAtMs, System.currentTimeMillis(), force)) return
        loadJob = viewModelScope.launch { load() }
    }

    private suspend fun load() {
        raw.update { it.copy(loading = it.state == null, error = null) }
        when (val result = repository.getState()) {
            is AppResult.Success -> {
                lastLoadedAtMs = System.currentTimeMillis()
                raw.update { it.copy(state = result.data, loading = false, error = null, unavailable = false) }
                loadSunatStatusIfNeeded(result.data)
            }
            is AppResult.Error -> {
                if (result.cause is OnboardingUnavailableException) {
                    // 403/404: no es un error que mostrar ni que reintentar a cada resume.
                    lastLoadedAtMs = System.currentTimeMillis()
                    raw.update { it.copy(state = null, loading = false, error = null, unavailable = true) }
                } else {
                    // Con datos previos se conservan (mejor viejos que nada); sin ellos, aviso con "Reintentar".
                    raw.update { it.copy(loading = false, error = result.message) }
                }
            }
            AppResult.Loading -> Unit
        }
    }

    /** Solo si hay un paso de SUNAT visible y sin resolver; si falla, se ofrece el botón. */
    private suspend fun loadSunatStatusIfNeeded(state: OnboardingState) {
        val sunat = state.steps.firstOrNull { it.key == OnboardingStepKey.SUNAT.apiKey } ?: return
        if (sunat.done || sunat.skipped) return
        val result = repository.getSunatStatus()
        if (result is AppResult.Success) raw.update { it.copy(sunat = result.data) }
    }

    fun retry() = refresh(force = true)

    fun skip(key: OnboardingStepKey) = updatePreferences(OnboardingPreferencesUpdate(skip = listOf(key.apiKey)))

    /** "Ocultar": se puede recuperar desde Configuración → "Mostrar primeros pasos". */
    fun dismiss() = updatePreferences(OnboardingPreferencesUpdate(dismissed = true))

    private fun updatePreferences(update: OnboardingPreferencesUpdate) {
        if (raw.value.busy) return
        viewModelScope.launch {
            raw.update { it.copy(busy = true, feedback = null) }
            when (val result = repository.updatePreferences(update)) {
                is AppResult.Success -> {
                    lastLoadedAtMs = System.currentTimeMillis()
                    raw.update { it.copy(state = result.data, busy = false) }
                }
                is AppResult.Error -> raw.update {
                    it.copy(busy = false, feedback = OnboardingFeedback(result.message, isError = true))
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun askSampleMenu() = raw.update { it.copy(confirm = OnboardingConfirm.SAMPLE_MENU, feedback = null) }

    fun askSunatRequest() = raw.update { it.copy(confirm = OnboardingConfirm.SUNAT_REQUEST, feedback = null) }

    fun dismissConfirm() = raw.update { it.copy(confirm = null) }

    fun dismissFeedback() = raw.update { it.copy(feedback = null) }

    fun confirm() {
        val pending = raw.value.confirm ?: return
        if (raw.value.busy) return
        raw.update { it.copy(confirm = null, busy = true, feedback = null) }
        viewModelScope.launch {
            when (pending) {
                OnboardingConfirm.SAMPLE_MENU -> doLoadSampleMenu()
                OnboardingConfirm.SUNAT_REQUEST -> doRequestSunat()
            }
        }
    }

    private suspend fun doLoadSampleMenu() {
        when (val result = repository.loadSampleMenu()) {
            is AppResult.Success -> {
                val n = result.data.created
                finishAction(
                    OnboardingFeedback(
                        message = (if (n == 1) "Cargamos 1 plato de ejemplo." else "Cargamos $n platos de ejemplo.") +
                            " Edítalos o bórralos cuando quieras.",
                        isError = false,
                    ),
                    refreshAfter = true,
                )
            }
            is AppResult.Error -> finishAction(OnboardingFeedback(result.message, isError = true), refreshAfter = false)
            AppResult.Loading -> Unit
        }
    }

    private suspend fun doRequestSunat() {
        when (val result = repository.requestSunatActivation()) {
            is AppResult.Success -> {
                raw.update { it.copy(sunat = result.data) }
                finishAction(
                    OnboardingFeedback("Recibimos tu solicitud. Te avisamos cuando esté lista.", isError = false),
                    refreshAfter = true,
                )
            }
            is AppResult.Error -> finishAction(OnboardingFeedback(result.message, isError = true), refreshAfter = false)
            AppResult.Loading -> Unit
        }
    }

    private fun finishAction(feedback: OnboardingFeedback, refreshAfter: Boolean) {
        raw.update { it.copy(busy = false, feedback = feedback) }
        if (refreshAfter) refresh(force = true)
    }
}
