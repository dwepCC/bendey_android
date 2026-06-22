package com.bendey.restaurant.feature.auth.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextField

@Composable
fun RucValidationSection(
    ruc: String,
    onRucChange: (String) -> Unit,
    razonSocial: String,
    commercialName: String,
    onCommercialNameChange: (String) -> Unit,
    rucValidated: Boolean,
    validating: Boolean,
    showCommercialName: Boolean,
    onValidate: () -> Unit,
    onClearRuc: () -> Unit,
    modifier: Modifier = Modifier,
    commercialNameLabel: String = "Nombre comercial *",
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BendeyTextField(
            value = ruc,
            onValueChange = onRucChange,
            label = "RUC *",
            modifier = Modifier.weight(1f),
            enabled = !rucValidated && !validating,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        BendeyPrimaryButton(
            text = if (validating) "Validando…" else "Validar",
            onClick = onValidate,
            loading = validating,
            enabled = !validating && !rucValidated && ruc.length == 11,
            modifier = Modifier.weight(0.55f),
        )
    }
    if (rucValidated) {
        Spacer(modifier = Modifier.height(BendeySpacing.sm))
        TextButton(onClick = onClearRuc) {
            Text(
                text = "Cambiar RUC",
                color = BendeyColors.Primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        BendeyTextField(
            value = razonSocial,
            onValueChange = {},
            label = "Razón social",
            enabled = false,
        )
        if (showCommercialName) {
            Spacer(modifier = Modifier.height(BendeySpacing.xs))
            BendeyTextField(
                value = commercialName,
                onValueChange = onCommercialNameChange,
                label = commercialNameLabel,
            )
        }
    }
}
