package com.bendey.restaurant.feature.auth.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
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
    commercialNameFocusRequester: FocusRequester? = null,
    onCommercialNameDone: (() -> Unit)? = null,
    rucFocusRequester: FocusRequester? = null,
) {
    val canValidate = !validating && !rucValidated && ruc.length == 11
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 220)),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
    ) {
        BendeyTextField(
            value = ruc,
            onValueChange = onRucChange,
            label = "RUC *",
            modifier = Modifier
                .fillMaxWidth()
                .then(rucFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
            enabled = !rucValidated && !validating,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Go,
            ),
            keyboardActions = KeyboardActions(
                onGo = {
                    if (canValidate) onValidate()
                },
            ),
        )
        BendeyPrimaryButton(
            text = if (validating) "Validando…" else "Validar",
            onClick = onValidate,
            loading = validating,
            enabled = canValidate,
            modifier = Modifier.fillMaxWidth(),
        )
        AnimatedVisibility(
            visible = validating,
            enter = fadeIn(animationSpec = BendeyMotion.ExpressiveEffectsTween),
            exit = fadeOut(animationSpec = BendeyMotion.ExpressiveEffectsTween),
        ) {
            Text(
                text = "Consultando SUNAT…",
                style = MaterialTheme.typography.bodySmall,
                color = BendeyColors.OnSurfaceVariant,
                modifier = Modifier.padding(top = BendeySpacing.xxs),
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
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(BendeySpacing.formFieldGap),
                ) {
                    BendeyTextButton(text = "Cambiar RUC", onClick = onClearRuc)
                    BendeyTextField(
                        value = razonSocial,
                        onValueChange = {},
                        label = "Razón social",
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                    )
                    if (showCommercialName) {
                        BendeyTextField(
                            value = commercialName,
                            onValueChange = onCommercialNameChange,
                            label = commercialNameLabel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    commercialNameFocusRequester?.let { Modifier.focusRequester(it) }
                                        ?: Modifier,
                                ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { onCommercialNameDone?.invoke() },
                            ),
                        )
                    }
                }
            }
        }
    }
}
