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
        assertTrue("基准和当前记录应由 Compose Text 排版", "InlineQuantitySummary(" in source)
        assertTrue("两个方向应使用独立 Compose Text 区域", "negativeMeaning" in source && "positiveMeaning" in source)
        assertFalse("基准轴 Canvas 内不得绘制文字", "drawText(" in source || "nativeCanvas" in source)
        assertFalse("基准轴不得重新使用圆角卡片分区", "RoundedCornerShape" in source)
    }

    @Test
    fun coreLearningStyleUsesLinesAndWhitespaceInsteadOfCards() {
        val shared = uiFile("InteractiveLessonScreen.kt").readText(Charsets.UTF_8)
        val opposite = uiFile("OppositeQuantitiesSceneVisual.kt").readText(Charsets.UTF_8)
        assertTrue("学习界面共享面板必须保持透明", "InteractivePanel = Color.Transparent" in shared)
        assertFalse("通用学习操作不应使用圆角卡片", "RoundedCornerShape" in shared)
        assertFalse("相反意义量场景不应使用圆角卡片", "RoundedCornerShape" in opposite)
        assertTrue("通用操作应使用底部强调线", "Alignment.BottomCenter" in shared)
        assertTrue("场景分区应使用细线", "InteractiveLine" in opposite)
    }

    @Test
    fun sharedDesignSystemUsesOpenSectionsInsteadOfCards() {
        val design = uiFile("DesignSystem.kt").readText(Charsets.UTF_8)
        assertFalse("共享设计系统不得创建 Material Card", "Card(" in design)
        assertFalse("共享设计系统不得创建 Material Surface", "Surface(" in design)
        assertFalse("共享设计系统不得依赖圆角卡片", "RoundedCornerShape" in design)
        assertTrue("共享信息段落应使用细线建立层级", ".height(1.dp)" in design || ".height(2.dp)" in design)

        listOf(
            "SubjectTextbookCenterComponents.kt",
            "AssessmentLearningContentRenderer.kt",
            "RationalConceptFlowVisual.kt",
        ).forEach { name ->
            val source = uiFile(name).readText(Charsets.UTF_8)
            assertFalse("$name 的普通信息区不得使用圆角卡片", "RoundedCornerShape" in source)
        }
    }

    @Test
    fun assessmentFeedbackAndActionsStayCardless() {
        val session = uiFile("AssessmentSessionScreen.kt").readText(Charsets.UTF_8)
        val ai = uiFile("AssessmentAiJudgeSection.kt").readText(Charsets.UTF_8)

        assertFalse(
            "选择题选项不应使用填充圆角卡片",
            ".background(\n                                if (checked) InteractiveBlue.copy(alpha = 0.10f) else InteractivePanel" in session,
        )
        assertFalse(
            "判题反馈不应使用填充圆角卡片",
            ".background(color.copy(alpha = 0.10f), RoundedCornerShape" in session,
        )
        assertFalse(
            "提示内容不应使用填充圆角卡片",
            ".background(InteractiveBlue.copy(alpha = 0.08f), RoundedCornerShape" in session,
        )
        assertFalse(
            "题目导航不应使用填充圆角按钮",
            "if (enabled) color.copy(alpha = 0.12f) else InteractivePanel" in session,
        )
        assertFalse(
            "AI 答案区不应使用面板卡片",
            ".background(InteractivePanel, RoundedCornerShape" in ai,
        )
        assertFalse(
            "AI 判题操作不应使用填充圆角按钮",
            "if (enabled || busy) color.copy(alpha = 0.14f) else InteractivePanel" in ai,
        )
        assertTrue("判题操作应使用底部强调线", "Alignment.BottomCenter" in session && "Alignment.BottomCenter" in ai)
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
