package com.bendey.restaurant.core.domain.onboarding.wizard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Pantalla real a la que lleva cada paso de la guía (la app lo traduce a una ruta). */
enum class CoachTarget { CAJA, MESAS, POS, COCINA }

enum class CoachStepId { CASH, TABLE, COUNTER, ADD_DISH, KITCHEN, CHARGE }

data class CoachStep(
    val id: CoachStepId,
    val title: String,
    val body: String,
    val target: CoachTarget,
)

/**
 * Los pasos de W3 (guía de la primera venta). Con "En mesas" la venta parte de una mesa; sin él, del
 * mostrador (POS). Los pasos NO bloquean nada: son un panel flotante descartable sobre las pantallas
 * reales.
 */
fun coachSteps(usesTables: Boolean): List<CoachStep> = listOf(
    CoachStep(CoachStepId.CASH, WizardCopy.W3_STEP_CASH, WizardCopy.W3_STEP_CASH_BODY, CoachTarget.CAJA),
    if (usesTables) {
        CoachStep(CoachStepId.TABLE, WizardCopy.W3_STEP_TABLE, "", CoachTarget.MESAS)
    } else {
        CoachStep(CoachStepId.COUNTER, WizardCopy.W3_STEP_COUNTER, "", CoachTarget.POS)
    },
    CoachStep(
        CoachStepId.ADD_DISH,
        WizardCopy.W3_STEP_ADD,
        "",
        if (usesTables) CoachTarget.MESAS else CoachTarget.POS,
    ),
    CoachStep(CoachStepId.KITCHEN, WizardCopy.W3_STEP_KITCHEN, WizardCopy.W3_STEP_KITCHEN_BODY, CoachTarget.COCINA),
    CoachStep(
        CoachStepId.CHARGE,
        WizardCopy.W3_STEP_CHARGE,
        "",
        if (usesTables) CoachTarget.MESAS else CoachTarget.POS,
    ),
)

data class CoachState(
    val active: Boolean = false,
    val steps: List<CoachStep> = emptyList(),
    val index: Int = 0,
    val minimized: Boolean = false,
) {
    val current: CoachStep? get() = steps.getOrNull(index)
    val isLast: Boolean get() = index >= steps.lastIndex
}

/**
 * Estado en memoria de la guía W3 (como `ConfigTabHint`): vive mientras la app está abierta. Si se
 * cierra, el administrador la retoma desde "Continuar configuración".
 */
object WizardCoach {
    private val _state = MutableStateFlow(CoachState())
    val state: StateFlow<CoachState> = _state.asStateFlow()

    fun start(usesTables: Boolean) {
        _state.value = CoachState(active = true, steps = coachSteps(usesTables))
    }

    fun next() {
        _state.value.let { s ->
            if (!s.active) return
            _state.value = if (s.isLast) CoachState() else s.copy(index = s.index + 1)
        }
    }

    fun previous() {
        _state.value.let { s -> if (s.active && s.index > 0) _state.value = s.copy(index = s.index - 1) }
    }

    fun setMinimized(minimized: Boolean) {
        _state.value = _state.value.copy(minimized = minimized)
    }

    fun close() {
        _state.value = CoachState()
    }
}
