package com.bendey.restaurant.feature.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.onboarding.OnboardingRepository
import com.bendey.restaurant.core.domain.onboarding.wizard.CoachStepId
import com.bendey.restaurant.core.domain.onboarding.wizard.CoachTarget
import com.bendey.restaurant.core.domain.onboarding.wizard.WizardCoach
import com.bendey.restaurant.core.domain.onboarding.wizard.WizardCopy
import com.bendey.restaurant.core.domain.onboarding.wizard.hasSales
import com.bendey.restaurant.core.ui.components.BendeyOutlinedButton
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class WizardCoachViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
) : ViewModel() {
    /** ¿Ya hay una venta efectiva? El servidor manda: la guía termina sola cuando `first_sale` es true. */
    suspend fun firstSaleDone(): Boolean =
        (onboardingRepository.getState() as? AppResult.Success)?.data?.hasSales() == true
}

/**
 * W3: guía de la primera venta como panel flotante, NO bloqueante y descartable, sobre las pantallas
 * reales. Termina sola cuando el servidor reporta la primera venta (el momento celebratorio es R6).
 */
@Composable
fun WizardCoachPanel(
    onNavigate: (CoachTarget) -> Unit,
    onOpenCash: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WizardCoachViewModel = hiltViewModel(),
) {
    val coach by WizardCoach.state.collectAsStateWithLifecycle()
    if (!coach.active) return
    val step = coach.current ?: return

    // Mientras la guía está abierta se consulta al servidor (sin tocar el flujo de cobro).
    LaunchedEffect(Unit) {
        while (true) {
            delay(POLL_MS)
            if (viewModel.firstSaleDone()) {
                WizardCoach.close()
                onFinished()
                break
            }
        }
    }

    if (coach.minimized) {
        Row(modifier = modifier.padding(BendeySpacing.s8), horizontalArrangement = Arrangement.End) {
            BendeyPrimaryButton(
                text = "Guía: paso ${coach.index + 1} de ${coach.steps.size}",
                onClick = { WizardCoach.setMinimized(false) },
                fillWidth = false,
            )
        }
        return
    }

    Card(
        modifier = modifier
            .padding(BendeySpacing.s8)
            .widthIn(max = 560.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface),
        border = BorderStroke(1.dp, BendeyColors.Primary),
        shape = BendeyShapeTokens.md,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier.padding(BendeySpacing.s12),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
        ) {
            Text(
                text = "${WizardCopy.W3_TITLE} · paso ${coach.index + 1} de ${coach.steps.size}",
                style = MaterialTheme.typography.labelLarge,
                color = BendeyColors.OnSurfaceVariant,
            )
            Text(step.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (step.body.isNotBlank()) {
                Text(step.body, style = MaterialTheme.typography.bodyMedium, color = BendeyColors.OnSurfaceVariant)
            }
            if (step.id == CoachStepId.CHARGE || step.id == CoachStepId.CASH) {
                Text(WizardCopy.W3_REAL_NOTICE, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8), verticalAlignment = Alignment.CenterVertically) {
                if (step.id == CoachStepId.CASH) {
                    BendeyOutlinedButton(
                        text = WizardCopy.W3_OPEN_CASH,
                        onClick = onOpenCash,
                        modifier = Modifier.heightIn(min = BendeySpacing.touchMin),
                    )
                } else {
                    BendeyOutlinedButton(
                        text = "Ir a la pantalla",
                        onClick = { onNavigate(step.target) },
                        modifier = Modifier.heightIn(min = BendeySpacing.touchMin),
                    )
                }
                BendeyPrimaryButton(
                    text = if (coach.isLast) "Terminar" else WizardCopy.W3_NEXT,
                    onClick = {
                        WizardCoach.next()
                        WizardCoach.state.value.current?.let { onNavigate(it.target) }
                    },
                    fillWidth = false,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
                BendeyTextButton(text = "Minimizar", onClick = { WizardCoach.setMinimized(true) })
                BendeyTextButton(text = WizardCopy.W3_CLOSE, onClick = { WizardCoach.close() })
            }
        }
    }
}

private const val POLL_MS = 8_000L
