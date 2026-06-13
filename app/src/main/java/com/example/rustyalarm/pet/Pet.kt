package com.example.rustyalarm.pet

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PetSkin(val label: String, val unlockLevel: Int) {
    DEFAULT("기본", 0),
    GOLDEN("황금", 5),
    RAINBOW("무지개", 10),
}

@Entity(tableName = "pet")
data class Pet(
    @PrimaryKey
    val id: Long = 1L,                 // singleton row
    val name: String = "버디",
    val exp: Int = 0,
    val lastFedAt: Long = 0L,
    val skin: String = PetSkin.DEFAULT.name,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val level: Int get() = exp / 100
    val skinEnum: PetSkin get() = runCatching { PetSkin.valueOf(skin) }.getOrDefault(PetSkin.DEFAULT)

    val stage: PetStage get() = when (level) {
        0     -> PetStage.EGG
        1     -> PetStage.HATCHING
        in 2..4   -> PetStage.CHICK
        in 5..9   -> PetStage.YOUNG
        in 10..19 -> PetStage.ADULT
        else      -> PetStage.LEGEND
    }

    val progressToNextLevel: Float get() = (exp % 100) / 100f

    /**
     * Soft "happiness" gauge derived purely from time since last meal.
     * No DB column needed — drops ~3 percentage points per hour, floors at 0.
     */
    val happiness: Int get() {
        if (lastFedAt == 0L) return 60
        val hours = (System.currentTimeMillis() - lastFedAt) / 3_600_000L
        val decay = (hours * 3).coerceAtMost(100L).toInt()
        return (100 - decay).coerceAtLeast(0)
    }

    val happinessLabel: String get() = when (happiness) {
        in 80..100 -> "💖 기분 최고"
        in 60..79  -> "😊 기분 좋음"
        in 40..59  -> "😐 보통이에요"
        in 20..39  -> "😔 좀 시무룩"
        else       -> "😢 배고파요"
    }
}

enum class PetStage(val emoji: String, val label: String) {
    EGG     ("🥚", "알"),
    HATCHING("🐣", "부화 중"),
    CHICK   ("🐥", "병아리"),
    YOUNG   ("🐔", "청년"),
    ADULT   ("🦅", "성체"),
    LEGEND  ("🦄", "전설"),
}
