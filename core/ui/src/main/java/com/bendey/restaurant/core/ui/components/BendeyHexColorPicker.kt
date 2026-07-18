package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

val BendeyMenuColorPresets = listOf(
    "#C9393B", "#EF4444", "#F97316", "#EAB308", "#10B981",
    "#06B6D4", "#3B82F6", "#A855F7", "#EC4899",
)

private val hexColorRegex = Regex("^#[0-9A-Fa-f]{6}$")

fun isValidHexColor(raw: String): Boolean = hexColorRegex.matches(raw.trim())

fun normalizeHexColor(raw: String): String {
    val v = raw.trim()
    return when {
        hexColorRegex.matches(v) -> v.uppercase()
        Regex("^[0-9A-Fa-f]{6}$").matches(v) -> "#${v.uppercase()}"
        else -> raw
    }
}

fun parseHexColorOrNull(hex: String): Color? = if (isValidHexColor(hex)) {
    try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: IllegalArgumentException) {
        null
    }
} else {
    null
}

/** Selector de color: campo hex editable + swatches preset. Espejo del picker de Tauri. */
@Composable
fun BendeyHexColorPicker(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    presets: List<String> = BendeyMenuColorPresets,
    enabled: Boolean = true,
) {
    Column(modifier = modifier) {
        BendeyTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            placeholder = "#C9393B",
            enabled = enabled,
        )
        Row(
            modifier = Modifier
                .padding(top = BendeySpacing.xs)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
        ) {
            presets.forEach { preset ->
                val selected = normalizeHexColor(value) == preset
                val swatchColor = parseHexColorOrNull(preset) ?: BendeyColors.Primary
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(swatchColor)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) BendeyColors.OnSurface else BendeyColors.Outline,
                            shape = CircleShape,
                        )
                        .clickable(enabled = enabled) { onValueChange(preset) },
                )
            }
        }
    }
}
