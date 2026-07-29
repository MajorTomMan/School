package com.majortomman.school.ui.visualization

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualizationArchitectureSourcePolicyTest {
    @Test
    fun frameworkProvidesSubjectParentsAndExplicitRegistry() {
        val parents = source("app/src/main/java/com/majortomman/school/ui/visualization/subjects/SubjectVisualizationRenderers.kt")
        val catalog = source("app/src/main/java/com/majortomman/school/ui/visualization/SchoolVisualizationCatalog.kt")
        val framework = source("app/src/main/java/com/majortomman/school/ui/visualization/core/VisualizationFramework.kt")

        listOf(
            "MathematicsVisualizationRenderer",
            "PhysicsVisualizationRenderer",
            "ChemistryVisualizationRenderer",
            "BiologyVisualizationRenderer",
        ).forEach { parent -> assertTrue("缺少学科父类 $parent", "abstract class $parent" in parents) }

        assertTrue("框架应校验参数 schema", "VisualizationSchema" in framework && "fun validate(" in framework)
        assertTrue("框架应拒绝重复 key", "重复的可视化 key" in framework)
        assertTrue("学校目录应显式包含四大学科模块", listOf(
            "MathematicsVisualizationModule",
            "PhysicsVisualizationModule",
            "ChemistryVisualizationModule",
            "BiologyVisualizationModule",
        ).all(catalog::contains))
    }

    @Test
    fun sharedToolsStaySubjectNeutralAndCardless() {
        val zoom = source("app/src/main/java/com/majortomman/school/ui/ZoomableMathCanvas.kt")
        val chart = source("app/src/main/java/com/majortomman/school/ui/visualization/core/TechnicalLineChart.kt")
        val primitives = source("app/src/main/java/com/majortomman/school/ui/visualization/core/VisualizationPrimitives.kt")

        assertTrue("通用缩放工具必须使用学科中立名称", "ZoomableVisualizationCanvas(" in zoom)
        assertTrue("数学旧接口应保留兼容包装", "ZoomableMathCanvas(" in zoom)
        assertTrue("折线图必须使用通用缩放画布", "ZoomableVisualizationCanvas(" in chart)
        assertTrue("折线图必须复用网格和标记原语", "drawTechnicalGrid(" in chart && "drawDataMarker(" in chart)
        assertTrue("原语应提供向量箭头供物理化学生物复用", "drawVectorArrow(" in primitives)
        assertFalse("通用图表不得引入圆角卡片", "RoundedCornerShape" in chart || "Card(" in chart)
        assertFalse("Canvas 内不得绘制文字", "nativeCanvas" in chart || "drawText(" in chart)
    }

    @Test
    fun accountAndGrowthScenesUseRegisteredLineRenderers() {
        val entry = source("app/src/main/java/com/majortomman/school/ui/OppositeQuantitiesSceneVisual.kt")
        val renderers = source(
            "app/src/main/java/com/majortomman/school/ui/visualization/subjects/math/OppositeQuantityTrendRenderers.kt",
        )

        assertTrue("收支场景应走折线渲染器", "AccountTrendVisualizationRenderer.key" in entry)
        assertTrue("增长率场景应走折线渲染器", "GrowthRateTrendVisualizationRenderer.key" in entry)
        assertTrue("收支渲染器必须继承数学父类", "AccountTrendVisualizationRenderer : MathematicsVisualizationRenderer()" in renderers)
        assertTrue("增长率渲染器必须继承数学父类", "GrowthRateTrendVisualizationRenderer : MathematicsVisualizationRenderer()" in renderers)
        assertTrue("趋势图应使用 TechnicalLineChart", renderers.countOccurrences("TechnicalLineChart(") >= 2)
    }

    @Test
    fun toleranceSceneUsesRegisteredPartOverlayRenderer() {
        val entry = source("app/src/main/java/com/majortomman/school/ui/OppositeQuantitiesSceneVisual.kt")
        val catalog = source("app/src/main/java/com/majortomman/school/ui/visualization/SchoolVisualizationCatalog.kt")
        val part = source(
            "app/src/main/java/com/majortomman/school/ui/visualization/subjects/math/PartToleranceVisualizationRenderer.kt",
        )

        assertTrue("允许偏差应走零件叠影渲染器", "PartToleranceVisualizationRenderer.key" in entry)
        assertTrue("零件公差渲染器应注册到数学模块", "PartToleranceVisualizationRenderer" in catalog)
        assertTrue("零件公差必须继承数学父类", "PartToleranceVisualizationRenderer : MathematicsVisualizationRenderer()" in part)
        assertTrue("零件图必须包含标准件虚线轮廓", "dashPathEffect" in part && "标准件" in part)
        assertTrue("零件图必须包含当前件实线轮廓", "实线：当前件" in part)
        assertTrue("零件图必须包含允许公差带", "ToleranceBand(" in part && "允许范围" in part)
        assertTrue("零件图应明确教学比例放大", "教学比例放大" in part)
        assertFalse("零件可视化 Canvas 内不得绘制文字", "nativeCanvas" in part || "drawText(" in part)
        assertFalse("零件可视化不得退化成卡片", "RoundedCornerShape" in part || "Card(" in part)
    }

    private fun String.countOccurrences(token: String): Int = windowed(token.length).count { it == token }

    private fun source(relative: String): String = repositoryFile(relative).readText(Charsets.UTF_8)

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
