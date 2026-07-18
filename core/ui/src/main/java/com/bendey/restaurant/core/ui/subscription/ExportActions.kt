package com.bendey.restaurant.core.ui.subscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

/**
 * Fila de 2 botones (PDF/Excel) usada en Ventas, Caja, Dashboard y Reportes de productos.
 * Cuando el plan no incluye [allowsExport], conserva los mismos 2 slots pero los bloquea
 * con candado y navega a la suscripción en vez de exportar — así no se rompen los layouts
 * existentes que asumen exactamente 2 botones en la fila.
 */
@Composable
fun BendeyExportActionsRow(
    allowsExport: Boolean,
    exportBusy: String?,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit,
    onLockedClick: () -> Unit,
    modifier: Modifier = Modifier,
    pdfLabel: String = "Exportar PDF",
    excelLabel: String = "Exportar Excel",
    pdfBusyLabel: String = "Exportando PDF…",
    excelBusyLabel: String = "Exportando Excel…",
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
    ) {
        if (allowsExport) {
            OutlinedButton(
                onClick = onExportPdf,
                enabled = exportBusy == null,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (exportBusy == "pdf") pdfBusyLabel else pdfLabel)
            }
            OutlinedButton(
                onClick = onExportExcel,
                enabled = exportBusy == null,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (exportBusy == "excel") excelBusyLabel else excelLabel)
            }
        } else {
            OutlinedButton(
                onClick = onLockedClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BendeyColors.OnSurfaceVariant),
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                Text(" PDF Pro")
            }
            OutlinedButton(
                onClick = onLockedClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BendeyColors.OnSurfaceVariant),
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                Text(" Excel Pro")
            }
        }
    }
}
