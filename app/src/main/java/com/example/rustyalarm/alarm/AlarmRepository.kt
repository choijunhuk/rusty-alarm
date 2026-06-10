package com.example.rustyalarm.alarm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlarmRepository(
    private val dao: AlarmDao,
    private val scheduler: AlarmScheduler,
) {
    val alarms: Flow<List<Alarm>> = dao.getAllFlow().map { list ->
        list.map { it.toAlarm() }
    }

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
        dao.deleteById(alarm.id)
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        dao.setEnabled(id, enabled)
        val alarm = dao.getById(id)?.toAlarm() ?: return
        if (enabled) scheduler.schedule(alarm) else scheduler.cancel(id)
    }

    suspend fun rescheduleAll() {
        dao.getAllEnabled().forEach { entity ->
            scheduler.schedule(entity.toAlarm())
        }
    }
}
