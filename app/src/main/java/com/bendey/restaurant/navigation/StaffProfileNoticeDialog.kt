package com.bendey.restaurant.navigation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.data.subscription.PlanNoticePreferencesStore
import com.bendey.restaurant.core.domain.model.UserSession
import com.bendey.restaurant.core.domain.session.StaffProfileNotice
import com.bendey.restaurant.core.domain.session.UserSessionStore
import com.bendey.restaurant.core.domain.subscription.PlanNoticePolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class StaffProfileNoticeViewModel @Inject constructor(
    private val preferences: PlanNoticePreferencesStore,
    sessionStore: UserSessionStore,
) : ViewModel() {

    val session: StateFlow<UserSession?> = sessionStore.userSessionFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Hasta leer lo guardado no se muestra nada: evita un destello del aviso ya cerrado hoy. */
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _dismissedDay = MutableStateFlow<String?>(null)
    val dismissedDay: StateFlow<String?> = _dismissedDay.asStateFlow()

    private val _closed = MutableStateFlow(false)
    val closed: StateFlow<Boolean> = _closed.asStateFlow()

    val today: String = PlanNoticePolicy.limaDay(System.currentTimeMillis())

    fun load(userId: Int) {
        viewModelScope.launch {
            _dismissedDay.value = preferences.staffProfileNoticeDismissedDay(userId)
            _loaded.value = true
        }
    }

    fun close(userId: Int) {
        _closed.value = true
        viewModelScope.launch { preferences.saveStaffProfileNoticeDismissedDay(userId, today) }
    }
}

/** Explica por qué falta el PIN y las notificaciones cuando el usuario no tiene perfil de restaurante. */
@Composable
fun StaffProfileNoticeDialog(viewModel: StaffProfileNoticeViewModel = hiltViewModel()) {
    val session by viewModel.session.collectAsState()
    val loaded by viewModel.loaded.collectAsState()
    val dismissedDay by viewModel.dismissedDay.collectAsState()
    val closed by viewModel.closed.collectAsState()

    val user = session?.user ?: return
    if (!loaded) {
        androidx.compose.runtime.LaunchedEffect(user.id) { viewModel.load(user.id) }
        return
    }
    if (!StaffProfileNotice.shouldShow(user.staffId, closed, dismissedDay, viewModel.today)) return

    AlertDialog(
        onDismissRequest = { viewModel.close(user.id) },
        title = { Text(StaffProfileNotice.TITLE) },
        text = { Text(StaffProfileNotice.MESSAGE) },
        confirmButton = {
            TextButton(onClick = { viewModel.close(user.id) }) { Text("Entendido") }
        },
    )
}
