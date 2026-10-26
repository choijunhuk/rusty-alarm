package com.example.rustyalarm.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeupPresetApplierTest {

    @Test
    fun newAlarmsDoNotRequireChallengesOrWakeBeforeTheirTime() {
        val alarm = Alarm()
        assertEquals(ChallengeType.NONE, alarm.challengeType)
        assertTrue(alarm.routineItems.isEmpty())
        assertEquals(0, alarm.preAlarmMinutes)
        assertEquals(0, alarm.maxSnoozes)
    }

    @Test
    fun forcedWakeupAddsTheAdvertisedChallengeAndRoutine() {
        val alarm = WakeupPresetApplier.apply(Alarm(), WakeupPreset.FORCED)
        assertEquals(ChallengeType.MATH_MEDIUM, alarm.challengeType)
        assertEquals(1, alarm.mathProblemCount)
        assertEquals(1, alarm.maxSnoozes)
        assertEquals(2, alarm.routineItems.size)
        assertEquals(100, alarm.alarmVolumePercent)
        assertTrue(alarm.gradualWakeup)
    }

    @Test
    fun comfortableWakeupClearsPreviousChallengeAndRoutine() {
        val forced = WakeupPresetApplier.apply(Alarm(), WakeupPreset.FORCED)
        val alarm = WakeupPresetApplier.apply(forced, WakeupPreset.COMFORTABLE)

        assertEquals(ChallengeType.NONE, alarm.challengeType)
        assertTrue(alarm.routineItems.isEmpty())
        assertEquals(3, alarm.maxSnoozes)
        assertEquals(70, alarm.alarmVolumePercent)
        assertEquals(20, alarm.volumeRampSeconds)
        assertEquals(0, alarm.preAlarmMinutes)
    }

    @Test
    fun standardModeRemovesPreviousWakeupRestrictions() {
        val forced = WakeupPresetApplier.apply(Alarm(), WakeupPreset.FORCED)
        val alarm = WakeupPresetApplier.apply(forced, WakeupPreset.STANDARD)

        assertEquals(ChallengeType.NONE, alarm.challengeType)
        assertTrue(alarm.routineItems.isEmpty())
        assertEquals(0, alarm.maxSnoozes)
        assertEquals(0, alarm.preAlarmMinutes)
        assertEquals(0, alarm.volumeRampSeconds)
        assertFalse(alarm.gradualWakeup)
    }

    @Test
    fun onTimeWakeupReplacesThePreviousGateWithoutAddingARoutine() {
        val alarm = WakeupPresetApplier.apply(
            Alarm(
                challengeType = ChallengeType.MATH_HARD,
                mathProblemCount = 10,
                routineItems = listOf("양치"),
            ),
            WakeupPreset.ON_TIME,
        )

        assertEquals(ChallengeType.TYPING, alarm.challengeType)
        assertEquals(1, alarm.maxSnoozes)
        assertEquals(90, alarm.alarmVolumePercent)
        assertTrue(alarm.routineItems.isEmpty())
    }

    @Test
    fun switchingModesHasTheSameResultAsApplyingTheDestinationDirectly() {
        val original = Alarm(
            id = 42,
            title = "출근",
            hour = 7,
            minute = 30,
            repeatDays = listOf(1, 2, 3, 4, 5),
            message = "오늘도 잘 다녀와",
            youtubeUrl = "https://youtu.be/example",
            preAlarmMinutes = 30,
            isSmartAlarm = true,
        )

        for (from in WakeupPreset.entries) {
            for (to in WakeupPreset.entries) {
                val switched = WakeupPresetApplier.apply(
                    WakeupPresetApplier.apply(original, from),
                    to,
                )
                assertEquals(WakeupPresetApplier.apply(original, to), switched)
                assertEquals(original.id, switched.id)
                assertEquals(original.repeatDays, switched.repeatDays)
                assertEquals(original.hour, switched.hour)
                assertEquals(original.minute, switched.minute)
                assertEquals(original.title, switched.title)
                assertEquals(original.message, switched.message)
                assertEquals(original.youtubeUrl, switched.youtubeUrl)
                assertEquals(0, switched.preAlarmMinutes)
                assertFalse(switched.isSmartAlarm)
            }
        }
    }
}
