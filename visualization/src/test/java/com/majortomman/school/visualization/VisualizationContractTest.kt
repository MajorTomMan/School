package com.majortomman.school.visualization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class VisualizationContractTest {
    @Test
    fun catalogRegistersOneSharedNumberLineFamily() {
        val keys = SchoolVisualizationCatalog.registeredKeys().map(VisualizationKey::value).toSet()
        assertTrue("mathematics.number-line.basic" in keys)
        assertTrue("mathematics.number-line.construction" in keys)
        assertTrue("mathematics.number-line.points" in keys)
        assertTrue("mathematics.number-line.opposite" in keys)
        assertTrue("mathematics.number-line.absolute-value" in keys)
        assertTrue("mathematics.number-line.comparison" in keys)
        assertTrue("mathematics.number-line.movement" in keys)
        assertTrue("mathematics.number-line.root" in keys)
    }

    @Test
    fun checkedInStructuralSnapshotMatchesRuntimeSchemas() {
        val expected = requireNotNull(javaClass.classLoader?.getResourceAsStream("visualization-contract.snapshot")) { "missing visualization-contract.snapshot" }.bufferedReader().use { it.readText() }
        assertEquals(expected, runtimeContractSnapshot())
    }

    @Test
    fun schemaRejectsUnknownParametersAndTexts() {
        val invocation = VisualizationInvocation(
            renderer = VisualizationKey("mathematics.number-line.basic"),
            parameters = VisualizationParameters.of(
                "value" to VisualizationParameterValue.NumberValue(2.0),
                "remoteUrl" to VisualizationParameterValue.NumberValue(1.0),
            ),
            texts = VisualizationTexts.of(
                "title" to "位置",
                "unexpected" to "不能偷偷传额外文本",
            ),
        )

        val issues = SchoolVisualizationCatalog.validate(invocation)
        assertTrue(issues.any { "remoteUrl" in it })
        assertTrue(issues.any { "unexpected" in it })
    }

    @Test
    fun requiredRendererFieldsAreValidatedBeforeRendering() {
        val invocation = VisualizationInvocation(
            renderer = VisualizationKey("mathematics.number-line.root"),
            parameters = VisualizationParameters.Empty,
            texts = VisualizationTexts.Empty,
        )
        val issues = SchoolVisualizationCatalog.validate(invocation)
        assertTrue(issues.any { "value" in it })
        assertTrue(issues.any { "pointLabel" in it })
    }

    @Test
    fun semanticValidationRejectsNumberLineThatWouldRenderIncorrectTicks() {
        val invocation = VisualizationInvocation(
            renderer = VisualizationKey("mathematics.number-line.basic"),
            parameters = VisualizationParameters.of(
                "min" to VisualizationParameterValue.NumberValue(-10.0),
                "max" to VisualizationParameterValue.NumberValue(10.0),
                "step" to VisualizationParameterValue.NumberValue(0.1),
            ),
        )
        val issues = SchoolVisualizationCatalog.validate(invocation)
        assertTrue(issues.any { "80" in it })
    }

    @Test
    fun semanticValidationRejectsNumberLineBelowRendererPrecision() {
        val tinyStep = VisualizationInvocation(
            renderer = VisualizationKey("mathematics.number-line.basic"),
            parameters = VisualizationParameters.of(
                "min" to VisualizationParameterValue.NumberValue(0.0),
                "max" to VisualizationParameterValue.NumberValue(0.05),
                "step" to VisualizationParameterValue.NumberValue(0.005),
            ),
        )
        assertTrue(SchoolVisualizationCatalog.validate(tinyStep).any { "0.01" in it })

        val collapsedRange = VisualizationInvocation(
            renderer = VisualizationKey("mathematics.number-line.basic"),
            parameters = VisualizationParameters.of(
                "min" to VisualizationParameterValue.NumberValue(100_000_000.0),
                "max" to VisualizationParameterValue.NumberValue(100_000_001.0),
                "step" to VisualizationParameterValue.NumberValue(1.0),
            ),
        )
        assertTrue(SchoolVisualizationCatalog.validate(collapsedRange).any { "Float" in it })
    }

    @Test
    fun semanticValidationRejectsOutOfRangeNumberLinePoint() {
        val invocation = VisualizationInvocation(
            renderer = VisualizationKey("mathematics.number-line.points"),
            parameters = VisualizationParameters.of(
                "min" to VisualizationParameterValue.NumberValue(-5.0),
                "max" to VisualizationParameterValue.NumberValue(5.0),
                "values" to VisualizationParameterValue.NumberListValue(listOf(-3.0, 9.0)),
            ),
        )
        val issues = SchoolVisualizationCatalog.validate(invocation)
        assertTrue(issues.any { "数轴范围" in it })
    }

    @Test
    fun semanticValidationRejectsOversizedCartesianGrid() {
        val invocation = VisualizationInvocation(
            renderer = VisualizationKey("mathematics.cartesian.linear"),
            parameters = VisualizationParameters.of(
                "slope" to VisualizationParameterValue.NumberValue(1.0),
                "intercept" to VisualizationParameterValue.NumberValue(0.0),
                "xMin" to VisualizationParameterValue.NumberValue(-100.0),
                "xMax" to VisualizationParameterValue.NumberValue(100.0),
            ),
        )
        val issues = SchoolVisualizationCatalog.validate(invocation)
        assertTrue(issues.any { "横轴范围" in it })
    }

    @Test
    fun semanticValidationRejectsEmptyOrFloatOverflowChart() {
        val empty = VisualizationInvocation(
            renderer = VisualizationKey("mathematics.chart.line"),
            parameters = VisualizationParameters.of("values" to VisualizationParameterValue.NumberListValue(emptyList())),
        )
        assertTrue(SchoolVisualizationCatalog.validate(empty).any { "不能为空" in it })

        val overflow = VisualizationInvocation(
            renderer = VisualizationKey("mathematics.chart.line"),
            parameters = VisualizationParameters.of("values" to VisualizationParameterValue.NumberListValue(listOf(-3.0e38, 3.0e38))),
        )
        assertTrue(SchoolVisualizationCatalog.validate(overflow).any { "Float" in it })
    }

    @Test
    fun semanticValidationRejectsInvalidPowerSliderRange() {
        val invocation = VisualizationInvocation(
            renderer = VisualizationKey("mathematics.process.power"),
            parameters = VisualizationParameters.of(
                "base" to VisualizationParameterValue.NumberValue(2.0),
                "exponent" to VisualizationParameterValue.NumberValue(2.5),
                "minBase" to VisualizationParameterValue.NumberValue(4.0),
                "maxBase" to VisualizationParameterValue.NumberValue(-4.0),
            ),
        )
        val issues = SchoolVisualizationCatalog.validate(invocation)
        assertTrue(issues.any { "exponent" in it })
        assertTrue(issues.any { "maxBase" in it })
    }

    @Test
    fun parameterContractContainsNoGenericObjectOrStringParameterType() {
        assertEquals(setOf(VisualizationParameterType.NUMBER, VisualizationParameterType.BOOLEAN, VisualizationParameterType.NUMBER_LIST), VisualizationParameterType.entries.toSet())
    }

    @Test
    fun nonFiniteOrFloatOverflowNumbersAreRejectedAtBoundary() {
        assertThrows(IllegalArgumentException::class.java) { VisualizationParameterValue.NumberValue(Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) { VisualizationParameterValue.NumberValue(Double.MAX_VALUE) }
        assertThrows(IllegalArgumentException::class.java) { VisualizationParameterValue.NumberListValue(listOf(1.0, Double.POSITIVE_INFINITY)) }
        assertThrows(IllegalArgumentException::class.java) { VisualizationParameterValue.NumberListValue(listOf(Double.MAX_VALUE)) }
    }

    @Test
    fun numberListsAreDefensivelyCopiedAtBoundary() {
        val source = mutableListOf(1.0, 2.0)
        val value = VisualizationParameterValue.NumberListValue(source)
        source[0] = Double.NaN
        source += 3.0
        assertEquals(listOf(1.0, 2.0), value.values)
    }

    @Test
    fun semanticallyEqualInvocationsHaveValueEquality() {
        fun invocation() = VisualizationInvocation(
            renderer = VisualizationKey("mathematics.number-line.points"),
            parameters = VisualizationParameters.of(
                "min" to VisualizationParameterValue.NumberValue(-5.0),
                "max" to VisualizationParameterValue.NumberValue(5.0),
                "values" to VisualizationParameterValue.NumberListValue(listOf(-2.0, 3.0)),
            ),
            texts = VisualizationTexts.of("title" to "数轴"),
        )
        assertEquals(invocation(), invocation())
        assertEquals(invocation().hashCode(), invocation().hashCode())
    }

    @Test
    fun unknownRendererIsRejected() {
        val issues = SchoolVisualizationCatalog.validate(VisualizationInvocation(VisualizationKey("mathematics.missing")))
        assertFalse(issues.isEmpty())
    }

    private fun runtimeContractSnapshot(): String = buildString {
        SchoolVisualizationCatalog.contractSchemas().entries.sortedBy { it.key.value }.forEach { (key, schema) ->
            val parameters = schema.parameters.sortedBy { it.name }.joinToString(",") { spec -> "${spec.name}:${spec.type.name}:${if (spec.required) "required" else "optional"}" }
            val texts = schema.texts.sortedBy { it.name }.joinToString(",") { spec -> "${spec.name}:${if (spec.required) "required" else "optional"}:${spec.allowBlank}" }
            append(key.value).append('|').append(parameters).append('|').append(texts).append('\n')
        }
    }
}
