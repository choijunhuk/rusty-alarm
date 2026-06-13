package com.example.rustyalarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.rustyalarm.alarm.AlarmDatabase
import com.example.rustyalarm.alarm.AlarmEvent
import com.example.rustyalarm.alarm.AlarmEventType
import com.example.rustyalarm.alarm.AlarmNotificationManager
import com.example.rustyalarm.alarm.AlarmReceiver
import com.example.rustyalarm.alarm.AlarmRingService
import com.example.rustyalarm.alarm.AlarmSchedulerSnooze
import com.example.rustyalarm.alarm.Alarm
import com.example.rustyalarm.alarm.ChallengeType
import com.example.rustyalarm.ui.screens.AlarmRingScreen
import com.example.rustyalarm.ui.theme.RustyAlarmTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * UI-only host for the active alarm. Sound + vibration live in
 * [AlarmRingService] so pressing Home or Back cannot silence the alarm —
 * the service keeps running and re-presents this Activity via the
 * full-screen notification intent.
 */
class AlarmRingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        // Block back press — user must solve the challenge / press dismiss.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Intentionally do nothing. Alarm cannot be dismissed via back.
            }
        })

        val alarmId       = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L)
        val title         = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_TITLE) ?: "알람"
        val hour          = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_HOUR, 0)
        val minute        = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, 0)
        val vibrate       = intent.getBooleanExtra(AlarmReceiver.EXTRA_VIBRATE, true)
        val soundEnabled  = intent.getBooleanExtra(AlarmReceiver.EXTRA_SOUND_ENABLED, true)
        val ringtoneUri   = intent.getStringExtra(AlarmReceiver.EXTRA_RINGTONE_URI)
        val maxSnoozes    = intent.getIntExtra(AlarmReceiver.EXTRA_MAX_SNOOZES, 0)
        val message       = intent.getStringExtra(AlarmReceiver.EXTRA_MESSAGE) ?: ""
        val mathProblemCount = intent.getIntExtra(AlarmReceiver.EXTRA_MATH_PROBLEM_COUNT, 1)
        val routineItems = intent.getStringArrayExtra(AlarmReceiver.EXTRA_ROUTINE_ITEMS)?.toList() ?: emptyList()
        val youtubeUrl = intent.getStringExtra(AlarmReceiver.EXTRA_YOUTUBE_URL)
        val geofenceLat   = if (intent.hasExtra(AlarmReceiver.EXTRA_GEOFENCE_LAT))
            intent.getDoubleExtra(AlarmReceiver.EXTRA_GEOFENCE_LAT, 0.0) else null
        val geofenceLng   = if (intent.hasExtra(AlarmReceiver.EXTRA_GEOFENCE_LNG))
            intent.getDoubleExtra(AlarmReceiver.EXTRA_GEOFENCE_LNG, 0.0) else null
        val geofenceRadius = intent.getIntExtra(AlarmReceiver.EXTRA_GEOFENCE_RADIUS, 100)
        val challengeName = intent.getStringExtra(AlarmReceiver.EXTRA_CHALLENGE_TYPE)
            ?: ChallengeType.NONE.name
        val challengeType = runCatching { ChallengeType.valueOf(challengeName) }
            .getOrDefault(ChallengeType.NONE)

        val snoozesRemaining = if (maxSnoozes <= 0) Int.MAX_VALUE else {
            val used = getSharedPreferences("rusty_alarm_stats", MODE_PRIVATE)
                .getInt("snooze_count_$alarmId", 0)
            (maxSnoozes - used).coerceAtLeast(0)
        }

        val firedAt = System.currentTimeMillis()

        setContent {
            RustyAlarmTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AlarmRingScreen(
                        alarmId       = alarmId,
                        title         = title,
                        hour          = hour,
                        minute        = minute,
                        challengeType = challengeType,
                        message       = message,
                        snoozesRemaining = snoozesRemaining,
                        geofenceLat = geofenceLat,
                        geofenceLng = geofenceLng,
                        geofenceRadius = geofenceRadius,
                        mathProblemCount = mathProblemCount,
                        routineItems = routineItems,
                        youtubeUrl = youtubeUrl,
                        onDismiss = {
                            AlarmRingService.stop(this)
                            AlarmNotificationManager.cancelNotification(this, alarmId)
                            getSharedPreferences("rusty_alarm_stats", MODE_PRIVATE)
                                .edit().remove("snooze_count_$alarmId").apply()
                            val responseSec = (System.currentTimeMillis() - firedAt) / 1000L
                            CoroutineScope(Dispatchers.IO).launch {
                                val db = AlarmDatabase.getDatabase(this@AlarmRingActivity)
                                db.alarmEventDao().insert(
                                    AlarmEvent(
                                        alarmId = alarmId,
                                        eventType = AlarmEventType.DISMISSED.name,
                                        responseSeconds = responseSec,
                                        challengeType = challengeType.name,
                                    )
                                )
                                val bonus = if (challengeType != ChallengeType.NONE) 5 else 0
                                db.petDao().addExp(10 + bonus)
                            }
                            finishAndRemoveTask()
                        },
                        onSnooze = snooze@{
                            if (snoozesRemaining <= 0) return@snooze
                            // Single source of truth: dispatch ACTION_SNOOZE to
                            // AlarmReceiver. The receiver handles counter,
                            // logging, and scheduling. Prevents double-count
                            // when the notification snooze action also fires.
                            val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
                                action = AlarmReceiver.ACTION_SNOOZE
                                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                                putExtra(AlarmReceiver.EXTRA_ALARM_TITLE, title)
                                putExtra(AlarmReceiver.EXTRA_ALARM_HOUR, hour)
                                putExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, minute)
                                putExtra(AlarmReceiver.EXTRA_VIBRATE, vibrate)
                                putExtra(AlarmReceiver.EXTRA_SOUND_ENABLED, soundEnabled)
                                putExtra(AlarmReceiver.EXTRA_RINGTONE_URI, ringtoneUri)
                                putExtra(AlarmReceiver.EXTRA_CHALLENGE_TYPE, challengeType.name)
                                putExtra(AlarmReceiver.EXTRA_MAX_SNOOZES, maxSnoozes)
                            }
                            sendBroadcast(snoozeIntent)
                            finishAndRemoveTask()
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent) }
}
