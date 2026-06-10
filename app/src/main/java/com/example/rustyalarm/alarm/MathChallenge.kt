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
    ChallengeType.NONE -> MathProblem("", 0)
}
