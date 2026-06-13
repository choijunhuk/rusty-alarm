package com.example.rustyalarm.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.rustyalarm.AlarmRingActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground service that owns the alarm sound + vibration so the alarm
 * cannot be silenced by pressing Home, Back, or by the Activity being
 * destroyed for any reason. The Activity is only the UI shell — this
 * service is the source of truth for "is the alarm ringing?".
 *
 * Lifecycle:
 *  - START_RING starts (or no-ops if same alarm already ringing).
 *  - STOP cancels everything and stops the service.
 *  - Service auto-stops after AUTO_QUIET_MILLIS to avoid running forever.
 */
class AlarmRingService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val scope = MainScope()
    private var rampJob: Job? = null
    private var timeoutJob: Job? = null
    private var savedAlarmVolume: Int = -1
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentAlarmId: Long = -1L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopRinging()
                return START_NOT_STICKY
            }
        }

        val alarmId = intent?.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L) ?: -1L
        // Already ringing this alarm — keep going, don't restart sound
        if (currentAlarmId == alarmId && mediaPlayer != null) {
            startForegroundNotification(intent)
            return START_STICKY
        }
        currentAlarmId = alarmId

        acquireWakeLock()
        startForegroundNotification(intent)

        val soundEnabled = intent?.getBooleanExtra(AlarmReceiver.EXTRA_SOUND_ENABLED, true) ?: true
        val vibrate      = intent?.getBooleanExtra(AlarmReceiver.EXTRA_VIBRATE, true) ?: true
        val ringtoneUri  = intent?.getStringExtra(AlarmReceiver.EXTRA_RINGTONE_URI)
        val volumeRamp   = intent?.getIntExtra(AlarmReceiver.EXTRA_VOLUME_RAMP_SECONDS, 0) ?: 0
        val gradual      = intent?.getBooleanExtra(AlarmReceiver.EXTRA_GRADUAL_WAKEUP, false) ?: false
        val volumeScale  = (intent?.getIntExtra(AlarmReceiver.EXTRA_ALARM_VOLUME_PERCENT, 100) ?: 100)
            .coerceIn(0, 100) / 100f

        if (soundEnabled && volumeScale > 0f) {
            forceMaxAlarmVolume()
            requestAudioFocusForAlarm()
            if (gradual) {
                // 30s vibrate-only prelude, then ramp sound up over 60s.
                scope.launch {
                    delay(30_000)
                    startAlarmSound(ringtoneUri, 60, volumeScale)
                }
            } else {
                startAlarmSound(ringtoneUri, volumeRamp, volumeScale)
            }
        }
        if (vibrate) startVibration()

        scheduleAutoQuiet()
        return START_STICKY
    }

    private fun startForegroundNotification(intent: Intent?) {
        AlarmNotificationManager.createNotificationChannel(this)

        val title  = intent?.getStringExtra(AlarmReceiver.EXTRA_ALARM_TITLE) ?: "알람"
        val hour   = intent?.getIntExtra(AlarmReceiver.EXTRA_ALARM_HOUR, 0) ?: 0
        val minute = intent?.getIntExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, 0) ?: 0

        val ringActivity = Intent(this, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NO_USER_ACTION
            intent?.extras?.let { putExtras(it) }
        }
        val pi = PendingIntent.getActivity(
            this, currentAlarmId.toRequestCode(), ringActivity,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notif: Notification = NotificationCompat.Builder(this, AlarmNotificationManager.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText("%02d:%02d — 울리는 중".format(hour, minute))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                FOREGROUND_ID,
                notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(FOREGROUND_ID, notif)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "RustyAlarm:AlarmRingService",
        ).apply {
            setReferenceCounted(false)
            acquire(15 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        runCatching { wakeLock?.takeIf { it.isHeld }?.release() }
        wakeLock = null
    }

    private fun startAlarmSound(
        ringtoneUriStr: String?,
        rampSeconds: Int,
        volumeScale: Float,
    ) {
        val uri: Uri = ringtoneUriStr?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: return
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmRingService, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                if (rampSeconds > 0) setVolume(0f, 0f) else setVolume(volumeScale, volumeScale)
                prepareAsync()
                setOnPreparedListener {
                    start()
                    if (rampSeconds > 0) startVolumeRamp(rampSeconds, volumeScale)
                }
            }
        } catch (_: Exception) {}
    }

    private fun startVolumeRamp(rampSeconds: Int, volumeScale: Float) {
        rampJob?.cancel()
        rampJob = scope.launch {
            val steps = rampSeconds * 4
            for (i in 1..steps) {
                val v = (i / steps.toFloat()) * volumeScale
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

    private fun scheduleAutoQuiet() {
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(AUTO_QUIET_MILLIS)
            val steps = 30
            val tick = FADE_OUT_MILLIS / steps
            for (i in steps - 1 downTo 0) {
                val v = i / steps.toFloat()
                runCatching { mediaPlayer?.setVolume(v, v) }
                delay(tick)
            }
            stopRinging()
        }
    }

    private fun requestAudioFocusForAlarm() {
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(attrs)
                    .setOnAudioFocusChangeListener { /* alarm holds focus */ }
                    .build()
                audioFocusRequest = req
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    null, AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE,
                )
            }
        } catch (_: Throwable) {}
    }

    private fun abandonAudioFocus() {
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        } catch (_: Throwable) {}
        audioFocusRequest = null
    }

    private fun forceMaxAlarmVolume() {
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            savedAlarmVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
            am.setStreamVolume(AudioManager.STREAM_ALARM, max, 0)
        } catch (_: SecurityException) {}
    }

    private fun restoreAlarmVolume() {
        if (savedAlarmVolume < 0) return
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.setStreamVolume(AudioManager.STREAM_ALARM, savedAlarmVolume, 0)
        } catch (_: SecurityException) {}
        savedAlarmVolume = -1
    }

    private fun stopRinging() {
        rampJob?.cancel(); rampJob = null
        timeoutJob?.cancel(); timeoutJob = null
        mediaPlayer?.runCatching { stop(); release() }
        mediaPlayer = null
        vibrator?.cancel(); vibrator = null
        restoreAlarmVolume()
        abandonAudioFocus()
        releaseWakeLock()
        currentAlarmId = -1L
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRinging()
        scope.cancel()
    }

    private fun Long.toRequestCode(): Int =
        kotlin.math.abs(this % Int.MAX_VALUE).toInt()

    companion object {
        const val ACTION_START_RING = "com.example.rustyalarm.ACTION_START_RING"
        const val ACTION_STOP       = "com.example.rustyalarm.ACTION_STOP_RING"

        private const val FOREGROUND_ID = 911
        private const val AUTO_QUIET_MILLIS = 5L * 60 * 1000L
        private const val FADE_OUT_MILLIS  = 30L * 1000L

        /** Convenience for callers to stop the ring from anywhere. */
        fun stop(context: Context) {
            val i = Intent(context, AlarmRingService::class.java).apply { action = ACTION_STOP }
            runCatching { context.startService(i) }
        }
    }
}
