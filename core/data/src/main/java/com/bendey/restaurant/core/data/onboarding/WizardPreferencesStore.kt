package com.bendey.restaurant.core.data.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bendey.restaurant.core.domain.onboarding.wizard.ServiceMode
import com.bendey.restaurant.core.domain.onboarding.wizard.WizardLocalState
import com.bendey.restaurant.core.domain.onboarding.wizard.WizardPreferences
import com.bendey.restaurant.core.domain.onboarding.wizard.WizardStep
import com.bendey.restaurant.core.domain.session.UserSessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.wizardDataStore: DataStore<Preferences> by preferencesDataStore(name = "bendey_wizard_prefs")

/**
 * Preferencias LOCALES del wizard, separadas por restaurante (slug): "wizard cerrado", modos de
 * atención y paso actual. No se sincronizan ni se guardan en el servidor (`dismissed` del servidor
 * oculta el checklist y no se usa para esto).
 */
@Singleton
class WizardPreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionStore: UserSessionStore,
) : WizardPreferences {

    private val dataStore = context.wizardDataStore

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: Flow<WizardLocalState> = sessionStore.tenantFlow
        .map { it?.slug.orEmpty() }
        .distinctUntilChanged()
        .flatMapLatest { slug ->
            if (slug.isEmpty()) {
                flowOf(WizardLocalState())
            } else {
                dataStore.data.map { prefs -> prefs.toState(slug) }
            }
        }
        .distinctUntilChanged()

    private suspend fun slug(): String? = sessionStore.tenantFlow.first()?.slug?.takeIf { it.isNotEmpty() }

    override suspend fun setClosed(closed: Boolean) {
        val slug = slug() ?: return
        dataStore.edit { it[closedKey(slug)] = closed }
    }

    override suspend fun setServiceModes(modes: Set<ServiceMode>) {
        val slug = slug() ?: return
        dataStore.edit { it[modesKey(slug)] = encodeModes(modes) }
    }

    override suspend fun setStep(step: WizardStep) {
        val slug = slug() ?: return
        dataStore.edit { it[stepKey(slug)] = step.name }
    }

    private fun Preferences.toState(slug: String) = WizardLocalState(
        closed = this[closedKey(slug)] ?: false,
        serviceModes = decodeModes(this[modesKey(slug)]),
        step = this[stepKey(slug)]?.let { name -> WizardStep.entries.firstOrNull { it.name == name } }
            ?: WizardStep.WELCOME,
    )

    private fun closedKey(slug: String) = booleanPreferencesKey("closed_$slug")
    private fun modesKey(slug: String) = stringPreferencesKey("modes_$slug")
    private fun stepKey(slug: String) = stringPreferencesKey("step_$slug")

    internal companion object {
        fun encodeModes(modes: Set<ServiceMode>): String = modes.joinToString(",") { it.key }

        /** Sin valor guardado (o ilegible) se asume "En mesas", el valor por defecto de W1. */
        fun decodeModes(raw: String?): Set<ServiceMode> {
            val parsed = raw.orEmpty().split(',')
                .mapNotNull { key -> ServiceMode.entries.firstOrNull { it.key == key } }
                .toSet()
            return parsed.ifEmpty { setOf(ServiceMode.DINE_IN) }
        }
    }
}
