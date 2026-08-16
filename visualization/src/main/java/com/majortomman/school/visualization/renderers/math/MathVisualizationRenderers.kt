package com.majortomman.school.visualization.renderers.math

import com.majortomman.school.visualization.VisualizationKey
import com.majortomman.school.visualization.VisualizationRenderer

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
    )
}
