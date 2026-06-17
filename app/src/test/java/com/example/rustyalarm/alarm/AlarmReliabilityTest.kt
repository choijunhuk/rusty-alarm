package com.example.rustyalarm.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmReliabilityTest {

    @Test
    fun diagnose_marksMissingPermissionsAsBlockingReliabilityIssues() {
        val diagnostic = AlarmReliability.diagnose(
            permissions = PermissionsStatus(
                notifications = false,
                exactAlarm = false,
                ignoringBatteryOpts = false,
            ),
            enabledAlarmCount = 1,
            nextAlarm = strongAlarm(),
        )

        assertEquals(ReliabilityLevel.BLOCKED, diagnostic.level)
        assertTrue(diagnostic.score < 60)
        assertTrue(diagnostic.issues.any { it.id == "notifications" && it.blocking })
        assertTrue(diagnostic.issues.any { it.id == "exact_alarm" && it.blocking })
        assertTrue(diagnostic.issues.any { it.id == "battery_optimization" })
    }

    @Test
    fun diagnose_rewardsChallengeSnoozeCapAndRoutineForNextAlarm() {
        val diagnostic = AlarmReliability.diagnose(
            permissions = PermissionsStatus(
                notifications = true,
                exactAlarm = true,
                ignoringBatteryOpts = true,
            ),
            enabledAlarmCount = 1,
            nextAlarm = strongAlarm(),
        )

        assertEquals(ReliabilityLevel.READY, diagnostic.level)
        assertEquals(100, diagnostic.score)
        assertTrue(diagnostic.strengths.any { it.contains("챌린지") })
        assertTrue(diagnostic.strengths.any { it.contains("스누즈") })
        assertTrue(diagnostic.strengths.any { it.contains("루틴") })
    }

    @Test
    fun diagnoseFlagsAlarmsThatCanBeSilencedTooEasily() {
        val diagnostic = AlarmReliability.diagnose(
            permissions = PermissionsStatus(
                notifications = true,
                exactAlarm = true,
                ignoringBatteryOpts = true,
            ),
            enabledAlarmCount = 1,
            nextAlarm = Alarm(
                soundEnabled = false,
                vibrate = false,
                challengeType = ChallengeType.NONE,
                maxSnoozes = 0,
                routineItems = emptyList(),
                alarmVolumePercent = 20,
            ),
        )

        assertEquals(ReliabilityLevel.CAUTION, diagnostic.level)
        assertTrue(diagnostic.score in 60..89)
        assertTrue(diagnostic.issues.any { it.id == "silent_alarm" })
        assertTrue(diagnostic.issues.any { it.id == "unlimited_snooze" })
        assertTrue(diagnostic.issues.any { it.id == "no_dismiss_gate" })
    }

    @Test
    fun diagnoseMapsIssuesToUserActions() {
        val diagnostic = AlarmReliability.diagnose(
            permissions = PermissionsStatus(
                notifications = false,
                exactAlarm = false,
                ignoringBatteryOpts = false,
            ),
            enabledAlarmCount = 0,
            nextAlarm = Alarm(
                soundEnabled = false,
                vibrate = false,
                challengeType = ChallengeType.NONE,
                maxSnoozes = 0,
                routineItems = emptyList(),
            ),
        )

        assertEquals(
            ReliabilityActionKind.NOTIFICATION_SETTINGS,
            diagnostic.issues.first { it.id == "notifications" }.actionKind,
        )
        assertEquals(
            ReliabilityActionKind.EXACT_ALARM_SETTINGS,
            diagnostic.issues.first { it.id == "exact_alarm" }.actionKind,
        )
        assertEquals(
            ReliabilityActionKind.BATTERY_SETTINGS,
            diagnostic.issues.first { it.id == "battery_optimization" }.actionKind,
        )
        assertEquals(
            ReliabilityActionKind.CREATE_TEST_ALARM,
            diagnostic.issues.first { it.id == "no_enabled_alarm" }.actionKind,
        )
        assertEquals(
            ReliabilityActionKind.EDIT_ALARM,
            diagnostic.issues.first { it.id == "silent_alarm" }.actionKind,
        )
    }

    @Test
    fun primaryIssuePrefersBlockingPermissionBeforeAlarmTuning() {
        val diagnostic = AlarmReliability.diagnose(
            permissions = PermissionsStatus(
                notifications = true,
                exactAlarm = false,
                ignoringBatteryOpts = true,
            ),
            enabledAlarmCount = 1,
            nextAlarm = Alarm(
                soundEnabled = false,
                vibrate = false,
                challengeType = ChallengeType.NONE,
                maxSnoozes = 0,
                routineItems = emptyList(),
            ),
        )

        assertEquals("exact_alarm", AlarmReliability.primaryIssue(diagnostic.issues)?.id)
    }

    @Test
    fun recommendedPresetUsesWeakestUsefulModeForDraftAlarm() {
        assertEquals(
            WakeupPreset.FORCED,
            AlarmReliability.recommendedPreset(
                Alarm(
                    challengeType = ChallengeType.NONE,
                    maxSnoozes = 0,
                    routineItems = emptyList(),
                    soundEnabled = false,
                    vibrate = false,
                ),
            ),
        )
        assertEquals(
            WakeupPreset.ON_TIME,
            AlarmReliability.recommendedPreset(
                Alarm(
                    challengeType = ChallengeType.NONE,
                    maxSnoozes = 0,
                    routineItems = emptyList(),
                    soundEnabled = true,
                    vibrate = true,
                    alarmVolumePercent = 90,
                ),
            ),
        )
        assertEquals(null, AlarmReliability.recommendedPreset(strongAlarm()))
    }

    private fun strongAlarm(): Alarm = Alarm(
        challengeType = ChallengeType.MATH_MEDIUM,
        maxSnoozes = 1,
        routineItems = listOf("물 마시기", "창문 열기"),
        gradualWakeup = true,
        preAlarmMinutes = 10,
        alarmVolumePercent = 90,
    )
}
