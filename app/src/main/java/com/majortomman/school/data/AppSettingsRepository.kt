package com.majortomman.school.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.schoolSettingsDataStore by preferencesDataStore(name = "school_preferences")

data class AiSettings(
    val endpoint: String = "",
    val model: String = "gemma-4",
    val apiKey: String = "",
)

class AppSettingsRepository(
    private val context: Context,
) {
    private val preferencesFlow = context.schoolSettingsDataStore.data.safeData()

    private object Keys {
        val aiEndpoint = stringPreferencesKey("ai_endpoint")
        val aiModel = stringPreferencesKey("ai_model")
        val aiApiKey = stringPreferencesKey("ai_api_key")
    }

    val aiSettings: Flow<AiSettings> = preferencesFlow.map { preferences ->
        AiSettings(
            endpoint = preferences[Keys.aiEndpoint] ?: AiSettings().endpoint,
            model = preferences[Keys.aiModel] ?: AiSettings().model,
            apiKey = preferences[Keys.aiApiKey].orEmpty(),
        )
    }

    suspend fun saveAiSettings(settings: AiSettings) {
        context.schoolSettingsDataStore.edit { preferences ->
            preferences[Keys.aiEndpoint] = settings.endpoint.trim().trimEnd('/')
            preferences[Keys.aiModel] = settings.model.trim()
            preferences[Keys.aiApiKey] = settings.apiKey.trim()
        }
    }

    private fun Flow<Preferences>.safeData(): Flow<Preferences> = catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }
}
