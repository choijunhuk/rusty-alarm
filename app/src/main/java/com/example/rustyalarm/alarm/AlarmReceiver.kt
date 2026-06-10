package com.example.rustyalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ALARM_FIRED -> handleAlarmFired(context, intent)
            ACTION_SNOOZE      -> handleSnooze(context, intent)
            ACTION_DISMISS     -> handleDismiss(context, intent)
        }
    }

    private fun handleAlarmFired(context: Context, intent: Intent) {
        val pendingResult   = goAsync()
        val alarmId         = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        val title           = intent.getStringExtra(EXTRA_ALARM_TITLE) ?: "알람"
        val hour            = intent.getIntExtra(EXTRA_ALARM_HOUR, 0)
        val minute          = intent.getIntExtra(EXTRA_ALARM_MINUTE, 0)
        val vibrate         = intent.getBooleanExtra(EXTRA_VIBRATE, true)
        val soundEnabled    = intent.getBooleanExtra(EXTRA_SOUND_ENABLED, true)
        val ringtoneUri     = intent.getStringExtra(EXTRA_RINGTONE_URI)
        val challengeType   = intent.getStringExtra(EXTRA_CHALLENGE_TYPE) ?: ChallengeType.NONE.name
        val repeatDays      = intent.getIntArrayExtra(EXTRA_REPEAT_DAYS) ?: intArrayOf()

        AlarmNotificationManager.showAlarmNotification(
            context, alarmId, title, hour, minute, soundEnabled, ringtoneUri, challengeType,
        )

        if (vibrate) vibrate(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db        = AlarmDatabase.getDatabase(context)
                val scheduler = AlarmScheduler(context)
                if (repeatDays.isNotEmpty()) {
                    db.alarmDao().getById(alarmId)?.let {
                        if (it.enabled) scheduler.schedule(it.toAlarm())
                    }
                } else {
                    db.alarmDao().setEnabled(alarmId, false)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleSnooze(context: Context, intent: Intent) {
        val alarmId      = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        val title        = intent.getStringExtra(EXTRA_ALARM_TITLE) ?: "알람"
        val hour         = intent.getIntExtra(EXTRA_ALARM_HOUR, 0)
        val minute       = intent.getIntExtra(EXTRA_ALARM_MINUTE, 0)
        val vibrate      = intent.getBooleanExtra(EXTRA_VIBRATE, true)
        val soundEnabled = intent.getBooleanExtra(EXTRA_SOUND_ENABLED, true)
        val ringtoneUri  = intent.getStringExtra(EXTRA_RINGTONE_URI)

        AlarmNotificationManager.cancelNotification(context, alarmId)

        val snoozeMillis = System.currentTimeMillis() + 5 * 60 * 1000L
        val snoozeAlarm  = Alarm(
            id           = alarmId + SNOOZE_ID_OFFSET,
            title        = "$title (다시 알림)",
            hour         = hour,
            minute       = minute,
            vibrate      = vibrate,
            soundEnabled = soundEnabled,
            ringtoneUri  = ringtoneUri,
            challengeType = ChallengeType.NONE,
        )

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AlarmSchedulerSnooze(context).scheduleAt(snoozeAlarm, snoozeMillis)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleDismiss(context: Context, intent: Intent) {
        AlarmNotificationManager.cancelNotification(context, intent.getLongExtra(EXTRA_ALARM_ID, -1L))
    }

    @Suppress("DEPRECATION")
    private fun vibrate(context: Context) {
        val pattern = longArrayOf(0, 500, 300, 500, 300, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                v.vibrate(VibrationEffect.createWaveform(pattern, 0))
            else
                v.vibrate(pattern, 0)
        }
    }

    companion object {
        const val ACTION_ALARM_FIRED   = "com.example.rustyalarm.ALARM_FIRED"
        const val ACTION_SNOOZE        = "com.example.rustyalarm.SNOOZE"
        const val ACTION_DISMISS       = "com.example.rustyalarm.DISMISS"

        const val EXTRA_ALARM_ID       = "alarm_id"
        const val EXTRA_ALARM_TITLE    = "alarm_title"
        const val EXTRA_ALARM_HOUR     = "alarm_hour"
        const val EXTRA_ALARM_MINUTE   = "alarm_minute"
        const val EXTRA_VIBRATE        = "vibrate"
        const val EXTRA_SOUND_ENABLED  = "sound_enabled"
        const val EXTRA_RINGTONE_URI   = "ringtone_uri"
        const val EXTRA_CHALLENGE_TYPE = "challenge_type"
        const val EXTRA_REPEAT_DAYS    = "repeat_days"

        const val SNOOZE_ID_OFFSET     = 100_000L
    }
}
