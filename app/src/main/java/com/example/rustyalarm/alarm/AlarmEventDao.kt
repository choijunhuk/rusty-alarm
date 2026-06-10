package com.example.rustyalarm.alarm

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class DailyCount(val dayKey: String, val cnt: Int)
data class ChallengeStat(val challengeType: String, val cnt: Int)
data class WakeupSummary(
    val fired: Int,
    val dismissed: Int,
    val snoozed: Int,
    val avgResponseSec: Double,
)

@Dao
interface AlarmEventDao {

    @Insert
    suspend fun insert(event: AlarmEvent): Long

    @Query("SELECT * FROM alarm_events ORDER BY timestamp DESC LIMIT :limit")
    fun recentFlow(limit: Int = 100): Flow<List<AlarmEvent>>

    /** Count of FIRED events per day, last [days] days. */
    @Query("""
        SELECT strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch', 'localtime') AS dayKey,
               COUNT(*) AS cnt
        FROM alarm_events
        WHERE eventType = 'FIRED'
          AND timestamp >= :sinceMillis
        GROUP BY dayKey
        ORDER BY dayKey ASC
    """)
    fun dailyFireCounts(sinceMillis: Long): Flow<List<DailyCount>>

    /** Counts per challenge type, total. */
    @Query("""
        SELECT challengeType, COUNT(*) AS cnt
        FROM alarm_events
        WHERE eventType = 'FIRED'
        GROUP BY challengeType
        ORDER BY cnt DESC
    """)
    fun challengeBreakdown(): Flow<List<ChallengeStat>>

    /** Aggregate response stats. */
    @Query("""
        SELECT
            SUM(CASE WHEN eventType = 'FIRED'     THEN 1 ELSE 0 END) AS fired,
            SUM(CASE WHEN eventType = 'DISMISSED' THEN 1 ELSE 0 END) AS dismissed,
            SUM(CASE WHEN eventType = 'SNOOZED'   THEN 1 ELSE 0 END) AS snoozed,
            COALESCE(AVG(CASE WHEN eventType = 'DISMISSED' THEN responseSeconds END), 0)
                AS avgResponseSec
        FROM alarm_events
    """)
    fun summaryFlow(): Flow<WakeupSummary>

    @Query("DELETE FROM alarm_events WHERE alarmId = :alarmId")
    suspend fun deleteByAlarmId(alarmId: Long)

    /** Distinct local-day keys with a DISMISSED event, newest-first. */
    @Query("""
        SELECT DISTINCT strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch', 'localtime') AS dayKey
        FROM alarm_events
        WHERE eventType = 'DISMISSED'
        ORDER BY dayKey DESC
    """)
    suspend fun dismissedDayKeys(): List<String>

    /** All events newer than [sinceMillis]. */
    @Query("SELECT * FROM alarm_events WHERE timestamp >= :sinceMillis ORDER BY timestamp ASC")
    suspend fun eventsSince(sinceMillis: Long): List<AlarmEvent>
}
