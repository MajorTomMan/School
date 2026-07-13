package com.majortomman.school.data.material

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextbookOcrDiagnosticsTest {
    @Test
    fun `ocr page keeps raw text and cleaned text independently`() {
        val result = OcrPageResult(
            printedPage = 7,
            pdfIndex = 10,
            width = 1200,
            height = 1800,
            rawText = "数轴的定义\n7402\n规定了原点、正方向和单位长度的直线叫做数轴。",
            text = "数轴的定义\n规定了原点、正方向和单位长度的直线叫做数轴。",
            lines = listOf(
                OcrTextLine("数轴的定义", 0.1f, 0.1f, 0.5f, 0.15f),
                OcrTextLine("7402", 0.45f, 0.94f, 0.54f, 0.97f, ignoredReason = "页眉页脚数字", suspicious = true),
                OcrTextLine("规定了原点、正方向和单位长度的直线叫做数轴。", 0.1f, 0.3f, 0.9f, 0.38f),
            ),
            diagnostics = OcrPageDiagnostics(
                durationMs = 138,
                rawLineCount = 3,
                keptLineCount = 2,
                ignoredLineCount = 1,
                suspiciousTokens = listOf("7402"),
            ),
        )

        val restored = OcrPageResult.fromJson(result.toJson())

        assertTrue("7402" in restored.rawText)
        assertFalse("7402" in restored.text)
        assertEquals(1, restored.diagnostics.ignoredLineCount)
        assertEquals(listOf("7402"), restored.diagnostics.suspiciousTokens)
        assertEquals("页眉页脚数字", restored.lines[1].ignoredReason)
    }

    @Test
    fun `diagnostic store exposes ignored lines for in app log viewer`() {
        val root = Files.createTempDirectory("school-ocr-diagnostics").toFile()
        val result = OcrPageResult(
            printedPage = 12,
            pdfIndex = 15,
            width = 1000,
            height = 1600,
            rawText = "绝对值\n7402",
            text = "绝对值",
            lines = listOf(
                OcrTextLine("绝对值", 0.1f, 0.2f, 0.4f, 0.25f),
                OcrTextLine("7402", 0.46f, 0.94f, 0.54f, 0.97f, ignoredReason = "孤立长数字", suspicious = true),
            ),
            diagnostics = OcrPageDiagnostics(
                durationMs = 80,
                rawLineCount = 2,
                keptLineCount = 1,
                ignoredLineCount = 1,
                suspiciousTokens = listOf("7402"),
            ),
        )

        TextbookOcrStore.write(root, result)
        val records = TextbookOcrStore.readDiagnostics(root)

        assertEquals(1, records.size)
        assertEquals(12, records.first().printedPage)
        assertTrue(records.first().ignoredLines.any { "7402" in it })
        assertTrue("绝对值" in records.first().cleanedPreview)
    }
}
