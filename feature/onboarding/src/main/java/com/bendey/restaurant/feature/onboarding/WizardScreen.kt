package com.bendey.restaurant.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.auth.initialPinNotice
import com.bendey.restaurant.core.domain.onboarding.wizard.BusinessSubtype
import com.bendey.restaurant.core.domain.onboarding.wizard.PastePreviewRow
import com.bendey.restaurant.core.domain.onboarding.wizard.ServiceMode
import com.bendey.restaurant.core.domain.onboarding.wizard.WizardCopy
import com.bendey.restaurant.core.domain.onboarding.wizard.WizardStep
import com.bendey.restaurant.core.domain.onboarding.wizard.wizardProgressDone
import com.bendey.restaurant.core.ui.components.BendeyAlert
import com.bendey.restaurant.core.ui.components.BendeyAlertSeverity
import com.bendey.restaurant.core.ui.components.BendeyCheckboxRow
import com.bendey.restaurant.core.ui.components.BendeyOutlinedButton
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.components.ProductImportDialog
import com.bendey.restaurant.core.ui.layout.bendeySafeDrawingPadding

/**
 * Wizard de configuración (R4): W0 bienvenida, W1 tu restaurante, W2 tu carta y la puerta a W3 (la
 * guía de la primera venta, que vive como panel flotante sobre las pantallas reales).
 */
@Composable
fun WizardScreen(
    onExit: () -> Unit,
    onStartGuide: () -> Unit,
    onOpenProductos: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WizardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val step = state.step
    val progress = wizardProgressDone(state.server, state.local)

    // Salir de cualquier forma (atrás incluido) es "Explorar primero": no se reabre solo.
    BackHandler {
        when {
            state.menuMode != MenuMode.CHOOSE && step == WizardStep.MENU -> viewModel.backToChoose()
            else -> viewModel.close(onExit)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BendeyColors.Background)
            .bendeySafeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = BendeySpacing.s16, vertical = BendeySpacing.s16),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.s16),
        ) {
            ProgressHeader(done = progress)
            state.initialPin?.let { pin ->
                if (step == WizardStep.WELCOME) PinNotice(pin = pin, onGotIt = viewModel::dismissPin)
            }
            when (step) {
                WizardStep.WELCOME -> WelcomePage(
                    onStart = viewModel::start,
                    onExplore = { viewModel.close(onExit) },
                )
                WizardStep.RESTAURANT -> RestaurantPage(state, viewModel)
                WizardStep.MENU -> MenuPage(state, viewModel, onExit = { viewModel.close(onExit) }, onOpenProductos = onOpenProductos)
                WizardStep.FIRST_SALE -> FirstSalePage(
                    onStart = { viewModel.startFirstSaleGuide(onStartGuide) },
                    onLater = { viewModel.close(onExit) },
                )
            }
        }
    }

    ProductImportDialog(
        open = state.excelOpen,
        validation = state.excelValidation,
        progress = state.excelProgress,
        loading = state.excelLoading,
        error = state.error?.takeIf { state.excelOpen },
        onDismiss = viewModel::closeExcel,
        onFilePicked = viewModel::validateExcel,
        onImport = viewModel::importExcel,
        onDownloadSimpleTemplate = viewModel::simpleTemplateBytes,
        onDownloadAdvancedTemplate = viewModel::advancedTemplateBytes,
    )
}

@Composable
private fun ProgressHeader(done: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
        Text(
            text = WizardCopy.progress(done),
            style = MaterialTheme.typography.labelLarge,
            color = BendeyColors.OnSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { done / 3f },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = WizardCopy.progress(done) },
            color = BendeyColors.Primary,
            trackColor = BendeyColors.PrimaryContainer,
        )
    }
}

@Composable
private fun PinNotice(pin: String, onGotIt: () -> Unit) {
    val notice = initialPinNotice(pin) ?: return
    Card(
        colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface),
        border = BorderStroke(1.dp, BendeyColors.Outline),
        shape = BendeyShapeTokens.md,
    ) {
        Column(
            modifier = Modifier.padding(BendeySpacing.s16),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
        ) {
            Text(WizardCopy.PIN_NOTICE_TITLE, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = pin,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = BendeyColors.OnSurface,
            )
            Text(notice, style = MaterialTheme.typography.bodyMedium, color = BendeyColors.OnSurfaceVariant)
            BendeyOutlinedButton(text = WizardCopy.PIN_NOTICE_GOT_IT, onClick = onGotIt)
        }
    }
}

// ── W0 ───────────────────────────────────────────────────────────────────────────────────────────

@Composable
private fun WelcomePage(onStart: () -> Unit, onExplore: () -> Unit) {
    Text(WizardCopy.W0_TITLE, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(WizardCopy.W0_BODY, style = MaterialTheme.typography.bodyLarge, color = BendeyColors.OnSurfaceVariant)
    BendeyPrimaryButton(text = WizardCopy.W0_START, onClick = onStart)
    BendeyTextButton(text = WizardCopy.W0_EXPLORE, onClick = onExplore, modifier = Modifier.fillMaxWidth())
}

// ── W1 ───────────────────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RestaurantPage(state: WizardUiState, vm: WizardViewModel) {
    Text(WizardCopy.W1_TITLE, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    if (state.businessName.isNotBlank()) {
        Text(WizardCopy.W1_BUSINESS_NAME, style = MaterialTheme.typography.labelLarge, color = BendeyColors.OnSurfaceVariant)
        Text(state.businessName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
    Text(WizardCopy.W1_SUBTYPE_TITLE, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8), verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
        BusinessSubtype.entries.forEach { subtype ->
            BendeyFilterChip(
                selected = state.selectedSubtype == subtype,
                onClick = { vm.selectSubtype(subtype) },
                text = subtype.label,
                enabled = !state.busy,
                modifier = Modifier.heightIn(min = BendeySpacing.touchMin),
            )
        }
    }
    Text(WizardCopy.W1_SERVICE_TITLE, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8), verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
        ServiceMode.entries.forEach { mode ->
            BendeyFilterChip(
                selected = mode in state.local.serviceModes,
                onClick = { vm.toggleServiceMode(mode) },
                text = mode.label,
                modifier = Modifier.heightIn(min = BendeySpacing.touchMin),
            )
        }
    }
    BendeyAlert(message = WizardCopy.W1_TABLES_NOTICE, severity = BendeyAlertSeverity.Info)
    state.error?.let { BendeyAlert(message = it, severity = BendeyAlertSeverity.Danger) }
    BendeyPrimaryButton(text = WizardCopy.W1_NEXT, onClick = { vm.goTo(WizardStep.MENU) })
}

// ── W2 ───────────────────────────────────────────────────────────────────────────────────────────

@Composable
private fun MenuPage(state: WizardUiState, vm: WizardViewModel, onExit: () -> Unit, onOpenProductos: () -> Unit) {
    when (state.menuMode) {
        MenuMode.CHOOSE -> ChooseMenuPage(state, vm, onExit)
        MenuMode.PASTE -> PastePage(state, vm)
        MenuMode.PREVIEW -> PreviewPage(state, vm)
        MenuMode.MANUAL -> ManualPage(state, vm, onOpenProductos)
    }
}

@Composable
private fun ChooseMenuPage(state: WizardUiState, vm: WizardViewModel, onExit: () -> Unit) {
    Text(WizardCopy.W2_TITLE, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    if (state.menuDone) {
        BendeyAlert(message = WizardCopy.W2_DONE, severity = BendeyAlertSeverity.Success)
    } else {
        Text(WizardCopy.W2_INTRO, style = MaterialTheme.typography.bodyLarge, color = BendeyColors.OnSurfaceVariant)
    }
    state.notice?.let { BendeyAlert(message = it, severity = BendeyAlertSeverity.Success) }
    state.error?.let { BendeyAlert(message = it, severity = BendeyAlertSeverity.Danger) }
    state.loadError?.let {
        BendeyAlert(message = it, severity = BendeyAlertSeverity.Warning, primaryAction = com.bendey.restaurant.core.ui.components.BendeyAlertAction("Reintentar", vm::refresh))
    }

    if (!state.menuDone) {
        OptionCard(WizardCopy.W2_SAMPLE_TITLE, WizardCopy.W2_SAMPLE_BODY, enabled = !state.busy, onClick = vm::useSampleMenu)
        OptionCard(WizardCopy.W2_PASTE_TITLE, WizardCopy.W2_PASTE_BODY, enabled = !state.busy, onClick = vm::openPaste)
        OptionCard(WizardCopy.W2_EXCEL_TITLE, WizardCopy.W2_EXCEL_BODY, enabled = !state.busy, onClick = vm::openExcel)
        OptionCard(WizardCopy.W2_MANUAL_TITLE, WizardCopy.W2_MANUAL_BODY, enabled = !state.busy, onClick = vm::openManual)
        Text(
            WizardCopy.W2_LATER_NOTICE,
            style = MaterialTheme.typography.bodyMedium,
            color = BendeyColors.OnSurfaceVariant,
        )
        BendeyTextButton(text = WizardCopy.W2_LATER, onClick = onExit, modifier = Modifier.fillMaxWidth())
    } else {
        OptionCard(WizardCopy.W2_PASTE_TITLE, WizardCopy.W2_PASTE_BODY, enabled = !state.busy, onClick = vm::openPaste)
        OptionCard(WizardCopy.W2_EXCEL_TITLE, WizardCopy.W2_EXCEL_BODY, enabled = !state.busy, onClick = vm::openExcel)
        BendeyPrimaryButton(text = WizardCopy.W1_NEXT, onClick = { vm.goTo(WizardStep.FIRST_SALE) })
    }
}

@Composable
private fun OptionCard(title: String, body: String, enabled: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = BendeySpacing.touchPrimary)
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface),
        border = BorderStroke(1.dp, BendeyColors.Outline),
        shape = BendeyShapeTokens.md,
    ) {
        Column(modifier = Modifier.padding(BendeySpacing.s16), verticalArrangement = Arrangement.spacedBy(BendeySpacing.s4)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = BendeyColors.OnSurface)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = BendeyColors.OnSurfaceVariant)
        }
    }
}

@Composable
private fun PastePage(state: WizardUiState, vm: WizardViewModel) {
    Text(WizardCopy.PASTE_TITLE, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(WizardCopy.PASTE_HINT, style = MaterialTheme.typography.bodyMedium, color = BendeyColors.OnSurfaceVariant)
    BendeyTextField(
        value = state.pasteText,
        onValueChange = vm::onPasteTextChange,
        label = "Tu lista",
        singleLine = false,
        placeholder = "Entradas:\nCeviche clásico 35\nTiradito 38\nBebidas:\nChicha morada 6",
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp),
    )
    state.error?.let { BendeyAlert(message = it, severity = BendeyAlertSeverity.Danger) }
    BendeyPrimaryButton(text = "Revisar mi lista", onClick = vm::buildPreview, loading = state.busy)
    BendeyTextButton(text = "Volver", onClick = vm::backToChoose, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun PreviewPage(state: WizardUiState, vm: WizardViewModel) {
    Text(WizardCopy.PASTE_PREVIEW, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    if (state.pasteLimitReached) BendeyAlert(message = com.bendey.restaurant.core.domain.onboarding.wizard.PasteMenu.MSG_LIMIT, severity = BendeyAlertSeverity.Warning)
    if (state.preview.any { it.doubtful }) {
        BendeyAlert(message = WizardCopy.PASTE_DOUBTFUL_NOTICE, severity = BendeyAlertSeverity.Warning)
    }
    state.preview.forEach { row -> PreviewRow(row, vm) }
    state.error?.let { BendeyAlert(message = it, severity = BendeyAlertSeverity.Danger) }
    BendeyPrimaryButton(
        text = "${WizardCopy.PASTE_CREATE} (${state.importableCount})",
        onClick = vm::createFromPreview,
        loading = state.busy,
        enabled = state.importableCount > 0,
    )
    BendeyTextButton(text = "Volver a mi lista", onClick = vm::openPaste, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun PreviewRow(row: PastePreviewRow, vm: WizardViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface),
        border = BorderStroke(1.dp, if (row.error != null) BendeyColors.Error else BendeyColors.Outline),
        shape = BendeyShapeTokens.md,
    ) {
        Column(modifier = Modifier.padding(BendeySpacing.s12), verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BendeyCheckboxRow(
                    label = if (row.alreadyExists) WizardCopy.PASTE_EXISTING_EXCLUDED else "Crear este plato",
                    checked = row.importable,
                    onCheckedChange = { vm.togglePreviewRow(row.id, it) },
                    enabled = row.error == null && !row.alreadyExists,
                    modifier = Modifier.weight(1f),
                )
                BendeyTextButton(text = WizardCopy.PASTE_REMOVE, onClick = { vm.removePreviewRow(row.id) })
            }
            BendeyTextField(
                value = row.name,
                onValueChange = { vm.editPreviewRow(row.id, name = it) },
                label = "Nombre",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
                BendeyTextField(
                    value = row.priceText,
                    onValueChange = { vm.editPreviewRow(row.id, priceText = it) },
                    label = "Precio (S/)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    fillWidth = false,
                )
                BendeyTextField(
                    value = row.category,
                    onValueChange = { vm.editPreviewRow(row.id, category = it) },
                    label = "Categoría",
                    modifier = Modifier.weight(1.4f),
                    fillWidth = false,
                )
            }
            (row.error ?: row.warning)?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (row.error != null) BendeyColors.Error else BendeyColors.OnSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManualPage(state: WizardUiState, vm: WizardViewModel, onOpenProductos: () -> Unit) {
    Text(WizardCopy.W2_MANUAL_TITLE, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    state.notice?.let { BendeyAlert(message = it, severity = BendeyAlertSeverity.Success) }
    BendeyTextField(value = state.manualName, onValueChange = { vm.onManualChange(name = it) }, label = "Nombre del plato")
    BendeyTextField(
        value = state.manualPrice,
        onValueChange = { vm.onManualChange(price = it) },
        label = "Precio (S/)",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
    BendeyTextField(
        value = state.manualCategory,
        onValueChange = { vm.onManualChange(category = it) },
        label = "Categoría (si la dejas vacía, será General)",
    )
    if (state.categories.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8), verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
            state.categories.forEach { category ->
                BendeyFilterChip(
                    selected = state.manualCategory.trim().equals(category, ignoreCase = true),
                    onClick = { vm.onManualChange(category = category) },
                    text = category,
                    modifier = Modifier.heightIn(min = BendeySpacing.touchMin),
                )
            }
        }
    }
    state.error?.let { BendeyAlert(message = it, severity = BendeyAlertSeverity.Danger) }
    BendeyPrimaryButton(text = "Guardar plato", onClick = vm::saveManual, loading = state.busy)
    BendeyOutlinedButton(text = "Más opciones (IGV, código, stock…)", onClick = onOpenProductos, fillWidth = true)
    BendeyTextButton(
        text = if (state.manualCreated > 0) "Listo" else "Volver",
        onClick = vm::backToChoose,
        modifier = Modifier.fillMaxWidth(),
    )
}

// ── W3 (puerta) ──────────────────────────────────────────────────────────────────────────────────

@Composable
private fun FirstSalePage(onStart: () -> Unit, onLater: () -> Unit) {
    Text(WizardCopy.W3_TITLE, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    BendeyAlert(message = WizardCopy.W3_REAL_NOTICE, severity = BendeyAlertSeverity.Warning)
    Text(
        "Te acompañamos con una guía pequeña sobre las pantallas de siempre. No bloquea nada y puedes cerrarla cuando quieras.",
        style = MaterialTheme.typography.bodyLarge,
        color = BendeyColors.OnSurfaceVariant,
    )
    BendeyPrimaryButton(text = "Empezar mi primera venta", onClick = onStart)
    BendeyTextButton(text = WizardCopy.W2_LATER, onClick = onLater, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(BendeySpacing.s16))
}
