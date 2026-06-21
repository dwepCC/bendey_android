package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bendey.restaurant.core.designsystem.theme.BendeyColors

data class BendeySelectOption(
    val id: Int,
    val label: String,
)

@Composable
fun BendeyFormDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    confirmText: String = "Guardar",
    dismissText: String = "Cancelar",
    confirmEnabled: Boolean = true,
    loading: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = onDismissRequest,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 720.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = BendeyColors.OnSurface,
                )
                Column(
                    modifier = Modifier
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    content()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BendeySecondaryButton(
                        text = dismissText,
                        onClick = onDismiss,
                        enabled = !loading,
                        modifier = Modifier.weight(1f),
                    )
                    BendeyPrimaryButton(
                        text = if (loading) "Procesando…" else confirmText,
                        onClick = onConfirm,
                        enabled = confirmEnabled && !loading,
                        fillWidth = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun BendeySearchableSelect(
    options: List<BendeySelectOption>,
    selectedId: Int?,
    onSelect: (Int) -> Unit,
    label: String,
    placeholder: String = "Buscar…",
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val selectedLabel = options.firstOrNull { it.id == selectedId }?.label.orEmpty()
    val filtered = remember(options, query) {
        if (query.isBlank()) options
        else options.filter { it.label.contains(query, ignoreCase = true) }
    }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = BendeyColors.OnSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, BendeyColors.Outline, RoundedCornerShape(12.dp))
                        .background(BendeyColors.Surface)
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = selectedLabel.ifBlank { "Seleccionar" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedLabel.isBlank()) BendeyColors.OnSurfaceVariant else BendeyColors.OnSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = BendeyColors.OnSurfaceVariant,
                    )
                }
                if (expanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, BendeyColors.Outline, RoundedCornerShape(12.dp))
                            .background(BendeyColors.Surface),
                    ) {
                    BendeyTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = placeholder,
                        modifier = Modifier.padding(8.dp),
                    )
                    HorizontalDivider(color = BendeyColors.Outline.copy(alpha = 0.5f))
                    LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                        items(filtered, key = { it.id }) { option ->
                            Text(
                                text = option.label,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelect(option.id)
                                        expanded = false
                                        query = ""
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (option.id == selectedId) BendeyColors.Primary else BendeyColors.OnSurface,
                                fontWeight = if (option.id == selectedId) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                        if (filtered.isEmpty()) {
                            item {
                                Text(
                                    text = "Sin resultados",
                                    modifier = Modifier.padding(12.dp),
                                    color = BendeyColors.OnSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}

data class BendeyOption(
    val value: String,
    val label: String,
)

/** Select compacto (valor texto) — categorías, roles, IGV, etc. */
@Composable
fun BendeySimpleSelect(
    options: List<BendeyOption>,
    selectedValue: String?,
    onSelect: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "Seleccionar",
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.value == selectedValue }?.label.orEmpty()

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = BendeyColors.OnSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, BendeyColors.Outline, RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = selectedLabel.ifBlank { placeholder },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedLabel.isBlank()) BendeyColors.OnSurfaceVariant else BendeyColors.OnSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = BendeyColors.OnSurfaceVariant,
                    )
                }
                if (expanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, BendeyColors.Outline, RoundedCornerShape(12.dp))
                            .background(Color.White),
                    ) {
                    options.forEach { option ->
                        Text(
                            text = option.label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(option.value)
                                    expanded = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (option.value == selectedValue) BendeyColors.Primary else BendeyColors.OnSurface,
                            fontWeight = if (option.value == selectedValue) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                    }
                }
            }
        }
    }
}

@Composable
fun BendeySecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, BendeyColors.Outline, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = if (enabled) BendeyColors.OnSurface else BendeyColors.OnSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
    }
}
