package com.bendey.restaurant.core.domain.onboarding

/**
 * Lógica pura del checklist de activación (sin Android): combina los pasos del servidor con el
 * paso LOCAL de impresora y decide qué se muestra, en qué orden y con qué acción.
 *
 * Reglas (ONBOARDING §5):
 *  - Tres secciones: PARA VENDER (required), PARA OPERAR (recommended) y PARA CRECER (advanced).
 *  - Los omitidos desaparecen. Los avanzados NO cuentan para el porcentaje.
 *  - El avance es siempre "Para operar: X de Y", calculado en el cliente: Y = recomendados no
 *    omitidos (incluida la impresora local), X = los hechos. Con `sell_ready` (o carta y primera
 *    venta hechas) el título pasa a "Ya puedes vender 🎉".
 *  - Si no queda nada pendiente en vender/operar, o el usuario lo ocultó, no hay tarjeta (null).
 */

/** A dónde lleva el botón de un ítem. La app traduce esto a una ruta real; aquí no hay navegación. */
enum class OnboardingDestination {
    PRODUCTOS,
    POS,
    MESAS_ADMIN,
    IMPRESORAS,
    CONFIG_OPERACION,
    CONFIG_MENU_DIGITAL,
    CONFIG_SUCURSALES,
    REPARTIDORES,

    /** Retomar el wizard de configuración (R4). */
    WIZARD,
}

sealed interface OnboardingAction {
    data class Navigate(val destination: OnboardingDestination) : OnboardingAction

    /** Pide activar la facturación electrónica (requiere confirmar antes de llamar al servidor). */
    data object RequestSunat : OnboardingAction
}

enum class OnboardingSectionKind(val title: String) {
    SELL("PARA VENDER"),
    OPERATE("PARA OPERAR"),
    GROW("PARA CRECER (cuando quieras)"),
}

data class OnboardingItem(
    val key: OnboardingStepKey,
    val tier: OnboardingTier,
    val title: String,
    val description: String,
    val done: Boolean,
    /** Es una configuración de ESTE equipo (impresora): se marca con "este equipo". */
    val thisDevice: Boolean,
    /** Verbo del botón principal; null cuando ya está hecho o no hay nada que pulsar. */
    val primaryLabel: String?,
    val primaryAction: OnboardingAction?,
    /** Ofrece "Usar carta de ejemplo" (solo en el paso de la carta, mientras no hay carta). */
    val offersSampleMenu: Boolean,
    val canSkip: Boolean,
    /** Estado en texto cuando no hay botón (SUNAT: "Solicitada el …", "En revisión", "Activa"). */
    val statusText: String?,
)

data class OnboardingSection(
    val kind: OnboardingSectionKind,
    val items: List<OnboardingItem>,
) {
    val title: String get() = kind.title
}

data class OnboardingChecklist(
    val title: String,
    val progressLabel: String,
    val progressDone: Int,
    val progressTotal: Int,
    val percent: Int,
    val sellReady: Boolean,
    val sections: List<OnboardingSection>,
)

object OnboardingCopy {
    const val TITLE_DEFAULT = "🚀 Tu restaurante"
    const val TITLE_SELL_READY = "Ya puedes vender 🎉"
    const val SAMPLE_MENU_CTA = "Usar carta de ejemplo"
}

/**
 * @param localPrinterConfigured si ESTE equipo tiene una impresora configurada (el servidor no lo sabe).
 * @param sunat estado de la solicitud de SUNAT; null = no se pudo saber (se ofrece el botón).
 * @return null cuando no hay que mostrar la tarjeta.
 */
fun buildOnboardingChecklist(
    state: OnboardingState,
    localPrinterConfigured: Boolean,
    sunat: SunatRequestStatus? = null,
): OnboardingChecklist? {
    if (state.dismissed) return null

    val items = state.steps
        .mapNotNull { step -> OnboardingStepKey.fromApi(step.key)?.let { it to step } }
        .distinctBy { (key, _) -> key }
        .sortedBy { (key, _) -> key.ordinal }
        .filter { (_, step) -> !step.skipped }
        .map { (key, step) -> toItem(key, step, localPrinterConfigured, sunat) }
    if (items.isEmpty()) return null

    val sell = items.filter { it.tier == OnboardingTier.REQUIRED }
    val operate = items.filter { it.tier == OnboardingTier.RECOMMENDED }
    val grow = items.filter { it.tier == OnboardingTier.ADVANCED }

    // "Todo completo": nada pendiente en vender ni operar (los omitidos ya no están en la lista).
    if ((sell + operate).all { it.done }) return null

    // sell_ready del servidor, o menu y first_sale hechos. El avance es SIEMPRE "Para operar" y se
    // calcula aquí (misma regla que Tauri): se ignora progress.operate_* y percent.
    val sellReady = state.progress.sellReady ||
        (sell.isNotEmpty() && sell.all { it.done })
    val done = operate.count { it.done }
    val total = operate.size

    return OnboardingChecklist(
        title = if (sellReady) OnboardingCopy.TITLE_SELL_READY else OnboardingCopy.TITLE_DEFAULT,
        progressLabel = "Para operar: $done de $total",
        progressDone = done,
        progressTotal = total,
        percent = percentOf(done, total),
        sellReady = sellReady,
        sections = listOf(
            OnboardingSection(OnboardingSectionKind.SELL, sell),
            OnboardingSection(OnboardingSectionKind.OPERATE, operate),
            OnboardingSection(OnboardingSectionKind.GROW, grow),
        ).filter { it.items.isNotEmpty() },
    )
}

private fun percentOf(done: Int, total: Int): Int =
    if (total <= 0) 100 else (done * 100 / total).coerceIn(0, 100)

private fun toItem(
    key: OnboardingStepKey,
    step: OnboardingStep,
    localPrinterConfigured: Boolean,
    sunat: SunatRequestStatus?,
): OnboardingItem {
    val tier = step.tier ?: key.defaultTier
    val isPrinter = key == OnboardingStepKey.PRINTER
    val sunatState = if (key == OnboardingStepKey.SUNAT) sunat?.state ?: SunatRequestState.NONE else null

    val done = step.done ||
        (isPrinter && localPrinterConfigured) ||
        sunatState == SunatRequestState.ACTIVE

    val copy = copyFor(key)
    val count = step.count ?: 0
    val title = if (key == OnboardingStepKey.MENU && done && count > 0) {
        "${copy.title} ($count ${if (count == 1) "plato" else "platos"})"
    } else {
        copy.title
    }

    val statusText = when {
        key != OnboardingStepKey.SUNAT -> null
        sunatState == SunatRequestState.ACTIVE || step.done -> "Activa"
        sunatState == SunatRequestState.IN_REVIEW -> "En revisión"
        sunatState == SunatRequestState.REQUESTED ->
            sunat?.requestedAt?.let(::formatIsoDate)?.let { "Solicitada el $it" } ?: "Solicitada"
        else -> null
    }
    val showButton = !done && statusText == null

    return OnboardingItem(
        key = key,
        tier = tier,
        title = title,
        description = copy.description,
        done = done,
        thisDevice = isPrinter || step.localOnly,
        primaryLabel = if (showButton) copy.cta else null,
        primaryAction = if (showButton) copy.action else null,
        offersSampleMenu = key == OnboardingStepKey.MENU && !done,
        canSkip = !done && tier != OnboardingTier.REQUIRED && statusText == null,
        statusText = statusText,
    )
}

private data class StepCopy(
    val title: String,
    val description: String,
    val cta: String,
    val action: OnboardingAction,
)

private fun nav(destination: OnboardingDestination) = OnboardingAction.Navigate(destination)

private fun copyFor(key: OnboardingStepKey): StepCopy = when (key) {
    OnboardingStepKey.MENU -> StepCopy(
        "Tu carta",
        "Carga tus platos para que tus mozos puedan vender.",
        "Subir mi carta",
        nav(OnboardingDestination.PRODUCTOS),
    )
    OnboardingStepKey.FIRST_SALE -> StepCopy(
        "Tu primera venta",
        "Cobra un pedido para ver cómo funciona todo junto.",
        "Hacer mi primera venta",
        nav(OnboardingDestination.POS),
    )
    OnboardingStepKey.TABLES -> StepCopy(
        "Revisa tus mesas",
        "Ya creamos 10 mesas. Ponles el nombre que usas en tu local.",
        "Revisar mesas",
        nav(OnboardingDestination.MESAS_ADMIN),
    )
    OnboardingStepKey.PRINTER -> StepCopy(
        "Conecta tu impresora (este equipo)",
        "Para imprimir comandas y tickets en papel.",
        "Configurar impresora",
        nav(OnboardingDestination.IMPRESORAS),
    )
    OnboardingStepKey.AUTH_PIN -> StepCopy(
        "Crea tu PIN de autorización",
        "Lo necesitas para anular o devolver una venta.",
        "Crear mi PIN",
        nav(OnboardingDestination.CONFIG_OPERACION),
    )
    OnboardingStepKey.TEAM -> StepCopy(
        "Agrega a tu equipo",
        "Crea un PIN para cada mozo y cocinero.",
        "Agregar equipo",
        nav(OnboardingDestination.CONFIG_OPERACION),
    )
    OnboardingStepKey.QR_MENU -> StepCopy(
        "Activa tu carta QR",
        "Para que tus clientes pidan desde la mesa.",
        "Activar carta QR",
        nav(OnboardingDestination.CONFIG_MENU_DIGITAL),
    )
    OnboardingStepKey.SUNAT -> StepCopy(
        "Emite boletas y facturas con SUNAT",
        "Te avisamos cuando esté lista.",
        "Solicitar activación",
        OnboardingAction.RequestSunat,
    )
    OnboardingStepKey.RECIPES -> StepCopy(
        "Calcula el costo de tus platos",
        "Agrega ingredientes a tus platos.",
        "Ver mis platos",
        nav(OnboardingDestination.PRODUCTOS),
    )
    OnboardingStepKey.DELIVERY -> StepCopy(
        "Configura tu delivery",
        "Registra a tus repartidores.",
        "Agregar repartidor",
        nav(OnboardingDestination.REPARTIDORES),
    )
    OnboardingStepKey.BRANCHES -> StepCopy(
        "Agrega otro local",
        "Si tienes más de una sucursal.",
        "Ver sucursales",
        nav(OnboardingDestination.CONFIG_SUCURSALES),
    )
}

/** `2026-10-04T15:30:00Z` -> `04/10/2026`. Sin zona horaria: basta para "Solicitada el …". */
internal fun formatIsoDate(iso: String): String? {
    val match = Regex("""^(\d{4})-(\d{2})-(\d{2})""").find(iso.trim()) ?: return null
    val (y, m, d) = match.destructured
    return "$d/$m/$y"
}
