package com.bendey.restaurant.core.domain.onboarding.wizard

import com.bendey.restaurant.core.domain.onboarding.OnboardingState
import com.bendey.restaurant.core.domain.onboarding.OnboardingStepKey

/** Pasos del wizard (el contador "N de 3" cuenta W1, W2 y W3). */
enum class WizardStep { WELCOME, RESTAURANT, MENU, FIRST_SALE }

/** Lo único que el wizard guarda en el equipo (el servidor manda en lo demás). */
data class WizardLocalState(
    /** El administrador lo cerró ("Explorar primero" / "Más tarde") o lo terminó. */
    val closed: Boolean = false,
    val serviceModes: Set<ServiceMode> = setOf(ServiceMode.DINE_IN),
    val step: WizardStep = WizardStep.WELCOME,
) {
    /** Sin "En mesas" la primera venta se hace en mostrador (POS). */
    val usesTables: Boolean get() = ServiceMode.DINE_IN in serviceModes
}

private fun OnboardingState.step(key: OnboardingStepKey) = steps.firstOrNull { it.key == key.apiKey }

/** El paso "Tu carta" ya está hecho (la carta de ejemplo cuenta, como en el checklist). */
fun OnboardingState.menuDone(): Boolean = step(OnboardingStepKey.MENU)?.done == true

/** Hay al menos una venta efectiva (o el servidor ya sabe que vio la primera venta). */
fun OnboardingState.hasSales(): Boolean =
    step(OnboardingStepKey.FIRST_SALE)?.done == true || firstSaleSeen

/**
 * ¿Se abre el wizard SOLO tras el primer login? Solo si el tenant es nuevo: carta pendiente, sin
 * ventas, checklist no ocultado, wizard no cerrado en este equipo ni completado en el servidor.
 * Quien ya tiene carta o ventas no ve nada nuevo. Si el servidor no trae el paso `menu` (backend
 * viejo), no se muestra.
 */
fun shouldAutoShowWizard(state: OnboardingState, local: WizardLocalState): Boolean {
    if (local.closed) return false
    if (state.dismissed) return false
    if (state.completedAt != null) return false
    val menu = state.step(OnboardingStepKey.MENU) ?: return false
    if (menu.done) return false
    if (state.hasSales()) return false
    return true
}

/** Progreso "N de 3" para el encabezado del wizard, derivado de datos del servidor. */
fun wizardProgressDone(state: OnboardingState?, local: WizardLocalState): Int {
    var done = 0
    // W1 se da por hecho cuando ya eligió tipo de local o pasó de pantalla.
    if (local.step.ordinal > WizardStep.RESTAURANT.ordinal || !state?.businessSubtype.isNullOrBlank()) done++
    if (state?.menuDone() == true) done++
    if (state?.hasSales() == true) done++
    return done
}
