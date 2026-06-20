package com.bendey.restaurant.feature.caja

data class CashMovementCategoryOption(
    val value: String,
    val label: String,
)

val incomeMovementCategories = listOf(
    CashMovementCategoryOption("ingreso_manual", "Ingreso manual"),
    CashMovementCategoryOption("venta_efectivo", "Venta (efectivo manual)"),
    CashMovementCategoryOption("devolucion", "Devolución"),
    CashMovementCategoryOption("prestamo_cobro", "Cobro de préstamo"),
    CashMovementCategoryOption("otro_ingreso", "Otro ingreso"),
)

val expenseMovementCategories = listOf(
    CashMovementCategoryOption("egreso_manual", "Egreso manual"),
    CashMovementCategoryOption("gasto", "Gasto"),
    CashMovementCategoryOption("retiro", "Retiro"),
    CashMovementCategoryOption("pago_proveedor", "Pago a proveedor"),
    CashMovementCategoryOption("prestamo_entrega", "Préstamo entregado"),
    CashMovementCategoryOption("otro_egreso", "Otro egreso"),
)

fun cashMovementCategoryLabel(value: String): String =
    (incomeMovementCategories + expenseMovementCategories)
        .firstOrNull { it.value == value }
        ?.label
        ?: value.replace('_', ' ').replaceFirstChar { it.uppercaseChar() }
