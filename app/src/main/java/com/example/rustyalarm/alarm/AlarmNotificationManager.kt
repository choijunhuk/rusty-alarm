package com.example.rustyalarm.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.rustyalarm.AlarmRingActivity

object AlarmNotificationManager {

    const val CHANNEL_ID = "rusty_alarm_channel"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "알람",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "알람 알림 채널"
                enableVibration(true)
                setBypassDnd(true)
                setSound(null, null)   // sound handled by AlarmRingActivity MediaPlayer
            }
            nm(context).createNotificationChannel(channel)
        }
    }

    fun showAlarmNotification(
        context: Context,
        alarmId: Long,
        title: String,
        hour: Int,
        minute: Int,
        soundEnabled: Boolean = true,
        challengeType: String = ChallengeType.NONE.name,
    ) {
        val ringIntent = Intent(context, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_TITLE, title)
            putExtra(AlarmReceiver.EXTRA_ALARM_HOUR, hour)
            putExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, minute)
            putExtra(AlarmReceiver.EXTRA_SOUND_ENABLED, soundEnabled)
            putExtra(AlarmReceiver.EXTRA_CHALLENGE_TYPE, challengeType)
        }
        val contentPi = PendingIntent.getActivity(
            context, alarmId.toInt(), ringIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val dismissPi = buildBroadcastPi(context, AlarmReceiver.ACTION_DISMISS, alarmId, (alarmId + 1).toInt()) {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        val snoozePi = buildBroadcastPi(context, AlarmReceiver.ACTION_SNOOZE, alarmId, (alarmId + 2).toInt()) {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_TITLE, title)
            putExtra(AlarmReceiver.EXTRA_ALARM_HOUR, hour)
            putExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, minute)
            putExtra(AlarmReceiver.EXTRA_SOUND_ENABLED, soundEnabled)
        }

        val timeText = "%02d:%02d".format(hour, minute)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(timeText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(contentPi, true)
            .setContentIntent(contentPi)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "끄기", dismissPi)
            .addAction(android.R.drawable.ic_menu_recent_history, "5분 뒤", snoozePi)
            .build()

        nm(context).notify(alarmId.toInt(), notification)
    }

    fun cancelNotification(context: Context, alarmId: Long) {
        nm(context).cancel(alarmId.toInt())
    }

    private fun buildBroadcastPi(
        context: Context,
        action: String,
        alarmId: Long,
        requestCode: Int,
        extras: Intent.() -> Unit,
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
            extras()
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun nm(context: Context) =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun Long.toInt(): Int = (this % Int.MAX_VALUE).toInt()
}
