package com.example.rustyalarm

import android.app.Application
import com.example.rustyalarm.alarm.AlarmDatabase
import dagger.hilt.android.HiltAndroidApp
import com.example.rustyalarm.alarm.AlarmNotificationManager
import com.example.rustyalarm.alarm.AlarmRepository
import com.example.rustyalarm.alarm.AlarmScheduler
import com.example.rustyalarm.backup.BackupWorker
import com.example.rustyalarm.pet.Pet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class RustyAlarmApplication : Application() {

    val database by lazy { AlarmDatabase.getDatabase(this) }

    val repository by lazy {
        AlarmRepository(
            dao = database.alarmDao(),
            eventDao = database.alarmEventDao(),
            scheduler = AlarmScheduler(this),
            appContext = applicationContext,
        )
    }

    override fun onCreate() {
        super.onCreate()
        AlarmNotificationManager.createNotificationChannel(this)
        // Ensure the singleton pet row exists (no-op if already inserted)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            database.petDao().insert(Pet())
        }
        BackupWorker.ensureScheduled(this)
    }
}
