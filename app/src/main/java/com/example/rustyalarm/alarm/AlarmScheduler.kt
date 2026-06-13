package com.example.rustyalarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.rustyalarm.rust.RustAlarmCore
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(alarm: Alarm) {
        if (!alarm.enabled) return

        val specificDate = alarm.specificDate
        var triggerAtMillis = if (specificDate != null) {
            Calendar.getInstance().apply {
                timeInMillis = specificDate
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } else {
            RustAlarmCore.calculateNextAlarmTimestamp(
                currentTimestampMillis = System.currentTimeMillis(),
                hour = alarm.hour,
                minute = alarm.minute,
                repeatDays = alarm.repeatDays.toIntArray(),
            )
        }

        // Weather-adjusted wake-up: shift earlier when rain expected
        triggerAtMillis = applyWeatherAdjust(triggerAtMillis)

        // Fallback exact alarm — always armed
        val firePendingIntent = buildPendingIntent(alarm)
        setExact(triggerAtMillis, firePendingIntent)

        // Smart alarm: also schedule a window-start broadcast that boots the monitor service
        if (alarm.isSmartAlarm && alarm.smartWindowMinutes > 0) {
            val windowStart = triggerAtMillis - alarm.smartWindowMinutes * 60_000L
            if (windowStart > System.currentTimeMillis()) {
                val smartIntent = buildSmartStartIntent(alarm, triggerAtMillis)
                setExact(windowStart, smartIntent)
            }
        }

        // Pre-alarm nudge — configurable lead time (0 disables it)
        if (alarm.preAlarmMinutes > 0) {
            val preAt = triggerAtMillis - alarm.preAlarmMinutes * 60_000L
            if (preAt > System.currentTimeMillis()) {
                val preIntent = Intent(context, PreAlarmReceiver::class.java).apply {
                    putExtra(PreAlarmReceiver.EXTRA_TITLE, alarm.title)
                    putExtra(PreAlarmReceiver.EXTRA_MINUTES_BEFORE, alarm.preAlarmMinutes)
                }
                val pi = PendingIntent.getBroadcast(
                    context, alarm.id.toPreAlarmRequestCode(), preIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                setExact(preAt, pi)
            }
        }
    }

    fun cancel(alarmId: Long) {
        // Cancel main fire PendingIntent
        val fireIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_FIRED
        }
        PendingIntent.getBroadcast(
            context, alarmId.toRequestCode(), fireIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ).also {
            alarmManager.cancel(it)
            it.cancel()
        }
        // Cancel smart-start PendingIntent
        val smartIntent = Intent(context, SmartAlarmReceiver::class.java).apply {
            action = SmartAlarmReceiver.ACTION_SMART_START
        }
        PendingIntent.getBroadcast(
            context, alarmId.toSmartRequestCode(), smartIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ).also {
            alarmManager.cancel(it)
            it.cancel()
        }
        // Cancel pre-alarm PendingIntent
        val preIntent = Intent(context, PreAlarmReceiver::class.java)
        PendingIntent.getBroadcast(
            context, alarmId.toPreAlarmRequestCode(), preIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ).also {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun applyWeatherAdjust(triggerAtMillis: Long): Long {
        val (enabled, mins) = com.example.rustyalarm.prefs.UserPreferences
            .weatherAdjustSync(context)
        if (!enabled) return triggerAtMillis
        val prefs = context.getSharedPreferences(
            com.example.rustyalarm.prefs.UserPreferences.MIRROR_FILE,
            Context.MODE_PRIVATE,
        )
        val precip = prefs.getInt(
            com.example.rustyalarm.weather.WeatherFetcher.PRECIP_MIRROR_KEY, -1,
        )
        val cachedAt = prefs.getLong(
            com.example.rustyalarm.weather.WeatherFetcher.PRECIP_MIRROR_AT_KEY, 0L,
        )
        val fresh = System.currentTimeMillis() - cachedAt < 6L * 60 * 60 * 1000L
        val shifted = if (fresh && precip > 70) triggerAtMillis - mins * 60_000L
        else triggerAtMillis
        // Clamp to the future so the smart-window math at the call site doesn't
        // underflow when an alarm is scheduled <mins minutes from now.
        return shifted.coerceAtLeast(System.currentTimeMillis() + 1_000L)
    }

    private fun setExact(triggerAtMillis: Long, pi: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms())
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            else
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    private fun buildPendingIntent(alarm: Alarm): PendingIntent {
        val intent = buildFireIntent(alarm)
        return PendingIntent.getBroadcast(
            context, alarm.id.toRequestCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun buildFireIntent(alarm: Alarm): Intent =
        Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_FIRED
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_ALARM_TITLE, alarm.title)
            putExtra(AlarmReceiver.EXTRA_ALARM_HOUR, alarm.hour)
            putExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, alarm.minute)
            putExtra(AlarmReceiver.EXTRA_VIBRATE, alarm.vibrate)
            putExtra(AlarmReceiver.EXTRA_SOUND_ENABLED, alarm.soundEnabled)
            putExtra(AlarmReceiver.EXTRA_RINGTONE_URI, alarm.ringtoneUri)
            putExtra(AlarmReceiver.EXTRA_CHALLENGE_TYPE, alarm.challengeType.name)
            putExtra(AlarmReceiver.EXTRA_REPEAT_DAYS, alarm.repeatDays.toIntArray())
            putExtra(AlarmReceiver.EXTRA_VOLUME_RAMP_SECONDS, alarm.volumeRampSeconds)
            putExtra(AlarmReceiver.EXTRA_MAX_SNOOZES, alarm.maxSnoozes)
            putExtra(AlarmReceiver.EXTRA_MESSAGE, alarm.message)
            putExtra(AlarmReceiver.EXTRA_GRADUAL_WAKEUP, alarm.gradualWakeup)
            putExtra(AlarmReceiver.EXTRA_MATH_PROBLEM_COUNT, alarm.mathProblemCount)
            putExtra(AlarmReceiver.EXTRA_ROUTINE_ITEMS, alarm.routineItems.toTypedArray())
            alarm.youtubeUrl?.let { putExtra(AlarmReceiver.EXTRA_YOUTUBE_URL, it) }
            putExtra(AlarmReceiver.EXTRA_ALARM_VOLUME_PERCENT, alarm.alarmVolumePercent)
            alarm.geofenceLat?.let { putExtra(AlarmReceiver.EXTRA_GEOFENCE_LAT, it) }
            alarm.geofenceLng?.let { putExtra(AlarmReceiver.EXTRA_GEOFENCE_LNG, it) }
            putExtra(AlarmReceiver.EXTRA_GEOFENCE_RADIUS, alarm.geofenceRadius)
        }

    private fun buildSmartStartIntent(alarm: Alarm, deadlineMillis: Long): PendingIntent {
        val intent = Intent(context, SmartAlarmReceiver::class.java).apply {
            action = SmartAlarmReceiver.ACTION_SMART_START
            putExtra(SmartAlarmReceiver.EXTRA_DEADLINE, deadlineMillis)
            putExtra(SmartAlarmReceiver.EXTRA_ALARM_INTENT, buildFireIntent(alarm))
        }
        return PendingIntent.getBroadcast(
            context, alarm.id.toSmartRequestCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun Long.toRequestCode(): Int = (this % Int.MAX_VALUE).toInt()
    private fun Long.toSmartRequestCode(): Int =
        ((this + SMART_REQUEST_OFFSET) % Int.MAX_VALUE).toInt()
    private fun Long.toPreAlarmRequestCode(): Int =
        ((this + PRE_ALARM_REQUEST_OFFSET) % Int.MAX_VALUE).toInt()

    companion object {
        private const val SMART_REQUEST_OFFSET = 500_000_000L
        private const val PRE_ALARM_REQUEST_OFFSET = 700_000_000L
        private const val PRE_ALARM_LEAD_MINUTES = 15L
    }
}
