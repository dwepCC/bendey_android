package com.bendey.restaurant.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.onboarding.OnboardingRepository
import com.bendey.restaurant.core.domain.onboarding.canSeeOnboarding
import com.bendey.restaurant.core.domain.onboarding.wizard.WizardPreferences
import com.bendey.restaurant.core.domain.onboarding.wizard.shouldAutoShowWizard
import com.bendey.restaurant.core.domain.session.UserSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Decide si el wizard se abre solo tras el login (una vez por sesión) y solo para el administrador de
 * sesión completa de un restaurante NUEVO: sin carta, sin ventas, checklist visible y wizard no
 * cerrado en este equipo. Quien ya tiene carta o ventas no ve nada. Si el servidor no responde, no se
 * abre nada (el wizard sigue disponible desde el checklist).
 */
@HiltViewModel
class WizardGateViewModel @Inject constructor(
    sessionStore: UserSessionStore,
    private val onboardingRepository: OnboardingRepository,
    private val preferences: WizardPreferences,
) : ViewModel() {

    private val _showWizard = MutableStateFlow(false)
    val showWizard: StateFlow<Boolean> = _showWizard.asStateFlow()

    private var evaluatedToken: String? = null

    init {
        viewModelScope.launch {
            sessionStore.userSessionFlow
                .map { session -> session?.takeIf { it.canSeeOnboarding() }?.token }
                .distinctUntilChanged()
                .collect { token ->
                    if (token != null && token != evaluatedToken) {
                        evaluatedToken = token
                        evaluate()
                    }
                }
        }
    }

    private suspend fun evaluate() {
        val result = onboardingRepository.getState() as? AppResult.Success ?: return
        val local = preferences.state.first()
        if (shouldAutoShowWizard(result.data, local)) _showWizard.value = true
    }

    /** El shell ya navegó al wizard. */
    fun consume() {
        _showWizard.value = false
    }
}
