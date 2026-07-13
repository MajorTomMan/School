package com.majortomman.school.data.material

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalKnowledgeCompilerTest {
    private val mathSlot = TextbookSlot(
        subjectId = "math",
        subjectTitle = "数学",
        grade = 7,
        volume = TextbookVolume.FIRST,
    )

    @Test
    fun `number line definition becomes structured animated lesson`() {
        val lesson = lesson("number-line", "数轴", 7, 10)
        val pages = listOf(
            page(
                printedPage = 7,
                "规定了原点、正方向和单位长度的直线叫做数轴。",
                "数轴上的点可以表示有理数，右边的数大于左边的数。",
            ),
        )

        val compilation = LocalKnowledgeCompiler.compile(mathSlot, lesson, pages)

        assertNotNull(compilation)
        compilation!!
        assertEquals("number-line", compilation.primary.id)
        assertTrue(compilation.primary.confidence >= 0.72f)
        assertEquals(LessonSceneType.NUMBER_LINE, compilation.analysis.scene.type)
        assertEquals(LessonAnalysisSource.OCR_FALLBACK, compilation.analysis.source)
        assertTrue(compilation.analysis.summary.contains("原点"))
        assertTrue(compilation.analysis.summary.contains("正方向"))
        assertFalse(compilation.analysis.summary.startsWith("教材本地识别内容"))
        assertEquals(7, compilation.analysis.scene.sourcePage)
    }

    @Test
    fun `absolute value text becomes distance scene`() {
        val lesson = lesson("absolute-value", "绝对值", 20, 24)
        val pages = listOf(
            page(
                printedPage = 21,
                "一般地，数轴上表示数a的点与原点的距离叫做数a的绝对值。",
                "一个正数的绝对值是它本身，一个负数的绝对值是它的相反数。",
                "任何一个数的绝对值都是非负数。",
            ),
        )

        val compilation = LocalKnowledgeCompiler.compile(mathSlot, lesson, pages)

        assertNotNull(compilation)
        assertEquals("absolute-value", compilation!!.primary.id)
        assertEquals(LessonSceneType.DISTANCE, compilation.analysis.scene.type)
        assertTrue(compilation.analysis.misconception.contains("距离"))
        assertTrue(compilation.analysis.exercise.acceptedAnswers.contains("5"))
    }

    @Test
    fun `equation method becomes balance process without model`() {
        val lesson = lesson("linear-equation", "一元一次方程", 68, 76)
        val pages = listOf(
            page(
                printedPage = 70,
                "解方程时，等式两边同时加上或减去同一个数，所得方程与原方程的解相同。",
                "把含有未知数的项移到一边，把常数项移到另一边。",
                "2x+3=9，2x=6，x=3。",
            ),
        )

        val compilation = LocalKnowledgeCompiler.compile(mathSlot, lesson, pages)

        assertNotNull(compilation)
        assertEquals("linear-equation", compilation!!.primary.id)
        assertEquals(LessonSceneType.PROCESS, compilation.analysis.scene.type)
        assertTrue(compilation.analysis.scene.expression.contains("x = 3"))
        assertTrue(compilation.analysis.objectives.any { it.contains("相同") || it.contains("等式") })
    }

    @Test
    fun `generic definition is structured instead of dumping raw paragraph`() {
        val slot = TextbookSlot("physics", "物理", 8, TextbookVolume.FIRST)
        val lesson = lesson("speed", "速度", 12, 15)
        val pages = listOf(
            page(
                printedPage = 12,
                "在物理学中，把路程与时间之比叫做速度。",
                "速度表示物体运动的快慢。",
            ),
        )

        val compilation = LocalKnowledgeCompiler.compile(slot, lesson, pages)

        assertNotNull(compilation)
        assertEquals(KnowledgeCategory.DEFINITION, compilation!!.primary.category)
        assertEquals(LessonSceneType.PROCESS, compilation.analysis.scene.type)
        assertTrue(compilation.analysis.summary.contains("定义"))
        assertFalse(compilation.analysis.summary.startsWith("教材本地识别内容"))
    }

    @Test
    fun `unrelated short ocr does not invent a knowledge point`() {
        val lesson = lesson("section", "教材内容 1", 1, 8)
        val pages = listOf(page(printedPage = 1, "目录", "第一章 1"))

        val compilation = LocalKnowledgeCompiler.compile(mathSlot, lesson, pages)

        assertNull(compilation)
    }

    @Test
    fun `ocr fallback delegates to local compiler`() {
        val lesson = lesson("opposite-number", "相反数", 14, 18)
        val pages = listOf(
            page(
                printedPage = 15,
                "只有符号不同的两个数互为相反数。",
                "互为相反数的两个数在数轴上关于原点对称，它们的和为0。",
            ),
        )

        val analysis = LessonAnalysisFallback.generateFromOcr(mathSlot, lesson, pages)

        assertEquals(LessonAnalysisSource.OCR_FALLBACK, analysis.source)
        assertEquals(LessonSceneType.MIRROR, analysis.scene.type)
        assertTrue(analysis.summary.contains("相反数"))
    }

    private fun lesson(
        id: String,
        title: String,
        start: Int,
        end: Int,
    ): GeneratedLesson = GeneratedLesson(
        id = "math-7-1:$id",
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

    private fun page(printedPage: Int, vararg lines: String): OcrPageResult = OcrPageResult(
        printedPage = printedPage,
        pdfIndex = printedPage - 1,
        width = 1200,
        height = 1800,
        text = lines.joinToString("\n"),
        lines = lines.mapIndexed { index, text ->
            OcrTextLine(
                text = text,
                left = 0.08f,
                top = 0.1f + index * 0.08f,
                right = 0.92f,
                bottom = 0.15f + index * 0.08f,
            )
        },
    )
}
