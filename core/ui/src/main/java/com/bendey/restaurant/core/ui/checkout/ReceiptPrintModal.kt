package com.bendey.restaurant.core.ui.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.bendey.restaurant.core.domain.billing.SalePrintData
import java.text.NumberFormat
import java.util.Locale

private val ReceiptDocActive = Color(0xFF1E3A8A)
private val WhatsAppGreen = Color(0xFF25D366)

enum class ReceiptPdfFormatUi(val label: String) {
    TICKET("Ticket"),
    A4("A4"),
}

@Composable
fun ReceiptPrintModal(
    open: Boolean,
    printData: SalePrintData?,
    saleNumber: String,
    total: Double,
    hasPrinter: Boolean,
    busyAction: String?,
    onPrint: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onOpenPdf: (ReceiptPdfFormatUi) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!open) return

    val currency = remember { NumberFormat.getCurrencyInstance(Locale("es", "PE")) }
    var pdfFormat by remember(open) { mutableStateOf(ReceiptPdfFormatUi.TICKET) }

    val paidTotal = remember(printData, total) {
        val payments = printData?.payments.orEmpty()
        if (payments.isEmpty()) total else payments.sumOf { it.amount }
    }
    val change = (paidTotal - total).coerceAtLeast(0.0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 640.dp),
            shape = RoundedCornerShape(20.dp),
            color = BendeyColors.Surface,
            shadowElevation = 12.dp,
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(end = 36.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(BendeyColors.PrimaryContainer, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = BendeyColors.Primary,
                                )
                            }
                            Column {
                                Text(
                                    "Venta registrada",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    saleNumber,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BendeyColors.OnSurfaceVariant,
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .background(
                                    BendeyColors.PrimaryContainer.copy(alpha = 0.35f),
                                    RoundedCornerShape(14.dp),
                                )
                                .border(
                                    1.dp,
                                    BendeyColors.Primary.copy(alpha = 0.2f),
                                    RoundedCornerShape(14.dp),
                                )
                                .padding(12.dp),
                        ) {
                            Text(
                                "Resumen de pago",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Total", fontWeight = FontWeight.SemiBold)
                                Text(
                                    currency.format(total),
                                    fontWeight = FontWeight.Bold,
                                    color = BendeyColors.Primary,
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Pagado", color = BendeyColors.OnSurfaceVariant)
                                Text(currency.format(paidTotal), fontWeight = FontWeight.SemiBold)
                            }
                            if (change > 0.009) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .background(BendeyColors.WarningContainer, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("Vuelto", fontWeight = FontWeight.Bold, color = BendeyColors.OnWarning)
                                    Text(
                                        currency.format(change),
                                        fontWeight = FontWeight.Bold,
                                        color = BendeyColors.OnWarning,
                                    )
                                }
                            }
                        }

                        Text(
                            "Acciones",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (hasPrinter) {
                                ReceiptActionButton(
                                    label = "Imprimir",
                                    icon = {
                                        if (busyAction == "print") {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                            )
                                        } else {
                                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    },
                                    containerColor = BendeyColors.SurfaceVariant,
                                    contentColor = BendeyColors.OnSurface,
                                    enabled = busyAction == null && printData != null,
                                    onClick = onPrint,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            ReceiptActionButton(
                                label = "Comprobante",
                                icon = {
                                    if (busyAction == "pdf") {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White,
                                        )
                                    } else {
                                        Icon(Icons.Default.Article, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                },
                                containerColor = ReceiptDocActive,
                                contentColor = Color.White,
                                enabled = busyAction == null && printData != null,
                                onClick = { onOpenPdf(pdfFormat) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ReceiptPdfFormatUi.entries.forEach { format ->
                                FormatToggleChip(
                                    label = format.label,
                                    selected = pdfFormat == format,
                                    selectedColor = if (format == ReceiptPdfFormatUi.TICKET) ReceiptDocActive else Color(0xFF991B1B),
                                    onClick = { pdfFormat = format },
                                    enabled = busyAction == null,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        Text(
                            "Se abre con el visor PDF del dispositivo",
                            style = MaterialTheme.typography.labelSmall,
                            color = BendeyColors.OnSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        ReceiptActionButton(
                            label = "WhatsApp",
                            icon = {
                                if (busyAction == "share") {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White,
                                    )
                                }
                            },
                            containerColor = WhatsAppGreen,
                            contentColor = Color.White,
                            enabled = busyAction == null && printData != null,
                            onClick = onShareWhatsApp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BendeyColors.SurfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BendeyColors.Primary)
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Cerrar",
                            color = BendeyColors.OnPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptActionButton(
    label: String,
    icon: (@Composable () -> Unit)?,
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor.copy(alpha = if (enabled) 1f else 0.5f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        icon?.invoke()
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = contentColor)
    }
}

@Composable
private fun FormatToggleChip(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) selectedColor else selectedColor.copy(alpha = 0.55f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
