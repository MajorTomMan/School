package com.majortomman.school.ui.visualization

import com.majortomman.school.ui.visualization.core.VisualizationArguments
import com.majortomman.school.ui.visualization.core.VisualizationFieldSpec
import com.majortomman.school.ui.visualization.core.VisualizationRegistry
import com.majortomman.school.ui.visualization.core.VisualizationSchema
import com.majortomman.school.ui.visualization.core.VisualizationSubject
import com.majortomman.school.ui.visualization.core.VisualizationValueType
import com.majortomman.school.ui.visualization.core.formatSignedChartValue
import com.majortomman.school.ui.visualization.subjects.math.AccountTrendVisualizationRenderer
import com.majortomman.school.ui.visualization.subjects.math.GrowthRateTrendVisualizationRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class VisualizationFrameworkTest {
    @Test
    fun schemaRejectsMissingAndWrongTypedArguments() {
        val schema = VisualizationSchema(
            fields = listOf(
                VisualizationFieldSpec("value", VisualizationValueType.NUMBER),
                VisualizationFieldSpec("unit", VisualizationValueType.STRING),
            ),
        )

        val missing = schema.validate(VisualizationArguments.of("value" to 2f))
        assertTrue(missing.any { "unit" in it })

        val wrongType = schema.validate(VisualizationArguments.of("value" to "2", "unit" to "%"))
        assertTrue(wrongType.any { "value" in it })
    }

    @Test
    fun registryRejectsDuplicateStableKeys() {
        try {
            VisualizationRegistry(
                listOf(
                    AccountTrendVisualizationRenderer,
                    AccountTrendVisualizationRenderer,
                ),
            )
            fail("重复 key 应被拒绝")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    @Test
    fun subjectModulesContainOnlyTheirOwnRenderers() {
        MathematicsVisualizationModule.validatedRenderers().forEach {
            assertEquals(VisualizationSubject.MATHEMATICS, it.subject)
        }
        assertEquals(VisualizationSubject.PHYSICS, PhysicsVisualizationModule.subject)
        assertEquals(VisualizationSubject.CHEMISTRY, ChemistryVisualizationModule.subject)
        assertEquals(VisualizationSubject.BIOLOGY, BiologyVisualizationModule.subject)
    }

    @Test
    fun schoolCatalogRegistersBothTrendVisualizations() {
        val keys = SchoolVisualizationCatalog.registeredKeys()
        assertTrue(AccountTrendVisualizationRenderer.key in keys)
        assertTrue(GrowthRateTrendVisualizationRenderer.key in keys)
    }

    @Test
    fun signedChartFormattingKeepsUnitsAndDecimals() {
        assertEquals("+2.4%", formatSignedChartValue(2.4f, "%", 1))
        assertEquals("-0.7%", formatSignedChartValue(-0.7f, "%", 1))
        assertEquals("0万元", formatSignedChartValue(0f, "万元", 0))
    }
}
