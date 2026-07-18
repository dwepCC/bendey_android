package com.bendey.restaurant.core.ui.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import com.bendey.restaurant.core.designsystem.theme.BendeyCardDefaults
import com.bendey.restaurant.core.ui.components.BendeyVerticalScrollColumn
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
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
import com.bendey.restaurant.core.domain.billing.groupCheckoutDocTypesWithLocked
import com.bendey.restaurant.core.domain.billing.normalizeDocTypeKey
import com.bendey.restaurant.core.domain.billing.paidCoversTotal
import com.bendey.restaurant.core.domain.billing.roundDisplay
import com.bendey.restaurant.core.domain.billing.seriesForDocType
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeySearchableSelect
import com.bendey.restaurant.core.ui.components.BendeySelectOption
import com.bendey.restaurant.core.ui.components.BendeySecondaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import com.bendey.restaurant.core.ui.layout.adaptive.rememberBendeyAdaptiveProfile
import com.bendey.restaurant.core.ui.layout.adaptive.rememberPhysicalPortrait
import com.bendey.restaurant.core.ui.pos.PosPolishTokens
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
    lockedSeries: List<DocumentSeries> = emptyList(),
    onLockedDocTypeSelect: () -> Unit = {},
    extraBeforePayments: @Composable () -> Unit = {},
    onDismiss: () -> Unit,
    onSeriesChange: (Int, String) -> Unit,
    onContactChange: (Int) -> Unit,
    onDiscountModeChange: (CheckoutDiscountMode) -> Unit,
    onDiscountValueChange: (String) -> Unit,
    onPaymentsChange: (List<CheckoutPaymentDraft>) -> Unit,
    onConfirm: () -> Unit,
) {
    if (!open) return

    val profile = rememberBendeyAdaptiveProfile()
    val physicalPortrait = rememberPhysicalPortrait()
    val tabletLandscape = PosPolishTokens.usesPosTabletDialogLayout(profile, physicalPortrait)
    val dialogPadding = PosPolishTokens.dialogPadding(profile, physicalPortrait)
    val sectionGap = PosPolishTokens.dialogSectionGap(profile, physicalPortrait)
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
                .fillMaxWidth(
                    if (PosPolishTokens.isTabletProfile(profile)) {
                        PosPolishTokens.dialogWidthFraction(profile, physicalPortrait)
                    } else {
                        0.96f
                    },
                )
                .heightIn(max = PosPolishTokens.dialogMaxHeight(profile, physicalPortrait))
                .border(BendeyCardDefaults.border, BendeyShapeTokens.xl),
            shape = BendeyShapeTokens.xl,
            color = BendeyColors.Surface,
            tonalElevation = 0.dp,
            shadowElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(dialogPadding)) {
                Text(
                    text = title,
                    style = if (tabletLandscape) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = BendeyColors.OnSurface,
                )
                BendeyVerticalScrollColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(top = if (tabletLandscape) BendeySpacing.sm else BendeySpacing.md),
                    verticalArrangement = Arrangement.spacedBy(sectionGap),
                ) {
                    if (metaLoading) {
                        Text("Cargando series y métodos…", color = BendeyColors.OnSurfaceVariant)
                    } else {
                        CheckoutHeroTotal(
                            currency = currency,
                            payableTotal = payableTotal,
                            rawTotal = rawTotal,
                            discountAmount = discountAmount,
                            tablet = tabletLandscape,
                        )
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val twoColumns = tabletLandscape && maxWidth >= 480.dp
                            if (twoColumns) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
                                ) {
                                    ContactSelector(
                                        contacts = meta?.contacts.orEmpty(),
                                        selectedId = contactId,
                                        onSelect = onContactChange,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        DocTypeSelector(
                                            series = series,
                                            selectedDocType = docType,
                                            lockedSeries = lockedSeries,
                                            onLockedDocTypeSelect = onLockedDocTypeSelect,
                                            onSelectDocType = { key ->
                                                firstSeriesForDocTypeKey(series, key)?.let { item ->
                                                    onSeriesChange(item.id, item.docType)
                                                }
                                            },
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(sectionGap),
                                ) {
                                    DocTypeSelector(
                                        series = series,
                                        selectedDocType = docType,
                                        onSelectDocType = { key ->
                                            firstSeriesForDocTypeKey(series, key)?.let { item ->
                                                onSeriesChange(item.id, item.docType)
                                            }
                                        },
                                    )
                                    ContactSelector(
                                        contacts = meta?.contacts.orEmpty(),
                                        selectedId = contactId,
                                        onSelect = onContactChange,
                                    )
                                }
                            }
                        }
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
                        extraBeforePayments()
                        PaymentLinesSection(
                            methods = meta?.paymentMethods.orEmpty(),
                            payments = payments,
                            payableTotal = payableTotal,
                            remaining = remaining,
                            onPaymentsChange = onPaymentsChange,
                            tablet = tabletLandscape,
                        )
                        if (!tabletLandscape) {
                            CheckoutPaymentSummary(
                                currency = currency,
                                payableTotal = payableTotal,
                                paidTotal = paidTotal,
                                remaining = remaining,
                                change = change,
                                hasCashPayment = hasCashPayment,
                            )
                        } else if (change > 0.009 && hasCashPayment || !paidCoversTotal(paidTotal, payableTotal)) {
                            CheckoutPaymentSummary(
                                currency = currency,
                                payableTotal = payableTotal,
                                paidTotal = paidTotal,
                                remaining = remaining,
                                change = change,
                                hasCashPayment = hasCashPayment,
                                compact = true,
                            )
                        }
                    }
                    error?.let {
                        Text(it, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (tabletLandscape) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = BendeySpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                    ) {
                        BendeyPrimaryButton(
                            text = if (loading) "Procesando…" else confirmLabel,
                            onClick = onConfirm,
                            enabled = canConfirm,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = BendeySpacing.touchTarget),
                        )
                        BendeyTextButton(
                            text = "Cancelar",
                            onClick = onDismiss,
                            enabled = !loading,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = BendeySpacing.md),
                        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm, Alignment.End),
                    ) {
                        BendeySecondaryButton(
                            text = "Cancelar",
                            onClick = onDismiss,
                            enabled = !loading,
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                        BendeyPrimaryButton(
                            text = if (loading) "Procesando…" else confirmLabel,
                            onClick = onConfirm,
                            enabled = canConfirm,
                            fillWidth = false,
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
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
    lockedSeries: List<DocumentSeries> = emptyList(),
    onLockedDocTypeSelect: () -> Unit = {},
) {
    val groups = remember(series, lockedSeries) {
        groupCheckoutDocTypesWithLocked(unlockedSeries = series, lockedSeries = lockedSeries)
    }
    if (groups.isEmpty()) {
        CheckoutFieldLabel("Comprobante")
        Text("Sin series configuradas", color = BendeyColors.OnSurfaceVariant)
        return
    }
    val selectedKey = normalizeDocTypeKey(selectedDocType)
    CheckoutFieldLabel("Comprobante")
    Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs)) {
        groups.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
            ) {
                row.forEach { group ->
                    val selected = !group.locked && selectedKey == group.key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(BendeyShapeTokens.sm)
                            .border(
                                width = 1.dp,
                                color = when {
                                    group.locked -> BendeyColors.Outline
                                    selected -> CheckoutDocActive
                                    else -> BendeyColors.Outline
                                },
                                shape = BendeyShapeTokens.sm,
                            )
                            .background(if (selected) CheckoutDocActive else BendeyColors.Surface)
                            .clickable {
                                if (group.locked) onLockedDocTypeSelect() else onSelectDocType(group.key)
                            }
                            .padding(horizontal = BendeySpacing.xs, vertical = BendeySpacing.sm),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (group.locked) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = BendeyColors.OnSurfaceVariant,
                                    modifier = Modifier.height(12.dp).width(12.dp),
                                )
                                Text(
                                    text = group.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = BendeyColors.OnSurfaceVariant,
                                    maxLines = 2,
                                )
                            }
                        } else {
                            Text(
                                text = group.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (selected) BendeyColors.OnPrimary else BendeyColors.OnSurface,
                                maxLines = 2,
                            )
                        }
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
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            CheckoutFieldLabel("Serie")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clip(BendeyShapeTokens.md)
                    .border(1.dp, BendeyColors.Outline, BendeyShapeTokens.md)
                    .background(BendeyColors.SurfaceVariant.copy(alpha = 0.55f))
                    .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.sm),
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
            .clip(BendeyShapeTokens.md)
            .border(1.dp, BendeyColors.Outline, BendeyShapeTokens.md)
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
                color = BendeyColors.OnPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.sm),
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
    tablet: Boolean = false,
) {
    val defaultMethod = methods.firstOrNull()?.code ?: "cash"
    val methodOptions = if (methods.isEmpty()) {
        listOf(PaymentMethodOption(id = 0, name = "Efectivo", code = "cash", destinationType = "cash", active = true))
    } else {
        methods
    }

    CheckoutFieldLabel(
        title = "Métodos de pago",
        emphasized = tablet,
    )
    if (remaining > 0.009 && payments.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(BendeyShapeTokens.md)
                .background(BendeyColors.PrimaryContainer.copy(alpha = 0.35f))
                .border(1.dp, BendeyColors.Primary.copy(alpha = 0.25f), BendeyShapeTokens.md)
                .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.xs),
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
            tablet = tablet,
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
        modifier = if (tablet) Modifier.fillMaxWidth(0.6f) else Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = BendeySpacing.xxs))
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
    tablet: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (tablet) Modifier else Modifier.clipSection()),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
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
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
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
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
            shape = BendeyShapeTokens.sm,
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
            modifier = Modifier.padding(bottom = BendeySpacing.xxs),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.fillMaxWidth(),
            shape = BendeyShapeTokens.sm,
            textStyle = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun CheckoutHeroTotal(
    currency: NumberFormat,
    payableTotal: Double,
    rawTotal: Double,
    discountAmount: Double,
    tablet: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BendeyShapeTokens.xl)
            .background(BendeyColors.PrimaryContainer.copy(alpha = if (tablet) 0.38f else 0.45f))
            .border(1.dp, BendeyColors.Primary.copy(alpha = 0.15f), BendeyShapeTokens.xl)
            .padding(
                horizontal = if (tablet) BendeySpacing.sm else BendeySpacing.md,
                vertical = if (tablet) BendeySpacing.xs else BendeySpacing.sm,
            ),
    ) {
        Text(
            text = "Total a cobrar",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = BendeyColors.OnSurfaceVariant,
        )
        Text(
            text = currency.format(payableTotal),
            style = if (tablet) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = BendeyColors.Primary,
            modifier = Modifier.padding(top = BendeySpacing.xxs),
        )
        if (discountAmount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = BendeySpacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Subtotal ${currency.format(rawTotal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
                Text(
                    "− ${currency.format(discountAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CheckoutPaymentSummary(
    currency: NumberFormat,
    payableTotal: Double,
    paidTotal: Double,
    remaining: Double,
    change: Double,
    hasCashPayment: Boolean,
    compact: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (compact) {
                    Modifier.padding(top = BendeySpacing.xxs)
                } else {
                    Modifier
                        .clip(BendeyShapeTokens.md)
                        .background(BendeyColors.SurfaceVariant.copy(alpha = 0.35f))
                        .border(1.dp, BendeyColors.Outline.copy(alpha = 0.35f), BendeyShapeTokens.md)
                        .padding(BendeySpacing.sm)
                },
            ),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
    ) {
        if (!compact) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Monto recibido",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = BendeyColors.OnSurfaceVariant,
                )
                Text(
                    currency.format(paidTotal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        if (change > 0.009 && hasCashPayment) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(BendeyShapeTokens.sm)
                    .background(BendeyColors.WarningContainer)
                    .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Vuelto",
                    fontWeight = FontWeight.Bold,
                    color = BendeyColors.OnWarning,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    currency.format(change),
                    fontWeight = FontWeight.Bold,
                    color = BendeyColors.OnWarning,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
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
    modifier: Modifier = Modifier,
) {
    if (contacts.isEmpty()) {
        CheckoutFieldLabel("Cliente")
        Text("Sin clientes", color = BendeyColors.OnSurfaceVariant, modifier = modifier)
        return
    }
    BendeySearchableSelect(
        options = contacts.map { BendeySelectOption(it.id, it.displayLabel) },
        selectedId = selectedId,
        onSelect = onSelect,
        label = "Cliente",
        placeholder = "Buscar cliente…",
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun CheckoutFieldLabel(title: String, emphasized: Boolean = false) {
    Text(
        title,
        style = if (emphasized) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = BendeyColors.OnSurfaceVariant,
        modifier = Modifier.padding(bottom = BendeySpacing.xxs),
    )
}

@Composable
private fun Modifier.clipSection(): Modifier = this
    .clip(BendeyShapeTokens.md)
    .background(BendeyColors.SurfaceVariant.copy(alpha = 0.35f))
    .border(1.dp, BendeyColors.Outline.copy(alpha = 0.45f), BendeyShapeTokens.md)
    .padding(BendeySpacing.sm)

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
