package com.example.rustyalarm.wear

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.example.rustyalarm.alarm.WatchControlAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pushes the soonest upcoming alarm to the Wear OS companion via the Data
 * Layer. The watch listens on path `/rusty-alarm/next` and renders the watch
 * face from it.
 */
object WearSync {

    private const val PATH       = "/rusty-alarm/next"
    private const val KEY_TIME   = "trigger_at"
    private const val KEY_TITLE  = "title"

    suspend fun pushNextAlarm(
        context: Context,
        triggerAtMillis: Long?,
        title: String?,
        readinessLabel: String? = null,
        watchAccess: WatchControlAccess? = null,
    ) = withContext(Dispatchers.IO) {
        runCatching {
            val req = PutDataMapRequest.create(PATH).apply {
                dataMap.putLong(KEY_TIME, triggerAtMillis ?: -1L)
                title?.let { dataMap.putString(KEY_TITLE, it) }
                readinessLabel?.let { dataMap.putString(KEY_READINESS, it) }
                dataMap.putBoolean(KEY_CAN_SNOOZE, watchAccess?.canSnooze ?: false)
                dataMap.putBoolean(KEY_CAN_DISMISS, watchAccess?.canDismiss ?: false)
                dataMap.putBoolean(KEY_REQUIRES_PHONE, watchAccess?.requiresPhone ?: false)
                dataMap.putLong("ts", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
            Wearable.getDataClient(context).putDataItem(req)
        }
    }

    private const val KEY_READINESS = "readiness"
    private const val KEY_CAN_SNOOZE = "can_snooze"
    private const val KEY_CAN_DISMISS = "can_dismiss"
    private const val KEY_REQUIRES_PHONE = "requires_phone"
}
