package com.bendey.restaurant.feature.repartidores

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.delivery.DELIVERY_REASON_OTHER
import com.bendey.restaurant.core.domain.delivery.DeliveryAction
import com.bendey.restaurant.core.domain.delivery.DeliveryBoardData
import com.bendey.restaurant.core.domain.delivery.DeliveryBoardDriver
import com.bendey.restaurant.core.domain.delivery.DeliveryCard
import com.bendey.restaurant.core.domain.delivery.DeliveryChipTone
import com.bendey.restaurant.core.domain.delivery.DeliveryCopy
import com.bendey.restaurant.core.domain.delivery.DeliveryReasonKind
import com.bendey.restaurant.core.domain.delivery.DeliverySection
import com.bendey.restaurant.core.domain.delivery.DeliveryTone
import com.bendey.restaurant.core.domain.delivery.deliveryAcceptAlertLabel
import com.bendey.restaurant.core.domain.delivery.deliveryActionLabel
import com.bendey.restaurant.core.domain.delivery.deliveryAssignmentLabel
import com.bendey.restaurant.core.domain.delivery.deliveryBoardCounts
import com.bendey.restaurant.core.domain.delivery.deliveryCardActions
import com.bendey.restaurant.core.domain.delivery.deliveryCardClock
import com.bendey.restaurant.core.domain.delivery.deliveryDriverChoice
import com.bendey.restaurant.core.domain.delivery.deliveryKitchenLabel
import com.bendey.restaurant.core.domain.delivery.deliveryKitchenTone
import com.bendey.restaurant.core.domain.delivery.deliveryNeedsAttention
import com.bendey.restaurant.core.domain.delivery.deliveryReasonError
import com.bendey.restaurant.core.domain.delivery.deliveryResolveReason
import com.bendey.restaurant.core.domain.delivery.deliverySectionCards
import com.bendey.restaurant.core.domain.delivery.deliverySectionEmpty
import com.bendey.restaurant.core.domain.delivery.deliverySectionLabel
import com.bendey.restaurant.core.domain.delivery.deliverySortDrivers
import com.bendey.restaurant.core.domain.delivery.deliverySourceLabel
import com.bendey.restaurant.core.domain.delivery.deliveryTelUri
import com.bendey.restaurant.core.domain.time.PeruDateTime
import com.bendey.restaurant.core.realtime.delivery.DeliveryBoardState
import com.bendey.restaurant.core.ui.components.BendeyAlert
import com.bendey.restaurant.core.ui.components.BendeyAlertAction
import com.bendey.restaurant.core.ui.components.BendeyAlertDialog
import com.bendey.restaurant.core.ui.components.BendeyAlertSeverity
import com.bendey.restaurant.core.ui.components.BendeyBottomSheet
import com.bendey.restaurant.core.ui.components.BendeyDestructiveButton
import com.bendey.restaurant.core.ui.components.BendeyIconButton
import com.bendey.restaurant.core.ui.components.BendeyLoadError
import com.bendey.restaurant.core.ui.components.BendeyOutlinedButton
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.components.BendeySkeletonListRow
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import com.bendey.restaurant.core.ui.components.BendeyTextField
import kotlinx.coroutines.delay
import java.time.Instant
import java.util.Locale

/** Ancho desde el que las 5 secciones van en columnas (tablet horizontal); por debajo, pestañas con contador. */
private val WIDE_LAYOUT_MIN_WIDTH = 840.dp

/** Cada cuánto avanza el "12 min" sin pedir nada al servidor (el tablero se refresca por tiempo real). */
internal const val DELIVERY_CLOCK_TICK_MS = 30_000L

/** Reloj de la vista: solo recalcula semáforos y "hace N min" mientras la pantalla está a la vista. */
@Composable
internal fun rememberDeliveryNow(): Instant {
    var now by remember { mutableStateOf(Instant.now()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            now = Instant.now()
            while (true) {
                delay(DELIVERY_CLOCK_TICK_MS)
                now = Instant.now()
            }
        }
    }
    return now
}

/**
 * Entregas (D1): la vista Delivery completa. Secciones Por asignar · Asignados · En camino · Incidencias ·
 * Entregados hoy desde `GET /api/restaurant/delivery/board`; con `d.u` se asigna, cancela y se marca
 * entregado/fallido; sin `d.u` es de solo lectura.
 */
@Composable
fun EntregasScreen(
    modifier: Modifier = Modifier,
    onShowMessage: (String) -> Unit = {},
    viewModel: DeliveryViewModel = hiltViewModel(),
) {
    val board by viewModel.board.collectAsStateWithLifecycle()
    Column(modifier.fillMaxSize()) {
        BendeyScreenToolbar(
            title = DeliveryCopy.text("title"),
            subtitle = DeliveryCopy.text("subtitle"),
            actions = {
                BendeyIconButton(
                    onClick = viewModel::refresh,
                    icon = Icons.Default.Refresh,
                    contentDescription = DeliveryCopy.text("action.refresh"),
                    enabled = !board.loading,
                )
            },
        )
        DeliveryContent(viewModel = viewModel, onShowMessage = onShowMessage, modifier = Modifier.weight(1f))
    }
}

/** Cuerpo de la vista Delivery (lo comparte la pantalla Entregas y la pestaña de Mi negocio > Repartidores). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryContent(
    viewModel: DeliveryViewModel,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.board.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val now = rememberDeliveryNow()

    LaunchedEffect(viewModel) { viewModel.messages.collect { onShowMessage(it) } }

    PullToRefreshBox(isRefreshing = state.loading && state.board != null, onRefresh = viewModel::refresh, modifier = modifier.fillMaxSize()) {
        val data = state.board
        when {
            data == null && !state.loaded -> Column(
                Modifier.fillMaxSize().padding(BendeySpacing.s16),
                verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
            ) { repeat(3) { BendeySkeletonListRow() } }
            // R5: un fallo de carga nunca se muestra como "no hay pedidos".
            data == null -> BendeyLoadError(onRetry = viewModel::refresh, message = DeliveryCopy.text("load_error"))
            else -> DeliveryBoardBody(state = state, data = data, ui = ui, now = now, viewModel = viewModel)
        }
    }

    DeliveryDialogs(ui = ui, board = state.board, viewModel = viewModel)
}

@Composable
private fun DeliveryBoardBody(
    state: DeliveryBoardState,
    data: DeliveryBoardData,
    ui: DeliveryUiState,
    now: Instant,
    viewModel: DeliveryViewModel,
) {
    val counts = remember(data) { deliveryBoardCounts(data) }
    val context = LocalContext.current
    val callbacks = remember(viewModel, context) {
        CardCallbacks(
            onAssign = viewModel::openAssign,
            onCancel = viewModel::openCancel,
            onDelivered = viewModel::openDelivered,
            onFailed = viewModel::openFailed,
            onCall = { phone -> dial(context, phone) },
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= WIDE_LAYOUT_MIN_WIDTH
        Column(Modifier.fillMaxSize()) {
            if (state.error) {
                BendeyAlert(
                    message = DeliveryCopy.text("load_error"),
                    severity = BendeyAlertSeverity.Warning,
                    primaryAction = BendeyAlertAction(DeliveryCopy.text("action.refresh"), viewModel::refresh),
                    modifier = Modifier.padding(horizontal = BendeySpacing.s16, vertical = BendeySpacing.s4),
                )
            }
            if (!ui.canAssign) {
                Text(
                    DeliveryCopy.text("readonly.hint"),
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = BendeySpacing.s16, vertical = BendeySpacing.s4),
                )
            }
            if (wide) {
                // Tablet horizontal: las 5 secciones en columnas y los repartidores en una franja arriba.
                DriversStrip(data.drivers)
                Row(
                    Modifier.weight(1f).fillMaxWidth().padding(horizontal = BendeySpacing.s8),
                    horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
                ) {
                    DeliverySection.entries.forEach { section ->
                        Column(Modifier.weight(1f).fillMaxSize()) {
                            Text(
                                "${deliverySectionLabel(section)} · ${counts[section]}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = BendeyColors.OnSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(vertical = BendeySpacing.s8),
                            )
                            SectionList(section, data, ui, now, callbacks, Modifier.weight(1f))
                        }
                    }
                }
            } else {
                // Teléfono y tablet vertical: pestañas (chips con contador) y UNA lista a la vez.
                SectionTabs(
                    selected = ui.selected,
                    showDrivers = ui.showDrivers,
                    counts = counts,
                    driversCount = data.drivers.size,
                    onSelect = viewModel::selectSection,
                    onDrivers = viewModel::selectDrivers,
                )
                if (ui.showDrivers) {
                    DriversList(data.drivers, Modifier.weight(1f))
                } else {
                    SectionList(ui.selected, data, ui, now, callbacks, Modifier.weight(1f))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Pestañas, listas y panel de repartidores
// ---------------------------------------------------------------------------------------------

@Composable
private fun SectionTabs(
    selected: DeliverySection,
    showDrivers: Boolean,
    counts: com.bendey.restaurant.core.domain.delivery.DeliveryBoardCounts,
    driversCount: Int,
    onSelect: (DeliverySection) -> Unit,
    onDrivers: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = BendeySpacing.s16, vertical = BendeySpacing.s8),
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DeliverySection.entries.forEach { section ->
            BendeyFilterChip(
                selected = !showDrivers && selected == section,
                onClick = { onSelect(section) },
                text = "${deliverySectionLabel(section)} (${counts[section]})",
            )
        }
        BendeyFilterChip(
            selected = showDrivers,
            onClick = onDrivers,
            text = "${DeliveryCopy.text("drivers.title")} ($driversCount)",
        )
    }
}

@Composable
private fun SectionList(
    section: DeliverySection,
    data: DeliveryBoardData,
    ui: DeliveryUiState,
    now: Instant,
    callbacks: CardCallbacks,
    modifier: Modifier = Modifier,
) {
    val cards = remember(data, section) { deliverySectionCards(data, section) }
    if (cards.isEmpty()) {
        Text(
            deliverySectionEmpty(section),
            style = MaterialTheme.typography.bodyMedium,
            color = BendeyColors.OnSurfaceVariant,
            modifier = modifier.fillMaxWidth().padding(BendeySpacing.s16),
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = BendeySpacing.s16, vertical = BendeySpacing.s8),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.s12),
    ) {
        items(cards, key = { "${section.key}-${it.sessionId}" }) { card ->
            DeliveryCardView(card = card, section = section, now = now, canAssign = ui.canAssign, busy = ui.busy, callbacks = callbacks)
        }
    }
}

@Composable
private fun DriversList(drivers: List<DeliveryBoardDriver>, modifier: Modifier = Modifier) {
    if (drivers.isEmpty()) {
        Text(
            DeliveryCopy.text("drivers.empty"),
            style = MaterialTheme.typography.bodyMedium,
            color = BendeyColors.OnSurfaceVariant,
            modifier = modifier.fillMaxWidth().padding(BendeySpacing.s16),
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = BendeySpacing.s16, vertical = BendeySpacing.s8),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
    ) {
        items(deliverySortDrivers(drivers), key = { it.id }) { driver -> DriverInfoRow(driver) }
    }
}

/** Panel de repartidores en tablet: franja horizontal con disponibilidad y pedidos activos (solo lectura). */
@Composable
private fun DriversStrip(drivers: List<DeliveryBoardDriver>) {
    Column(Modifier.fillMaxWidth().padding(horizontal = BendeySpacing.s16, vertical = BendeySpacing.s4)) {
        Text(
            DeliveryCopy.text("drivers.title"),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = BendeyColors.OnSurfaceVariant,
        )
        if (drivers.isEmpty()) {
            Text(DeliveryCopy.text("drivers.empty"), style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
        } else {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = BendeySpacing.s4),
                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
            ) {
                deliverySortDrivers(drivers).forEach { d ->
                    val choice = deliveryDriverChoice(d)
                    BendeyStatusChip(
                        label = "${d.name} · ${choice.activeLabel}",
                        accentColor = if (d.isAvailable) BendeyColors.Success else BendeyColors.OnSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DriverInfoRow(driver: DeliveryBoardDriver) {
    val choice = deliveryDriverChoice(driver)
    Row(
        Modifier
            .fillMaxWidth()
            .background(BendeyColors.Surface, BendeyShapeTokens.md)
            .border(1.dp, BendeyColors.Outline, BendeyShapeTokens.md)
            .padding(BendeySpacing.s12),
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(driver.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = BendeyColors.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val meta = listOf(driver.vehicleType, driver.phone).filter { it.isNotBlank() }.joinToString(" · ")
            if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(choice.activeLabel, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
        }
        BendeyStatusChip(
            label = DeliveryCopy.text(if (driver.isAvailable) "driver.available" else "driver.unavailable"),
            accentColor = if (driver.isAvailable) BendeyColors.Success else BendeyColors.OnSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Tarjeta
// ---------------------------------------------------------------------------------------------

internal class CardCallbacks(
    val onAssign: (DeliveryCard) -> Unit,
    val onCancel: (DeliveryCard) -> Unit,
    val onDelivered: (DeliveryCard) -> Unit,
    val onFailed: (DeliveryCard) -> Unit,
    val onCall: (String) -> Unit,
)

private fun toneColor(tone: DeliveryTone): Color = when (tone) {
    DeliveryTone.OK -> BendeyColors.Success
    DeliveryTone.WARN -> BendeyColors.Warning
    DeliveryTone.DANGER -> BendeyColors.Error
    DeliveryTone.NONE -> BendeyColors.OnSurfaceVariant
}

private fun chipColor(tone: DeliveryChipTone): Color = when (tone) {
    DeliveryChipTone.SUCCESS -> BendeyColors.Success
    DeliveryChipTone.WARNING -> BendeyColors.Warning
    DeliveryChipTone.DANGER -> BendeyColors.Error
    DeliveryChipTone.INFO -> BendeyColors.Info
    DeliveryChipTone.BRAND -> BendeyColors.Primary
    DeliveryChipTone.NEUTRAL -> BendeyColors.OnSurfaceVariant
}

private fun money(value: Double) = String.format(Locale.US, "S/ %.2f", value)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeliveryCardView(
    card: DeliveryCard,
    section: DeliverySection,
    now: Instant,
    canAssign: Boolean,
    busy: Boolean,
    callbacks: CardCallbacks,
) {
    val clock = deliveryCardClock(card, section, now)
    val attention = deliveryNeedsAttention(card, section, now)
    val borderColor = when (clock.overall) {
        DeliveryTone.DANGER -> BendeyColors.Error
        DeliveryTone.WARN -> BendeyColors.Warning
        else -> if (attention) BendeyColors.Warning else BendeyColors.Outline
    }
    val actions = deliveryCardActions(card, section, canAssign)

    Column(
        Modifier
            .fillMaxWidth()
            .background(BendeyColors.Surface, BendeyShapeTokens.md)
            .border(if (clock.overall == DeliveryTone.NONE && !attention) 1.dp else 2.dp, borderColor, BendeyShapeTokens.md)
            .padding(BendeySpacing.s12),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
    ) {
        // Origen + estado de cocina + pagado.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8), verticalArrangement = Arrangement.spacedBy(BendeySpacing.s4)) {
            BendeyStatusChip(deliverySourceLabel(card.source), BendeyColors.Primary)
            if (section != DeliverySection.DELIVERED_TODAY) {
                BendeyStatusChip(deliveryKitchenLabel(card.orderStatus), chipColor(deliveryKitchenTone(card.orderStatus)))
            }
            if (card.paid) BendeyStatusChip(DeliveryCopy.text("chip.paid"), BendeyColors.Success)
        }

        // Cliente (a la izquierda, con weight) + tiempo transcurrido con semáforo (a la derecha).
        Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    card.customerName.ifBlank { "Sin nombre" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = BendeyColors.OnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    card.customerPhone.ifBlank { DeliveryCopy.text("chip.no_phone") },
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
            if (clock.label.isNotEmpty()) {
                if (section == DeliverySection.DELIVERED_TODAY) {
                    Text(
                        PeruDateTime.formatTimeOrRaw(card.deliveredAt),
                        style = MaterialTheme.typography.labelLarge,
                        color = BendeyColors.OnSurfaceVariant,
                    )
                } else {
                    BendeyStatusChip(clock.label, toneColor(clock.tone))
                }
            }
        }

        if (card.address.isNotBlank()) {
            Text(card.address, style = MaterialTheme.typography.bodyMedium, color = BendeyColors.OnSurface)
        }
        if (card.reference.isNotBlank()) {
            Text(card.reference, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
        }
        Text(
            "${if (card.itemsCount == 1) "1 producto" else "${card.itemsCount} productos"} · ${money(card.totalAmount)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = BendeyColors.OnSurface,
        )

        // Repartidor + su estado de asignación + llamarlo.
        card.driver?.let { driver ->
            Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(driver.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = BendeyColors.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val statusLabel = deliveryAssignmentLabel(card.assignmentStatus)
                    val line = listOf(driver.phone, statusLabel).filter { it.isNotBlank() }.joinToString(" · ")
                    if (line.isNotEmpty()) Text(line, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                }
                if (deliveryTelUri(driver.phone).isNotEmpty()) {
                    BendeyTextButton(
                        text = DeliveryCopy.text("action.call"),
                        onClick = { callbacks.onCall(driver.phone) },
                        modifier = Modifier.heightIn(min = BendeySpacing.touchMin),
                    )
                }
            }
        }

        val acceptAlert = deliveryAcceptAlertLabel(clock.accept)
        if (acceptAlert.isNotEmpty()) {
            Text(acceptAlert, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = if (clock.accept == DeliveryTone.DANGER) BendeyColors.ErrorText else BendeyColors.WarningText)
        }

        // Incidencias: motivo + cuándo.
        card.incident?.let { incident ->
            Column(
                Modifier.fillMaxWidth().background(BendeyColors.ErrorContainer, BendeyShapeTokens.sm).padding(BendeySpacing.s8),
            ) {
                Text(
                    DeliveryCopy.text("incident.${incident.kind}").ifBlank { DeliveryCopy.text("assignment.failed") },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = BendeyColors.OnErrorContainer,
                )
                if (incident.reason.isNotBlank()) Text(incident.reason, style = MaterialTheme.typography.bodyMedium, color = BendeyColors.OnErrorContainer)
                val whenText = PeruDateTime.formatDateTime(incident.at)
                if (whenText != null) Text(whenText, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnErrorContainer)
            }
        }

        if (actions.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8), verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
                actions.forEach { action ->
                    val label = deliveryActionLabel(action)
                    val mod = Modifier.heightIn(min = BendeySpacing.touchMin)
                    when (action) {
                        DeliveryAction.ASSIGN, DeliveryAction.DELIVERED -> BendeyPrimaryButton(
                            text = label, onClick = {
                                if (action == DeliveryAction.ASSIGN) callbacks.onAssign(card) else callbacks.onDelivered(card)
                            },
                            modifier = mod, enabled = !busy, fillWidth = false,
                        )
                        DeliveryAction.REASSIGN -> BendeyOutlinedButton(label, { callbacks.onAssign(card) }, mod, enabled = !busy)
                        DeliveryAction.CALL -> BendeyOutlinedButton(label, { callbacks.onCall(card.customerPhone) }, mod)
                        DeliveryAction.FAILED -> BendeyOutlinedButton(label, { callbacks.onFailed(card) }, mod, enabled = !busy)
                        DeliveryAction.CANCEL -> BendeyTextButton(
                            text = label, onClick = { callbacks.onCancel(card) },
                            modifier = mod, enabled = !busy, textColor = BendeyColors.ErrorText,
                        )
                    }
                }
            }
        }
    }
}

private fun dial(context: Context, phone: String) {
    val uri = deliveryTelUri(phone)
    if (uri.isEmpty()) return
    try {
        // ACTION_DIAL abre el marcador con el número escrito: no necesita permiso ni llama solo.
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        /* tableta sin marcador: no hay nada que hacer */
    }
}

// ---------------------------------------------------------------------------------------------
// Diálogos: asignar, cancelar, fallido, entregado
// ---------------------------------------------------------------------------------------------

@Composable
private fun DeliveryDialogs(ui: DeliveryUiState, board: DeliveryBoardData?, viewModel: DeliveryViewModel) {
    when (val dialog = ui.dialog) {
        null -> Unit
        is DeliveryDialog.Assign -> AssignSheet(dialog.card, board?.drivers.orEmpty(), ui, viewModel)
        is DeliveryDialog.Cancel -> ReasonDialog(
            title = DeliveryCopy.text("cancel.title"),
            card = dialog.card,
            quick = DeliveryCopy.cancelQuickReasons,
            kind = DeliveryReasonKind.CANCEL,
            confirmLabel = DeliveryCopy.text("action.cancel"),
            ui = ui,
            viewModel = viewModel,
            onConfirm = viewModel::confirmCancel,
        )
        is DeliveryDialog.Failed -> ReasonDialog(
            title = DeliveryCopy.text("failed.title"),
            card = dialog.card,
            quick = DeliveryCopy.failedQuickReasons,
            kind = DeliveryReasonKind.FAILED,
            confirmLabel = DeliveryCopy.text("action.failed"),
            ui = ui,
            viewModel = viewModel,
            onConfirm = viewModel::confirmFailed,
        )
        is DeliveryDialog.Delivered -> BendeyAlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            title = { Text(DeliveryCopy.text("action.delivered")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
                    Text(dialog.card.customerName.ifBlank { "Sin nombre" }, fontWeight = FontWeight.SemiBold)
                    Text(DeliveryCopy.text("delivered.confirm"))
                    ui.dialogError?.let { Text(it, color = BendeyColors.ErrorText, style = MaterialTheme.typography.bodyMedium) }
                }
            },
            confirmButton = {
                BendeyPrimaryButton(
                    text = DeliveryCopy.text("action.delivered"),
                    onClick = viewModel::confirmDelivered,
                    enabled = !ui.busy,
                    loading = ui.busy,
                    fillWidth = false,
                )
            },
            dismissButton = { BendeyTextButton(text = "Volver", onClick = viewModel::dismissDialog, enabled = !ui.busy) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReasonDialog(
    title: String,
    card: DeliveryCard,
    quick: List<String>,
    kind: DeliveryReasonKind,
    confirmLabel: String,
    ui: DeliveryUiState,
    viewModel: DeliveryViewModel,
    onConfirm: () -> Unit,
) {
    val resolved = deliveryResolveReason(ui.reasonChoice, ui.reasonText)
    val error = if (ui.reasonShowError) deliveryReasonError(resolved, kind) else null
    BendeyAlertDialog(
        onDismissRequest = viewModel::dismissDialog,
        title = { Text(title) },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
            ) {
                Text(card.customerName.ifBlank { "Sin nombre" }, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8), verticalArrangement = Arrangement.spacedBy(BendeySpacing.s4)) {
                    quick.forEach { reason ->
                        BendeyFilterChip(selected = ui.reasonChoice == reason, onClick = { viewModel.chooseReason(reason) }, text = reason)
                    }
                    BendeyFilterChip(
                        selected = ui.reasonChoice == DELIVERY_REASON_OTHER,
                        onClick = { viewModel.chooseReason(DELIVERY_REASON_OTHER) },
                        text = DeliveryCopy.text("cancel.reason_other"),
                    )
                }
                if (ui.reasonChoice == DELIVERY_REASON_OTHER) {
                    BendeyTextField(
                        value = ui.reasonText,
                        onValueChange = viewModel::setReasonText,
                        label = "Motivo",
                        singleLine = false,
                    )
                }
                (error ?: ui.dialogError)?.let { Text(it, color = BendeyColors.ErrorText, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = {
            BendeyDestructiveButton(text = confirmLabel, onClick = onConfirm, enabled = !ui.busy, loading = ui.busy, fillWidth = false)
        },
        dismissButton = { BendeyTextButton(text = "Volver", onClick = viewModel::dismissDialog, enabled = !ui.busy) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignSheet(card: DeliveryCard, drivers: List<DeliveryBoardDriver>, ui: DeliveryUiState, viewModel: DeliveryViewModel) {
    BendeyBottomSheet(onDismissRequest = viewModel::dismissDialog) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = BendeySpacing.s16).padding(bottom = BendeySpacing.s16),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
        ) {
            Text(DeliveryCopy.text("assign.title"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BendeyColors.OnSurface)
            Text(
                listOf(card.customerName.ifBlank { "Sin nombre" }, card.address).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = BendeyColors.OnSurfaceVariant,
            )
            ui.dialogError?.let { BendeyAlert(message = it, severity = BendeyAlertSeverity.Danger) }
            if (drivers.isEmpty()) {
                Text(DeliveryCopy.text("assign.none"), style = MaterialTheme.typography.bodyMedium, color = BendeyColors.OnSurfaceVariant)
            } else {
                LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
                    items(deliverySortDrivers(drivers), key = { it.id }) { driver ->
                        DriverChoiceRow(driver = driver, current = card.driver?.id == driver.id, busy = ui.busy, onPick = { viewModel.assign(driver.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DriverChoiceRow(driver: DeliveryBoardDriver, current: Boolean, busy: Boolean, onPick: () -> Unit) {
    val choice = deliveryDriverChoice(driver)
    val enabled = !choice.disabled && !busy
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (choice.disabled) 0.55f else 1f)
            .background(BendeyColors.Surface, BendeyShapeTokens.md)
            .border(1.dp, if (current) BendeyColors.Primary else BendeyColors.Outline, BendeyShapeTokens.md)
            .clickable(enabled = enabled, onClick = onPick)
            .heightIn(min = 56.dp)
            .padding(BendeySpacing.s12),
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(driver.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = BendeyColors.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val meta = listOf(driver.phone, driver.vehicleType).filter { it.isNotBlank() }.joinToString(" · ")
            if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (choice.disabled) choice.reason else choice.activeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = if (choice.disabled) BendeyColors.ErrorText else BendeyColors.OnSurfaceVariant,
            )
        }
        BendeyStatusChip(
            label = when {
                current -> "Actual"
                driver.isAvailable -> DeliveryCopy.text("driver.available")
                else -> DeliveryCopy.text("driver.unavailable")
            },
            accentColor = if (driver.isAvailable) BendeyColors.Success else BendeyColors.OnSurfaceVariant,
        )
    }
}
