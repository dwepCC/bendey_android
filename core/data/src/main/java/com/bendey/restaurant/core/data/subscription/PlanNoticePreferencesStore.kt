package com.bendey.restaurant.core.data.subscription

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bendey.restaurant.core.domain.session.StaffProfileNotice
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

    /** Dia de Lima en que el usuario cerro el aviso de perfil de restaurante (por tenant y usuario). */
    private suspend fun staffProfileKey(userId: Int): Preferences.Key<String> =
        stringPreferencesKey(StaffProfileNotice.dismissKey(sessionStore.tenantFlow.first()?.slug, userId))

    suspend fun staffProfileNoticeDismissedDay(userId: Int): String? =
        runCatching { dataStore.data.first()[staffProfileKey(userId)] }.getOrNull()

    suspend fun saveStaffProfileNoticeDismissedDay(userId: Int, day: String) {
        runCatching {
            val k = staffProfileKey(userId)
            dataStore.edit { it[k] = day }
        }
    }
}
