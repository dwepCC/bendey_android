package com.bendey.restaurant.feature.repartidores

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.bendey.restaurant.core.domain.delivery.DELIVERY_BOARD_REFRESH_MS
import com.bendey.restaurant.core.domain.delivery.deliveryBoardTitle
import com.bendey.restaurant.core.ui.components.BendeyIconButton
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import kotlinx.coroutines.delay
import java.time.Instant

/**
 * Reloj del tablero de entregas: se refresca al entrar (init del ViewModel) y cada 60 s mientras la pantalla
 * esta a la vista; el "hace N min" avanza cada 30 s sin pedir nada al servidor.
 */
@Composable
internal fun rememberDeliveryBoardClock(onRefresh: () -> Unit): Instant {
    var now by remember { mutableStateOf(Instant.now()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            now = Instant.now()
            var sinceRefresh = 0L
            while (true) {
                delay(DELIVERY_CLOCK_TICK_MS)
                now = Instant.now()
                sinceRefresh += DELIVERY_CLOCK_TICK_MS
                if (sinceRefresh >= DELIVERY_BOARD_REFRESH_MS) {
                    sinceRefresh = 0L
                    onRefresh()
                }
            }
        }
    }
    return now
}

/**
 * Entregas (R2b): la vista de reparto de solo lectura (tablero R10.9) como destino propio. Es lo unico que ve
 * el repartidor; el administrador la tiene dentro de Mi negocio > Repartidores y entregas (con las altas).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntregasScreen(
    modifier: Modifier = Modifier,
    viewModel: RepartidoresViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val now = rememberDeliveryBoardClock(onRefresh = viewModel::refreshBoard)

    PullToRefreshBox(isRefreshing = state.boardLoading, onRefresh = viewModel::refreshBoard, modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BendeyScreenToolbar(
                title = "Entregas",
                subtitle = deliveryBoardTitle(state.board.size),
                actions = {
                    BendeyIconButton(
                        onClick = viewModel::refreshBoard,
                        icon = Icons.Default.Refresh,
                        contentDescription = "Actualizar",
                    )
                },
            )
            DeliveryBoard(
                items = state.board,
                loading = state.boardLoading,
                error = state.boardError,
                now = now,
                onRetry = viewModel::refreshBoard,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
