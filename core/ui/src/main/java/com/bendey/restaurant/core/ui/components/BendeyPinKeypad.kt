package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BendeyPinKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    maxDigits: Int = 6,
    currentLength: Int = 0,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(6) { index ->
                val filled = index < currentLength
                Text(
                    text = if (filled) "●" else "○",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (filled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "del"),
        )
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    when (key) {
                        "" -> androidx.compose.foundation.layout.Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp),
                        )
                        "del" -> OutlinedButton(
                            onClick = onBackspace,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp),
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Borrar")
                        }
                        else -> OutlinedButton(
                            onClick = {
                                if (currentLength < maxDigits) onDigit(key)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp),
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Text(text = key, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }
        }
    }
}
