package com.example.rustyalarm.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.themeDataStore by preferencesDataStore("theme_prefs")

class ThemePreferences(private val context: Context) {

    val mode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[KEY_MODE] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    suspend fun setMode(mode: ThemeMode) {
        context.themeDataStore.edit { it[KEY_MODE] = mode.name }
    }

    companion object {
        private val KEY_MODE: Preferences.Key<String> = stringPreferencesKey("mode")
    }
}
