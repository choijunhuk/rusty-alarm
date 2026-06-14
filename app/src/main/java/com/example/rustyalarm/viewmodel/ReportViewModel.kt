package com.example.rustyalarm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.rustyalarm.alarm.AlarmEventDao
import com.example.rustyalarm.alarm.AlarmEventType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class WeeklyReport(
    val fired: Int = 0,
    val dismissed: Int = 0,
    val snoozed: Int = 0,
    val avgWakeupHHMM: String = "—",
    val avgResponseLabel: String = "—",
    val completionRatePercent: Int = 0,
    val snoozeRatePercent: Int = 0,
    val streakDays: Int = 0,
    val challengesCompleted: Int = 0,
    val ready: Boolean = false,
    /** Map<yyyy-MM-dd, dismissedCount> for the last 30 days, oldest first. */
    val heatmap: List<Pair<String, Int>> = emptyList(),
    val insights: List<WakeupInsight> = emptyList(),
)

class ReportViewModel(private val eventDao: AlarmEventDao) : ViewModel() {

    private val _report = MutableStateFlow(WeeklyReport())
    val report: StateFlow<WeeklyReport> = _report.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -7)
            val since = cal.timeInMillis

            val events = eventDao.eventsSince(since)
            val fired      = events.count { it.eventType == AlarmEventType.FIRED.name }
            val dismissed  = events.count { it.eventType == AlarmEventType.DISMISSED.name }
            val snoozed    = events.count { it.eventType == AlarmEventType.SNOOZED.name }
            val challengesCompleted = events.count {
                it.eventType == AlarmEventType.DISMISSED.name &&
                    it.challengeType != "NONE"
            }
            val completionRate = if (fired == 0) 0 else dismissed * 100 / fired
            val snoozeRate = if (fired == 0) 0 else snoozed * 100 / fired

            val avg = events
                .filter { it.eventType == AlarmEventType.DISMISSED.name }
                .map { eventToMinuteOfDay(it.timestamp) }
                .takeIf { it.isNotEmpty() }
                ?.average()
                ?.toInt()
            val avgWakeup = avg?.let {
                "%02d:%02d".format(it / 60, it % 60)
            } ?: "—"
            val avgResponseSec = events
                .filter { it.eventType == AlarmEventType.DISMISSED.name }
                .mapNotNull { it.responseSeconds }
                .takeIf { it.isNotEmpty() }
                ?.average()
                ?.toLong()
                ?: 0L
            val avgResponseLabel = when {
                avgResponseSec <= 0L -> "—"
                avgResponseSec < 60L -> "${avgResponseSec}초"
                else -> "${avgResponseSec / 60}분 ${avgResponseSec % 60}초"
            }

            val streak = computeStreak(eventDao.dismissedDayKeys())
            val insights = ReportInsightEngine.insights(
                WakeupInsightInput(
                    fired = fired,
                    dismissed = dismissed,
                    snoozed = snoozed,
                    avgResponseSec = avgResponseSec,
                    challengesCompleted = challengesCompleted,
                    streakDays = streak,
                ),
            )

            // Heatmap — last 30 days of DISMISSED counts
            val cal30 = Calendar.getInstance()
            cal30.add(Calendar.DAY_OF_YEAR, -29)
            val sinceHeat = cal30.timeInMillis
            val dismissedDayMap = eventDao.eventsSince(sinceHeat)
                .filter { it.eventType == AlarmEventType.DISMISSED.name }
                .groupBy {
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it.timestamp))
                }
                .mapValues { it.value.size }
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val heatmap = (29 downTo 0).map { offset ->
                Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -offset)
                }.let {
                    val key = fmt.format(it.time)
                    key to (dismissedDayMap[key] ?: 0)
                }
            }

            _report.value = WeeklyReport(
                fired = fired,
                dismissed = dismissed,
                snoozed = snoozed,
                avgWakeupHHMM = avgWakeup,
                avgResponseLabel = avgResponseLabel,
                completionRatePercent = completionRate,
                snoozeRatePercent = snoozeRate,
                streakDays = streak,
                challengesCompleted = challengesCompleted,
                ready = true,
                heatmap = heatmap,
                insights = insights,
            )
        }
    }

    private fun eventToMinuteOfDay(ts: Long): Int {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    }

    private fun computeStreak(keysDesc: List<String>): Int {
        if (keysDesc.isEmpty()) return 0
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = fmt.format(Date())
        val yesterday = fmt.format(Date(System.currentTimeMillis() - 86_400_000L))
        val set = keysDesc.toSet()

        // Streak anchored on today OR yesterday (still "active")
        var cursor = when {
            today in set -> today
            yesterday in set -> yesterday
            else -> return 0
        }
        var streak = 0
        val cal = Calendar.getInstance()
        cal.time = fmt.parse(cursor)!!
        while (fmt.format(cal.time) in set) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    class Factory(private val dao: AlarmEventDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ReportViewModel(dao) as T
    }
}
