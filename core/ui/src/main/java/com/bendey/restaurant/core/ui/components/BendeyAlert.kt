package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.previews.BendeyPhonePreview
import com.bendey.restaurant.core.designsystem.previews.BendeyPreviewSurface
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.designsystem.theme.BendeyTone

/** Severidad de un [BendeyAlert] (DESIGN-SYSTEM §3.2 / §7.2). */
enum class BendeyAlertSeverity(
    internal val icon: ImageVector,
    internal val iconDescription: String,
) {
    Success(Icons.Default.CheckCircle, "Éxito"),
    Warning(Icons.Default.Warning, "Aviso"),
    Danger(Icons.Default.Error, "Error"),
    Info(Icons.Default.Info, "Información"),
}

/** Acción de texto de un [BendeyAlert] (máximo dos). */
data class BendeyAlertAction(
    val label: String,
    val onClick: () -> Unit,
)

internal fun BendeyAlertSeverity.tone(): BendeyTone = when (this) {
    BendeyAlertSeverity.Success -> BendeyColors.SuccessTone
    BendeyAlertSeverity.Warning -> BendeyColors.WarningTone
    BendeyAlertSeverity.Danger -> BendeyColors.DangerTone
    BendeyAlertSeverity.Info -> BendeyColors.InfoTone
}

/**
 * Alerta/banner inline: icono + título + mensaje + hasta 2 acciones. El estado nunca va solo en
 * color: siempre hay icono y texto. Texto = `onTint` sobre `tint`.
 *
 * Reemplazará progresivamente a [BendeyOverlayBanner] y a los `Text` de color suelto; ambos
 * conviven por ahora (cada fase migra lo suyo).
 *
 * Los errores de dinero (cobro, caja, anulación, SUNAT) deben usar [BendeyAlertSeverity.Danger]
 * con una acción, y no descartarse solos.
 */
@Composable
fun BendeyAlert(
    message: String,
    modifier: Modifier = Modifier,
    severity: BendeyAlertSeverity = BendeyAlertSeverity.Info,
    title: String? = null,
    primaryAction: BendeyAlertAction? = null,
    secondaryAction: BendeyAlertAction? = null,
    onDismiss: (() -> Unit)? = null,
) {
    val tone = severity.tone()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(tone.tint, BendeyShapeTokens.md)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(
                start = BendeySpacing.s12,
                top = BendeySpacing.s12,
                bottom = BendeySpacing.s12,
                end = if (onDismiss != null) BendeySpacing.s4 else BendeySpacing.s12,
            ),
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s12),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = severity.icon,
            contentDescription = severity.iconDescription,
            tint = tone.onTint,
            modifier = Modifier.size(24.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.s4),
        ) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = tone.onTint,
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = tone.onTint,
            )
            if (primaryAction != null || secondaryAction != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.touchGap)) {
                    secondaryAction?.let { AlertActionButton(it, tone) }
                    primaryAction?.let { AlertActionButton(it, tone, emphasized = true) }
                }
            }
        }
        if (onDismiss != null) {
            BendeyCompactIconButton(
                onClick = onDismiss,
                icon = Icons.Default.Close,
                contentDescription = "Cerrar aviso",
                tint = tone.onTint,
            )
        }
    }
}

@Composable
private fun AlertActionButton(
    action: BendeyAlertAction,
    tone: BendeyTone,
    emphasized: Boolean = false,
) {
    TextButton(
        onClick = action.onClick,
        modifier = Modifier.heightIn(min = BendeySpacing.touchMin),
        colors = ButtonDefaults.textButtonColors(contentColor = tone.onTint),
    ) {
        Text(
            text = action.label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@BendeyPhonePreview
@Composable
private fun BendeyAlertPreview() {
    BendeyPreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
            BendeyAlert(
                severity = BendeyAlertSeverity.Danger,
                title = "No se pudo cobrar",
                message = "La caja está cerrada. Abre la caja para cobrar en efectivo.",
                primaryAction = BendeyAlertAction("Abrir caja") {},
                secondaryAction = BendeyAlertAction("Cancelar") {},
            )
            BendeyAlert(severity = BendeyAlertSeverity.Warning, message = "Tu plan vence en 3 días.")
            BendeyAlert(severity = BendeyAlertSeverity.Success, message = "Guardamos tus cambios.")
            BendeyAlert(severity = BendeyAlertSeverity.Info, message = "Tienes 2 pedidos por revisar.", onDismiss = {})
        }
    }
}
