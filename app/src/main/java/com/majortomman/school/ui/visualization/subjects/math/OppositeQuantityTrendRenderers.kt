package com.majortomman.school.ui.visualization.subjects.math

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.majortomman.school.ui.visualization.core.TechnicalLineChart
import com.majortomman.school.ui.visualization.core.TechnicalLineChartModel
import com.majortomman.school.ui.visualization.core.VisualizationFieldSpec
import com.majortomman.school.ui.visualization.core.VisualizationKey
import com.majortomman.school.ui.visualization.core.VisualizationRenderContext
import com.majortomman.school.ui.visualization.core.VisualizationSchema
import com.majortomman.school.ui.visualization.core.VisualizationValueType
import com.majortomman.school.ui.visualization.core.VisualizationPoint
import com.majortomman.school.ui.visualization.subjects.MathematicsVisualizationRenderer

object AccountTrendVisualizationRenderer : MathematicsVisualizationRenderer() {
    override val key: VisualizationKey = VisualizationKey("mathematics.opposite.account-trend")
    override val schema: VisualizationSchema = oppositeTrendSchema()

    @Composable
    override fun RenderContent(context: VisualizationRenderContext, modifier: Modifier) {
        val value = context.arguments.float("value")
        val bound = context.arguments.float("bound", 50f).coerceAtLeast(1f)
        val unit = context.arguments.string("unit", "万元")
        TechnicalLineChart(
            model = TechnicalLineChartModel(
                points = orderedSeries(
                    historical = listOf(-20f, -8f, 12f, 5f, 24f, 16f),
                    current = value,
                ),
                minimum = -bound,
                maximum = bound,
                unit = unit,
                positiveMeaning = "盈利",
                negativeMeaning = "亏损",
                baselineLabel = "收支平衡 0 $unit",
            ),
            palette = context.palette,
            modifier = modifier,
        )
    }
}

object GrowthRateTrendVisualizationRenderer : MathematicsVisualizationRenderer() {
    override val key: VisualizationKey = VisualizationKey("mathematics.opposite.growth-rate-trend")
    override val schema: VisualizationSchema = oppositeTrendSchema()

    @Composable
    override fun RenderContent(context: VisualizationRenderContext, modifier: Modifier) {
        val value = context.arguments.float("value")
        val bound = context.arguments.float("bound", 10f).coerceAtLeast(0.1f)
        val unit = context.arguments.string("unit", "%")
        TechnicalLineChart(
            model = TechnicalLineChartModel(
                points = orderedSeries(
                    historical = listOf(2.4f, 1.2f, -0.4f, 0.8f, -1.1f, 0.3f),
                    current = value,
                ),
                minimum = -bound,
                maximum = bound,
                unit = unit,
                positiveMeaning = "增长",
                negativeMeaning = "减少",
                baselineLabel = "变化率 0%",
                decimals = 1,
            ),
            palette = context.palette,
            modifier = modifier,
        )
    }
}

private fun oppositeTrendSchema(): VisualizationSchema = VisualizationSchema(
    fields = listOf(
        VisualizationFieldSpec("value", VisualizationValueType.NUMBER, description = "当前期数值"),
        VisualizationFieldSpec("bound", VisualizationValueType.NUMBER, description = "纵轴绝对边界"),
        VisualizationFieldSpec("unit", VisualizationValueType.STRING, description = "显示单位"),
    ),
)

private fun orderedSeries(historical: List<Float>, current: Float): List<VisualizationPoint> {
    val labels = listOf("一", "二", "三", "四", "五", "六", "当前")
    return (historical.take(6) + current).mapIndexed { index, value ->
        VisualizationPoint(label = labels[index], value = value)
    }
}
