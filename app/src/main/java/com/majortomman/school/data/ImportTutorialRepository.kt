package com.majortomman.school.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.majortomman.school.data.material.IMPORT_TUTORIAL_VERSION
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.importTutorialDataStore by preferencesDataStore(name = "import_tutorial_preferences")

class ImportTutorialRepository(
    context: Context,
) {
    private val appContext = context.applicationContext

    private object Keys {
        val completed = stringSetPreferencesKey("completed_subject_tutorials")
    }

    val completedTutorials: Flow<Set<String>> = appContext.importTutorialDataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { preferences -> preferences[Keys.completed].orEmpty() }

    suspend fun markCompleted(subjectId: String, version: Int = IMPORT_TUTORIAL_VERSION) {
        val token = token(subjectId, version)
        appContext.importTutorialDataStore.edit { preferences ->
            preferences[Keys.completed] = preferences[Keys.completed].orEmpty() + token
        }
    }

    fun isCompleted(
        completed: Set<String>,
        subjectId: String,
        version: Int = IMPORT_TUTORIAL_VERSION,
    ): Boolean = token(subjectId, version) in completed

    private fun token(subjectId: String, version: Int): String = "$subjectId:$version"
}
