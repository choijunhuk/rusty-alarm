package com.example.rustyalarm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.rustyalarm.alarm.AlarmEventDao
import com.example.rustyalarm.alarm.ChallengeStat
import com.example.rustyalarm.alarm.DailyCount
import com.example.rustyalarm.alarm.WakeupSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class StatsViewModel @Inject constructor(private val eventDao: AlarmEventDao) : ViewModel() {

    private val sinceMillis = System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000L

    val dailyCounts: StateFlow<List<DailyCount>> =
        eventDao.dailyFireCounts(sinceMillis).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val challengeBreakdown: StateFlow<List<ChallengeStat>> =
        eventDao.challengeBreakdown().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val summary: StateFlow<WakeupSummary> =
        eventDao.summaryFlow().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WakeupSummary(0, 0, 0, 0.0),
        )

}
