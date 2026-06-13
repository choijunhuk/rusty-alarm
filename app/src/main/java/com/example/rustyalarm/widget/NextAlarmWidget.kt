package com.example.rustyalarm.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.ComponentName
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
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
 * Home-screen widget that surfaces the soonest upcoming alarm + current
 * wake-up streak (consecutive days the user actually dismissed an alarm).
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

        val streak = runCatching {
            computeStreak(app.database.alarmEventDao().dismissedDayKeys())
        }.getOrDefault(0)

        provideContent {
            WidgetContent(
                title = next?.first?.title,
                triggerAt = next?.second,
                streakDays = streak,
            )
        }
    }
}

private fun countdownLabel(deltaMs: Long): String {
    if (deltaMs <= 0) return "곧 울려요"
    val totalMin = (deltaMs / 60_000L).coerceAtLeast(1)
    val d = totalMin / (60 * 24)
    val h = (totalMin / 60) % 24
    val m = totalMin % 60
    return when {
        d > 0 -> "${d}일 ${h}시간 후"
        h > 0 -> "${h}시간 ${m}분 후"
        else  -> "${m}분 후"
    }
}

private fun computeStreak(keysDesc: List<String>): Int {
    if (keysDesc.isEmpty()) return 0
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val today = fmt.format(Date())
    val yesterday = fmt.format(Date(System.currentTimeMillis() - 86_400_000L))
    val set = keysDesc.toSet()
    var cursor = when {
        today in set -> today
        yesterday in set -> yesterday
        else -> return 0
    }
    var streak = 0
    val cal = Calendar.getInstance()
    cal.time = fmt.parse(cursor) ?: return 0
    while (fmt.format(cal.time) in set) {
        streak++
        cal.add(Calendar.DAY_OF_YEAR, -1)
    }
    return streak
}

// Brand palette — midnight violet / coral / gold
private const val BG_HEX        = "#1B1840"
private const val ACCENT_HEX    = "#9B8AFF"   // primary
private const val ACCENT2_HEX   = "#FFB088"   // secondary
private const val TEXT_HEX      = "#FFFFFF"
private const val MUTED_HEX     = "#CAC2E8"

@Composable
private fun WidgetContent(title: String?, triggerAt: Long?, streakDays: Int) {
    val openApp = actionStartActivity(
        ComponentName("com.example.rustyalarm", "com.example.rustyalarm.MainActivity"),
    )
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(android.graphics.Color.parseColor(BG_HEX)))
            .cornerRadius(20.dp)
            .padding(14.dp)
            .clickable(openApp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "다음 알람",
                style = TextStyle(
                    color = ColorProvider(android.graphics.Color.parseColor(ACCENT_HEX)),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            if (triggerAt == null) {
                Text(
                    text = "없음",
                    style = TextStyle(
                        color = ColorProvider(android.graphics.Color.parseColor(MUTED_HEX)),
                        fontSize = 20.sp,
                    ),
                )
            } else {
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(triggerAt))
                val day = SimpleDateFormat("M/d (E)", Locale.KOREAN).format(Date(triggerAt))
                Text(
                    text = time,
                    style = TextStyle(
                        color = ColorProvider(android.graphics.Color.parseColor(TEXT_HEX)),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    text = day,
                    style = TextStyle(
                        color = ColorProvider(android.graphics.Color.parseColor(MUTED_HEX)),
                        fontSize = 11.sp,
                    ),
                )
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        style = TextStyle(
                            color = ColorProvider(android.graphics.Color.parseColor(MUTED_HEX)),
                            fontSize = 11.sp,
                        ),
                    )
                }
                Text(
                    text = countdownLabel(triggerAt - System.currentTimeMillis()),
                    style = TextStyle(
                        color = ColorProvider(android.graphics.Color.parseColor(ACCENT_HEX)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }

            Spacer(GlanceModifier.height(8.dp))
            // Streak strip
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🔥",
                    style = TextStyle(fontSize = 12.sp),
                )
                Spacer(GlanceModifier.width(4.dp))
                Text(
                    text = "$streakDays 일 연속",
                    style = TextStyle(
                        color = ColorProvider(android.graphics.Color.parseColor(ACCENT2_HEX)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}
