package com.bendey.restaurant.feature.ayuda

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.help.HelpCatalog
import com.bendey.restaurant.core.domain.help.HelpIndex
import com.bendey.restaurant.core.domain.help.HelpListState
import com.bendey.restaurant.core.domain.help.HelpRole
import com.bendey.restaurant.core.domain.help.defaultHelpRoleFilter
import com.bendey.restaurant.core.domain.help.helpListState
import com.bendey.restaurant.core.domain.help.helpRoleFor
import com.bendey.restaurant.core.domain.session.UserSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Dónde vive el JSON copiado de Tauri (ver `scripts/sync-help.mjs`). */
const val HELP_ASSET_PATH = "help/help.json"

data class AyudaUiState(
    val loading: Boolean = true,
    /** Falló la lectura del archivo de ayuda (casi imposible: viaja dentro de la app). */
    val loadFailed: Boolean = false,
    val catalog: HelpCatalog? = null,
    val query: String = "",
    /** Filtro de puesto; null = todos los puestos. */
    val role: HelpRole? = null,
    val userRole: HelpRole? = null,
    val openArticleId: String? = null,
    val list: HelpListState? = null,
)

@HiltViewModel
class AyudaViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionStore: UserSessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AyudaUiState())
    val uiState: StateFlow<AyudaUiState> = _uiState.asStateFlow()

    private var index: HelpIndex? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, loadFailed = false) }
            val catalog = withContext(Dispatchers.IO) {
                runCatching {
                    HelpCatalogParser.parse(context.assets.open(HELP_ASSET_PATH).bufferedReader(Charsets.UTF_8).use { it.readText() })
                }.getOrNull()
            }
            if (catalog == null) {
                _uiState.update { it.copy(loading = false, loadFailed = true) }
                return@launch
            }
            index = HelpIndex(catalog.articles)
            val session = sessionStore.userSessionFlow.first()
            val userRole = helpRoleFor(session?.restaurantPermissions.orEmpty(), session?.user?.employeeType)
            _uiState.update {
                recompute(it.copy(loading = false, catalog = catalog, userRole = userRole, role = defaultHelpRoleFilter(userRole)))
            }
        }
    }

    fun setQuery(query: String) = _uiState.update { recompute(it.copy(query = query)) }

    fun clearSearch() = setQuery("")

    fun setRole(role: HelpRole?) = _uiState.update { recompute(it.copy(role = role)) }

    fun showAllRoles() = setRole(null)

    fun openArticle(id: String) = _uiState.update { it.copy(openArticleId = id) }

    fun closeArticle() = _uiState.update { it.copy(openArticleId = null) }

    private fun recompute(state: AyudaUiState): AyudaUiState {
        val catalog = state.catalog ?: return state
        val idx = index ?: return state
        return state.copy(list = helpListState(catalog, idx, state.query, state.role))
    }
}
