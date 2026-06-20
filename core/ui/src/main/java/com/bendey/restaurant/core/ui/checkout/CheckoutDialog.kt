package com.bendey.restaurant.core.ui.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.domain.billing.CheckoutDiscountMode
import com.bendey.restaurant.core.domain.billing.CheckoutMeta
import com.bendey.restaurant.core.domain.billing.CheckoutPaymentDraft
import com.bendey.restaurant.core.domain.billing.ContactBrief
import com.bendey.restaurant.core.domain.billing.DocumentSeries
import com.bendey.restaurant.core.domain.billing.PaymentMethodOption
import com.bendey.restaurant.core.domain.billing.calcCheckoutDiscountAmount
import com.bendey.restaurant.core.domain.billing.calcPayableTotal
import com.bendey.restaurant.core.domain.billing.firstSeriesForDocTypeKey
import com.bendey.restaurant.core.domain.billing.groupCheckoutDocTypes
import com.bendey.restaurant.core.domain.billing.normalizeDocTypeKey
import com.bendey.restaurant.core.domain.billing.paidCoversTotal
import com.bendey.restaurant.core.domain.billing.roundDisplay
import com.bendey.restaurant.core.domain.billing.seriesForDocType
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeySearchableSelect
import com.bendey.restaurant.core.ui.components.BendeySelectOption
import com.bendey.restaurant.core.ui.components.BendeySecondaryButton
import java.text.NumberFormat
import java.util.Locale

/** Azul oscuro para comprobante activo (React `blue-900`). */
private val CheckoutDocActive = Color(0xFF1E3A8A)

@Composable
fun CheckoutDialog(
    open: Boolean,
    title: String,
    loading: Boolean,
    metaLoading: Boolean,
    meta: CheckoutMeta?,
    rawTotal: Double,
    discountMode: CheckoutDiscountMode,
    discountValue: String,
    allowDiscount: Boolean,
    payments: List<CheckoutPaymentDraft>,
    seriesId: Int?,
    docType: String,
    contactId: Int?,
    error: String?,
    confirmLabel: String = "Confirmar cobro",
    onDismiss: () -> Unit,
    onSeriesChange: (Int, String) -> Unit,
    onContactChange: (Int) -> Unit,
    onDiscountModeChange: (CheckoutDiscountMode) -> Unit,
    onDiscountValueChange: (String) -> Unit,
    onPaymentsChange: (List<CheckoutPaymentDraft>) -> Unit,
    onConfirm: () -> Unit,
) {
    if (!open) return

    val currency = remember { NumberFormat.getCurrencyInstance(Locale("es", "PE")) }
    val series = meta?.series.orEmpty()
    val discountNumeric = discountValue.replace(',', '.').trim().toDoubleOrNull() ?: 0.0
    val discountAmount = calcCheckoutDiscountAmount(rawTotal, discountMode, discountNumeric)
    val payableTotal = calcPayableTotal(rawTotal, discountMode, discountNumeric)
    val paidTotal = payments.sumOf { it.amount.replace(',', '.').trim().toDoubleOrNull() ?: 0.0 }
    val remaining = (payableTotal - paidTotal).coerceAtLeast(0.0)
    val change = (paidTotal - payableTotal).coerceAtLeast(0.0)
    val hasCashPayment = payments.any { draft ->
        val method = meta?.paymentMethods?.firstOrNull { it.code == draft.method }
        method?.isCash == true || draft.method.equals("cash", ignoreCase = true)
    }
    val selectedSeries = series.firstOrNull { it.id == seriesId }
    val seriesCodeLabel = selectedSeries?.series?.trim().orEmpty().ifBlank { "—" }

    val canConfirm = seriesId != null &&
        contactId != null &&
        paidCoversTotal(paidTotal, payableTotal) &&
        payments.isNotEmpty() &&
        !loading &&
        !metaLoading

    Dialog(
        onDismissRequest = { if (!loading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .heightIn(max = 720.dp),
            shape = RoundedCornerShape(16.dp),
            color = BendeyColors.Surface,
            tonalElevation = 0.dp,
            shadowElevation = 10.dp,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = BendeyColors.OnSurface,
                )
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (metaLoading) {
                        Text("Cargando series y métodos…", color = BendeyColors.OnSurfaceVariant)
                    } else {
                        ContactSelector(
                            contacts = meta?.contacts.orEmpty(),
                            selectedId = contactId,
                            onSelect = onContactChange,
                        )
                        DocTypeSelector(
                            series = series,
                            selectedDocType = docType,
                            onSelectDocType = { key ->
                                firstSeriesForDocTypeKey(series, key)?.let { item ->
                                    onSeriesChange(item.id, item.docType)
                                }
                            },
                        )
                        val seriesOptions = seriesForDocType(series, docType)
                        if (seriesOptions.size > 1) {
                            BendeySearchableSelect(
                                options = seriesOptions.map {
                                    BendeySelectOption(it.id, it.series.trim().ifBlank { "Serie ${it.id}" })
                                },
                                selectedId = seriesId,
                                onSelect = { id ->
                                    seriesOptions.firstOrNull { it.id == id }?.let { item ->
                                        onSeriesChange(item.id, item.docType)
                                    }
                                },
                                label = "Serie",
                                placeholder = "Buscar serie…",
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        SerieDiscountRow(
                            seriesCode = seriesCodeLabel,
                            allowDiscount = allowDiscount,
                            discountMode = discountMode,
                            discountValue = discountValue,
                            onDiscountModeChange = onDiscountModeChange,
                            onDiscountValueChange = onDiscountValueChange,
                        )
                        PaymentLinesSection(
                            methods = meta?.paymentMethods.orEmpty(),
                            payments = payments,
                            payableTotal = payableTotal,
                            remaining = remaining,
                            onPaymentsChange = onPaymentsChange,
                        )
                        CheckoutTotalsSection(
                            currency = currency,
                            rawTotal = rawTotal,
                            discountAmount = discountAmount,
                            payableTotal = payableTotal,
                            paidTotal = paidTotal,
                            remaining = remaining,
                            change = change,
                            hasCashPayment = hasCashPayment,
                        )
                    }
                    error?.let {
                        Text(it, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    BendeySecondaryButton(text = "Cancelar", onClick = onDismiss, enabled = !loading)
                    BendeyPrimaryButton(
                        text = if (loading) "Procesando…" else confirmLabel,
                        onClick = onConfirm,
                        enabled = canConfirm,
                        fillWidth = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun DocTypeSelector(
    series: List<DocumentSeries>,
    selectedDocType: String,
    onSelectDocType: (String) -> Unit,
) {
    val groups = remember(series) { groupCheckoutDocTypes(series) }
    if (groups.isEmpty()) {
        CheckoutFieldLabel("Comprobante")
        Text("Sin series configuradas", color = BendeyColors.OnSurfaceVariant)
        return
    }
    val selectedKey = normalizeDocTypeKey(selectedDocType)
    CheckoutFieldLabel("Comprobante")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        groups.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { group ->
                    val selected = selectedKey == group.key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = 1.dp,
                                color = if (selected) CheckoutDocActive else BendeyColors.Outline,
                                shape = RoundedCornerShape(10.dp),
                            )
                            .background(if (selected) CheckoutDocActive else BendeyColors.Surface)
                            .clickable { onSelectDocType(group.key) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = group.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (selected) Color.White else BendeyColors.OnSurface,
                            maxLines = 2,
                        )
                    }
                }
                repeat(3 - row.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SerieDiscountRow(
    seriesCode: String,
    allowDiscount: Boolean,
    discountMode: CheckoutDiscountMode,
    discountValue: String,
    onDiscountModeChange: (CheckoutDiscountMode) -> Unit,
    onDiscountValueChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            CheckoutFieldLabel("Serie")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, BendeyColors.Outline, RoundedCornerShape(12.dp))
                    .background(BendeyColors.SurfaceVariant.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = seriesCode,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = BendeyColors.OnSurface,
                )
            }
        }
        if (allowDiscount) {
            Column(modifier = Modifier.weight(1f)) {
                CheckoutFieldLabel("Descuento")
                DiscountInputWithToggle(
                    mode = discountMode,
                    value = discountValue,
                    onModeChange = onDiscountModeChange,
                    onValueChange = onDiscountValueChange,
                )
            }
        }
    }
}

@Composable
private fun DiscountInputWithToggle(
    mode: CheckoutDiscountMode,
    value: String,
    onModeChange: (CheckoutDiscountMode) -> Unit,
    onValueChange: (String) -> Unit,
) {
    val toggleActiveColor = BendeyColors.Primary
    val toggleInactiveColor = Color(0xFF78716C)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, BendeyColors.Outline, RoundedCornerShape(12.dp))
            .background(BendeyColors.Surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(44.dp)
                .background(
                    if (mode == CheckoutDiscountMode.PERCENT) toggleActiveColor else toggleInactiveColor,
                )
                .clickable {
                    onModeChange(
                        if (mode == CheckoutDiscountMode.PERCENT) {
                            CheckoutDiscountMode.AMOUNT
                        } else {
                            CheckoutDiscountMode.PERCENT
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (mode == CheckoutDiscountMode.PERCENT) "%" else "S/",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp, vertical = 12.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = BendeyColors.OnSurface),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = if (mode == CheckoutDiscountMode.PERCENT) "0" else "0.00",
                            color = BendeyColors.OnSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun PaymentLinesSection(
    methods: List<PaymentMethodOption>,
    payments: List<CheckoutPaymentDraft>,
    payableTotal: Double,
    remaining: Double,
    onPaymentsChange: (List<CheckoutPaymentDraft>) -> Unit,
) {
    val defaultMethod = methods.firstOrNull()?.code ?: "cash"
    val methodOptions = if (methods.isEmpty()) {
        listOf(PaymentMethodOption(0, "Efectivo", "cash", "cash", true))
    } else {
        methods
    }

    CheckoutFieldLabel("Métodos de pago")
    if (remaining > 0.009 && payments.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BendeyColors.PrimaryContainer.copy(alpha = 0.35f))
                .border(1.dp, BendeyColors.Primary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Restante por asignar", style = MaterialTheme.typography.labelMedium, color = BendeyColors.OnSurface)
            Text(
                formatMoney(remaining),
                fontWeight = FontWeight.Bold,
                color = BendeyColors.Primary,
            )
        }
    }

    payments.forEachIndexed { index, line ->
        PaymentLineEditor(
            line = line,
            methods = methodOptions,
            canRemove = payments.size > 1,
            onUpdate = { updated ->
                onPaymentsChange(payments.mapIndexed { i, p -> if (i == index) updated else p })
            },
            onRemove = {
                if (payments.size <= 1) {
                    onPaymentsChange(
                        listOf(
                            CheckoutPaymentDraft(
                                method = defaultMethod,
                                amount = formatAmount(payableTotal),
                            ),
                        ),
                    )
                } else {
                    onPaymentsChange(payments.filterIndexed { i, _ -> i != index })
                }
            },
        )
    }

    OutlinedButton(
        onClick = {
            val currentPaid = payments.sumOf { it.amount.replace(',', '.').toDoubleOrNull() ?: 0.0 }
            val rem = (payableTotal - currentPaid).coerceAtLeast(0.0)
            val amount = if (payments.isEmpty()) payableTotal else rem
            onPaymentsChange(
                payments + CheckoutPaymentDraft(
                    method = defaultMethod,
                    amount = formatAmount(amount),
                ),
            )
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
        Text("Agregar método de pago")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentLineEditor(
    line: CheckoutPaymentDraft,
    methods: List<PaymentMethodOption>,
    canRemove: Boolean,
    onUpdate: (CheckoutPaymentDraft) -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clipSection(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PaymentMethodDropdown(
                methods = methods,
                selectedCode = line.method,
                onSelect = { onUpdate(line.copy(method = it)) },
                modifier = Modifier.weight(1f),
            )
            if (canRemove) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Quitar", tint = BendeyColors.Error)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CheckoutCompactField(
                label = "Monto",
                value = line.amount,
                onValueChange = { onUpdate(line.copy(amount = it)) },
                modifier = Modifier.weight(1f),
            )
            CheckoutCompactField(
                label = "Referencia",
                value = line.reference,
                onValueChange = { onUpdate(line.copy(reference = it)) },
                placeholder = "N° Op / Ref.",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentMethodDropdown(
    methods: List<PaymentMethodOption>,
    selectedCode: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = methods.firstOrNull { it.code == selectedCode }?.name ?: selectedCode

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            textStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            methods.forEach { method ->
                DropdownMenuItem(
                    text = { Text(method.name) },
                    onClick = {
                        onSelect(method.code)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun CheckoutCompactField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "0.00",
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = BendeyColors.OnSurfaceVariant,
            modifier = Modifier.padding(bottom = 2.dp),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun CheckoutTotalsSection(
    currency: NumberFormat,
    rawTotal: Double,
    discountAmount: Double,
    payableTotal: Double,
    paidTotal: Double,
    remaining: Double,
    change: Double,
    hasCashPayment: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clipSection()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (change > 0.009 && hasCashPayment) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BendeyColors.WarningContainer)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("VUELTO", fontWeight = FontWeight.Bold, color = BendeyColors.OnWarning, fontSize = 12.sp)
                Text(currency.format(change), fontWeight = FontWeight.Bold)
            }
        }
        if (discountAmount > 0) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Subtotal", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                Text(currency.format(rawTotal), style = MaterialTheme.typography.bodySmall)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Descuento", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                Text("− ${currency.format(discountAmount)}", style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total a pagar", fontWeight = FontWeight.Medium, color = BendeyColors.OnSurfaceVariant)
            Text(
                currency.format(payableTotal),
                fontWeight = FontWeight.Bold,
                color = BendeyColors.Primary,
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Suma de pagos", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
            Text(formatMoney(paidTotal), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
        }
        if (!paidCoversTotal(paidTotal, payableTotal)) {
            Text(
                "Falta ${formatMoney(remaining)}",
                color = BendeyColors.Error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ContactSelector(
    contacts: List<ContactBrief>,
    selectedId: Int?,
    onSelect: (Int) -> Unit,
) {
    if (contacts.isEmpty()) {
        CheckoutFieldLabel("Cliente")
        Text("Sin clientes", color = BendeyColors.OnSurfaceVariant)
        return
    }
    BendeySearchableSelect(
        options = contacts.map { BendeySelectOption(it.id, it.displayLabel) },
        selectedId = selectedId,
        onSelect = onSelect,
        label = "Cliente",
        placeholder = "Buscar cliente…",
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CheckoutFieldLabel(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = BendeyColors.OnSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun Modifier.clipSection(): Modifier = this
    .clip(RoundedCornerShape(12.dp))
    .background(BendeyColors.SurfaceVariant.copy(alpha = 0.35f))
    .border(1.dp, BendeyColors.Outline.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
    .padding(10.dp)

@Composable
fun CheckoutSuccessDialog(
    number: String,
    total: Double,
    subtitle: String,
    printNote: String? = null,
    onDismiss: () -> Unit,
) {
    val currency = remember { NumberFormat.getCurrencyInstance(Locale("es", "PE")) }
    BendeyFormDialog(
        onDismissRequest = onDismiss,
        title = "Venta generada",
        confirmText = "Aceptar",
        dismissText = "Cerrar",
        onConfirm = onDismiss,
        onDismiss = onDismiss,
        confirmEnabled = true,
    ) {
        Text("Documento: $number", fontWeight = FontWeight.SemiBold)
        Text("Total: ${currency.format(total)}")
        HorizontalDivider(color = BendeyColors.Outline.copy(alpha = 0.4f))
        Text(subtitle, color = BendeyColors.OnSurfaceVariant)
        printNote?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
        }
    }
}

private fun formatMoney(value: Double): String {
    val currency = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
    return currency.format(roundDisplay(value))
}

private fun formatAmount(value: Double): String {
    val rounded = roundDisplay(value)
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
}
