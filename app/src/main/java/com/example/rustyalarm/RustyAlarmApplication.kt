package com.example.rustyalarm

import android.app.Application
import com.example.rustyalarm.alarm.AlarmDatabase
import com.example.rustyalarm.alarm.AlarmNotificationManager
import com.example.rustyalarm.alarm.AlarmRepository
import com.example.rustyalarm.alarm.AlarmScheduler

class RustyAlarmApplication : Application() {

    val database by lazy { AlarmDatabase.getDatabase(this) }
    val repository by lazy {
        AlarmRepository(
            dao = database.alarmDao(),
            scheduler = AlarmScheduler(this),
        )
    }

    override fun onCreate() {
        super.onCreate()
        AlarmNotificationManager.createNotificationChannel(this)
    }
}
