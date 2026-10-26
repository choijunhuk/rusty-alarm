package com.example.rustyalarm.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmReliabilityTest {

    private val permissions = PermissionsStatus(
        notifications = true,
        exactAlarm = true,
        ignoringBatteryOpts = true,
    )

    @Test
    fun missingPermissionsStillBlockReadiness() {
        val diagnostic = AlarmReliability.diagnose(
            permissions = PermissionsStatus(false, false, false, false),
            enabledAlarmCount = 1,
            nextAlarm = Alarm(),
        )

        assertEquals(ReliabilityLevel.BLOCKED, diagnostic.level)
        assertTrue(diagnostic.issues.any { it.id == "notifications" && it.blocking })
        assertTrue(diagnostic.issues.any { it.id == "exact_alarm" && it.blocking })
        assertTrue(diagnostic.issues.any { it.id == "full_screen_intent" })
        assertTrue(diagnostic.issues.any { it.id == "battery_optimization" })
    }

    @Test
    fun ordinaryAlarmIsReadyWithoutChallengesOrSnoozeLimits() {
        val diagnostic = AlarmReliability.diagnose(permissions, 1, Alarm())

        assertEquals(ReliabilityLevel.READY, diagnostic.level)
        assertEquals(100, diagnostic.score)
        assertTrue(diagnostic.issues.isEmpty())
    }

    @Test
    fun addingChallengesDoesNotChangeDeliveryReadiness() {
        val ordinary = AlarmReliability.diagnose(permissions, 1, Alarm())
        val forced = AlarmReliability.diagnose(
            permissions, 1, WakeupPresetApplier.apply(Alarm(), WakeupPreset.FORCED),
        )

        assertEquals(ordinary.score, forced.score)
        assertEquals(ordinary.level, forced.level)
        assertEquals(ordinary.strengths, forced.strengths)
    }

    @Test
    fun soundAndVibrationBothOffStillProduceAnActionableWarning() {
        val diagnostic = AlarmReliability.diagnose(
            permissions, 1, Alarm(soundEnabled = false, vibrate = false),
        )

        assertEquals(ReliabilityLevel.CAUTION, diagnostic.level)
        assertEquals(listOf("silent_alarm"), diagnostic.issues.map { it.id })
        assertEquals(ReliabilityActionKind.EDIT_ALARM, diagnostic.issues.single().actionKind)
    }

    @Test
    fun lowVolumeIsCautionEvenWhenPermissionsAreGranted() {
        val diagnostic = AlarmReliability.diagnose(
            permissions, 1, Alarm(alarmVolumePercent = 20),
        )

        assertEquals(ReliabilityLevel.CAUTION, diagnostic.level)
        assertEquals(listOf("low_volume"), diagnostic.issues.map { it.id })
    }

    @Test
    fun vibrationOnlyAlarmDoesNotGetASoundWarning() {
        assertTrue(AlarmReliability.alarmIssues(Alarm(soundEnabled = false)).isEmpty())
    }

    @Test
    fun permissionIssuesKeepTheirSystemSettingsActions() {
        val diagnostic = AlarmReliability.diagnose(
            PermissionsStatus(false, false, false, false), 0, null,
        )
        val actions = diagnostic.issues.associate { it.id to it.actionKind }

        assertEquals(ReliabilityActionKind.NOTIFICATION_SETTINGS, actions["notifications"])
        assertEquals(ReliabilityActionKind.NOTIFICATION_SETTINGS, actions["full_screen_intent"])
        assertEquals(ReliabilityActionKind.EXACT_ALARM_SETTINGS, actions["exact_alarm"])
        assertEquals(ReliabilityActionKind.BATTERY_SETTINGS, actions["battery_optimization"])
        assertEquals(ReliabilityActionKind.CREATE_TEST_ALARM, actions["no_enabled_alarm"])
    }

    @Test
    fun primaryIssuePrefersMissingPermissionBeforeSoundTuning() {
        val diagnostic = AlarmReliability.diagnose(
            permissions.copy(exactAlarm = false),
            1,
            Alarm(soundEnabled = false, vibrate = false),
        )

        assertEquals("exact_alarm", AlarmReliability.primaryIssue(diagnostic.issues)?.id)
    }
}
