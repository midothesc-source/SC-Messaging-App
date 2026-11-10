package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "sc_preferences")

class UserPreferencesRepository(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme_mode")
    private val langKey = stringPreferencesKey("app_language")

    val themeModeFlow: Flow<AppThemeMode> = context.dataStore.data.map { prefs ->
        AppThemeMode.fromId(prefs[themeKey] ?: AppThemeMode.OBSIDIAN_GOLD.id)
    }

    val languageFlow: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        AppLanguage.fromCode(prefs[langKey] ?: AppLanguage.EN.code)
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[themeKey] = mode.id
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[langKey] = language.code
        }
    }
}
