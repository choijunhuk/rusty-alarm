package com.example.rustyalarm.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.rustyalarm.RustyAlarmApplication
import com.example.rustyalarm.rust.RustAlarmCore
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Home-screen widget that surfaces the soonest upcoming alarm. Reads the same
 * Room database used by the rest of the app and computes the next trigger
 * timestamp via the Rust crate (same code path as the in-app scheduler).
 */
class NextAlarmWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as RustyAlarmApplication
        val alarms = app.repository.alarms.first()
        val now = System.currentTimeMillis()

        val next = alarms
            .filter { it.enabled }
            .map { alarm ->
                val specificDate = alarm.specificDate
                val ts = if (specificDate != null) {
                    Calendar.getInstance().apply {
                        timeInMillis = specificDate
                        set(Calendar.HOUR_OF_DAY, alarm.hour)
                        set(Calendar.MINUTE, alarm.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                } else {
                    RustAlarmCore.calculateNextAlarmTimestamp(
                        now, alarm.hour, alarm.minute, alarm.repeatDays.toIntArray()
                    )
                }
                alarm to ts
            }
            .minByOrNull { it.second }

        provideContent {
            WidgetContent(
                title = next?.first?.title,
                triggerAt = next?.second,
            )
        }
    }
}

@Composable
private fun WidgetContent(title: String?, triggerAt: Long?) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(android.graphics.Color.parseColor("#16162A")))
            .cornerRadius(20.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "다음 알람",
                style = TextStyle(
                    color = ColorProvider(android.graphics.Color.parseColor("#00E5FF")),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            if (triggerAt == null) {
                Text(
                    text = "없음",
                    style = TextStyle(
                        color = ColorProvider(android.graphics.Color.parseColor("#8888AA")),
                        fontSize = 20.sp,
                    ),
                )
            } else {
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(triggerAt))
                val day = SimpleDateFormat("M/d (E)", Locale.KOREAN).format(Date(triggerAt))
                Text(
                    text = time,
                    style = TextStyle(
                        color = ColorProvider(android.graphics.Color.WHITE),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    text = day,
                    style = TextStyle(
                        color = ColorProvider(android.graphics.Color.parseColor("#BBBBDD")),
                        fontSize = 12.sp,
                    ),
                )
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        style = TextStyle(
                            color = ColorProvider(android.graphics.Color.parseColor("#BBBBDD")),
                            fontSize = 11.sp,
                        ),
                    )
                }
            }
        }
    }
}
