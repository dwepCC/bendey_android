package com.bendey.restaurant.core.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tokenDataStore: DataStore<Preferences> by preferencesDataStore(name = "bendey_tokens")

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val keyAccessToken = stringPreferencesKey("access_token")

    val tokenFlow: Flow<String?> = context.tokenDataStore.data.map { prefs ->
        prefs[keyAccessToken]
    }

    suspend fun getToken(): String? = context.tokenDataStore.data.first()[keyAccessToken]

    suspend fun setToken(token: String) {
        context.tokenDataStore.edit { it[keyAccessToken] = token }
    }

    suspend fun clearToken() {
        context.tokenDataStore.edit { it.remove(keyAccessToken) }
    }

    /** Lectura sincrónica para interceptores OkHttp. */
    fun getTokenSync(): String? = runBlocking { getToken() }
}
