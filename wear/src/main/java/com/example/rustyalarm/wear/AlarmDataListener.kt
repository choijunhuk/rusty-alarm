package com.example.rustyalarm.wear

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

/**
 * Watches the DataLayer for `/rusty-alarm/next` items pushed from the phone
 * and pumps them into [NextAlarmStore] for the UI to pick up.
 */
class AlarmDataListener : WearableListenerService() {

    override fun onDataChanged(events: DataEventBuffer) {
        events.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED) return@forEach
            val path = event.dataItem.uri.path ?: return@forEach
            if (path != PATH_NEXT_ALARM) return@forEach

            val map = DataMapItem.fromDataItem(event.dataItem).dataMap
            val triggerAt = if (map.containsKey(KEY_TRIGGER_AT))
                map.getLong(KEY_TRIGGER_AT) else null
            val title = map.getString(KEY_TITLE)
            NextAlarmStore.update(triggerAt, title)
        }
    }

    companion object {
        const val PATH_NEXT_ALARM = "/rusty-alarm/next"
        const val KEY_TRIGGER_AT  = "trigger_at"
        const val KEY_TITLE       = "title"
    }
}
