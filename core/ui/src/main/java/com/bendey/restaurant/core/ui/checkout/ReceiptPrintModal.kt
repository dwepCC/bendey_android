package com.bendey.restaurant.core.ui.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import com.bendey.restaurant.core.designsystem.theme.BendeyCardDefaults
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.domain.billing.SalePrintData
import com.bendey.restaurant.core.ui.R
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
                .heightIn(max = 640.dp)
                .border(BendeyCardDefaults.border, BendeyShapeTokens.xl),
            shape = BendeyShapeTokens.xl,
            color = BendeyColors.Surface,
            shadowElevation = 6.dp,
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(BendeySpacing.xs),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(
                                start = BendeySpacing.md,
                                end = BendeySpacing.md,
                                top = BendeySpacing.md,
                                bottom = BendeySpacing.xs,
                            ),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
                            modifier = Modifier.padding(end = 36.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(BendeyColors.PrimaryContainer, BendeyShapeTokens.md),
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
                                .padding(top = BendeySpacing.sm)
                                .background(
                                    BendeyColors.PrimaryContainer.copy(alpha = 0.35f),
                                    BendeyShapeTokens.lg,
                                )
                                .border(
                                    1.dp,
                                    BendeyColors.Primary.copy(alpha = 0.2f),
                                    BendeyShapeTokens.lg,
                                )
                                .padding(BendeySpacing.sm),
                        ) {
                            Text(
                                "Resumen de pago",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = BendeySpacing.xs),
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
                                    .padding(top = BendeySpacing.xxs),
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
                                        .background(BendeyColors.WarningContainer, BendeyShapeTokens.xs)
                                        .padding(horizontal = BendeySpacing.xs, vertical = BendeySpacing.xxs),
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
                            modifier = Modifier.padding(top = BendeySpacing.md, bottom = BendeySpacing.xs),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
                        ) {
                            if (hasPrinter) {
                                ReceiptActionButton(
                                    label = "Reimprimir",
                                    icon = {
                                        if (busyAction == "print") {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Print,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                            )
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
                                label = "WhatsApp",
                                icon = {
                                    if (busyAction == "share") {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = BendeyColors.OnPrimary,
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_whatsapp),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = BendeyColors.OnPrimary,
                                        )
                                    }
                                },
                                containerColor = WhatsAppGreen,
                                contentColor = BendeyColors.OnPrimary,
                                enabled = busyAction == null && printData != null,
                                onClick = onShareWhatsApp,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BendeyColors.SurfaceVariant.copy(alpha = 0.5f))
                        .padding(BendeySpacing.sm),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(BendeyShapeTokens.md)
                            .background(BendeyColors.Primary)
                            .clickable(onClick = onDismiss)
                            .padding(vertical = BendeySpacing.sm),
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(BendeyShapeTokens.md)
            .background(containerColor.copy(alpha = if (enabled) 1f else 0.5f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = BendeySpacing.sm, horizontal = BendeySpacing.sm),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.invoke()
        if (icon != null) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                modifier = Modifier.padding(start = BendeySpacing.xs),
            )
        } else {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
            )
        }
    }
}
