package com.example.rustyalarm.pet

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {

    @Query("SELECT * FROM pet WHERE id = 1")
    fun observe(): Flow<Pet?>

    @Query("SELECT * FROM pet WHERE id = 1")
    suspend fun get(): Pet?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(pet: Pet): Long

    @Update
    suspend fun update(pet: Pet)

    @Query("UPDATE pet SET exp = exp + :delta, lastFedAt = :now WHERE id = 1")
    suspend fun addExp(delta: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE pet SET name = :name WHERE id = 1")
    suspend fun rename(name: String)
}
