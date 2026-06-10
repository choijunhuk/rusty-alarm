package com.example.rustyalarm.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.rustyalarm.rust.RustAlarmCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Foreground service that samples the accelerometer during the smart-wake
 * window. When sustained movement is detected above a threshold, the alarm
 * fires early (caller is likely in light sleep). Otherwise the regular
 * AlarmManager exact alarm fires at the target time as a fallback.
 */
class SleepMonitorService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val samples = ArrayDeque<Float>()
    private var alarmIntent: Intent? = null
    private var deadlineMillis: Long = 0L
    private var scope: CoroutineScope? = null
    private var checkJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        ServiceCompat.startForeground(
            this,
            NOTI_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            else 0,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent ?: return START_NOT_STICKY
        deadlineMillis = intent.getLongExtra(EXTRA_DEADLINE, System.currentTimeMillis())
        @Suppress("DEPRECATION")
        alarmIntent = intent.getParcelableExtra<Intent>(EXTRA_ALARM_INTENT)

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        scope = CoroutineScope(Dispatchers.Default)
        checkJob = scope?.launch {
            while (System.currentTimeMillis() < deadlineMillis) {
                delay(CHECK_INTERVAL_MS)
                val snapshot = synchronized(samples) { samples.toFloatArray() }
                if (snapshot.size >= MIN_SAMPLES) {
                    val wakeful = RustAlarmCore.shouldWakeNow(snapshot, WAKE_THRESHOLD)
                    if (wakeful) {
                        fireAlarmAndStop()
                        return@launch
                    }
                }
                // Slide window — keep only the last MAX_WINDOW samples
                synchronized(samples) {
                    while (samples.size > MAX_WINDOW) samples.removeFirst()
                }
            }
            // Deadline reached — alarm will fire via fallback AlarmManager exact alarm
            stopSelf()
        }
        return START_REDELIVER_INTENT
    }

    override fun onSensorChanged(event: SensorEvent) {
        // Use magnitude (subtract gravity for clarity)
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)
        synchronized(samples) { samples.addLast(magnitude) }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    private fun fireAlarmAndStop() {
        alarmIntent?.let { intent ->
            // Cancel the fallback AlarmManager schedule so the alarm doesn't ring twice
            val alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L)
            if (alarmId != -1L) AlarmScheduler(this).cancel(alarmId)
            sendBroadcast(intent)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        checkJob?.cancel()
        scope?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        AlarmNotificationManager.createNotificationChannel(this)
        val pi = PendingIntent.getActivity(
            this, 0, packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, AlarmNotificationManager.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("스마트 알람 감시 중")
            .setContentText("잠이 얕아지면 자동으로 깨워 드려요")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
    }

    companion object {
        const val EXTRA_ALARM_INTENT = "alarm_intent"
        const val EXTRA_DEADLINE     = "deadline_millis"

        private const val NOTI_ID         = 9_000_001
        private const val CHECK_INTERVAL_MS = 60_000L
        private const val MIN_SAMPLES     = 100
        private const val MAX_WINDOW      = 600
        private const val WAKE_THRESHOLD  = 0.6f

        fun startIntent(
            context: Context,
            deadlineMillis: Long,
            alarmIntent: Intent,
        ): Intent = Intent(context, SleepMonitorService::class.java).apply {
            putExtra(EXTRA_DEADLINE, deadlineMillis)
            putExtra(EXTRA_ALARM_INTENT, alarmIntent)
        }
    }
}
