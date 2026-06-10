package com.example.rustyalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Fired at the start of the smart-wake window. Boots the foreground sleep
 * monitor service. The service watches the accelerometer and either:
 *   - sends the alarm intent early (light sleep detected), or
 *   - lets the fallback exact alarm fire at the target time.
 */
class SmartAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val deadline = intent.getLongExtra(EXTRA_DEADLINE, System.currentTimeMillis())
        @Suppress("DEPRECATION")
        val alarmIntent = intent.getParcelableExtra<Intent>(EXTRA_ALARM_INTENT) ?: return
        val serviceIntent = SleepMonitorService.startIntent(context, deadline, alarmIntent)
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    companion object {
        const val ACTION_SMART_START = "com.example.rustyalarm.SMART_START"
        const val EXTRA_ALARM_INTENT = "smart_alarm_intent"
        const val EXTRA_DEADLINE     = "smart_deadline"
    }
}
