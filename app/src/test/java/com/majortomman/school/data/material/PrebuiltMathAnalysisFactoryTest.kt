package com.majortomman.school.data.material

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrebuiltMathAnalysisFactoryTest {
    private val slot = TextbookSlot(
        subjectId = "math",
        subjectTitle = "数学",
        grade = 7,
        volume = TextbookVolume.FIRST,
        stage = EducationStage.JUNIOR_HIGH,
    )

    @Test
    fun `number line keeps the dedicated animated scene`() {
        val analysis = PrebuiltMathAnalysisFactory.create(
            slot,
            lesson("kp-number-line", "数轴", 8, 10),
        )

        assertEquals(LessonAnalysisSource.PACK, analysis.source)
        assertEquals(LessonSceneType.NUMBER_LINE, analysis.scene.type)
        assertEquals(listOf(-3.0, 2.0), analysis.scene.values)
        assertTrue(analysis.summary.contains("位置"))
        assertEquals(8, analysis.scene.sourcePage)
    }

    @Test
    fun `quadratic function becomes a structured process instead of raw text`() {
        val analysis = PrebuiltMathAnalysisFactory.create(
            slot.copy(grade = 9),
            lesson("kp-quadratic", "二次函数的图象和性质", 28, 42),
        )

        assertEquals(LessonAnalysisSource.PACK, analysis.source)
        assertEquals(LessonSceneType.PROCESS, analysis.scene.type)
        assertEquals("y=ax²+bx+c", analysis.scene.expression)
        assertTrue(analysis.scene.steps.size >= 4)
        assertTrue(analysis.misconception.contains("定义域"))
    }

    @Test
    fun `equation lesson contains deterministic learning steps`() {
        val analysis = PrebuiltMathAnalysisFactory.create(
            slot,
            lesson("kp-equation", "解一元一次方程", 120, 132),
        )

        assertEquals("2x+3=9 → 2x=6 → x=3", analysis.scene.expression)
        assertTrue(analysis.scene.steps.contains("代回检验"))
        assertTrue("x=3" in analysis.exercise.acceptedAnswers)
    }

    private fun lesson(id: String, title: String, start: Int, end: Int) = GeneratedLesson(
        id = "${slot.key}:$id",
        sourceId = id,
        title = title,
        subtitle = "教材第 $start—$end 页",
        estimatedMinutes = 18,
        pageStart = start,
        pageEnd = end,
        objectives = emptyList(),
        explanation = "",
        commonMistake = "",
    )
}
