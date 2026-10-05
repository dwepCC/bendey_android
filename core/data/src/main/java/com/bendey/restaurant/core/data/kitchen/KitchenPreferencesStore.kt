package com.bendey.restaurant.core.data.kitchen

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bendey.restaurant.core.domain.session.UserSessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.kitchenDataStore: DataStore<Preferences> by preferencesDataStore(name = "bendey_kitchen_prefs")

/** Filtro de area del KDS recordado POR DISPOSITIVO (y por restaurante, por si el equipo cambia de cuenta). */
@Singleton
class KitchenPreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionStore: UserSessionStore,
) {
    private val dataStore = context.kitchenDataStore

    private suspend fun key(): Preferences.Key<String> {
        val slug = sessionStore.tenantFlow.first()?.slug.orEmpty()
        return stringPreferencesKey("area_filter_$slug")
    }

    suspend fun areaFilter(): String = dataStore.data.first()[key()]?.takeIf { it.isNotBlank() } ?: "all"

    suspend fun saveAreaFilter(area: String) {
        val k = key()
        dataStore.edit { it[k] = area }
    }
}
