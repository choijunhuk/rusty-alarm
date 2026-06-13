package com.example.rustyalarm.alarm

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.rustyalarm.pet.Pet
import com.example.rustyalarm.pet.PetDao

@Database(
    entities = [AlarmEntity::class, AlarmEvent::class, Pet::class],
    version = 12,
    exportSchema = false,
)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun alarmEventDao(): AlarmEventDao
    abstract fun petDao(): PetDao

    companion object {
        @Volatile
        private var INSTANCE: AlarmDatabase? = null

        // v9 → v10: routineItems (pipe-delimited string) added.
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE alarms ADD COLUMN routineItems TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        // v10 → v11: youtubeUrl + alarmVolumePercent.
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN youtubeUrl TEXT")
                db.execSQL(
                    "ALTER TABLE alarms ADD COLUMN alarmVolumePercent INTEGER NOT NULL DEFAULT 100"
                )
            }
        }

        // v11 → v12: preAlarmMinutes (0 disables pre-alarm).
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE alarms ADD COLUMN preAlarmMinutes INTEGER NOT NULL DEFAULT 15"
                )
            }
        }

        fun getDatabase(context: Context): AlarmDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AlarmDatabase::class.java,
                    "alarm_database"
                )
                    .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                    // Safety net: if migration is missing for some path, rebuild
                    // instead of crashing — alarm data is recoverable from JSON export.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
