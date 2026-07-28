package com.majortomman.school.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualizationSourcePolicyTest {
    private val directZoomVisualFiles = listOf(
        "CloudCourseBlockRenderer.kt",
        "CloudCourseVisualizations.kt",
        "TextbookMathVisualizations.kt",
        "RationalExamplesVisual.kt",
        "OppositeQuantityAxisPanel.kt",
        "NumberLineLessonVisual.kt",
    )

    private val productionVisualFiles = directZoomVisualFiles + "OppositeQuantitiesSceneVisual.kt"

    @Test
    fun productionMathVisualsUseSharedZoomSurface() {
        directZoomVisualFiles.forEach { name ->
            val source = uiFile(name).readText(Charsets.UTF_8)
            assertTrue("$name 应使用 ZoomableMathCanvas", "ZoomableMathCanvas(" in source)
            assertFalse("$name 不得继续直接导入裸 Canvas", "import androidx.compose.foundation.Canvas" in source)
        }

        val oppositeScene = uiFile("OppositeQuantitiesSceneVisual.kt").readText(Charsets.UTF_8)
        assertTrue(
            "相反意义量场景应委托给统一的基准轴布局",
            "OppositeQuantityAxisPanel(" in oppositeScene,
        )
        assertFalse(
            "相反意义量场景本身不应再直接绘制带文字的 Canvas",
            "nativeCanvas" in oppositeScene || "drawSceneLabel" in oppositeScene,
        )
    }

    @Test
    fun canvasTextUsesScaledTypographyInsteadOfRawPixels() {
        val textHelpers = productionVisualFiles
            .map(::uiFile)
            .joinToString("\n") { it.readText(Charsets.UTF_8) }
        assertFalse(
            "生产数学图不得把传入字号直接赋给 Android Paint 像素",
            "this.textSize = textSize\n" in textHelpers,
        )
        assertTrue(
            "生产数学图必须接入全局可视化字号换算或使用 Compose Text",
            "visualTextSizePx(" in textHelpers || "Text(" in textHelpers,
        )
    }

    @Test
    fun baselineAxisKeepsTextOutsideCanvas() {
        val source = uiFile("OppositeQuantityAxisPanel.kt").readText(Charsets.UTF_8)
        assertTrue("基准、方向和距离说明应由 Compose Text 排版", "QuantitySummaryCell(" in source)
        assertTrue("两个方向应使用独立布局单元", "DirectionMeaningCell(" in source)
        assertFalse("基准轴 Canvas 内不得绘制文字", "drawText(" in source || "nativeCanvas" in source)
    }

    @Test
    fun pdfReaderStatusIsSingleLineAndIndependentFromTitle() {
        val source = uiFile("PdfTextbookScreen.kt").readText(Charsets.UTF_8)
        assertTrue("PDF 页码状态应使用独立标题组件", "PdfReaderHeader(" in source)
        assertTrue("PDF 页码状态必须禁止逐字换行", "softWrap = false" in source)
        assertTrue("PDF 页码状态必须限制为一行", "maxLines = 1" in source)
    }

    private fun uiFile(name: String): File = repositoryFile(
        "app/src/main/java/com/majortomman/school/ui/$name",
    )

    private fun repositoryFile(relative: String): File {
        var current = File(System.getProperty("user.dir"))
        repeat(8) {
            val candidate = File(current, relative)
            if (candidate.isFile) return candidate
            current = current.parentFile ?: return@repeat
        }
        error("无法定位仓库文件：$relative")
    }
}
