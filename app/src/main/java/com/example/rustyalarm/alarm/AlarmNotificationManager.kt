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
    private const val CHANNEL_NAME = "알람"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "알람 알림 채널"
                enableVibration(true)
                setBypassDnd(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun showAlarmNotification(
        context: Context,
        alarmId: Long,
        title: String,
        hour: Int,
        minute: Int,
    ) {
        val ringIntent = Intent(context, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_TITLE, title)
            putExtra(AlarmReceiver.EXTRA_ALARM_HOUR, hour)
            putExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, minute)
        }
        val contentPi = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            ringIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DISMISS
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        val dismissPi = PendingIntent.getBroadcast(
            context,
            (alarmId + 1).toInt(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SNOOZE
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_TITLE, title)
            putExtra(AlarmReceiver.EXTRA_ALARM_HOUR, hour)
            putExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, minute)
        }
        val snoozePi = PendingIntent.getBroadcast(
            context,
            (alarmId + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

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

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(alarmId.toInt(), notification)
    }

    fun cancelNotification(context: Context, alarmId: Long) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(alarmId.toInt())
    }

    private fun Long.toInt(): Int = (this % Int.MAX_VALUE).toInt()
}
