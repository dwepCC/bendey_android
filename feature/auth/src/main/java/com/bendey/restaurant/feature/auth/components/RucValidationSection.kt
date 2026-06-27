package com.bendey.restaurant.feature.auth.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.bendey.restaurant.core.designsystem.theme.BendeyMotion
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextButton
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 220)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
        AnimatedContent(
            targetState = rucValidated,
            transitionSpec = {
                fadeIn(animationSpec = BendeyMotion.ExpressiveEffectsTween) togetherWith
                    fadeOut(animationSpec = BendeyMotion.ExpressiveEffectsTween)
            },
            label = "ruc_validated_fields",
        ) { validated ->
            if (validated) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(BendeySpacing.sm))
                    BendeyTextButton(text = "Cambiar RUC", onClick = onClearRuc)
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
        }
        AnimatedVisibility(
            visible = validating,
            enter = fadeIn(animationSpec = BendeyMotion.ExpressiveEffectsTween),
            exit = fadeOut(animationSpec = BendeyMotion.ExpressiveEffectsTween),
        ) {
            Text(
                text = "Consultando SUNAT…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = BendeySpacing.xs),
            )
        }
    }
}
