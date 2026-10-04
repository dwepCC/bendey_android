package com.bendey.restaurant.core.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bendey.restaurant.core.domain.model.PinStation
import com.bendey.restaurant.core.domain.session.UserSessionStore
import com.bendey.restaurant.core.domain.waiter.StationMemory
import com.bendey.restaurant.core.domain.waiter.StationPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.stationDataStore: DataStore<Preferences> by preferencesDataStore(name = "bendey_station_prefs")

/** Persistencia local (DataStore) de la estacion recordada, con clave por restaurante (slug). */
@Singleton
class StationPreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionStore: UserSessionStore,
) : StationPreferences {

    private val dataStore = context.stationDataStore

    private suspend fun slug(): String? = sessionStore.tenantFlow.first()?.slug?.takeIf { it.isNotEmpty() }

    override suspend fun remembered(): PinStation? {
        val slug = slug() ?: return null
        val raw = dataStore.data.first()[stringPreferencesKey(StationMemory.prefKey(slug))]
        return StationMemory.decode(raw)
    }

    override suspend fun remember(station: PinStation) {
        val slug = slug() ?: return
        val encoded = StationMemory.encode(station) ?: return
        dataStore.edit { it[stringPreferencesKey(StationMemory.prefKey(slug))] = encoded }
    }

    override suspend fun clear() {
        val slug = slug() ?: return
        dataStore.edit { it.remove(stringPreferencesKey(StationMemory.prefKey(slug))) }
    }
}
