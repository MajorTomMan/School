package com.majortomman.school.visualization.renderers.math

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.majortomman.school.visualization.VisualizationInvocation
import com.majortomman.school.visualization.VisualizationKey
import com.majortomman.school.visualization.VisualizationRenderContext
import com.majortomman.school.visualization.VisualizationRenderer
import kotlin.math.abs
import kotlin.math.round

internal object MathVisualizationRenderers {
    val all: List<VisualizationRenderer> = listOf(
        OppositeQuantitiesRenderer(),
        RationalClassificationRenderer(),
        IntegerToFractionRenderer(),
        ExpressionProcessRenderer(),
        SignRuleRenderer(),
        PowerProcessRenderer(),
        EquationBalanceRenderer(),
        NumberLineRenderer(VisualizationKey("mathematics.number-line.basic"), NumberLineVariant.BASIC),
        NumberLineRenderer(VisualizationKey("mathematics.number-line.construction"), NumberLineVariant.CONSTRUCTION),
        NumberLineRenderer(VisualizationKey("mathematics.number-line.points"), NumberLineVariant.POINTS),
        NumberLineRenderer(VisualizationKey("mathematics.number-line.opposite"), NumberLineVariant.OPPOSITE),
        NumberLineRenderer(VisualizationKey("mathematics.number-line.absolute-value"), NumberLineVariant.ABSOLUTE_VALUE),
        NumberLineRenderer(VisualizationKey("mathematics.number-line.comparison"), NumberLineVariant.COMPARISON),
        NumberLineRenderer(VisualizationKey("mathematics.number-line.movement"), NumberLineVariant.MOVEMENT),
        NumberLineRenderer(VisualizationKey("mathematics.number-line.root"), NumberLineVariant.ROOT),
        CartesianRenderer(VisualizationKey("mathematics.cartesian.point"), CartesianVariant.POINT),
        CartesianRenderer(VisualizationKey("mathematics.cartesian.linear"), CartesianVariant.LINEAR),
        CartesianRenderer(VisualizationKey("mathematics.cartesian.quadratic"), CartesianVariant.QUADRATIC),
        CartesianRenderer(VisualizationKey("mathematics.cartesian.inverse"), CartesianVariant.INVERSE),
        GeometryRenderer(VisualizationKey("mathematics.geometry.triangle"), GeometryVariant.TRIANGLE),
        GeometryRenderer(VisualizationKey("mathematics.geometry.circle"), GeometryVariant.CIRCLE),
        GeometryRenderer(VisualizationKey("mathematics.geometry.angle"), GeometryVariant.ANGLE),
        GeometryRenderer(VisualizationKey("mathematics.geometry.parallel"), GeometryVariant.PARALLEL),
        GeometryRenderer(VisualizationKey("mathematics.geometry.right-triangle"), GeometryVariant.RIGHT_TRIANGLE),
        GeometryRenderer(VisualizationKey("mathematics.geometry.line-ray-segment"), GeometryVariant.LINE_RAY_SEGMENT),
        GeometryRenderer(VisualizationKey("mathematics.geometry.projection"), GeometryVariant.PROJECTION),
        GeometryRenderer(VisualizationKey("mathematics.geometry.object-abstraction"), GeometryVariant.OBJECT_ABSTRACTION),
        TransformationRenderer(VisualizationKey("mathematics.geometry.translation"), TransformVariant.TRANSLATION),
        TransformationRenderer(VisualizationKey("mathematics.geometry.symmetry"), TransformVariant.SYMMETRY),
        TransformationRenderer(VisualizationKey("mathematics.geometry.rotation"), TransformVariant.ROTATION),
        BasicChartRenderer(VisualizationKey("mathematics.chart.line"), ChartVariant.LINE),
        BasicChartRenderer(VisualizationKey("mathematics.chart.bar"), ChartVariant.BAR),
        ProbabilityTreeRenderer(),
    ).map(::ValidatedMathRenderer)
}

private class ValidatedMathRenderer(private val delegate: VisualizationRenderer) : VisualizationRenderer() {
    override val key = delegate.key
    override val subject = delegate.subject
    override val schema = delegate.schema

    override fun validate(invocation: VisualizationInvocation): List<String> {
        val structural = delegate.validate(invocation)
        if (structural.isNotEmpty()) return structural
        return validateMathSemantics(invocation)
    }

    @Composable
    override fun Render(context: VisualizationRenderContext, modifier: Modifier) {
        key(context.invocation) { delegate.Render(context, modifier) }
    }
}

private fun validateMathSemantics(invocation: VisualizationInvocation): List<String> = buildList {
    val key = invocation.renderer.value
    val parameters = invocation.parameters

    if (key.startsWith("mathematics.number-line.")) {
        val min = parameters.number("min", -8.0)
        val max = parameters.number("max", 8.0)
        val step = parameters.number("step", 1.0)
        if (max <= min) add("参数 max 必须大于 min")
        if (step <= 0.0) add("参数 step 必须大于 0")
        if (max > min && step > 0.0 && (max - min) / step > 80.0 + 1e-9) add("数轴刻度数量不能超过 80")
        if (isEmpty()) {
            fun inRange(value: Double): Boolean = value in min..max
            when (key) {
                "mathematics.number-line.basic" -> if ("value" in parameters.keys && !inRange(parameters.number("value"))) add("参数 value 必须位于数轴范围内")
                "mathematics.number-line.construction" -> if (0.0 !in min..max) add("construction 数轴范围必须包含 0")
                "mathematics.number-line.points" -> {
                    val values = parameters.numberList("values")
                    if (values.isEmpty()) add("参数 values 不能为空")
                    if (values.size > 8) add("参数 values 最多包含 8 个点")
                    if (values.any { !inRange(it) }) add("参数 values 中的点必须位于数轴范围内")
                }
                "mathematics.number-line.opposite" -> {
                    val value = abs(parameters.number("value", 3.0))
                    if (0.0 !in min..max) add("opposite 数轴范围必须包含 0")
                    if (value > minOf(abs(min), abs(max))) add("参数 value 的正负两个点必须都位于数轴范围内")
                }
                "mathematics.number-line.absolute-value" -> {
                    val value = parameters.number("value", -3.0)
                    if (!inRange(value)) add("参数 value 必须位于数轴范围内")
                    if (0.0 !in min..max || abs(value) > max) add("value 与其绝对值必须都能显示在数轴范围内")
                }
                "mathematics.number-line.comparison" -> {
                    if ("left" in parameters.keys && !inRange(parameters.number("left"))) add("参数 left 必须位于数轴范围内")
                    if ("right" in parameters.keys && !inRange(parameters.number("right"))) add("参数 right 必须位于数轴范围内")
                }
                "mathematics.number-line.movement" -> {
                    val start = parameters.number("start", -3.0)
                    val delta = parameters.number("delta", 2.0)
                    if (!inRange(start)) add("参数 start 必须位于数轴范围内")
                    if (!inRange(start + delta)) add("start + delta 必须位于数轴范围内")
                }
                "mathematics.number-line.root" -> if (!inRange(parameters.number("value"))) add("参数 value 必须位于数轴范围内")
            }
        }
    }

    if (key.startsWith("mathematics.cartesian.")) {
        val xMin = parameters.number("xMin", -5.0)
        val xMax = parameters.number("xMax", 5.0)
        val yMin = parameters.number("yMin", -4.0)
        val yMax = parameters.number("yMax", 4.0)
        if (xMax <= xMin) add("参数 xMax 必须大于 xMin")
        if (yMax <= yMin) add("参数 yMax 必须大于 yMin")
        if (key == "mathematics.cartesian.point" && xMax > xMin && yMax > yMin) {
            if (parameters.number("x") !in xMin..xMax) add("参数 x 必须位于横轴范围内")
            if (parameters.number("y") !in yMin..yMax) add("参数 y 必须位于纵轴范围内")
        }
    }

    if (key == "mathematics.chart.line" || key == "mathematics.chart.bar") {
        val values = parameters.numberList("values")
        if (values.isEmpty()) add("参数 values 不能为空")
        if (values.size > 8) add("参数 values 最多包含 8 个值")
    }

    if (key == "mathematics.process.power") {
        val exponent = parameters.number("exponent")
        val minBase = parameters.number("minBase", -4.0)
        val maxBase = parameters.number("maxBase", 4.0)
        val base = parameters.number("base")
        if (abs(exponent - round(exponent)) > 1e-9 || exponent !in 1.0..8.0) add("参数 exponent 必须是 1 到 8 的整数")
        if (maxBase <= minBase) add("参数 maxBase 必须大于 minBase")
        if (maxBase > minBase && base !in minBase..maxBase) add("参数 base 必须位于 minBase..maxBase 范围内")
    }

    if (key == "mathematics.balance.equation" && "tilt" in parameters.keys && parameters.number("tilt") !in -1.0..1.0) add("参数 tilt 必须位于 -1..1 范围内")
}
