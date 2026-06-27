package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.layout.bendeyHorizontalSafeInsetsPadding
import com.bendey.restaurant.core.ui.layout.bendeyImePadding
import com.bendey.restaurant.core.ui.layout.bendeyTopSystemInsetsPadding

/**
 * Diálogo casi pantalla completa para configuración POS (productos, combos, modificadores).
 * Encabezado y pie fijos; contenido con scroll independiente; fondo blanco; SafeArea.
 */
@Composable
fun BendeyConfigureFullscreenDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    loading: Boolean = false,
    loadingMessage: String = "Cargando opciones…",
    confirmText: String = "Agregar",
    dismissText: String = "Cancelar",
    confirmEnabled: Boolean = true,
    footerSummary: String? = null,
    validationError: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = onDismissRequest,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = BendeyColors.Surface,
            tonalElevation = 0.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bendeyTopSystemInsetsPadding()
                        .padding(
                            horizontal = BendeySpacing.md,
                            vertical = BendeySpacing.xs,
                        ),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BendeyColors.OnSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    subtitle?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = BendeyColors.OnSurfaceVariant,
                            modifier = Modifier.padding(top = BendeySpacing.xxs),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (loading) {
                        Text(
                            text = loadingMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = BendeyColors.OnSurfaceVariant,
                            modifier = Modifier.padding(top = BendeySpacing.xxs),
                        )
                    }
                }

                HorizontalDivider(color = BendeyColors.Outline.copy(alpha = 0.6f))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    BendeyVerticalScrollColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = BendeySpacing.md,
                            vertical = BendeySpacing.xs,
                        ),
                        verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
                    ) {
                        content()
                    }
                }

                HorizontalDivider(color = BendeyColors.Outline.copy(alpha = 0.6f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .bendeyHorizontalSafeInsetsPadding()
                        .bendeyImePadding()
                        .padding(
                            horizontal = BendeySpacing.md,
                            vertical = BendeySpacing.sm,
                        ),
                    verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                ) {
                    footerSummary?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = BendeyColors.OnSurface,
                        )
                    }
                    validationError?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = BendeyColors.Error,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
                    ) {
                        BendeySecondaryButton(
                            text = dismissText,
                            onClick = onDismiss,
                            enabled = !loading,
                            modifier = Modifier.weight(1f),
                        )
                        BendeyPrimaryButton(
                            text = confirmText,
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
}
