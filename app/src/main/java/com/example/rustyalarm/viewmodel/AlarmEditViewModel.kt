package com.example.rustyalarm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.rustyalarm.alarm.Alarm
import com.example.rustyalarm.alarm.AlarmRepository
import com.example.rustyalarm.alarm.ChallengeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlarmEditViewModel(private val repository: AlarmRepository) : ViewModel() {

    private val _alarm    = MutableStateFlow(Alarm())
    val alarm: StateFlow<Alarm> = _alarm.asStateFlow()

    private val _saved    = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    fun load(alarmId: Long) {
        if (alarmId == -1L) { _isLoaded.value = true; return }
        viewModelScope.launch {
            repository.getById(alarmId)?.let { _alarm.value = it }
            _isLoaded.value = true
        }
    }

    fun updateHour(hour: Int)                   { _alarm.value = _alarm.value.copy(hour = hour) }
    fun updateMinute(minute: Int)               { _alarm.value = _alarm.value.copy(minute = minute) }
    fun updateTitle(title: String)              { _alarm.value = _alarm.value.copy(title = title) }
    fun updateVibrate(v: Boolean)               { _alarm.value = _alarm.value.copy(vibrate = v) }
    fun updateSoundEnabled(s: Boolean)          { _alarm.value = _alarm.value.copy(soundEnabled = s) }
    fun updateEnabled(e: Boolean)               { _alarm.value = _alarm.value.copy(enabled = e) }
    fun updateChallengeType(c: ChallengeType)   { _alarm.value = _alarm.value.copy(challengeType = c) }
    fun updateSpecificDate(date: Long?)         { _alarm.value = _alarm.value.copy(specificDate = date) }
    fun updateRingtoneUri(uri: String?)         { _alarm.value = _alarm.value.copy(ringtoneUri = uri) }
    fun updateSmartAlarm(s: Boolean)            { _alarm.value = _alarm.value.copy(isSmartAlarm = s) }
    fun updateSmartWindow(min: Int)             { _alarm.value = _alarm.value.copy(smartWindowMinutes = min) }
    fun updateVolumeRamp(sec: Int)              { _alarm.value = _alarm.value.copy(volumeRampSeconds = sec) }
    fun updateGroupTag(tag: String?)            { _alarm.value = _alarm.value.copy(groupTag = tag?.ifBlank { null }) }

    fun toggleRepeatDay(day: Int) {
        val days = _alarm.value.repeatDays.toMutableList()
        if (days.contains(day)) days.remove(day) else days.add(day)
        _alarm.value = _alarm.value.copy(repeatDays = days.sorted())
    }

    fun save() {
        viewModelScope.launch { repository.save(_alarm.value); _saved.value = true }
    }

    fun delete() {
        viewModelScope.launch {
            if (_alarm.value.id != 0L) repository.delete(_alarm.value)
            _saved.value = true
        }
    }

    class Factory(private val repository: AlarmRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AlarmEditViewModel(repository) as T
    }
}
