package com.bendey.restaurant.core.data.subscription

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bendey.restaurant.core.domain.session.UserSessionStore
import com.bendey.restaurant.core.domain.subscription.PlanNoticePolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.planNoticeDataStore: DataStore<Preferences> by preferencesDataStore(name = "bendey_plan_notice_prefs")

/** R10.6: dia de Lima en que se cerro el aviso del plan, POR tenant (equipo compartido entre cuentas). */
@Singleton
class PlanNoticePreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionStore: UserSessionStore,
) {
    private val dataStore = context.planNoticeDataStore

    private suspend fun key(): Preferences.Key<String> =
        stringPreferencesKey(PlanNoticePolicy.dismissKey(sessionStore.tenantFlow.first()?.slug))

    suspend fun dismissedDay(): String? = runCatching { dataStore.data.first()[key()] }.getOrNull()

    suspend fun saveDismissedDay(day: String) {
        runCatching {
            val k = key()
            dataStore.edit { it[k] = day }
        }
    }
}
