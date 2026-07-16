package com.majortomman.school.ui

import com.majortomman.school.data.Lesson
import com.majortomman.school.data.MasteryStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InteractiveLessonCatalogTest {
    @Test
    fun resolvesLinearFunctionLesson() {
        val spec = InteractiveLessonCatalog.resolve("math", lesson("一次函数", 114..126))
        assertEquals(InteractiveLessonKind.LINEAR_FUNCTION, spec?.kind)
        assertEquals(114, spec?.sourcePage)
        assertEquals(126, spec?.sourcePageEnd)
    }

    @Test
    fun resolvesNewtonFirstLawLesson() {
        val spec = InteractiveLessonCatalog.resolve("physics", lesson("1.牛顿第一定律", 82..86))
        assertEquals(InteractiveLessonKind.NEWTON_FIRST_LAW, spec?.kind)
    }

    @Test
    fun doesNotHijackUnrelatedLesson() {
        assertNull(InteractiveLessonCatalog.resolve("math", lesson("二次函数", 1..5)))
    }

    private fun lesson(title: String, pages: IntRange) = Lesson(
        id = "test:$title",
        title = title,
        subtitle = "",
        estimatedMinutes = 15,
        textbookPages = pages,
        status = MasteryStatus.NOT_STARTED,
        objectives = emptyList(),
        explanation = "",
        commonMistake = "",
    )
}
