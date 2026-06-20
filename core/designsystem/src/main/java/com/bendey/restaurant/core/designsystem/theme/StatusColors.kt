package com.bendey.restaurant.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import com.bendey.restaurant.core.domain.restaurant.ComandaStatus
import com.bendey.restaurant.core.domain.restaurant.TableStatus

fun TableStatus.accentColor(): Color = when (this) {
    TableStatus.LIBRE -> BendeyColors.TableLibre
    TableStatus.OCUPADA -> BendeyColors.TableOcupada
    TableStatus.RESERVADA -> BendeyColors.TableReservada
    TableStatus.EN_CONSUMO -> BendeyColors.TableEnConsumo
}

fun ComandaStatus.accentColor(): Color = when (this) {
    ComandaStatus.PENDIENTE -> BendeyColors.KitchenPendiente
    ComandaStatus.PREPARACION -> BendeyColors.KitchenPreparando
    ComandaStatus.LISTA -> BendeyColors.KitchenListo
    ComandaStatus.ENTREGADA -> BendeyColors.KitchenEntregado
}
