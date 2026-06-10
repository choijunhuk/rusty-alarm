package com.example.rustyalarm.alarm

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun getAllFlow(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getById(id: Long): AlarmEntity?

    @Query("SELECT * FROM alarms WHERE enabled = 1")
    suspend fun getAllEnabled(): List<AlarmEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarm: AlarmEntity): Long

    @Update
    suspend fun update(alarm: AlarmEntity)

    @Delete
    suspend fun delete(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE alarms SET enabled = :enabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM alarms WHERE groupTag = :tag")
    suspend fun getByGroup(tag: String): List<AlarmEntity>

    @Query("SELECT DISTINCT groupTag FROM alarms WHERE groupTag IS NOT NULL AND groupTag != ''")
    fun distinctGroupsFlow(): Flow<List<String>>

    @Query("UPDATE alarms SET enabled = :enabled, updatedAt = :updatedAt WHERE groupTag = :tag")
    suspend fun setGroupEnabled(tag: String, enabled: Boolean, updatedAt: Long = System.currentTimeMillis())
}
