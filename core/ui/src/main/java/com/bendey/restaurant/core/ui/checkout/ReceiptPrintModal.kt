package com.bendey.restaurant.core.ui.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import com.bendey.restaurant.core.designsystem.theme.BendeyCardDefaults
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.bendey.restaurant.core.ui.layout.adaptive.rememberBendeyAdaptiveProfile
import com.bendey.restaurant.core.ui.layout.adaptive.rememberPhysicalPortrait
import com.bendey.restaurant.core.ui.pos.PosPolishTokens
import java.text.NumberFormat
import java.util.Locale

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

    val profile = rememberBendeyAdaptiveProfile()
    val physicalPortrait = rememberPhysicalPortrait()
    val tabletLandscape = PosPolishTokens.usesPosTabletDialogLayout(profile, physicalPortrait)
    val dialogPadding = PosPolishTokens.dialogPadding(profile, physicalPortrait)
    val currency = remember { NumberFormat.getCurrencyInstance(Locale("es", "PE")) }

    val paidTotal = remember(printData, total) {
        val payments = printData?.payments.orEmpty()
        if (payments.isEmpty()) total else payments.sumOf { it.amount }
    }
    val change = (paidTotal - total).coerceAtLeast(0.0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(
                    if (PosPolishTokens.isTabletProfile(profile)) {
                        PosPolishTokens.dialogWidthFraction(profile, physicalPortrait)
                    } else {
                        0.94f
                    },
                )
                .wrapContentHeight()
                .heightIn(max = PosPolishTokens.receiptModalMaxHeight(profile, physicalPortrait))
                .border(BendeyCardDefaults.border, BendeyShapeTokens.xl),
            shape = BendeyShapeTokens.xl,
            color = BendeyColors.Surface,
            shadowElevation = 6.dp,
        ) {
            Column(modifier = Modifier.wrapContentHeight()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = dialogPadding, end = BendeySpacing.xs, top = BendeySpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (tabletLandscape) 40.dp else 36.dp)
                            .background(BendeyColors.PrimaryContainer, BendeyShapeTokens.md),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Receipt,
                            contentDescription = null,
                            tint = BendeyColors.Primary,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = BendeySpacing.sm),
                    ) {
                        Text(
                            "Venta registrada",
                            style = if (tabletLandscape) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            saleNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = BendeyColors.OnSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Column(
                    modifier = Modifier.padding(horizontal = dialogPadding),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(BendeyShapeTokens.lg)
                            .background(BendeyColors.PrimaryContainer.copy(alpha = 0.32f))
                            .border(1.dp, BendeyColors.Primary.copy(alpha = 0.18f), BendeyShapeTokens.lg)
                            .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                "Total",
                                style = MaterialTheme.typography.labelMedium,
                                color = BendeyColors.OnSurfaceVariant,
                            )
                            Text(
                                currency.format(total),
                                style = if (tabletLandscape) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = BendeyColors.Primary,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Pagado",
                                style = MaterialTheme.typography.labelSmall,
                                color = BendeyColors.OnSurfaceVariant,
                            )
                            Text(
                                currency.format(paidTotal),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    if (change > 0.009) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = BendeySpacing.xxs)
                                .clip(BendeyShapeTokens.sm)
                                .background(BendeyColors.WarningContainer)
                                .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.xxs),
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

                HorizontalDivider(
                    modifier = Modifier.padding(top = BendeySpacing.sm),
                    color = BendeyColors.Outline.copy(alpha = 0.25f),
                )

                ReceiptPostSaleActions(
                    hasPrinter = hasPrinter,
                    busyAction = busyAction,
                    printData = printData,
                    onPrint = onPrint,
                    onShareWhatsApp = onShareWhatsApp,
                    modifier = Modifier.padding(
                        horizontal = dialogPadding,
                        vertical = BendeySpacing.sm,
                    ),
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dialogPadding, vertical = BendeySpacing.sm),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(BendeyShapeTokens.md)
                            .background(BendeyColors.Primary)
                            .clickable(onClick = onDismiss)
                            .padding(vertical = if (tabletLandscape) BendeySpacing.sm else BendeySpacing.sm),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Continuar",
                            color = BendeyColors.OnPrimary,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReceiptPostSaleActions(
    hasPrinter: Boolean,
    busyAction: String?,
    printData: SalePrintData?,
    onPrint: () -> Unit,
    onShareWhatsApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            "Acciones opcionales",
            style = MaterialTheme.typography.labelMedium,
            color = BendeyColors.OnSurfaceVariant,
            modifier = Modifier.padding(bottom = BendeySpacing.xs),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
        ) {
            if (hasPrinter) {
                ReceiptActionChip(
                    label = "Reimprimir",
                    icon = {
                        if (busyAction == "print") {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    },
                    containerColor = BendeyColors.SurfaceVariant,
                    contentColor = BendeyColors.OnSurface,
                    enabled = busyAction == null && printData != null,
                    onClick = onPrint,
                )
            }
            ReceiptActionChip(
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
            )
        }
    }
}

@Composable
private fun ReceiptActionChip(
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
            .wrapContentWidth()
            .heightIn(min = 44.dp)
            .clip(BendeyShapeTokens.md)
            .background(containerColor.copy(alpha = if (enabled) 1f else 0.5f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.sm),
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
