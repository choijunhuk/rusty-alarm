package com.example.rustyalarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.rustyalarm.alarm.AlarmNotificationManager
import com.example.rustyalarm.alarm.AlarmReceiver
import com.example.rustyalarm.alarm.AlarmSchedulerSnooze
import com.example.rustyalarm.alarm.Alarm
import com.example.rustyalarm.ui.screens.AlarmRingScreen
import com.example.rustyalarm.ui.theme.RustyAlarmTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmRingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock screen and keep screen on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            )
        }

        val alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L)
        val title   = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_TITLE) ?: "알람"
        val hour    = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_HOUR, 0)
        val minute  = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, 0)
        val vibrate = intent.getBooleanExtra(AlarmReceiver.EXTRA_VIBRATE, true)

        setContent {
            RustyAlarmTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AlarmRingScreen(
                        alarmId = alarmId,
                        title = title,
                        hour = hour,
                        minute = minute,
                        onDismiss = {
                            AlarmNotificationManager.cancelNotification(this, alarmId)
                            finish()
                        },
                        onSnooze = {
                            AlarmNotificationManager.cancelNotification(this, alarmId)
                            val snoozeMillis = System.currentTimeMillis() + 5 * 60 * 1000L
                            val snoozeAlarm = Alarm(
                                id = alarmId + AlarmReceiver.SNOOZE_ID_OFFSET,
                                title = "$title (다시 알림)",
                                hour = hour,
                                minute = minute,
                                vibrate = vibrate,
                            )
                            CoroutineScope(Dispatchers.IO).launch {
                                AlarmSchedulerSnooze(this@AlarmRingActivity)
                                    .scheduleAt(snoozeAlarm, snoozeMillis)
                            }
                            finish()
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
