package com.majortomman.school.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualizationSourcePolicyTest {
    @Test
    fun cloudCourseUsesDedicatedTextbookAlignedRenderers() {
        val source = uiFile("CloudCourseBlockRenderer.kt").readText(Charsets.UTF_8)
        assertTrue("相反意义量必须使用专用教材渲染器", "OppositeQuantitiesSceneVisual(scene.data)" in source)
        assertTrue("有理数分类必须使用专用教材渲染器", "RationalConceptFlowVisual(scene.data)" in source)
        assertTrue("整数化分数必须使用专用教材渲染器", "IntegerToFractionTextbookVisual()" in source)
        assertTrue("数轴必须使用专用教材渲染器", "NumberLineLessonVisual(scene.data)" in source)
    }

    @Test
    fun oppositeQuantitySceneUsesIllustratedPanels() {
        val source = uiFile("OppositeQuantitiesSceneVisual.kt").readText(Charsets.UTF_8)
        assertTrue("相反意义量必须根据 mode 选择具象场景", "OppositeQuantityMode.from" in source)
        assertTrue("温度必须进入温度计具象面板", "OppositeQuantityMode.TEMPERATURE -> TemperatureQuantityPanel" in source)
        assertTrue("海拔必须进入山峰海平面具象面板", "OppositeQuantityMode.ALTITUDE -> AltitudeQuantityPanel" in source)
        assertTrue("质量偏差必须进入天平具象面板", "OppositeQuantityMode.MASS_DEVIATION -> MassDeviationQuantityPanel" in source)
    }

    @Test
    fun oppositeQuantitySceneKeepsOriginalMeaningLabels() {
        val source = uiFile("OppositeQuantitiesSceneVisual.kt").readText(Charsets.UTF_8)
        assertTrue("具象场景必须继续读取 positiveLabel", "positiveLabel = data.string(\"positiveLabel\")" in source)
        assertTrue("具象场景必须继续读取 negativeLabel", "negativeLabel = data.string(\"negativeLabel\")" in source)
        assertTrue("具象场景必须继续读取 baselineLabel", "baselineLabel = data.string(\"baselineLabel\")" in source)
    }

    @Test
    fun rationalClassificationUsesHierarchyNotDecorativeCards() {
        val source = uiFile("RationalConceptFlowVisual.kt").readText(Charsets.UTF_8)
        assertTrue("有理数分类应使用层级分支结构", "RationalBranch(" in source)
        assertTrue("有理数分类必须呈现整数分支", "title = \"整数\"" in source)
        assertTrue("有理数分类必须呈现分数分支", "title = \"分数\"" in source)
        assertFalse("有理数分类不应退化成圆角卡片", "RoundedCornerShape" in source)
    }

    @Test
    fun numberLineUsesDedicatedInteractionInsteadOfGenericDiagram() {
        val source = uiFile("NumberLineLessonVisual.kt").readText(Charsets.UTF_8)
        assertTrue("数轴场景必须允许拖动点", "detectDragGestures" in source)
        assertTrue("数轴场景必须绘制原点", "原点" in source)
        assertTrue("数轴场景必须表达正方向", "正方向" in source)
        assertTrue("数轴场景必须表达单位长度", "单位长度" in source)
    }

    @Test
    fun illustratedPanelsUseComposeTextAndConcreteObjects() {
        val source = uiFile("OppositeQuantityIllustratedPanels.kt").readText(Charsets.UTF_8)
        assertTrue("温度场景必须绘制温度计", "TemperatureQuantityPanel(" in source && "tubeHalfWidth" in source)
        assertTrue("海拔场景必须包含山脉与海平面", "foregroundMountain" in source && "seaY" in source)
        assertTrue("质量偏差场景必须包含天平与偏差轴", "MassDeviationQuantityPanel(" in source && "drawPan(" in source)
        assertTrue("具象场景文字应由 Compose 排版", "DirectionLegend(" in source && "IllustratedStatusLine(" in source)
        assertFalse("具象场景不得使用 nativeCanvas 绘制文字", "nativeCanvas" in source || "drawText(" in source)
        assertFalse("具象场景不得退化成圆角信息卡片", "RoundedCornerShape" in source)
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
    fun courseNavigationUsesAdaptiveLineActions() {
        val source = uiFile("InteractiveLessonScreen.kt").readText(Charsets.UTF_8)
        val shared = uiFile("SchoolUiSystem.kt").readText(Charsets.UTF_8)
        assertFalse("课程导航不得使用固定面板背景", ".background(InteractivePanel" in source)
        assertFalse("课程导航不得使用固定圆角背景", "RoundedCornerShape" in source)
        assertTrue("课程底部导航应避开系统导航栏", "navigationBarsPadding()" in source)
        assertTrue("课程正文与底部导航应使用统一细线分隔", "SchoolDivider(color = InteractiveLine)" in source && ".height(1.dp)" in shared)
        assertTrue("完成操作应保持开放式文字翻页交互", "完成并继续 →" in source && "LessonPagerFooter(" in source && ".clickable(onClick = onNext)" in source)
    }

    @Test
    fun sharedDesignSystemUsesOpenSectionsInsteadOfCards() {
        val design = uiFile("DesignSystem.kt").readText(Charsets.UTF_8)
        assertFalse("共享设计系统不得导入 Material Card", "import androidx.compose.material3.Card" in design)
        assertFalse("共享设计系统不得实例化 Material Card", "\n    Card(" in design)
        assertFalse("共享设计系统不得导入 Material Surface", "import androidx.compose.material3.Surface" in design)
        assertFalse("共享设计系统不得实例化 Material Surface", "\n    Surface(" in design)
        assertFalse("共享设计系统不得依赖圆角卡片", "RoundedCornerShape" in design)
        assertTrue("共享信息段落应使用细线建立层级", ".height(1.dp)" in design || ".height(2.dp)" in design)

        listOf("SubjectTextbookCenterComponents.kt", "AssessmentLearningContentRenderer.kt", "RationalConceptFlowVisual.kt").forEach { name ->
            val source = uiFile(name).readText(Charsets.UTF_8)
            assertFalse("$name 的普通信息区不得使用圆角卡片", "RoundedCornerShape" in source)
        }
    }

    @Test
    fun declarativeDiagramUsesOpenCanvas() {
        val source = uiFile("TextbookMathVisual.kt").readText(Charsets.UTF_8)
        assertTrue("声明式 diagram 必须仍由开放式 Canvas 渲染", "DeclarativeDiagramVisual(" in source)
        assertFalse("声明式 diagram 不得使用圆角卡片", "RoundedCornerShape" in source)
    }

    private fun uiFile(name: String): File = repositoryFile("app/src/main/java/com/majortomman/school/ui/$name")

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
