package com.example.rustyalarm.alarm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class AlarmRepository(
    private val dao: AlarmDao,
    private val eventDao: AlarmEventDao,
    private val scheduler: AlarmScheduler,
) {
    val alarms: Flow<List<Alarm>> = dao.getAllFlow().map { list -> list.map { it.toAlarm() } }
    val groups: Flow<List<String>> = dao.distinctGroupsFlow()

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
        return savedId
    }

    suspend fun delete(alarm: Alarm) {
        scheduler.cancel(alarm.id)
        eventDao.deleteByAlarmId(alarm.id)
        dao.deleteById(alarm.id)
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        dao.setEnabled(id, enabled)
        val alarm = dao.getById(id)?.toAlarm() ?: return
        if (enabled) scheduler.schedule(alarm) else scheduler.cancel(id)
    }

    /** Toggle every alarm in a named group on/off; reschedules each accordingly. */
    suspend fun setGroupEnabled(tag: String, enabled: Boolean) {
        dao.setGroupEnabled(tag, enabled)
        dao.getByGroup(tag).forEach { entity ->
            val alarm = entity.toAlarm().copy(enabled = enabled)
            if (enabled) scheduler.schedule(alarm) else scheduler.cancel(entity.id)
        }
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
            )
            save(alarm)
            count++
        }
        return count
    }
}

