package com.bendey.restaurant.core.ui.checkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.billing.PaymentMethodOption
import com.bendey.restaurant.core.domain.tips.TipCopy
import com.bendey.restaurant.core.domain.tips.TipDraft
import com.bendey.restaurant.core.domain.tips.TipRules
import com.bendey.restaurant.core.domain.tips.TipsText
import com.bendey.restaurant.core.ui.components.BendeyOption
import com.bendey.restaurant.core.ui.components.BendeySimpleSelect
import com.bendey.restaurant.core.ui.components.BendeyTextField

/** Persona a quien puede ir la propina (id = id de personal del restaurante). */
data class TipStaffChoice(val id: Int, val name: String)

/**
 * Estado del bloque «Propina» del cobro. Lo arma el llamador (Mesa o POS); el diálogo solo lo dibuja.
 *
 * @property isTableSession en mesa el servidor usa el mozo de la sesión si no se elige otro; en venta rápida
 * hay que elegir a quién va la propina.
 */
data class CheckoutTipState(
    val draft: TipDraft,
    val staff: List<TipStaffChoice>,
    val isTableSession: Boolean,
    /** Nombre del mozo de la mesa, para el texto de la opción por defecto. */
    val tableWaiterName: String? = null,
    val onChange: (TipDraft) -> Unit,
)

private const val DEFAULT_STAFF = "__default__"

/** El cobro se puede confirmar con esta propina: el monto, si lo hay, es válido. */
fun CheckoutTipState?.allowsConfirm(): Boolean {
    if (this == null) return true
    return TipRules.validate(draft.amount) == null
}

/**
 * Bloque opcional «Propina»: monto, medio y mozo. La propina NO cambia el total a cobrar ni el vuelto; se muestra
 * aparte como «Propina (no va en el comprobante)».
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckoutTipSection(
    state: CheckoutTipState,
    /** Total a cobrar (con descuento y recargo) sobre el que se calculan los atajos de 5 % y 10 %. */
    payableTotal: Double,
    methods: List<PaymentMethodOption>,
    /** Medio principal elegido en los pagos; es el que se usa si no se elige otro. */
    defaultMethodCode: String,
    modifier: Modifier = Modifier,
) {
    val draft = state.draft
    val amountError = TipRules.validate(draft.amount)
    val hasTip = TipRules.hasTip(draft)
    val effectiveMethod = draft.method ?: defaultMethodCode
    val methodOptions = (if (methods.isEmpty()) listOf(BendeyOption("cash", "Efectivo")) else methods.map { BendeyOption(it.code, it.name) })
    val selectedPercent = TipRules.SHORTCUT_PERCENTS.firstOrNull { p ->
        hasTip && TipRules.parse(draft.amount) == TipRules.suggest(payableTotal, p)
    }
    val otherSelected = hasTip && selectedPercent == null

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
        Text(
            TipCopy.TITLE + " (opcional)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = BendeyColors.OnSurfaceVariant,
        )
        Text(TipCopy.HELP, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
        ) {
            BendeyFilterChip(
                selected = !hasTip,
                onClick = { state.onChange(draft.copy(amount = "")) },
                text = "Sin propina",
            )
            TipRules.SHORTCUT_PERCENTS.forEach { p ->
                BendeyFilterChip(
                    selected = selectedPercent == p,
                    onClick = { state.onChange(draft.copy(amount = TipRules.format(TipRules.suggest(payableTotal, p)))) },
                    text = "$p %",
                )
            }
            BendeyFilterChip(
                selected = otherSelected,
                onClick = { if (selectedPercent != null || !hasTip) state.onChange(draft.copy(amount = "")) },
                text = "Otro",
            )
        }
        BendeyTextField(
            value = draft.amount,
            onValueChange = { state.onChange(draft.copy(amount = it)) },
            label = "Monto de la propina (S/)",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = amountError != null,
            supportingText = amountError,
            modifier = Modifier.fillMaxWidth(),
        )
        if (hasTip && amountError == null) {
            BendeySimpleSelect(
                options = methodOptions,
                selectedValue = effectiveMethod,
                onSelect = { state.onChange(draft.copy(method = it)) },
                label = "Medio de la propina",
                modifier = Modifier.fillMaxWidth(),
            )
            val staffOptions = buildList {
                add(
                    BendeyOption(
                        DEFAULT_STAFF,
                        if (state.isTableSession) {
                            state.tableWaiterName?.let { "Mozo de la mesa ($it)" } ?: "Mozo de la mesa"
                        } else {
                            "Sin mozo asignado"
                        },
                    ),
                )
                state.staff.forEach { add(BendeyOption(it.id.toString(), it.name)) }
            }
            if (staffOptions.isNotEmpty()) {
                BendeySimpleSelect(
                    options = staffOptions,
                    selectedValue = draft.staffId?.toString() ?: DEFAULT_STAFF,
                    onSelect = { v -> state.onChange(draft.copy(staffId = if (v == DEFAULT_STAFF) null else v.toIntOrNull())) },
                    label = "Mozo que la recibe",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(TipCopy.LINE_NOT_IN_RECEIPT, style = MaterialTheme.typography.bodyMedium, color = BendeyColors.OnSurface)
                Text(
                    TipsText.money(TipRules.parse(draft.amount) ?: 0.0),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = BendeyColors.OnSurface,
                )
            }
        }
    }
}
