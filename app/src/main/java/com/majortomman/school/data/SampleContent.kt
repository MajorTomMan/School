package com.majortomman.school.data

object SampleContent {
    val lessons = listOf(
        Lesson(
            id = "positive-negative",
            title = "正数和负数",
            subtitle = "用正负数描述方向相反或意义相反的量",
            estimatedMinutes = 18,
            textbookPages = 9..14,
            status = MasteryStatus.MASTERED,
            objectives = listOf(
                "知道正数、负数和 0 的区别",
                "能用正负数表示相反意义的量",
                "理解 0 既不是正数也不是负数",
            ),
            explanation = "正号和负号不只是计算符号，它们还可以表达方向。温度上升 3℃ 和下降 3℃、收入 100 元和支出 100 元，都可以用一对符号相反的数表示。",
            commonMistake = "把 0 当作正数。0 是正数与负数的分界点，本身既不是正数也不是负数。",
        ),
        Lesson(
            id = "number-line",
            title = "数轴",
            subtitle = "把数放到一条有方向、有原点、有单位长度的直线上",
            estimatedMinutes = 22,
            textbookPages = 15..20,
            status = MasteryStatus.LEARNING,
            objectives = listOf(
                "认识数轴的原点、正方向和单位长度",
                "能在数轴上表示有理数",
                "能利用左右位置比较数的大小",
            ),
            explanation = "数轴把抽象的数变成了位置。规定向右为正方向后，越靠右的数越大；原点右侧是正数，左侧是负数。",
            commonMistake = "只画一条带箭头的直线，却没有标出原点或统一的单位长度。这样的直线还不能称为数轴。",
        ),
        Lesson(
            id = "opposite-number",
            title = "相反数",
            subtitle = "到原点距离相等、方向相反的两个数",
            estimatedMinutes = 16,
            textbookPages = 21..24,
            status = MasteryStatus.NOT_STARTED,
            objectives = listOf(
                "从数轴位置理解相反数",
                "会求一个数的相反数",
                "理解 0 的相反数仍然是 0",
            ),
            explanation = "3 和 -3 在数轴上分别位于原点两侧，而且到原点的距离相同，所以它们互为相反数。求相反数就是改变这个数的符号。",
            commonMistake = "把相反数和倒数混淆。-3 的相反数是 3，倒数则是 -1/3。",
        ),
        Lesson(
            id = "absolute-value",
            title = "绝对值",
            subtitle = "一个数在数轴上到原点的距离",
            estimatedMinutes = 24,
            textbookPages = 25..31,
            status = MasteryStatus.NOT_STARTED,
            objectives = listOf(
                "从距离理解绝对值",
                "会计算简单有理数的绝对值",
                "理解绝对值一定大于或等于 0",
            ),
            explanation = "绝对值只关心离原点有多远，不关心方向。因此 5 和 -5 的绝对值都是 5。",
            commonMistake = "认为负数的绝对值仍然是负数。距离不可能是负数，所以 |-5| = 5。",
        ),
        Lesson(
            id = "compare-rational",
            title = "有理数大小比较",
            subtitle = "结合数轴、正负号和绝对值比较大小",
            estimatedMinutes = 25,
            textbookPages = 32..37,
            status = MasteryStatus.NOT_STARTED,
            objectives = listOf(
                "能用数轴比较两个有理数",
                "掌握正数、0、负数之间的大小关系",
                "会比较两个负数的大小",
            ),
            explanation = "数轴上右边的数总比左边大。两个负数比较时，绝对值更大的反而更小，因为它离原点更远、位置更靠左。",
            commonMistake = "看到 8 > 3，就误以为 -8 > -3。实际上 -8 在数轴上更靠左，所以 -8 < -3。",
        ),
    )

    val reviews = listOf(
        ReviewItem(
            id = "review-negative-zero",
            title = "0 属于正数还是负数？",
            reason = "昨天曾把 0 归入正数",
            dueLabel = "今天",
        ),
        ReviewItem(
            id = "review-number-line",
            title = "数轴的三个必要条件",
            reason = "需要巩固原点、正方向和单位长度",
            dueLabel = "今天",
        ),
    )

    val dailyPlan = DailyPlan(
        newLessonId = "number-line",
        reviewItems = reviews,
        estimatedMinutes = 25,
    )
}
