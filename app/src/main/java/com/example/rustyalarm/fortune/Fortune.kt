package com.example.rustyalarm.fortune

import java.util.Calendar
import kotlin.random.Random

data class FortuneLine(val emoji: String, val text: String)

object Fortune {

    private val pool = listOf(
        FortuneLine("🌟", "오늘 작은 행운이 두 번 찾아옵니다."),
        FortuneLine("☕", "오전에 마시는 음료가 평소보다 더 맛있을 거예요."),
        FortuneLine("🎯", "미루던 일을 끝내기 좋은 날입니다."),
        FortuneLine("💡", "예상 못한 아이디어가 떠올라요. 메모해두세요."),
        FortuneLine("📚", "5분의 독서가 큰 변화를 만듭니다."),
        FortuneLine("🍀", "오늘은 평소보다 운이 따라줍니다."),
        FortuneLine("🌈", "비가 와도 무지개가 떠요. 긍정의 날."),
        FortuneLine("🌻", "햇살처럼 환한 만남이 있어요."),
        FortuneLine("🎁", "기대 못 한 선물 같은 순간."),
        FortuneLine("🚀", "도전을 시작하면 의외로 잘 풀려요."),
        FortuneLine("🌙", "밤하늘처럼 차분한 하루입니다."),
        FortuneLine("🔥", "에너지가 넘치는 날. 운동 추천."),
        FortuneLine("🍎", "건강 챙기기에 좋은 날이에요."),
        FortuneLine("🤝", "좋은 인연이 가까이 있어요."),
        FortuneLine("💬", "한마디가 누군가에게 큰 힘이 될 수 있어요."),
        FortuneLine("🎵", "좋아하는 음악이 위로가 됩니다."),
        FortuneLine("🌊", "흐름에 맡기면 더 쉬워져요."),
        FortuneLine("🧩", "퍼즐처럼 풀리지 않던 문제가 풀려요."),
        FortuneLine("🍵", "여유 있게 보낼수록 더 많이 얻는 하루."),
        FortuneLine("🏔", "한 발 한 발 묵묵히 올라가는 날."),
        FortuneLine("🦋", "변화가 시작되는 신호가 보입니다."),
        FortuneLine("📷", "오늘의 한 장면을 꼭 남겨두세요."),
        FortuneLine("✨", "남이 모르는 작은 성취가 빛납니다."),
        FortuneLine("🌸", "사소한 친절이 행운으로 돌아옵니다."),
        FortuneLine("🎲", "직감을 믿어보세요. 좋은 선택이 됩니다."),
        FortuneLine("🍞", "주변 사람과 음식을 나누면 운이 트여요."),
        FortuneLine("🛏", "잘 자는 게 가장 좋은 투자입니다."),
        FortuneLine("🌅", "새벽 같은 신선한 시작의 날."),
        FortuneLine("🪴", "꾸준함이 자라나는 하루."),
        FortuneLine("⚡", "찰나의 기회를 놓치지 마세요."),
    )

    /** Returns the same fortune for the same calendar day (local time). */
    fun forToday(): FortuneLine {
        val cal = Calendar.getInstance()
        val seed = cal.get(Calendar.YEAR) * 10_000 +
            (cal.get(Calendar.MONTH) + 1) * 100 +
            cal.get(Calendar.DAY_OF_MONTH)
        val idx = Random(seed.toLong()).nextInt(pool.size)
        return pool[idx]
    }
}
