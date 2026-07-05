package com.example.rustyalarm.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.rustyalarm.alarm.Alarm
import com.example.rustyalarm.alarm.AlarmRepository
import com.example.rustyalarm.weather.Weather
import com.example.rustyalarm.weather.WeatherFetcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AlarmListViewModel @Inject constructor(
    app: Application,
    private val repository: AlarmRepository,
) : AndroidViewModel(app) {

    val alarms: StateFlow<List<Alarm>> = repository.alarms.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val groups: StateFlow<List<String>> = repository.groups.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _lastDismissedAt = MutableStateFlow<Long?>(null)
    val lastDismissedAt: StateFlow<Long?> = _lastDismissedAt.asStateFlow()

    private val _weather = MutableStateFlow<Weather?>(null)
    val weather: StateFlow<Weather?> = _weather.asStateFlow()

    private val _weeklyDismissed = MutableStateFlow(0)
    val weeklyDismissed: StateFlow<Int> = _weeklyDismissed.asStateFlow()

    init {
        viewModelScope.launch { _lastDismissedAt.value = repository.lastDismissedAt() }
        viewModelScope.launch { _weather.value = WeatherFetcher.current(getApplication()) }
        viewModelScope.launch { _weeklyDismissed.value = repository.dismissedInLast(7) }
    }

    fun refreshWeather() {
        viewModelScope.launch { _weather.value = WeatherFetcher.current(getApplication()) }
    }

    fun recordBedtime() {
        viewModelScope.launch {
            val db = com.example.rustyalarm.alarm.AlarmDatabase
                .getDatabase(getApplication())
            db.alarmEventDao().insert(
                com.example.rustyalarm.alarm.AlarmEvent(
                    alarmId = 0L,
                    eventType = com.example.rustyalarm.alarm.AlarmEventType.BEDTIME.name,
                )
            )
        }
    }

    fun toggleAlarm(alarm: Alarm, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(alarm.id, enabled) }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch { repository.delete(alarm) }
    }

    /** One-shot alarm scheduled [minutes] from now. */
    fun quickAlarm(minutes: Int) {
        viewModelScope.launch {
            val now = java.util.Calendar.getInstance()
            now.add(java.util.Calendar.MINUTE, minutes)
            repository.save(
                Alarm(
                    title = "${minutes}분 후",
                    hour = now.get(java.util.Calendar.HOUR_OF_DAY),
                    minute = now.get(java.util.Calendar.MINUTE),
                    specificDate = now.timeInMillis,
                    repeatDays = emptyList(),
                    enabled = true,
                ),
            )
        }
    }

    fun duplicateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.save(
                alarm.copy(
                    id = 0L,
                    title = "${alarm.title} (복사)",
                    enabled = false,
                ),
            )
        }
    }

    fun toggleGroup(tag: String, enabled: Boolean) {
        viewModelScope.launch { repository.setGroupEnabled(tag, enabled) }
    }

}
