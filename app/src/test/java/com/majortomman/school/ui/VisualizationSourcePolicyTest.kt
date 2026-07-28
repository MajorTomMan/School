package com.majortomman.school.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualizationSourcePolicyTest {
    private val productionVisualFiles = listOf(
        "CloudCourseBlockRenderer.kt",
        "CloudCourseVisualizations.kt",
        "TextbookMathVisualizations.kt",
        "RationalExamplesVisual.kt",
        "OppositeQuantitiesSceneVisual.kt",
        "NumberLineLessonVisual.kt",
    )

    @Test
    fun productionMathVisualsUseSharedZoomSurface() {
        productionVisualFiles.forEach { name ->
            val source = uiFile(name).readText(Charsets.UTF_8)
            assertTrue("$name 应使用 ZoomableMathCanvas", "ZoomableMathCanvas(" in source)
            assertFalse("$name 不得继续直接导入裸 Canvas", "import androidx.compose.foundation.Canvas" in source)
        }
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
            "生产数学图必须接入全局可视化字号换算",
            "visualTextSizePx(" in textHelpers || "TextUnit" in textHelpers,
        )
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
