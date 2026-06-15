package com.example.rustyalarm.alarm

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import com.example.rustyalarm.rust.RustAlarmCore

class AlarmRepository(
    private val dao: AlarmDao,
    private val eventDao: AlarmEventDao,
    private val scheduler: AlarmScheduler,
    private val appContext: Context? = null,
) {
    private suspend fun refreshWidget() {
        val ctx = appContext ?: return
        runCatching {
            com.example.rustyalarm.widget.NextAlarmWidget().updateAll(ctx)
        }
        pushWearSnapshot()
    }

    private suspend fun pushWearSnapshot() {
        val ctx = appContext ?: return
        runCatching {
            val all = dao.getAllEnabled().map { it.toAlarm() }
            val now = System.currentTimeMillis()
            val next = all.mapNotNull { a ->
                val specific = a.specificDate
                val ts = if (specific != null) {
                    java.util.Calendar.getInstance().apply {
                        timeInMillis = specific
                        set(java.util.Calendar.HOUR_OF_DAY, a.hour)
                        set(java.util.Calendar.MINUTE, a.minute)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }.timeInMillis
                } else {
                    com.example.rustyalarm.rust.RustAlarmCore.calculateNextAlarmTimestamp(
                        now, a.hour, a.minute, a.repeatDays.toIntArray(),
                    )
                }
                a to ts
            }.minByOrNull { it.second }
            com.example.rustyalarm.wear.WearSync.pushNextAlarm(
                context = ctx,
                triggerAtMillis = next?.second,
                title = next?.first?.title,
                readinessLabel = AlarmReliability.diagnose(
                    permissions = Permissions.status(ctx),
                    enabledAlarmCount = all.size,
                    nextAlarm = next?.first,
                ).level.label,
                watchAccess = next?.first?.let { WatchControlPolicy.access(it, snoozesUsed = 0) },
            )
        }
    }
    val alarms: Flow<List<Alarm>> = dao.getAllFlow().map { list -> list.map { it.toAlarm() } }
    val groups: Flow<List<String>> = dao.distinctGroupsFlow()

    suspend fun allEnabled(): List<Alarm> = dao.getAllEnabled().map { it.toAlarm() }

    suspend fun applyPresetToNextEnabled(preset: WakeupPreset): Alarm? {
        val now = System.currentTimeMillis()
        val next = allEnabled().minByOrNull { alarm ->
            alarm.specificDate ?: RustAlarmCore.calculateNextAlarmTimestamp(
                now,
                alarm.hour,
                alarm.minute,
                alarm.repeatDays.toIntArray(),
            )
        } ?: return null
        val updated = WakeupPresetApplier.apply(next, preset)
        save(updated)
        return updated
    }

    /** Most-recent DISMISSED event timestamp, or null if none. */
    suspend fun lastDismissedAt(): Long? = runCatching {
        eventDao.recentDismissed(1).firstOrNull()?.timestamp
    }.getOrNull()

    /** Number of DISMISSED events within the last [days] days. */
    suspend fun dismissedInLast(days: Int = 7): Int = runCatching {
        val since = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000L
        eventDao.dismissedCountSince(since)
    }.getOrDefault(0)

    suspend fun getById(id: Long): Alarm? = dao.getById(id)?.toAlarm()

    suspend fun save(alarm: Alarm): Long {
        val entity = AlarmEntity.fromAlarm(alarm)
        val savedId = if (alarm.id == 0L) {
            dao.insert(entity)
        } else {
            dao.update(entity.copy(updatedAt = System.currentTimeMillis()))
            alarm.id
        }
        val saved = alarm.copy(id = savedId)
        if (saved.enabled) scheduler.schedule(saved)
        refreshWidget()
        return savedId
    }

    suspend fun delete(alarm: Alarm) {
        scheduler.cancel(alarm.id)
        eventDao.deleteByAlarmId(alarm.id)
        dao.deleteById(alarm.id)
        refreshWidget()
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        dao.setEnabled(id, enabled)
        val alarm = dao.getById(id)?.toAlarm() ?: return
        if (enabled) scheduler.schedule(alarm) else scheduler.cancel(id)
        refreshWidget()
    }

    /** Toggle every alarm in a named group on/off; reschedules each accordingly. */
    suspend fun setGroupEnabled(tag: String, enabled: Boolean) {
        dao.setGroupEnabled(tag, enabled)
        dao.getByGroup(tag).forEach { entity ->
            val alarm = entity.toAlarm().copy(enabled = enabled)
            if (enabled) scheduler.schedule(alarm) else scheduler.cancel(entity.id)
        }
        refreshWidget()
    }

    suspend fun rescheduleAll() {
        dao.getAllEnabled().forEach { scheduler.schedule(it.toAlarm()) }
    }

    // ── JSON export / import ─────────────────────────

    suspend fun exportToJson(): String {
        val all = dao.getAllFlow().first()
        val arr = JSONArray()
        all.forEach { e ->
            arr.put(
                JSONObject().apply {
                    put("title", e.title)
                    put("hour", e.hour)
                    put("minute", e.minute)
                    put("repeatDays", e.repeatDays)
                    put("specificDate", e.specificDate)
                    put("enabled", e.enabled)
                    put("vibrate", e.vibrate)
                    put("soundEnabled", e.soundEnabled)
                    put("ringtoneUri", e.ringtoneUri)
                    put("challengeType", e.challengeType)
                    put("isSmartAlarm", e.isSmartAlarm)
                    put("smartWindowMinutes", e.smartWindowMinutes)
                    put("volumeRampSeconds", e.volumeRampSeconds)
                    put("groupTag", e.groupTag)
                    put("maxSnoozes", e.maxSnoozes)
                    put("message", e.message)
                    put("gradualWakeup", e.gradualWakeup)
                    put("mathProblemCount", e.mathProblemCount)
                    put("routineItems", e.routineItems)
                    put("youtubeUrl", e.youtubeUrl)
                    put("alarmVolumePercent", e.alarmVolumePercent)
                    put("preAlarmMinutes", e.preAlarmMinutes)
                }
            )
        }
        return JSONObject().apply {
            put("version", 1)
            put("alarms", arr)
        }.toString(2)
    }

    /**
     * Imports a previously-exported JSON. Adds new alarms (does not overwrite IDs).
     * Returns the number of alarms imported.
     */
    suspend fun importFromJson(json: String): Int {
        val root = JSONObject(json)
        val arr = root.optJSONArray("alarms") ?: return 0
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val alarm = Alarm(
                id = 0L,
                title = o.optString("title", "알람"),
                hour = o.optInt("hour", 8),
                minute = o.optInt("minute", 0),
                repeatDays = o.optString("repeatDays").split(",").mapNotNull { it.trim().toIntOrNull() },
                specificDate = if (o.isNull("specificDate")) null else o.optLong("specificDate"),
                enabled = o.optBoolean("enabled", true),
                vibrate = o.optBoolean("vibrate", true),
                soundEnabled = o.optBoolean("soundEnabled", true),
                ringtoneUri = if (o.isNull("ringtoneUri")) null else o.optString("ringtoneUri"),
                challengeType = runCatching {
                    ChallengeType.valueOf(o.optString("challengeType", "NONE"))
                }.getOrDefault(ChallengeType.NONE),
                isSmartAlarm = o.optBoolean("isSmartAlarm", false),
                smartWindowMinutes = o.optInt("smartWindowMinutes", 30),
                volumeRampSeconds = o.optInt("volumeRampSeconds", 0),
                groupTag = if (o.isNull("groupTag")) null else o.optString("groupTag"),
                maxSnoozes = o.optInt("maxSnoozes", 0),
                message = o.optString("message", ""),
                gradualWakeup = o.optBoolean("gradualWakeup", false),
                mathProblemCount = o.optInt("mathProblemCount", 1),
                routineItems = o.optString("routineItems", "").let {
                    if (it.isBlank()) emptyList()
                    else it.split("|").filter { x -> x.isNotBlank() }
                },
                youtubeUrl = if (o.isNull("youtubeUrl")) null else o.optString("youtubeUrl").ifBlank { null },
                alarmVolumePercent = o.optInt("alarmVolumePercent", 100),
                preAlarmMinutes = o.optInt("preAlarmMinutes", 15),
            )
            save(alarm)
            count++
        }
        return count
    }
}
