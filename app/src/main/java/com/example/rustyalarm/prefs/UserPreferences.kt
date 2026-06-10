package com.example.rustyalarm.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userDataStore by preferencesDataStore("user_prefs")

data class UserProfile(
    val hasOnboarded: Boolean = false,
    val nickname: String = "사용자",
    val appLockEnabled: Boolean = false,
)

class UserPreferences(private val context: Context) {

    val profile: Flow<UserProfile> = context.userDataStore.data.map { p ->
        UserProfile(
            hasOnboarded   = p[KEY_ONBOARDED] ?: false,
            nickname       = p[KEY_NICKNAME] ?: "사용자",
            appLockEnabled = p[KEY_APP_LOCK] ?: false,
        )
    }

    suspend fun completeOnboarding(nickname: String) {
        context.userDataStore.edit { p ->
            p[KEY_ONBOARDED] = true
            p[KEY_NICKNAME] = nickname.ifBlank { "사용자" }
        }
    }

    suspend fun setNickname(nickname: String) {
        context.userDataStore.edit { it[KEY_NICKNAME] = nickname.ifBlank { "사용자" } }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.userDataStore.edit { it[KEY_APP_LOCK] = enabled }
    }

    companion object {
        private val KEY_ONBOARDED = booleanPreferencesKey("onboarded")
        private val KEY_NICKNAME  = stringPreferencesKey("nickname")
        private val KEY_APP_LOCK  = booleanPreferencesKey("app_lock_enabled")
    }
}
