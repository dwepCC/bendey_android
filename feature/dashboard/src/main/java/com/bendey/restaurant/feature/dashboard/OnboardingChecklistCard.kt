package com.bendey.restaurant.feature.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyCard
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.onboarding.OnboardingAction
import com.bendey.restaurant.core.domain.onboarding.OnboardingChecklist
import com.bendey.restaurant.core.domain.onboarding.OnboardingCopy
import com.bendey.restaurant.core.domain.onboarding.OnboardingDestination
import com.bendey.restaurant.core.domain.onboarding.OnboardingItem
import com.bendey.restaurant.core.domain.onboarding.OnboardingSection
import com.bendey.restaurant.core.domain.onboarding.OnboardingSectionKind
import com.bendey.restaurant.core.domain.onboarding.OnboardingStepKey
import com.bendey.restaurant.core.ui.components.BendeyAlert
import com.bendey.restaurant.core.ui.components.BendeyAlertAction
import com.bendey.restaurant.core.ui.components.BendeyAlertDialog
import com.bendey.restaurant.core.ui.components.BendeyAlertSeverity
import com.bendey.restaurant.core.ui.components.BendeyOutlinedButton
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeySkeletonCard
import com.bendey.restaurant.core.ui.components.BendeyTextButton

/**
 * Punto de entrada para el Dashboard: pinta el esqueleto, el aviso de error o la tarjeta según el
 * estado. Si no hay nada que mostrar, el llamador no debe reservar espacio (ver [visible]).
 */
@Composable
fun rememberOnboardingChecklistState(
    viewModel: OnboardingChecklistViewModel = hiltViewModel(),
): OnboardingChecklistHandle {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Al volver a primer plano se relee, pero respetando la caché de ~60 s.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    return OnboardingChecklistHandle(state, viewModel)
}

class OnboardingChecklistHandle internal constructor(
    val state: OnboardingCardUiState,
    private val viewModel: OnboardingChecklistViewModel,
) {
    val visible: Boolean get() = state.visible

    fun forceRefresh() = viewModel.refresh(force = true)

    @Composable
    fun Content(onNavigate: (OnboardingDestination) -> Unit, modifier: Modifier = Modifier) {
        OnboardingChecklistContent(
            state = state,
            onNavigate = onNavigate,
            onSkip = viewModel::skip,
            onDismiss = viewModel::dismiss,
            onAskSampleMenu = viewModel::askSampleMenu,
            onAskSunat = viewModel::askSunatRequest,
            onConfirm = viewModel::confirm,
            onDismissConfirm = viewModel::dismissConfirm,
            onDismissFeedback = viewModel::dismissFeedback,
            onRetry = viewModel::retry,
            modifier = modifier,
        )
    }
}

@Composable
internal fun OnboardingChecklistContent(
    state: OnboardingCardUiState,
    onNavigate: (OnboardingDestination) -> Unit,
    onSkip: (OnboardingStepKey) -> Unit,
    onDismiss: () -> Unit,
    onAskSampleMenu: () -> Unit,
    onAskSunat: () -> Unit,
    onConfirm: () -> Unit,
    onDismissConfirm: () -> Unit,
    onDismissFeedback: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.checklist != null -> OnboardingChecklistCard(
            checklist = state.checklist,
            busy = state.busy,
            feedback = state.feedback,
            onNavigate = onNavigate,
            onSkip = onSkip,
            onDismiss = onDismiss,
            onAskSampleMenu = onAskSampleMenu,
            onAskSunat = onAskSunat,
            onDismissFeedback = onDismissFeedback,
            modifier = modifier,
        )
        state.loading -> BendeySkeletonCard(modifier = modifier, height = 180.dp)
        state.error != null -> BendeyAlert(
            message = state.error,
            severity = BendeyAlertSeverity.Warning,
            primaryAction = BendeyAlertAction("Reintentar", onRetry),
            modifier = modifier,
        )
    }

    when (state.confirm) {
        OnboardingConfirm.SAMPLE_MENU -> BendeyAlertDialog(
            onDismissRequest = onDismissConfirm,
            title = "¿Usar una carta de ejemplo?",
            message = "Cargaremos algunos platos de ejemplo para que pruebes cómo funciona. " +
                "Podrás editarlos o borrarlos cuando quieras.",
            confirmText = "Cargar carta de ejemplo",
            onConfirm = onConfirm,
            onDismiss = onDismissConfirm,
        )
        OnboardingConfirm.SUNAT_REQUEST -> BendeyAlertDialog(
            onDismissRequest = onDismissConfirm,
            title = "Solicitar activación de SUNAT",
            message = "Registraremos tu solicitud para emitir boletas y facturas electrónicas. " +
                "Te avisamos cuando esté lista.",
            confirmText = "Solicitar",
            onConfirm = onConfirm,
            onDismiss = onDismissConfirm,
        )
        null -> Unit
    }
}

@Composable
internal fun OnboardingChecklistCard(
    checklist: OnboardingChecklist,
    busy: Boolean,
    feedback: OnboardingFeedback?,
    onNavigate: (OnboardingDestination) -> Unit,
    onSkip: (OnboardingStepKey) -> Unit,
    onDismiss: () -> Unit,
    onAskSampleMenu: () -> Unit,
    onAskSunat: () -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // El primer pendiente de vender/operar es el siguiente paso: su botón va relleno, el resto outlined.
    val nextKey = checklist.sections
        .filter { it.kind != OnboardingSectionKind.GROW }
        .flatMap { it.items }
        .firstOrNull { !it.done && it.primaryLabel != null }
        ?.key
    var growExpanded by rememberSaveable { mutableStateOf(false) }

    BendeyCard(modifier = modifier, contentPadding = PaddingValues(BendeySpacing.md)) {
        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = checklist.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BendeyColors.OnSurface,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = checklist.progressLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.OnSurfaceVariant,
                    )
                }
                Text(
                    text = "${checklist.percent} %",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BendeyColors.Primary,
                )
            }

            LinearProgressIndicator(
                progress = { checklist.percent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(BendeyShapeTokens.pill)
                    .semantics(mergeDescendants = true) {
                        progressBarRangeInfo = ProgressBarRangeInfo(
                            current = checklist.percent / 100f,
                            range = 0f..1f,
                        )
                        contentDescription = "Avance de tus primeros pasos"
                        stateDescription = "${checklist.progressLabel}, ${checklist.percent} por ciento"
                    },
                color = BendeyColors.Primary,
                trackColor = BendeyColors.PrimaryContainer,
            )

            feedback?.let {
                BendeyAlert(
                    message = it.message,
                    severity = if (it.isError) BendeyAlertSeverity.Danger else BendeyAlertSeverity.Success,
                    onDismiss = onDismissFeedback,
                )
            }

            checklist.sections.forEach { section ->
                if (section.kind == OnboardingSectionKind.GROW) {
                    GrowSectionHeader(
                        section = section,
                        expanded = growExpanded,
                        onToggle = { growExpanded = !growExpanded },
                    )
                    if (growExpanded) {
                        section.items.forEach { item ->
                            OnboardingItemRow(
                                item = item,
                                emphasized = false,
                                busy = busy,
                                onNavigate = onNavigate,
                                onSkip = onSkip,
                                onAskSampleMenu = onAskSampleMenu,
                                onAskSunat = onAskSunat,
                            )
                        }
                    }
                } else {
                    SectionLabel(section.title)
                    section.items.forEach { item ->
                        OnboardingItemRow(
                            item = item,
                            emphasized = item.key == nextKey,
                            busy = busy,
                            onNavigate = onNavigate,
                            onSkip = onSkip,
                            onAskSampleMenu = onAskSampleMenu,
                            onAskSunat = onAskSunat,
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                BendeyTextButton(
                    text = "Ocultar",
                    onClick = onDismiss,
                    enabled = !busy,
                    textColor = BendeyColors.OnSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = BendeyColors.OnSurfaceVariant,
        modifier = Modifier
            .padding(top = BendeySpacing.xxs)
            .semantics { heading() },
    )
}

@Composable
private fun GrowSectionHeader(
    section: OnboardingSection,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = BendeySpacing.touchMin)
            .clickable(onClick = onToggle)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                stateDescription = if (expanded) "Expandido" else "Contraído"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = BendeyColors.OnSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = BendeyColors.OnSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingItemRow(
    item: OnboardingItem,
    emphasized: Boolean,
    busy: Boolean,
    onNavigate: (OnboardingDestination) -> Unit,
    onSkip: (OnboardingStepKey) -> Unit,
    onAskSampleMenu: () -> Unit,
    onAskSunat: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = BendeySpacing.touchMin),
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
    ) {
        Icon(
            imageVector = if (item.done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = if (item.done) "Hecho" else "Pendiente",
            tint = if (item.done) BendeyColors.Success else BendeyColors.OnSurfaceVariant,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(24.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (item.done) BendeyColors.OnSurfaceVariant else BendeyColors.OnSurface,
            )
            if (!item.done) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
            item.statusText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.done) BendeyColors.SuccessText else BendeyColors.InfoText,
                )
            }
            val showActions = item.primaryLabel != null || item.canSkip || item.offersSampleMenu
            if (showActions) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    item.primaryLabel?.let { label ->
                        val click = {
                            when (val action = item.primaryAction) {
                                is OnboardingAction.Navigate -> onNavigate(action.destination)
                                OnboardingAction.RequestSunat -> onAskSunat()
                                null -> Unit
                            }
                        }
                        if (emphasized) {
                            BendeyPrimaryButton(text = label, onClick = click, enabled = !busy, fillWidth = false)
                        } else {
                            BendeyOutlinedButton(text = label, onClick = click, enabled = !busy)
                        }
                    }
                    if (item.offersSampleMenu) {
                        BendeyTextButton(
                            text = OnboardingCopy.SAMPLE_MENU_CTA,
                            onClick = onAskSampleMenu,
                            enabled = !busy,
                        )
                    }
                    if (item.canSkip) {
                        BendeyTextButton(
                            text = "Omitir",
                            onClick = { onSkip(item.key) },
                            enabled = !busy,
                            textColor = BendeyColors.OnSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
