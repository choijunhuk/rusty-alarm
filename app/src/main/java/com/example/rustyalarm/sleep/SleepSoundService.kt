package com.example.rustyalarm.sleep

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.rustyalarm.MainActivity
import com.example.rustyalarm.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Foreground service that keeps a [NoiseGenerator] running while the app is
 * backgrounded. UI observes [state] for the currently-playing color, and
 * controls playback by sending intents (or via [SleepSoundController]).
 */
class SleepSoundService : Service() {

    private val generator = NoiseGenerator()

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Always promote to foreground first to satisfy the 5-second contract from
        // startForegroundService; if the intent is malformed we'll stop ourselves below.
        val colorOnStart = intent
            ?.takeIf { it.action == ACTION_START }
            ?.getStringExtra(EXTRA_COLOR)
            ?.let { name -> runCatching { NoiseColor.valueOf(name) }.getOrNull() }
        startForeground(NOTIF_ID, buildNotification(colorOnStart ?: NoiseColor.WHITE))

        when (intent?.action) {
            ACTION_START -> {
                if (colorOnStart == null) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return START_NOT_STICKY
                }
                generator.start(colorOnStart)
                _state.value = colorOnStart
            }
            ACTION_STOP -> {
                generator.stop()
                _state.value = null
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        generator.release()
        _state.value = null
    }

    private fun buildNotification(color: NoiseColor): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPi = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopPi = PendingIntent.getService(
            this, 1,
            Intent(this, SleepSoundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("수면 사운드 — ${color.label}")
            .setContentText("탭하면 앱 열기 · 정지 버튼으로 끔")
            .setContentIntent(tapPi)
            .addAction(0, "정지", stopPi)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    companion object {
        const val ACTION_START = "com.example.rustyalarm.sleep.START"
        const val ACTION_STOP  = "com.example.rustyalarm.sleep.STOP"
        const val EXTRA_COLOR  = "color"

        private const val CHANNEL_ID = "sleep_sound"
        private const val NOTIF_ID   = 4242

        private val _state = MutableStateFlow<NoiseColor?>(null)
        val state: StateFlow<NoiseColor?> = _state

        fun ensureChannel(ctx: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val nm = ctx.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID, "수면 사운드",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "수면 사운드 백그라운드 재생 알림"
                    setSound(null, null)
                }
                nm.createNotificationChannel(channel)
            }
        }

        fun start(ctx: Context, color: NoiseColor) {
            val intent = Intent(ctx, SleepSoundService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_COLOR, color.name)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }

        fun stop(ctx: Context) {
            val intent = Intent(ctx, SleepSoundService::class.java).setAction(ACTION_STOP)
            ctx.startService(intent)
        }
    }
}
