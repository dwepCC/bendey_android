package com.bendey.restaurant.core.domain.onboarding

import com.bendey.restaurant.core.domain.onboarding.wizard.WizardCopy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * R6 — Primera venta: lógica PURA (sin Android, sin red). Réplica 1:1 de `firstSaleNext.ts` (Tauri);
 * la tabla de casos de `FirstSaleNextTest` es el contrato compartido: si cambias una regla, cambia
 * la tabla en los dos clientes.
 */
enum class NextActionId { PRINTER, TABLE_SALE, QR_MENU, SUNAT, TEAM, AUTH_PIN }

data class NextRecommendation(
    /** La primera acción que aplica según la prioridad. */
    val primary: NextActionId,
    /** Hasta 2 acciones más (en orden de prioridad). `AUTH_PIN`, si está pendiente, siempre entra. */
    val secondary: List<NextActionId> = emptyList(),
)

const val MAX_SECONDARY = 2

/** Pendiente = el servidor conoce el paso, no está hecho y no fue omitido. Paso ausente = no pendiente. */
private fun OnboardingState.pending(key: OnboardingStepKey): Boolean =
    steps.firstOrNull { it.key == key.apiKey }?.let { !it.done && !it.skipped } == true

/**
 * Prioridad: impresora (local) → QR de mesa (o, si es solo mostrador, "venta con mesa") → SUNAT →
 * equipo. `AUTH_PIN` pendiente se ofrece SIEMPRE: principal si nada más aplica, si no secundaria
 * (desplaza a la última secundaria si hubiera más de dos candidatas). Sin candidatas o sin estado
 * (API caída) → null.
 *
 * @param localPrinterConfigured ¿ESTE equipo tiene impresora? Estado LOCAL, no del servidor.
 * @param counterOnly negocio solo de mostrador (el wizard local no incluye atención en mesas).
 */
fun recommendNext(
    state: OnboardingState?,
    localPrinterConfigured: Boolean,
    counterOnly: Boolean,
): NextRecommendation? {
    if (state == null) return null

    val list = mutableListOf<NextActionId>()
    if (!localPrinterConfigured) list += NextActionId.PRINTER
    if (counterOnly) list += NextActionId.TABLE_SALE
    else if (state.pending(OnboardingStepKey.QR_MENU)) list += NextActionId.QR_MENU
    if (state.pending(OnboardingStepKey.SUNAT)) list += NextActionId.SUNAT
    if (state.pending(OnboardingStepKey.TEAM)) list += NextActionId.TEAM
    val authPin = state.pending(OnboardingStepKey.AUTH_PIN)
    if (authPin) list += NextActionId.AUTH_PIN

    if (list.isEmpty()) return null
    val primary = list.first()
    var secondary = list.drop(1).take(MAX_SECONDARY)
    if (authPin && primary != NextActionId.AUTH_PIN && NextActionId.AUTH_PIN !in secondary) {
        secondary = secondary.take(MAX_SECONDARY - 1) + NextActionId.AUTH_PIN
    }
    return NextRecommendation(primary, secondary)
}

/** Banda en el recibo: solo con el flag explícito del flujo de cobro (null = reimpresión) Y recibo abierto. */
fun shouldShowFirstSaleBand(firstSale: Boolean?, open: Boolean): Boolean = open && firstSale == true

data class NextActionCopy(val title: String, val action: String)

fun NextActionId.copy(): NextActionCopy = when (this) {
    NextActionId.PRINTER -> NextActionCopy(WizardCopy.NEXT_PRINTER_TITLE, WizardCopy.NEXT_PRINTER_ACTION)
    NextActionId.TABLE_SALE -> NextActionCopy(WizardCopy.NEXT_TABLE_TITLE, WizardCopy.NEXT_TABLE_ACTION)
    NextActionId.QR_MENU -> NextActionCopy(WizardCopy.NEXT_QR_TITLE, WizardCopy.NEXT_QR_ACTION)
    NextActionId.SUNAT -> NextActionCopy(WizardCopy.NEXT_SUNAT_TITLE, WizardCopy.NEXT_SUNAT_ACTION)
    NextActionId.TEAM -> NextActionCopy(WizardCopy.NEXT_TEAM_TITLE, WizardCopy.NEXT_TEAM_ACTION)
    NextActionId.AUTH_PIN -> NextActionCopy(WizardCopy.NEXT_AUTH_PIN_TITLE, WizardCopy.NEXT_AUTH_PIN_ACTION)
}

/**
 * Puente en memoria (NO persistente: el servidor ya garantiza "una vez" por restaurante) entre el
 * recibo de un cobro NUEVO con `first_sale` y la hoja "Lo que sigue", que vive a nivel de la app
 * porque tras cobrar en una mesa la pantalla se cierra. Mismo patrón que `WizardCoach`.
 */
object FirstSaleMoment {
    data class State(
        /** Identifica el cobro (número de venta); null = no hay momento en curso. */
        val token: String? = null,
        /** El recibo ya se cerró: ahora sí corresponde mostrar la hoja. */
        val receiptClosed: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private var handledToken: String? = null

    /** El recibo de un cobro nuevo con `first_sale` quedó en pantalla. Idempotente por [token]. */
    fun onReceiptShown(token: String) {
        if (token == handledToken) return
        handledToken = token
        _state.value = State(token = token)
    }

    fun onReceiptClosed() {
        _state.value = _state.value.let { if (it.token != null) it.copy(receiptClosed = true) else it }
    }

    /** La hoja se descartó (o no había nada que mostrar): el momento termina. */
    fun finish() {
        _state.value = State()
    }
}
