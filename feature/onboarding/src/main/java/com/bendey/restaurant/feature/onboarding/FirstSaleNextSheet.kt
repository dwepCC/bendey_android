package com.bendey.restaurant.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.data.printer.PrinterPreferencesStore
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.onboarding.FirstSaleMoment
import com.bendey.restaurant.core.domain.onboarding.NextActionId
import com.bendey.restaurant.core.domain.onboarding.NextRecommendation
import com.bendey.restaurant.core.domain.onboarding.OnboardingRepository
import com.bendey.restaurant.core.domain.onboarding.copy
import com.bendey.restaurant.core.domain.onboarding.recommendNext
import com.bendey.restaurant.core.domain.onboarding.wizard.WizardCopy
import com.bendey.restaurant.core.domain.onboarding.wizard.WizardPreferences
import com.bendey.restaurant.core.ui.components.BendeyAlertDialog
import com.bendey.restaurant.core.ui.components.BendeyBottomSheet
import com.bendey.restaurant.core.ui.components.BendeyOutlinedButton
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * R6: lee el estado de onboarding UNA vez por momento, en segundo plano y fuera de la ruta de
 * cobro. Cualquier fallo (API caída, 403 de un mozo, excepción) deja la recomendación en null y no
 * se muestra nada.
 */
@HiltViewModel
class FirstSaleNextViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
    private val printerStore: PrinterPreferencesStore,
    private val wizardPreferences: WizardPreferences,
) : ViewModel() {
    private val _recommendation = MutableStateFlow<NextRecommendation?>(null)
    val recommendation: StateFlow<NextRecommendation?> = _recommendation.asStateFlow()

    private val _sunatBusy = MutableStateFlow(false)
    val sunatBusy: StateFlow<Boolean> = _sunatBusy.asStateFlow()

    private var loadedToken: String? = null

    fun load(token: String) {
        if (loadedToken == token) return
        loadedToken = token
        _recommendation.value = null
        viewModelScope.launch {
            _recommendation.value = try {
                val state = (onboardingRepository.getState() as? AppResult.Success)?.data
                recommendNext(
                    state = state,
                    localPrinterConfigured = printerStore.hasConfiguredPrinter.first(),
                    counterOnly = !wizardPreferences.state.first().usesTables,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }
    }

    fun requestSunat(onMessage: (String) -> Unit, onDone: () -> Unit) {
        if (_sunatBusy.value) return
        _sunatBusy.value = true
        viewModelScope.launch {
            try {
                when (val result = onboardingRepository.requestSunatActivation()) {
                    is AppResult.Success -> {
                        onMessage(WizardCopy.NEXT_SUNAT_DONE)
                        onDone()
                    }
                    is AppResult.Error -> onMessage(result.message)
                    AppResult.Loading -> Unit
                }
            } finally {
                _sunatBusy.value = false
            }
        }
    }
}

/**
 * Hoja inferior "Lo que sigue" (R6). Se monta una vez a nivel de la app: tras cobrar en una mesa la
 * pantalla se cierra, así que no puede vivir en ella. Solo aparece cuando el recibo de un cobro
 * NUEVO con `first_sale` ya se cerró y hay al menos una recomendación.
 *
 * @param suppressed true dentro del wizard (`/primeros-pasos`): la banda/hoja no deben pisarlo.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FirstSaleNextSheet(
    suppressed: Boolean,
    onAction: (NextActionId) -> Unit,
    onShowMessage: (String) -> Unit,
    viewModel: FirstSaleNextViewModel = hiltViewModel(),
) {
    val moment by FirstSaleMoment.state.collectAsStateWithLifecycle()
    val recommendation by viewModel.recommendation.collectAsStateWithLifecycle()
    val sunatBusy by viewModel.sunatBusy.collectAsStateWithLifecycle()
    var confirmSunat by remember { mutableStateOf(false) }

    val token = moment.token
    LaunchedEffect(token) { if (token != null) viewModel.load(token) }

    val rec = recommendation
    if (token == null || !moment.receiptClosed || rec == null || suppressed) return

    fun finish() {
        confirmSunat = false
        FirstSaleMoment.finish()
    }

    fun act(id: NextActionId) {
        if (id == NextActionId.SUNAT) {
            confirmSunat = true
        } else {
            finish()
            onAction(id)
        }
    }

    BendeyBottomSheet(onDismissRequest = ::finish) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.sm),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
        ) {
            Text(
                WizardCopy.NEXT_TITLE,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            val primary = rec.primary.copy()
            Text(
                primary.title,
                style = MaterialTheme.typography.bodyLarge,
                color = BendeyColors.OnSurface,
            )
            BendeyPrimaryButton(text = primary.action, onClick = { act(rec.primary) }, enabled = !sunatBusy)
            rec.secondary.forEach { id ->
                val c = id.copy()
                Text(
                    c.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BendeyColors.OnSurfaceVariant,
                )
                BendeyOutlinedButton(text = c.action, onClick = { act(id) }, enabled = !sunatBusy, fillWidth = true)
            }
            BendeyTextButton(text = WizardCopy.NEXT_CLOSE, onClick = ::finish, modifier = Modifier.fillMaxWidth())
        }
    }

    if (confirmSunat) {
        BendeyAlertDialog(
            onDismissRequest = { confirmSunat = false },
            title = WizardCopy.NEXT_SUNAT_CONFIRM_TITLE,
            message = WizardCopy.NEXT_SUNAT_CONFIRM_MESSAGE,
            confirmText = WizardCopy.NEXT_SUNAT_ACTION,
            onConfirm = {
                confirmSunat = false
                viewModel.requestSunat(onMessage = onShowMessage, onDone = ::finish)
            },
        )
    }
}
