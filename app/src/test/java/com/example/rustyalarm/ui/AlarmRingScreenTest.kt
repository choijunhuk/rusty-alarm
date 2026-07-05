package com.example.rustyalarm.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.rustyalarm.alarm.ChallengeType
import com.example.rustyalarm.ui.screens.AlarmRingScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-hosted Compose tests for the alarm ring gates:
 * challenge must be solved and routine checked before dismiss unlocks.
 */
// Plain Application — the real one kicks off WorkManager in onCreate,
// which isn't initialized in Robolectric and isn't needed by this screen.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class AlarmRingScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun show(
        challengeType: ChallengeType = ChallengeType.NONE,
        snoozesRemaining: Int = 3,
        routineItems: List<String> = emptyList(),
        onDismiss: () -> Unit = {},
        onSnooze: () -> Unit = {},
    ) {
        // Infinite pulse animation is fine: the test rule's
        // InfiniteAnimationPolicy keeps it from blocking idleness.
        rule.setContent {
            AlarmRingScreen(
                alarmId = 1L,
                title = "테스트 알람",
                hour = 7,
                minute = 30,
                challengeType = challengeType,
                snoozesRemaining = snoozesRemaining,
                routineItems = routineItems,
                onDismiss = onDismiss,
                onSnooze = onSnooze,
            )
        }
    }

    @Test
    fun `no challenge - dismiss enabled and fires callback`() {
        var dismissed = false
        show(onDismiss = { dismissed = true })

        rule.onNodeWithText("끄기").performScrollTo().assertIsEnabled().performClick()
        assertTrue(dismissed)
    }

    @Test
    fun `math challenge - dismiss locked until solved`() {
        show(challengeType = ChallengeType.MATH_EASY)

        rule.onNodeWithText("🔒 먼저 챌린지를").assertIsNotEnabled()
    }

    @Test
    fun `routine gate - dismiss locked until all items checked`() {
        var dismissed = false
        show(
            routineItems = listOf("물 마시기", "이불 개기"),
            onDismiss = { dismissed = true },
        )

        rule.onNodeWithText("🔒 루틴 완료 필요").assertIsNotEnabled()

        rule.onNodeWithText("물 마시기").performClick()
        rule.onNodeWithText("이불 개기").performClick()
        rule.waitForIdle()

        rule.onNodeWithText("끄기").performScrollTo().assertIsEnabled().performClick()
        rule.runOnIdle { assertTrue(dismissed) }
    }

    @Test
    fun `snooze exhausted - snooze disabled`() {
        show(snoozesRemaining = 0)

        rule.onNodeWithText("스누즈 소진").assertIsNotEnabled()
    }

    @Test
    fun `snooze remaining - shows count and stays enabled`() {
        var snoozed = false
        show(snoozesRemaining = 2, onSnooze = { snoozed = true })

        rule.onNodeWithText("5분 뒤 (2 회 남음)").assertIsEnabled().performClick()
        assertTrue(snoozed)
    }
}
