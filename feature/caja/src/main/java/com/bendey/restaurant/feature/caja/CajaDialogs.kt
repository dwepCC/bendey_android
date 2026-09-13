package com.bendey.restaurant.feature.caja

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.cash.CashSessionReport
import com.bendey.restaurant.core.domain.sales.salePaymentMethodLabelEs
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyHorizontalScrollRow
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import com.bendey.restaurant.core.ui.components.BendeyTextField
import java.text.NumberFormat

/**
 * Contenido del arqueo — SIN su propio scroll: quien lo use (ArqueoDialog, CloseCashDialog) ya
 * vive dentro del área de scroll de un BendeyFormDialog; anidar otro scroll adentro rompía el
 * gesto (el cajero arrastraba y no se sabía qué contenedor se movía).
 */
@Composable
fun ArqueoDialogContent(
    values: Map<String, Int>,
    expectedBalance: Double,
    currency: NumberFormat,
    onQtyChange: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = sumArqueo(values)
    val diff = total - expectedBalance
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Saldo sistema", color = BendeyColors.OnSurfaceVariant)
            Text(currency.format(expectedBalance), fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total contado", fontWeight = FontWeight.SemiBold)
            Text(currency.format(total), fontWeight = FontWeight.Bold, color = BendeyColors.Primary)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Diferencia")
            Text(
                currency.format(diff),
                color = if (kotlin.math.abs(diff) < 0.01) BendeyColors.Success else BendeyColors.Error,
                fontWeight = FontWeight.SemiBold,
            )
        }
        HorizontalDivider()
        ArqueoSection("Billetes", ARQUEO_DENOMINATIONS.filter { it.kind == ArqueoKind.BILL }, values, currency, onQtyChange)
        ArqueoSection("Monedas", ARQUEO_DENOMINATIONS.filter { it.kind == ArqueoKind.COIN }, values, currency, onQtyChange)
    }
}

/**
 * Fila DENOMINACIÓN | CANTIDAD | SUBTOTAL — antes cada fila repetía un BendeyTextField completo
 * con su propia etiqueta flotante "Cant.", 13 veces: pesado para una interfaz que un cajero usa
 * para contar dinero rápido. Ahora la columna se rotula una sola vez (encabezado de sección) y
 * cada fila solo lleva la caja de cantidad — compacta, claramente editable — y el subtotal como
 * texto plano, sin borde, para que no se confunda con otro campo.
 */
@Composable
private fun ArqueoSection(
    title: String,
    items: List<ArqueoDenomination>,
    values: Map<String, Int>,
    currency: NumberFormat,
    onQtyChange: (String, Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("", modifier = Modifier.weight(1f))
            Text(
                "Cant.",
                style = MaterialTheme.typography.labelSmall,
                color = BendeyColors.OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(ArqueoQtyFieldWidth),
            )
            Text(
                "Subtotal",
                style = MaterialTheme.typography.labelSmall,
                color = BendeyColors.OnSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.width(84.dp).padding(start = BendeySpacing.xs),
            )
        }
        items.forEach { denom ->
            val qty = values[denom.value] ?: 0
            val subtotal = denom.value.toDouble() * qty
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(denom.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                ArqueoQtyField(qty = qty, onCommit = { n -> onQtyChange(denom.value, n) })
                Text(
                    currency.format(subtotal),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (qty > 0) BendeyColors.OnSurface else BendeyColors.OnSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(84.dp).padding(start = BendeySpacing.xs),
                )
            }
        }
    }
}

private val ArqueoQtyFieldWidth = 56.dp

/**
 * Campo de cantidad del arqueo — una caja numérica compacta con borde propio en vez de un
 * BendeyTextField completo (que trae una etiqueta flotante y ~56dp de alto pensados para un
 * formulario normal, no para 13 filas repetidas). Es una excepción documentada: el patrón oficial
 * de campo de texto sigue siendo BendeyTextField, este es el caso "fila muy densa, repetida
 * muchas veces" donde ese patrón se vuelve pesado.
 *
 * Al enfocar limpia el valor para escribir libremente (sin pelear con el 0). Al salir: si
 * escribió algo lo guarda; si lo dejó vacío, restaura el valor anterior.
 */
@Composable
private fun ArqueoQtyField(qty: Int, onCommit: (Int) -> Unit) {
    var text by remember(qty) { mutableStateOf(qty.toString()) }
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = text,
        onValueChange = { v -> text = v.filter { it.isDigit() }.take(4) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = BendeyColors.OnSurface,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(BendeyColors.Primary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        modifier = Modifier
            .width(ArqueoQtyFieldWidth)
            .height(40.dp)
            .onFocusChanged { focusState ->
                if (focusState.isFocused && !focused) {
                    focused = true
                    text = ""
                } else if (!focusState.isFocused && focused) {
                    focused = false
                    if (text.isBlank()) {
                        text = qty.toString() // sin cambios → restaura el valor anterior
                    } else {
                        onCommit(text.toIntOrNull()?.coerceAtLeast(0) ?: 0)
                    }
                }
            },
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(BendeyShapeTokens.sm)
                    .border(1.dp, if (focused) BendeyColors.Primary else BendeyColors.Outline, BendeyShapeTokens.sm)
                    .background(BendeyColors.Surface),
                contentAlignment = Alignment.Center,
            ) {
                innerTextField()
            }
        },
    )
}

@Composable
fun ArqueoDialog(
    open: Boolean,
    values: Map<String, Int>,
    expectedBalance: Double,
    loading: Boolean,
    currency: NumberFormat,
    canPrint: Boolean,
    docBusy: Boolean,
    error: String? = null,
    onQtyChange: (String, Int) -> Unit,
    onExportPdf: () -> Unit,
    onPrint: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!open) return
    // Antes: BendeyAlertDialog (AlertDialog de Material3, ancho angosto de diálogo estándar) —
    // demasiado angosto para un formulario de 13 filas de denominaciones. BendeyFormDialog usa
    // ~94% del ancho por defecto, exactamente el rango que pidió la prueba manual.
    BendeyFormDialog(
        onDismissRequest = onDismiss,
        title = "Arqueo de caja",
        confirmText = if (loading) "Guardando…" else "Guardar",
        confirmEnabled = !loading,
        enableContentScroll = true,
        validationError = error,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    ) {
        ArqueoDialogContent(values, expectedBalance, currency, onQtyChange)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
        ) {
            BendeyTextButton(
                text = if (docBusy) "Generando…" else "Descargar PDF",
                onClick = onExportPdf,
                enabled = !docBusy,
            )
            if (canPrint) {
                BendeyTextButton(
                    text = "Imprimir",
                    onClick = onPrint,
                    enabled = !docBusy,
                )
            }
        }
    }
}

@Composable
fun CloseCashDialog(
    form: CloseCashForm,
    expectedBalance: Double,
    loading: Boolean,
    currency: NumberFormat,
    operationalStatus: com.bendey.restaurant.core.domain.restaurant.BranchOperationalStatus? = null,
    salesSummary: CashSessionReport? = null,
    salesSummaryLoading: Boolean = false,
    error: String? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onFormChange: ((CloseCashForm) -> CloseCashForm) -> Unit,
    onArqueoQtyChange: (String, Int) -> Unit,
) {
    // Antes: BendeyAlertDialog (ancho angosto de diálogo estándar) con todo el contenido — resumen
    // de ventas, alerta operativa, arqueo completo — apretado en ese ancho. BendeyFormDialog da el
    // mismo ~94% de ancho que ArqueoDialog, con scroll propio (por eso el contenido ya no necesita
    // su propio Column envolvente con scroll anidado).
    //
    // `error` viajaba en el uiState pero nunca llegaba a este diálogo — cuando el cierre fallaba
    // (backend rechaza, sesión con arqueo inválido, etc.) el ViewModel sí guardaba el mensaje, pero
    // el único lugar que lo pintaba era un Text() en el fondo de la pantalla, TAPADO por este mismo
    // Dialog. El cajero veía el botón volver a "Cerrar" sin ninguna explicación — parecía que la
    // app se quedó pegada. Con `validationError` el mensaje aparece dentro del propio modal.
    BendeyFormDialog(
        onDismissRequest = onDismiss,
        title = "Cerrar caja",
        confirmText = if (loading) "Cerrando…" else "Cerrar",
        confirmEnabled = !loading,
        enableContentScroll = true,
        validationError = error,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {
                Text(
                    "Revise el resumen. Puede cerrar con arqueo para registrar el efectivo contado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
                // Sin esto el cajero cerraba a ciegas: el diálogo solo mostraba el saldo esperado,
                // nunca cuánto se vendió ni por qué método — justo lo que un tenant reportó que
                // faltaba al cerrar.
                if (salesSummaryLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(vertical = 4.dp))
                } else salesSummary?.let { report ->
                    Text("Ventas de esta sesión", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Ventas netas", style = MaterialTheme.typography.bodySmall)
                        Text(currency.format(report.totalNetSales), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                    if (report.totalVoidedSales > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Ventas anuladas", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                            Text(currency.format(report.totalVoidedSales), style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                        }
                    }
                    report.salesByMethod.forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(salePaymentMethodLabelEs(row.method), style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                            Text(currency.format(row.total), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    HorizontalDivider(color = BendeyColors.Outline.copy(alpha = 0.35f))
                }
                operationalStatus?.takeIf { it.hasActiveOperations }?.let { op ->
                    Text(
                        buildString {
                            append("Advertencia: hay operaciones activas.")
                            if (op.openTablesCount > 0) append(" ${op.openTablesCount} mesa(s) abiertas.")
                            if (op.openSessionsCount > 0) append(" ${op.openSessionsCount} sesión(es).")
                            if (op.activeComandasCount > 0) append(" ${op.activeComandasCount} comanda(s) activas.")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.Warning,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Saldo sistema")
                    Text(currency.format(expectedBalance), fontWeight = FontWeight.Bold)
                }
                BendeyHorizontalScrollRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BendeyFilterChip(
                        selected = form.useArqueo,
                        onClick = { onFormChange { it.copy(useArqueo = true) } },
                        text = "Con arqueo",
                    )
                    BendeyFilterChip(
                        selected = !form.useArqueo,
                        onClick = { onFormChange { it.copy(useArqueo = false) } },
                        text = "Sin arqueo",
                    )
                }
                if (form.useArqueo) {
                    ArqueoDialogContent(form.arqueo, expectedBalance, currency, onArqueoQtyChange)
                } else {
                    BendeyTextField(
                        value = form.closingBalance,
                        onValueChange = { v -> onFormChange { it.copy(closingBalance = v) } },
                        label = "Efectivo contado (S/)",
                    )
                }
                BendeyTextField(
                    value = form.notes,
                    onValueChange = { v -> onFormChange { it.copy(notes = v) } },
                    label = "Notas de cierre",
                    singleLine = false,
                )
        }
    }
}
