package com.example.rustyalarm.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.rustyalarm.RustyAlarmApplication
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Writes a JSON snapshot of all alarms to internal storage once a week.
 *
 * Files land in `filesDir/backups/alarms-YYYY-MM-DD.json`. The worker keeps
 * the 8 most recent snapshots and removes anything older so storage stays
 * bounded.
 */
class BackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        val app = applicationContext as RustyAlarmApplication
        val json = app.repository.exportToJson()

        val dir = File(applicationContext.filesDir, "backups").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val out = File(dir, "alarms-$stamp.json")
        out.writeText(json)
        prune(dir, keep = 8)
        Result.success()
    }.getOrElse { Result.retry() }

    private fun prune(dir: File, keep: Int) {
        val files = dir.listFiles { f -> f.name.startsWith("alarms-") && f.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() } ?: return
        files.drop(keep).forEach { runCatching { it.delete() } }
    }

    companion object {
        private const val UNIQUE_NAME = "weekly-alarm-backup"

        fun ensureScheduled(context: Context) {
            val req = PeriodicWorkRequestBuilder<BackupWorker>(
                repeatInterval = 7, repeatIntervalTimeUnit = TimeUnit.DAYS,
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, req,
            )
        }
    }
}
