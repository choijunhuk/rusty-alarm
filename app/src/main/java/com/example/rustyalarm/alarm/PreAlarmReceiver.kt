package com.example.rustyalarm.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.rustyalarm.MainActivity
import com.example.rustyalarm.R

/**
 * Fires 15 minutes before a main alarm. Posts a low-importance notification and
 * gently vibrates to nudge the user out of deep sleep without fully waking them.
 */
class PreAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "곧 알람이 울려요"
        val mins  = intent.getIntExtra(EXTRA_MINUTES_BEFORE, 15)

        ensureChannel(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPi = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("☀ $title")
            .setContentText("${mins}분 후 알람이 울려요. 천천히 깨어나세요.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(tapPi)
            .setAutoCancel(true)
            .setSilent(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        runCatching { nm.notify(NOTIF_ID, notif) }

        // Gentle 1.2s pre-vibrate — strong enough to surface awareness, soft
        // enough to stay below "wake up now" threshold.
        runCatching {
            val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                    .defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 250, 200, 250, 200, 250), -1),
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 250, 200, 250, 200, 250), -1)
            }
        }
    }

    private fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val ch = NotificationChannel(
                CHANNEL_ID, "알람 미리알림",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "기상 알람 직전 부드러운 안내 알림"
                setShowBadge(false)
            }
            nm.createNotificationChannel(ch)
        }
    }

    companion object {
        const val EXTRA_TITLE = "pre_alarm_title"
        const val EXTRA_MINUTES_BEFORE = "pre_alarm_minutes_before"
        private const val CHANNEL_ID = "pre_alarm"
        private const val NOTIF_ID   = 7777
    }
}
