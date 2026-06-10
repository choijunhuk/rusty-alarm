package com.example.rustyalarm.rust

/**
 * JNI bridge to the Rust alarm_core crate.
 * Falls back to Kotlin implementations when the native library is unavailable
 * (e.g. running unit tests on the host without cross-compiled .so files).
 */
object RustAlarmCore {

    val isAvailable: Boolean = runCatching {
        System.loadLibrary("alarm_core")
        true
    }.getOrDefault(false)

    // ── Native declarations ──────────────────────────────────────

    private external fun nativeValidateAlarmTime(hour: Int, minute: Int): Boolean
    private external fun nativeFormatTime(hour: Int, minute: Int): String
    private external fun nativeCalculateNextAlarmTimestamp(
        currentTimestampMillis: Long,
        hour: Int,
        minute: Int,
        repeatDays: IntArray,
    ): Long
    private external fun nativeGetRepeatDaysLabel(repeatDays: IntArray): String
    private external fun nativeAnalyzeSleepWindow(samples: FloatArray): Float
    private external fun nativeShouldWakeNow(samples: FloatArray, threshold: Float): Boolean

    // ── Public API (with Kotlin fallbacks) ───────────────────────

    fun validateAlarmTime(hour: Int, minute: Int): Boolean =
        if (isAvailable) nativeValidateAlarmTime(hour, minute)
        else hour in 0..23 && minute in 0..59

    fun formatTime(hour: Int, minute: Int): String =
        if (isAvailable) nativeFormatTime(hour, minute)
        else "%02d:%02d".format(hour, minute)

    fun calculateNextAlarmTimestamp(
        currentTimestampMillis: Long,
        hour: Int,
        minute: Int,
        repeatDays: IntArray,
    ): Long =
        if (isAvailable) {
            nativeCalculateNextAlarmTimestamp(currentTimestampMillis, hour, minute, repeatDays)
        } else {
            fallbackNextAlarm(currentTimestampMillis, hour, minute, repeatDays)
        }

    fun getRepeatDaysLabel(repeatDays: IntArray): String =
        if (isAvailable) nativeGetRepeatDaysLabel(repeatDays)
        else fallbackRepeatDaysLabel(repeatDays)

    fun analyzeSleepWindow(samples: FloatArray): Float =
        if (isAvailable) nativeAnalyzeSleepWindow(samples)
        else fallbackAnalyzeSleepWindow(samples)

    fun shouldWakeNow(samples: FloatArray, threshold: Float): Boolean =
        if (isAvailable) nativeShouldWakeNow(samples, threshold)
        else fallbackAnalyzeSleepWindow(samples) >= threshold

    // ── Kotlin fallbacks (used when .so not loaded) ──────────────

    private fun fallbackNextAlarm(
        currentMillis: Long,
        hour: Int,
        minute: Int,
        repeatDays: IntArray,
    ): Long {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = currentMillis }
        cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
        cal.set(java.util.Calendar.MINUTE, minute)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)

        if (repeatDays.isEmpty()) {
            if (cal.timeInMillis <= currentMillis) cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            return cal.timeInMillis
        }

        val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) - 1 // 0=Sun
        var minDays = Int.MAX_VALUE
        for (day in repeatDays) {
            val diff = when {
                day > today -> day - today
                day == today && cal.timeInMillis > currentMillis -> 0
                else -> 7 - (today - day)
            }
            if (diff < minDays) minDays = diff
        }
        cal.add(java.util.Calendar.DAY_OF_YEAR, minDays)
        return cal.timeInMillis
    }

    private fun fallbackRepeatDaysLabel(repeatDays: IntArray): String {
        if (repeatDays.isEmpty()) return "일회성"
        val names = arrayOf("일", "월", "화", "수", "목", "금", "토")
        return repeatDays.sorted().joinToString(" ") { names[it] }
    }

    private fun fallbackAnalyzeSleepWindow(samples: FloatArray): Float {
        if (samples.size < 2) return 0f
        val mean = samples.sum() / samples.size
        val variance = samples.fold(0f) { acc, x ->
            val d = x - mean
            acc + d * d
        } / samples.size
        val stdDev = kotlin.math.sqrt(variance)
        return stdDev / (1f + stdDev)
    }
}
