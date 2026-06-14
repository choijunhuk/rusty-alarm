package com.example.rustyalarm.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeupPresetApplierTest {

    @Test
    fun applyStrictPresetAddsDismissGateAndLimitsSnooze() {
        val alarm = WakeupPresetApplier.apply(
            alarm = Alarm(
                challengeType = ChallengeType.NONE,
                maxSnoozes = 0,
                routineItems = emptyList(),
                soundEnabled = true,
                vibrate = true,
                alarmVolumePercent = 35,
            ),
            preset = WakeupPreset.STRICT,
        )

        assertEquals(ChallengeType.MATH_MEDIUM, alarm.challengeType)
        assertEquals(1, alarm.maxSnoozes)
        assertTrue(alarm.routineItems.isNotEmpty())
        assertTrue(alarm.alarmVolumePercent >= 85)
        assertTrue(alarm.gradualWakeup)
    }

    @Test
    fun applyGentlePresetKeepsExistingChallengeButAddsPreAlarmSafety() {
        val alarm = WakeupPresetApplier.apply(
            alarm = Alarm(
                challengeType = ChallengeType.TYPING,
                maxSnoozes = 3,
                preAlarmMinutes = 0,
            ),
            preset = WakeupPreset.GENTLE_SAFE,
        )

        assertEquals(ChallengeType.TYPING, alarm.challengeType)
        assertEquals(3, alarm.maxSnoozes)
        assertEquals(15, alarm.preAlarmMinutes)
        assertTrue(alarm.gradualWakeup)
    }

    @Test
    fun profileExplainsWeakAlarmConfiguration() {
        val profile = WakeupPresetApplier.profile(
            Alarm(
                challengeType = ChallengeType.NONE,
                maxSnoozes = 0,
                routineItems = emptyList(),
            ),
        )

        assertEquals(WakeupProfileLevel.WEAK, profile.level)
        assertTrue(profile.recommendation.contains("확실히"))
    }
}
