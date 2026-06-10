package com.example.rustyalarm

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rustyalarm.alarm.AlarmDatabase
import com.example.rustyalarm.alarm.AlarmEvent
import com.example.rustyalarm.alarm.AlarmEventType
import com.example.rustyalarm.alarm.AlarmNotificationManager
import com.example.rustyalarm.alarm.AlarmReceiver
import com.example.rustyalarm.alarm.AlarmSchedulerSnooze
import com.example.rustyalarm.alarm.Alarm
import com.example.rustyalarm.alarm.ChallengeType
import com.example.rustyalarm.prefs.ThemeMode
import com.example.rustyalarm.prefs.ThemePreferences
import com.example.rustyalarm.ui.screens.AlarmRingScreen
import com.example.rustyalarm.ui.theme.RustyAlarmTheme
import androidx.compose.runtime.getValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlarmRingActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val rampScope = MainScope()
    private var rampJob: Job? = null

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

        val alarmId       = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L)
        val title         = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_TITLE) ?: "알람"
        val hour          = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_HOUR, 0)
        val minute        = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, 0)
        val vibrate       = intent.getBooleanExtra(AlarmReceiver.EXTRA_VIBRATE, true)
        val soundEnabled  = intent.getBooleanExtra(AlarmReceiver.EXTRA_SOUND_ENABLED, true)
        val ringtoneUri   = intent.getStringExtra(AlarmReceiver.EXTRA_RINGTONE_URI)
        val volumeRamp    = intent.getIntExtra(AlarmReceiver.EXTRA_VOLUME_RAMP_SECONDS, 0)
        val challengeName = intent.getStringExtra(AlarmReceiver.EXTRA_CHALLENGE_TYPE)
            ?: ChallengeType.NONE.name
        val challengeType = runCatching { ChallengeType.valueOf(challengeName) }
            .getOrDefault(ChallengeType.NONE)

        if (soundEnabled) startAlarmSound(ringtoneUri, volumeRamp)
        if (vibrate)      startVibration()

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
                        onDismiss = {
                            stopSounds()
                            AlarmNotificationManager.cancelNotification(this, alarmId)
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
                                // Pet: +10 EXP for dismiss, +5 bonus when a challenge gated the dismiss
                                val bonus = if (challengeType != ChallengeType.NONE) 5 else 0
                                db.petDao().addExp(10 + bonus)
                            }
                            finish()
                        },
                        onSnooze = {
                            stopSounds()
                            AlarmNotificationManager.cancelNotification(this, alarmId)
                            val snoozeMillis = System.currentTimeMillis() + 5 * 60 * 1000L
                            val snooze = Alarm(
                                id           = alarmId + AlarmReceiver.SNOOZE_ID_OFFSET,
                                title        = "$title (다시 알림)",
                                hour         = hour,
                                minute       = minute,
                                vibrate      = vibrate,
                                soundEnabled = soundEnabled,
                                ringtoneUri  = ringtoneUri,
                                challengeType = ChallengeType.NONE,
                            )
                            CoroutineScope(Dispatchers.IO).launch {
                                AlarmDatabase.getDatabase(this@AlarmRingActivity)
                                    .alarmEventDao().insert(
                                        AlarmEvent(
                                            alarmId = alarmId,
                                            eventType = AlarmEventType.SNOOZED.name,
                                            challengeType = challengeType.name,
                                        )
                                    )
                                AlarmSchedulerSnooze(this@AlarmRingActivity).scheduleAt(snooze, snoozeMillis)
                            }
                            finish()
                        },
                    )
                }
            }
        }
    }

    private fun startAlarmSound(ringtoneUriStr: String?, rampSeconds: Int) {
        val uri = ringtoneUriStr?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: return
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmRingActivity, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                if (rampSeconds > 0) setVolume(0f, 0f)
                prepareAsync()
                setOnPreparedListener {
                    start()
                    if (rampSeconds > 0) startVolumeRamp(rampSeconds)
                }
            }
        } catch (_: Exception) {}
    }

    private fun startVolumeRamp(rampSeconds: Int) {
        rampJob?.cancel()
        rampJob = rampScope.launch {
            val steps = rampSeconds * 4   // 4 steps per second
            for (i in 1..steps) {
                val v = i / steps.toFloat()
                runCatching { mediaPlayer?.setVolume(v, v) }
                delay(250)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun startVibration() {
        val pattern = longArrayOf(0, 500, 300, 500, 300, 500)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                it.vibrate(VibrationEffect.createWaveform(pattern, 0))
            else
                it.vibrate(pattern, 0)
        }
    }

    private fun stopSounds() {
        rampJob?.cancel()
        rampJob = null
        mediaPlayer?.runCatching { stop(); release() }
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSounds()
        rampScope.cancel()
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent) }
}
