package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Campo de fecha — reemplaza la entrada de texto libre "AAAA-MM-DD" que tenía el caso CUSTOM del
 * Dashboard (dos `BendeyTextField` sin validación real: cualquier typo producía un rango
 * inválido que el backend simplemente rechazaba en silencio). Al tocar el campo se abre un
 * [DatePicker] de Material3; el valor sigue siendo un `String` en formato ISO (`AAAA-MM-DD`), el
 * mismo contrato que ya usan los callers — no cambia ninguna firma de función ni de red, solo
 * cómo se captura el dato.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BendeyDateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val parsed = remember(value) {
        runCatching { LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
    }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            placeholder = { Text("AAAA-MM-DD") },
            readOnly = true,
            enabled = true,
            singleLine = true,
            trailingIcon = {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = BendeyColors.OnSurfaceVariant)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BendeyColors.Primary,
                unfocusedBorderColor = BendeyColors.Outline,
                focusedLabelColor = BendeyColors.Primary,
                unfocusedLabelColor = BendeyColors.OnSurfaceVariant,
                focusedContainerColor = BendeyColors.Surface,
                unfocusedContainerColor = BendeyColors.Surface,
                disabledBorderColor = BendeyColors.Outline,
                disabledLabelColor = BendeyColors.OnSurfaceVariant,
                disabledTextColor = BendeyColors.OnSurface,
            ),
        )
        // El propio OutlinedTextField en modo readOnly no dispara click/foco de forma fiable en
        // todas las versiones de Compose; esta capa transparente encima es el patrón estándar
        // para "tocar para abrir selector" sobre un campo de solo lectura.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showPicker = true },
        )
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (parsed ?: LocalDate.now())
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onValueChange(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    }
                    showPicker = false
                }) {
                    Text("Aceptar", color = BendeyColors.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancelar")
                }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

