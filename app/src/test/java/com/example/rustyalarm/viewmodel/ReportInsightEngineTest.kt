package com.example.rustyalarm.viewmodel

import com.example.rustyalarm.alarm.WakeupPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportInsightEngineTest {

    @Test
    fun insightsSuggestReliabilityWhenManyFiredAlarmsAreNotDismissed() {
        val insights = ReportInsightEngine.insights(
            WakeupInsightInput(
                fired = 10,
                dismissed = 5,
                snoozed = 1,
                avgResponseSec = 80,
                challengesCompleted = 2,
                streakDays = 1,
            ),
        )

        assertTrue(insights.any { it.id == "low_completion" })
    }

    @Test
    fun insightsSuggestSnoozeCapWhenSnoozeRateIsHigh() {
        val insights = ReportInsightEngine.insights(
            WakeupInsightInput(
                fired = 8,
                dismissed = 8,
                snoozed = 6,
                avgResponseSec = 120,
                challengesCompleted = 0,
                streakDays = 2,
            ),
        )

        assertTrue(insights.any { it.id == "high_snooze" })
        assertFalse(insights.any { it.id == "no_challenge" })
        assertTrue(insights.first { it.id == "high_snooze" }.cause!!.contains("75%"))
        assertTrue(
            insights.first { it.id == "high_snooze" }
                .action!!.expectedResult.contains("스누즈 1회"),
        )
        assertEquals(
            WakeupPreset.ON_TIME,
            insights.first { it.id == "high_snooze" }.action?.preset,
        )
    }

    @Test
    fun insightsCelebrateStrongWeekBeforeSuggestingMoreWork() {
        val insights = ReportInsightEngine.insights(
            WakeupInsightInput(
                fired = 7,
                dismissed = 7,
                snoozed = 0,
                avgResponseSec = 45,
                challengesCompleted = 7,
                streakDays = 7,
            ),
        )

        assertEquals("strong_week", insights.first().id)
        assertTrue(insights.first().title.contains("좋아요"))
        assertEquals(null, insights.first().action)
    }
    @Test
    fun ordinaryAlarmUseDoesNotTriggerAChallengeRecommendation() {
        val insights = ReportInsightEngine.insights(
            WakeupInsightInput(
                fired = 5,
                dismissed = 5,
                snoozed = 0,
                avgResponseSec = 30,
                challengesCompleted = 0,
                streakDays = 5,
            ),
        )

        assertFalse(insights.any { it.id == "no_challenge" })
        assertTrue(insights.all { it.action == null })
    }

    @Test
    fun comfortableModeRecommendationDisclosesRemovalOfExistingGates() {
        val insights = ReportInsightEngine.insights(
            WakeupInsightInput(
                fired = 5,
                dismissed = 5,
                snoozed = 0,
                avgResponseSec = 240,
                challengesCompleted = 5,
                streakDays = 5,
            ),
        )

        val action = insights.first { it.id == "slow_response" }.action!!
        assertEquals(WakeupPreset.COMFORTABLE, action.preset)
        assertTrue(action.expectedResult.contains("챌린지와 루틴"))
        assertTrue(action.expectedResult.contains("스누즈 3회"))
    }

}
