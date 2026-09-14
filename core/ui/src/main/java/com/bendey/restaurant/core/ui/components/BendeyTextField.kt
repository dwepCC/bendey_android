package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.VisualTransformation
import com.bendey.restaurant.core.designsystem.theme.BendeyColors

@Composable
fun BendeyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    fillWidth: Boolean = true,
    placeholder: String? = null,
    // Antes, un campo con error se validaba a mano: cada pantalla dibujaba su propio Text() rojo
    // debajo del campo (Productos, Compras, ManualProductDialog, Configuración…), sin tocar el
    // borde/label del propio OutlinedTextField. Con isError/supportingText el campo se pinta con
    // el color de error del sistema y el mensaje vive en el slot nativo de Material3.
    isError: Boolean = false,
    supportingText: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { hint -> { Text(hint) } },
        modifier = if (fillWidth) modifier.fillMaxWidth() else modifier,
        singleLine = singleLine,
        enabled = enabled,
        isError = isError,
        supportingText = supportingText?.let { text -> { Text(text) } },
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BendeyColors.Primary,
            unfocusedBorderColor = BendeyColors.Outline,
            focusedLabelColor = BendeyColors.Primary,
            unfocusedLabelColor = BendeyColors.OnSurfaceVariant,
            cursorColor = BendeyColors.Primary,
            focusedContainerColor = BendeyColors.Surface,
            unfocusedContainerColor = BendeyColors.Surface,
            errorBorderColor = BendeyColors.Error,
            errorLabelColor = BendeyColors.Error,
            errorSupportingTextColor = BendeyColors.Error,
            errorCursorColor = BendeyColors.Error,
        ),
    )
}
