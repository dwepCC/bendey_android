package com.bendey.restaurant.feature.configuracion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.components.BendeySectionTitle
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.ops.BusinessHoursRules
import com.bendey.restaurant.core.domain.ops.DayDraft
import com.bendey.restaurant.core.domain.ops.SlotDraft
import com.bendey.restaurant.core.ui.components.BendeyAlertDialog
import com.bendey.restaurant.core.ui.components.BendeyOutlinedButton
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeySwitchRow
import com.bendey.restaurant.core.ui.components.BendeyTextButton

/**
 * Horario de atención del marketplace: por día un interruptor «Abierto» y hasta 4 tramos con selector de hora.
 * Solo `s.m` lo edita (el PUT reemplaza TODO el horario); el resto lo ve en solo lectura.
 */
@Composable
fun BusinessHoursCard(
    state: BusinessHoursState,
    onDayOpen: (Int, Boolean) -> Unit,
    onAddSlot: (Int) -> Unit,
    onRemoveSlot: (Int, Int) -> Unit,
    onUpdateSlot: (Int, Int, SlotDraft) -> Unit,
    onCopyToAll: (Int) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
) {
    BendeyManagementCard {
        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {
            BendeySectionTitle(text = "Horario de atención")
            Text(
                state.status.summary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (state.status.configured && !state.status.open) BendeyColors.ErrorText else BendeyColors.OnSurface,
            )
            Text(
                "Fuera de este horario Bendey Cliente no recibe pedidos de tu restaurante. " + BusinessHoursRules.MIDNIGHT_NOTE,
                style = MaterialTheme.typography.bodySmall,
                color = BendeyColors.OnSurfaceVariant,
            )
            when {
                state.loading && state.saved == null -> Text("Cargando…", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                state.saved == null && state.loadFailed -> {
                    state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = BendeyColors.ErrorText) }
                    BendeyOutlinedButton(text = "Reintentar", onClick = onRetry, fillWidth = true)
                }
                else -> {
                    for (day in 1..7) {
                        DayEditor(
                            day = day,
                            draft = state.week[day] ?: DayDraft(),
                            editable = state.canEdit && !state.saving,
                            onOpen = { onDayOpen(day, it) },
                            onAddSlot = { onAddSlot(day) },
                            onRemoveSlot = { onRemoveSlot(day, it) },
                            onUpdateSlot = { i, slot -> onUpdateSlot(day, i, slot) },
                            onCopyToAll = { onCopyToAll(day) },
                        )
                    }
                    if (!state.status.configured && state.week.values.none { it.open }) {
                        Text(
                            BusinessHoursRules.ALWAYS_OPEN_NOTICE + ".",
                            style = MaterialTheme.typography.bodySmall,
                            color = BendeyColors.OnSurfaceVariant,
                        )
                    }
                    if (state.canEdit) {
                        state.validation?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = BendeyColors.ErrorText)
                        }
                        state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = BendeyColors.ErrorText) }
                        BendeyPrimaryButton(
                            text = if (state.saving) "Guardando…" else "Guardar",
                            onClick = onSave,
                            enabled = state.canSave,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (state.savedNotice) {
                            Text("Guardado.", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                        }
                    } else {
                        Text(
                            "Solo el administrador puede cambiar el horario.",
                            style = MaterialTheme.typography.bodySmall,
                            color = BendeyColors.OnSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayEditor(
    day: Int,
    draft: DayDraft,
    editable: Boolean,
    onOpen: (Boolean) -> Unit,
    onAddSlot: () -> Unit,
    onRemoveSlot: (Int) -> Unit,
    onUpdateSlot: (Int, SlotDraft) -> Unit,
    onCopyToAll: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs)) {
        BendeySwitchRow(
            label = "${BusinessHoursRules.DAY_NAMES[day]}: " + if (draft.open) "Abierto" else "Cerrado",
            checked = draft.open,
            onCheckedChange = onOpen,
            enabled = editable,
        )
        if (draft.open) {
            draft.slots.forEachIndexed { index, slot ->
                SlotEditor(
                    slot = slot,
                    editable = editable,
                    onChange = { onUpdateSlot(index, it) },
                    onRemove = if (draft.slots.size > 1) ({ onRemoveSlot(index) }) else null,
                )
            }
            if (editable) {
                Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                    if (draft.slots.size < BusinessHoursRules.MAX_PER_DAY) {
                        BendeyTextButton(text = "Agregar tramo", onClick = onAddSlot)
                    }
                    BendeyTextButton(text = "Copiar a todos los días", onClick = onCopyToAll)
                }
            }
        }
    }
}

@Composable
private fun SlotEditor(
    slot: SlotDraft,
    editable: Boolean,
    onChange: (SlotDraft) -> Unit,
    onRemove: (() -> Unit)?,
) {
    var picking by remember { mutableStateOf<Boolean?>(null) } // true = apertura, false = cierre
    Column(modifier = Modifier.padding(start = BendeySpacing.sm), verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BendeyOutlinedButton(
                text = "Abre ${slot.openAt.ifBlank { "--:--" }}",
                onClick = { picking = true },
                enabled = editable,
            )
            BendeyOutlinedButton(
                text = "Cierra ${slot.closeAt.ifBlank { "--:--" }}",
                onClick = { picking = false },
                enabled = editable,
            )
            if (onRemove != null && editable) BendeyTextButton(text = "Quitar", onClick = onRemove)
        }
        BusinessHoursRules.midnightExplanation(slot)?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
        }
        BusinessHoursRules.validateSlot(slot)?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = BendeyColors.ErrorText)
        }
    }
    picking?.let { isOpen ->
        TimePickerDialog(
            title = if (isOpen) "Hora de apertura" else "Hora de cierre",
            initial = if (isOpen) slot.openAt else slot.closeAt,
            onDismiss = { picking = null },
            onPick = { hm ->
                picking = null
                onChange(if (isOpen) slot.copy(openAt = hm) else slot.copy(closeAt = hm))
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(title: String, initial: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val minutes = BusinessHoursRules.parseHm(initial)
        ?: BusinessHoursRules.parseHm(BusinessHoursRules.DEFAULT_OPEN)!!
    val state = rememberTimePickerState(initialHour = minutes / 60, initialMinute = minutes % 60, is24Hour = true)
    BendeyAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = {
            BendeyTextButton(text = "Aceptar", onClick = { onPick(BusinessHoursRules.formatHm(state.hour, state.minute)) })
        },
        dismissButton = { BendeyTextButton(text = "Cancelar", onClick = onDismiss) },
    )
}
