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
        val volumeRamp      = intent.getIntExtra(EXTRA_VOLUME_RAMP_SECONDS, 0)
        val maxSnoozes      = intent.getIntExtra(EXTRA_MAX_SNOOZES, 0)
        val message         = intent.getStringExtra(EXTRA_MESSAGE) ?: ""

        AlarmNotificationManager.showAlarmNotification(
            context, alarmId, title, hour, minute, soundEnabled, ringtoneUri, challengeType,
            volumeRamp,
        )

        // Belt-and-suspenders: also start the ring activity directly so the
        // alarm screen appears even on devices where the full-screen intent
        // is downgraded to a heads-up notification (e.g. OEM customisations,
        // Android 14+ when the app loses USE_FULL_SCREEN_INTENT permission).
        val ringActivity = Intent(context, com.example.rustyalarm.AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_TITLE, title)
            putExtra(EXTRA_ALARM_HOUR, hour)
            putExtra(EXTRA_ALARM_MINUTE, minute)
            putExtra(EXTRA_VIBRATE, vibrate)
            putExtra(EXTRA_SOUND_ENABLED, soundEnabled)
            putExtra(EXTRA_RINGTONE_URI, ringtoneUri)
            putExtra(EXTRA_CHALLENGE_TYPE, challengeType)
            putExtra(EXTRA_VOLUME_RAMP_SECONDS, volumeRamp)
            putExtra(EXTRA_MAX_SNOOZES, maxSnoozes)
            putExtra(EXTRA_MESSAGE, message)
        }
        runCatching { context.startActivity(ringActivity) }

        if (vibrate) vibrate(context)

        val now = System.currentTimeMillis()
        rememberFiredAt(context, alarmId, now)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AlarmDatabase.getDatabase(context)
                db.alarmEventDao().insert(
                    AlarmEvent(
                        alarmId = alarmId,
                        eventType = AlarmEventType.FIRED.name,
                        timestamp = now,
                        challengeType = challengeType,
                    )
                )

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
        val challengeType = intent.getStringExtra(EXTRA_CHALLENGE_TYPE) ?: ChallengeType.NONE.name
        val maxSnoozes   = intent.getIntExtra(EXTRA_MAX_SNOOZES, 0)

        AlarmNotificationManager.cancelNotification(context, alarmId)

        // Honour snooze cap (max == 0 means unlimited)
        val prefs = context.getSharedPreferences("rusty_alarm_stats", Context.MODE_PRIVATE)
        val key = "snooze_count_$alarmId"
        val used = prefs.getInt(key, 0)
        if (maxSnoozes > 0 && used >= maxSnoozes) {
            // Cap reached — treat as dismiss
            return
        }
        prefs.edit().putInt(key, used + 1).apply()

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
                AlarmDatabase.getDatabase(context).alarmEventDao().insert(
                    AlarmEvent(
                        alarmId = alarmId,
                        eventType = AlarmEventType.SNOOZED.name,
                        challengeType = challengeType,
                    )
                )
                AlarmSchedulerSnooze(context).scheduleAt(snoozeAlarm, snoozeMillis)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleDismiss(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        AlarmNotificationManager.cancelNotification(context, alarmId)
        // Reset snooze counter once the user actually dismisses
        context.getSharedPreferences("rusty_alarm_stats", Context.MODE_PRIVATE)
            .edit().remove("snooze_count_$alarmId").apply()

        val firedAt = consumeFiredAt(context, alarmId)
        val responseSec = firedAt?.let { (System.currentTimeMillis() - it) / 1000L }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AlarmDatabase.getDatabase(context).alarmEventDao().insert(
                    AlarmEvent(
                        alarmId = alarmId,
                        eventType = AlarmEventType.DISMISSED.name,
                        responseSeconds = responseSec,
                    )
                )
            } finally {
                pendingResult.finish()
            }
        }
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

    private fun rememberFiredAt(context: Context, alarmId: Long, ts: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong("fired_$alarmId", ts).apply()
    }

    private fun consumeFiredAt(context: Context, alarmId: Long): Long? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = "fired_$alarmId"
        val v = if (prefs.contains(key)) prefs.getLong(key, -1L) else null
        prefs.edit().remove(key).apply()
        return v
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
        const val EXTRA_VOLUME_RAMP_SECONDS = "volume_ramp_seconds"
        const val EXTRA_MAX_SNOOZES    = "max_snoozes"
        const val EXTRA_MESSAGE        = "message"

        const val SNOOZE_ID_OFFSET     = 100_000L
        private const val PREFS        = "rusty_alarm_stats"
    }
}
