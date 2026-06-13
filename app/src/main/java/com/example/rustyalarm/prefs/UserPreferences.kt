package com.example.rustyalarm.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userDataStore by preferencesDataStore("user_prefs")

data class UserProfile(
    val hasOnboarded: Boolean = false,
    val nickname: String = "사용자",
    val appLockEnabled: Boolean = false,
    val vacationUntil: Long = 0L,            // millis; alarms suppressed until this
    val weatherAdjust: Boolean = false,      // shift alarm earlier when rain expected
    val weatherAdjustMinutes: Int = 15,
)

class UserPreferences(private val context: Context) {

    val profile: Flow<UserProfile> = context.userDataStore.data.map { p ->
        UserProfile(
            hasOnboarded   = p[KEY_ONBOARDED] ?: false,
            nickname       = p[KEY_NICKNAME] ?: "사용자",
            appLockEnabled = p[KEY_APP_LOCK] ?: false,
            vacationUntil  = p[KEY_VACATION] ?: 0L,
            weatherAdjust  = p[KEY_WEATHER_ADJUST] ?: false,
            weatherAdjustMinutes = (p[KEY_WEATHER_ADJUST_MIN] ?: 15).coerceIn(5, 60),
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

    suspend fun setVacationUntil(millis: Long) {
        context.userDataStore.edit { it[KEY_VACATION] = millis }
        // Mirror to SharedPreferences so AlarmReceiver can read it synchronously
        context.getSharedPreferences(MIRROR_FILE, Context.MODE_PRIVATE)
            .edit().putLong(MIRROR_KEY, millis).apply()
    }

    suspend fun setWeatherAdjust(enabled: Boolean, minutes: Int = 15) {
        context.userDataStore.edit { p ->
            p[KEY_WEATHER_ADJUST] = enabled
            p[KEY_WEATHER_ADJUST_MIN] = minutes.coerceIn(5, 60)
        }
        context.getSharedPreferences(MIRROR_FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(MIRROR_WX_KEY, enabled)
            .putInt(MIRROR_WX_MIN_KEY, minutes.coerceIn(5, 60))
            .apply()
    }

    companion object {
        private val KEY_ONBOARDED = booleanPreferencesKey("onboarded")
        private val KEY_NICKNAME  = stringPreferencesKey("nickname")
        private val KEY_APP_LOCK  = booleanPreferencesKey("app_lock_enabled")
        private val KEY_VACATION  = longPreferencesKey("vacation_until")
        private val KEY_WEATHER_ADJUST = booleanPreferencesKey("weather_adjust")
        private val KEY_WEATHER_ADJUST_MIN = intPreferencesKey("weather_adjust_min")

        const val MIRROR_FILE = "rusty_alarm_stats"
        const val MIRROR_KEY  = "vacation_until"
        const val MIRROR_WX_KEY = "weather_adjust"
        const val MIRROR_WX_MIN_KEY = "weather_adjust_min"

        /** Synchronous read for use from AlarmReceiver / scheduler hot paths. */
        fun vacationUntilSync(context: Context): Long =
            context.getSharedPreferences(MIRROR_FILE, Context.MODE_PRIVATE)
                .getLong(MIRROR_KEY, 0L)

        /** Synchronous read of weather-adjust toggle for the scheduler. */
        fun weatherAdjustSync(context: Context): Pair<Boolean, Int> {
            val p = context.getSharedPreferences(MIRROR_FILE, Context.MODE_PRIVATE)
            return p.getBoolean(MIRROR_WX_KEY, false) to p.getInt(MIRROR_WX_MIN_KEY, 15)
        }
    }
}
