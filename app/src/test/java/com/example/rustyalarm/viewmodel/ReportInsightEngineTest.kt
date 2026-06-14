package com.example.rustyalarm.viewmodel

import org.junit.Assert.assertEquals
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
        assertTrue(insights.any { it.id == "no_challenge" })
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
    }
}
