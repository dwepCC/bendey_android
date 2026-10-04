package com.bendey.restaurant.feature.mesas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChipVariant
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyTextField

private enum class SessionEdit { GUESTS, NOTES }

private val GUEST_CHOICES = (1..8).toList()

/**
 * Chips "Comensales: 2 ✎" y "Nota ✎" de la cabecera de la mesa (R8 paso 2). Reemplazan al diálogo
 * "Abrir mesa": la mesa se abre al tocarla y estos datos se corrigen aquí, solo si el mozo lo necesita.
 */
@Composable
fun MesaSessionChips(
    guests: Int,
    notes: String?,
    saving: Boolean,
    onSaveGuests: (Int, onDone: () -> Unit) -> Unit,
    onSaveNotes: (String, onDone: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<SessionEdit?>(null) }
    var guestsDraft by remember { mutableStateOf(guests.coerceAtLeast(1)) }
    var notesDraft by remember { mutableStateOf(notes.orEmpty()) }
    val hasNote = !notes.isNullOrBlank()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
    ) {
        BendeyFilterChip(
            selected = false,
            onClick = {
                guestsDraft = if (guests > 0) guests else 2
                editing = SessionEdit.GUESTS
            },
            label = { Text("Comensales: ${if (guests > 0) guests else "—"}") },
            trailingIcon = { Icon(Icons.Default.Edit, contentDescription = "Editar comensales", modifier = Modifier.size(14.dp)) },
            variant = BendeyFilterChipVariant.Pos,
        )
        BendeyFilterChip(
            selected = hasNote,
            onClick = {
                notesDraft = notes.orEmpty()
                editing = SessionEdit.NOTES
            },
            label = {
                Text(
                    text = if (hasNote) "Nota: ${notes!!.trim()}" else "Nota",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingIcon = { Icon(Icons.Default.Edit, contentDescription = "Editar nota de la mesa", modifier = Modifier.size(14.dp)) },
            variant = BendeyFilterChipVariant.Pos,
            modifier = Modifier.weight(1f, fill = false),
        )
    }

    when (editing) {
        SessionEdit.GUESTS -> BendeyFormDialog(
            onDismissRequest = { if (!saving) editing = null },
            title = "Comensales",
            confirmText = if (saving) "Guardando…" else "Guardar",
            confirmEnabled = !saving,
            loading = saving,
            onConfirm = { onSaveGuests(guestsDraft) { editing = null } },
            onDismiss = { if (!saving) editing = null },
        ) {
            GUEST_CHOICES.chunked(4).forEach { rowChoices ->
                Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                    rowChoices.forEach { n ->
                        BendeyFilterChip(
                            selected = guestsDraft == n,
                            onClick = { guestsDraft = n },
                            text = n.toString(),
                            variant = BendeyFilterChipVariant.Pos,
                        )
                    }
                }
            }
        }
        SessionEdit.NOTES -> BendeyFormDialog(
            onDismissRequest = { if (!saving) editing = null },
            title = "Nota de la mesa",
            confirmText = if (saving) "Guardando…" else "Guardar",
            confirmEnabled = !saving,
            loading = saving,
            onConfirm = { onSaveNotes(notesDraft) { editing = null } },
            onDismiss = { if (!saving) editing = null },
        ) {
            BendeyTextField(
                value = notesDraft,
                onValueChange = { notesDraft = it },
                label = "Nota",
                singleLine = false,
            )
        }
        null -> Unit
    }
}
