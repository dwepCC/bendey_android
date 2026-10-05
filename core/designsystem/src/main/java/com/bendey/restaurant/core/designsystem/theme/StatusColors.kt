package com.bendey.restaurant.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import com.bendey.restaurant.core.domain.restaurant.ComandaStatus
import com.bendey.restaurant.core.domain.restaurant.TableStatus

fun TableStatus.accentColor(browsingOnly: Boolean = false): Color = when {
    this == TableStatus.LIBRE && browsingOnly -> BendeyColors.TableBrowsing
    this == TableStatus.LIBRE -> BendeyColors.TableLibre
    this == TableStatus.OCUPADA -> BendeyColors.TableOcupada
    this == TableStatus.RESERVADA -> BendeyColors.TableReservada
    this == TableStatus.EN_CONSUMO -> BendeyColors.TableEnConsumo
    else -> BendeyColors.TableLibre
}

/** Mapa único de estados (DESIGN-SYSTEM §3.5): fill + tint + onTint de la mesa. */
fun TableStatus.stateColors(browsingOnly: Boolean = false): BendeyStateColors = when {
    this == TableStatus.LIBRE && browsingOnly -> BendeyColors.StateMesaViendoCarta
    this == TableStatus.LIBRE -> BendeyColors.StateMesaLibre
    this == TableStatus.OCUPADA -> BendeyColors.StateMesaOcupada
    this == TableStatus.RESERVADA -> BendeyColors.StateMesaReservada
    this == TableStatus.EN_CONSUMO -> BendeyColors.StateMesaEnConsumo
    else -> BendeyColors.StateMesaLibre
}

/** Mapa único de estados (DESIGN-SYSTEM §3.5): fill + tint + onTint de la comanda. */
fun ComandaStatus.stateColors(): BendeyStateColors = when (this) {
    ComandaStatus.PENDIENTE -> BendeyColors.StateNuevo
    ComandaStatus.PREPARACION -> BendeyColors.StatePreparando
    ComandaStatus.LISTA -> BendeyColors.StateListo
    ComandaStatus.ENTREGADA -> BendeyColors.StateEntregado
    // Por revisar: espera una decision del personal, se ve como "nuevo" (ambar).
    ComandaStatus.POR_APROBAR -> BendeyColors.StateNuevo
}

fun ComandaStatus.accentColor(): Color = when (this) {
    ComandaStatus.PENDIENTE -> BendeyColors.KitchenPendiente
    ComandaStatus.PREPARACION -> BendeyColors.KitchenPreparando
    ComandaStatus.LISTA -> BendeyColors.KitchenListo
    ComandaStatus.ENTREGADA -> BendeyColors.KitchenEntregado
    ComandaStatus.POR_APROBAR -> BendeyColors.KitchenPendiente
}

fun saleStatusAccentColor(status: String, billingStatus: String?): Color {
    val key = (billingStatus ?: status).trim().lowercase()
    return when {
        key in setOf("accepted", "observed", "paid", "pagado", "issued", "emitido") -> BendeyColors.Success
        key in setOf("cancelled", "canceled", "cancelado", "rejected", "voided", "error") -> BendeyColors.Error
        key in setOf("pending", "sent", "open", "abierto", "draft") -> BendeyColors.Warning
        else -> BendeyColors.AccentTeal
    }
}
