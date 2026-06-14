package com.example.rustyalarm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.rustyalarm.alarm.Alarm
import com.example.rustyalarm.alarm.AlarmRepository
import com.example.rustyalarm.alarm.ChallengeType
import com.example.rustyalarm.alarm.WakeupPreset
import com.example.rustyalarm.alarm.WakeupPresetApplier
import com.example.rustyalarm.rust.RustAlarmCore
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlarmEditViewModel(private val repository: AlarmRepository) : ViewModel() {

    private val _alarm    = MutableStateFlow(Alarm())
    val alarm: StateFlow<Alarm> = _alarm.asStateFlow()

    private val _saved    = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private val _saveToast = MutableStateFlow<String?>(null)
    val saveToast: StateFlow<String?> = _saveToast.asStateFlow()

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
    fun updateMaxSnoozes(n: Int)                { _alarm.value = _alarm.value.copy(maxSnoozes = n) }
    fun updateMessage(text: String)             { _alarm.value = _alarm.value.copy(message = text) }
    fun updateGradualWakeup(g: Boolean)         { _alarm.value = _alarm.value.copy(gradualWakeup = g) }
    fun updateMathProblemCount(n: Int)          { _alarm.value = _alarm.value.copy(mathProblemCount = n.coerceAtLeast(1)) }
    fun updateRoutineItems(text: String) {
        val items = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        _alarm.value = _alarm.value.copy(routineItems = items)
    }
    fun updateGeofence(lat: Double?, lng: Double?, radius: Int? = null) {
        _alarm.value = _alarm.value.copy(
            geofenceLat = lat,
            geofenceLng = lng,
            geofenceRadius = radius ?: _alarm.value.geofenceRadius,
        )
    }

    fun toggleRepeatDay(day: Int) {
        val days = _alarm.value.repeatDays.toMutableList()
        if (days.contains(day)) days.remove(day) else days.add(day)
        _alarm.value = _alarm.value.copy(repeatDays = days.sorted())
    }

    fun setRepeatDays(days: List<Int>) {
        _alarm.value = _alarm.value.copy(repeatDays = days.distinct().sorted())
    }

    fun updateYoutubeUrl(url: String) {
        _alarm.value = _alarm.value.copy(youtubeUrl = url.trim().ifBlank { null })
    }

    fun updateAlarmVolumePercent(percent: Int) {
        _alarm.value = _alarm.value.copy(alarmVolumePercent = percent.coerceIn(0, 100))
    }

    fun updatePreAlarmMinutes(mins: Int) {
        _alarm.value = _alarm.value.copy(preAlarmMinutes = mins.coerceIn(0, 60))
    }

    fun applyWakeupPreset(preset: WakeupPreset) {
        _alarm.value = WakeupPresetApplier.apply(_alarm.value, preset)
    }

    fun save() {
        viewModelScope.launch {
            val a = _alarm.value
            repository.save(a)
            _saveToast.value = friendlyOffsetLabel(computeTriggerMillis(a))
            _saved.value = true
        }
    }

    /** Returns a human-readable conflict message if another enabled alarm
     *  would fire within ±5 minutes of this one on overlapping days. */
    suspend fun checkConflict(): String? {
        val a = _alarm.value
        if (!a.enabled) return null
        val mins = a.hour * 60 + a.minute
        val others = repository.allEnabled().filter { it.id != a.id }
        val hit = others.firstOrNull { o ->
            val omin = o.hour * 60 + o.minute
            val timeClose = kotlin.math.abs(omin - mins) <= 5
            val daysOverlap = (a.repeatDays.isEmpty() && o.repeatDays.isEmpty()) ||
                a.repeatDays.intersect(o.repeatDays.toSet()).isNotEmpty()
            timeClose && daysOverlap
        }
        return hit?.let {
            "'${it.title}' 알람과 시간이 겹쳐요 (%02d:%02d)".format(it.hour, it.minute)
        }
    }

    private fun computeTriggerMillis(a: Alarm): Long {
        val specific = a.specificDate
        return if (specific != null) {
            Calendar.getInstance().apply {
                timeInMillis = specific
                set(Calendar.HOUR_OF_DAY, a.hour)
                set(Calendar.MINUTE, a.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } else {
            RustAlarmCore.calculateNextAlarmTimestamp(
                System.currentTimeMillis(), a.hour, a.minute, a.repeatDays.toIntArray(),
            )
        }
    }

    private fun friendlyOffsetLabel(triggerMillis: Long): String {
        val diff = triggerMillis - System.currentTimeMillis()
        if (diff <= 0L) return "곧 울려요"
        val mins = (diff / 60_000L).toInt()
        return when {
            mins < 60 -> "$mins 분 후 울려요"
            mins < 1440 -> "${mins / 60}시간 ${mins % 60}분 후 울려요"
            else -> "${mins / 1440}일 ${mins % 1440 / 60}시간 후 울려요"
        }
    }

    /**
     * Schedules a one-shot alarm [offsetMinutes] from now. Title gets a friendly
     * "N분 후" suffix unless the user typed something else.
     */
    fun saveQuickFromNow(offsetMinutes: Int, label: String) {
        val now = java.util.Calendar.getInstance()
        now.add(java.util.Calendar.MINUTE, offsetMinutes)
        val alarm = _alarm.value.copy(
            title = if (_alarm.value.title.isBlank() || _alarm.value.title == "알람") label
                    else _alarm.value.title,
            hour = now.get(java.util.Calendar.HOUR_OF_DAY),
            minute = now.get(java.util.Calendar.MINUTE),
            specificDate = now.timeInMillis,
            repeatDays = emptyList(),
            enabled = true,
        )
        viewModelScope.launch {
            repository.save(alarm)
            _saveToast.value = friendlyOffsetLabel(now.timeInMillis)
            _saved.value = true
        }
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
