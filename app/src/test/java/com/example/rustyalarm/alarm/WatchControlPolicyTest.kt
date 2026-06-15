package com.example.rustyalarm.alarm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchControlPolicyTest {

    @Test
    fun dismissIsBlockedWhenPhoneChallengeWouldBeBypassed() {
        val access = WatchControlPolicy.access(
            alarm = Alarm(challengeType = ChallengeType.QR_SCAN),
            snoozesUsed = 0,
        )

        assertFalse(access.canDismiss)
        assertTrue(access.requiresPhone)
    }

    @Test
    fun dismissIsBlockedWhenRoutineWouldBeBypassed() {
        val access = WatchControlPolicy.access(
            alarm = Alarm(routineItems = listOf("물 마시기")),
            snoozesUsed = 0,
        )

        assertFalse(access.canDismiss)
        assertTrue(access.requiresPhone)
    }

    @Test
    fun snoozeRespectsConfiguredLimit() {
        val allowed = WatchControlPolicy.access(
            alarm = Alarm(maxSnoozes = 1),
            snoozesUsed = 0,
        )
        val exhausted = WatchControlPolicy.access(
            alarm = Alarm(maxSnoozes = 1),
            snoozesUsed = 1,
        )

        assertTrue(allowed.canSnooze)
        assertFalse(exhausted.canSnooze)
    }
}
