package com.example.rustyalarm.alarm

import kotlin.random.Random

data class MathProblem(
    val expression: String,
    val answer: Int,
)

fun generateMathProblem(type: ChallengeType): MathProblem = when (type) {
    ChallengeType.MATH_EASY -> {
        val a = Random.nextInt(10, 50)
        val b = Random.nextInt(1, 30)
        if (Random.nextBoolean())
            MathProblem("$a + $b = ?", a + b)
        else
            MathProblem("${a + b} − $b = ?", a)
    }
    ChallengeType.MATH_MEDIUM -> {
        val a = Random.nextInt(2, 13)
        val b = Random.nextInt(2, 13)
        MathProblem("$a × $b = ?", a * b)
    }
    ChallengeType.MATH_HARD -> {
        val a = Random.nextInt(11, 30)
        val b = Random.nextInt(3, 9)
        MathProblem("$a × $b = ?", a * b)
    }
    else -> MathProblem("", 0)
}

private val TYPING_PHRASES = listOf(
    "잘 일어났어요",
    "오늘도 파이팅",
    "기상 완료",
    "좋은 아침",
    "일어나자",
)

fun getTypingPhrase(): String = TYPING_PHRASES[Random.nextInt(TYPING_PHRASES.size)]

const val SHAKE_TARGET_COUNT = 10

/** Total shakes required per difficulty. */
fun shakeTargetCount(type: ChallengeType): Int = when (type) {
    ChallengeType.SHAKE_EASY -> 5
    ChallengeType.SHAKE      -> 10
    ChallengeType.SHAKE_HARD -> 25
    else                     -> 10
}

/** Minimum delta magnitude on the accelerometer for a tick to count. */
fun shakeThreshold(type: ChallengeType): Float = when (type) {
    ChallengeType.SHAKE_EASY -> 10f
    ChallengeType.SHAKE      -> 15f
    ChallengeType.SHAKE_HARD -> 22f
    else                     -> 15f
}
