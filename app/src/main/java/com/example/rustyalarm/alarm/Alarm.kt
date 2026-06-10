package com.example.rustyalarm.alarm

data class Alarm(
    val id: Long = 0,
    val title: String = "알람",
    val hour: Int = 8,
    val minute: Int = 0,
    val repeatDays: List<Int> = emptyList(),
    val enabled: Boolean = true,
    val vibrate: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
