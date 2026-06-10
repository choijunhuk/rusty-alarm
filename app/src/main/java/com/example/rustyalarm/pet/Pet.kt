package com.example.rustyalarm.pet

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pet")
data class Pet(
    @PrimaryKey
    val id: Long = 1L,                 // singleton row
    val name: String = "버디",
    val exp: Int = 0,
    val lastFedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val level: Int get() = exp / 100

    val stage: PetStage get() = when (level) {
        0     -> PetStage.EGG
        1     -> PetStage.HATCHING
        in 2..4   -> PetStage.CHICK
        in 5..9   -> PetStage.YOUNG
        in 10..19 -> PetStage.ADULT
        else      -> PetStage.LEGEND
    }

    val progressToNextLevel: Float get() = (exp % 100) / 100f
}

enum class PetStage(val emoji: String, val label: String) {
    EGG     ("🥚", "알"),
    HATCHING("🐣", "부화 중"),
    CHICK   ("🐥", "병아리"),
    YOUNG   ("🐔", "청년"),
    ADULT   ("🦅", "성체"),
    LEGEND  ("🦄", "전설"),
}
