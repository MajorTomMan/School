package com.majortomman.school.learning

import com.majortomman.school.learning.capability.ConceptId
import com.majortomman.school.learning.capability.ExtensionPolicy
import com.majortomman.school.learning.capability.LessonCapability
import com.majortomman.school.learning.capability.NumberDomain
import com.majortomman.school.learning.capability.OperationId
import com.majortomman.school.learning.capability.WidgetType
import com.majortomman.school.learning.relation.RelationDefinition
import com.majortomman.school.learning.relation.RelationSolveResult
import com.majortomman.school.learning.relation.SolveRule
import com.majortomman.school.learning.relation.VariableDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningEngineFoundationTest {
    @Test
    fun capabilityBlocksConceptsOutsideCurrentLesson() {
        val capability = LessonCapability(
            allowedConcepts = setOf(ConceptId.FUNCTION, ConceptId.CORRESPONDING_VALUE),
            enabledOperations = setOf(OperationId.SUBSTITUTE),
            enabledWidgets = setOf(WidgetType.COORDINATE_GRAPH_2D),
            numberDomain = NumberDomain.REAL,
            extensionPolicy = ExtensionPolicy.NECESSARY_ONLY,
        )

        val validation = capability.validate(
            requestedConcepts = setOf(ConceptId.FUNCTION, ConceptId.COMPLEX_PLANE),
        )

        assertFalse(validation.allowed)
        assertTrue(ConceptId.COMPLEX_PLANE in validation.blockedConcepts)
    }

    @Test
    fun relationShowsEveryKnownInputExceptTarget() {
        val relation = xyzRelation()

        assertEquals(listOf("x", "y"), relation.requiredInputs("z").map { it.id })
        assertEquals(listOf("y", "z"), relation.requiredInputs("x").map { it.id })
    }

    @Test
    fun relationSolvesThreeVariableTargets() {
        val relation = xyzRelation()
        val result = relation.solve("x", mapOf("y" to 4.0, "z" to 9.0))

        assertTrue(result is RelationSolveResult.Success)
        assertEquals(5.0, (result as RelationSolveResult.Success).value, 1e-9)
    }

    private fun xyzRelation(): RelationDefinition = RelationDefinition(
        id = "xyz_sum",
        displayFormula = "z = x + y",
        variables = listOf(
            VariableDefinition("x", "x"),
            VariableDefinition("y", "y"),
            VariableDefinition("z", "z"),
        ),
        solveRules = mapOf(
            "z" to SolveRule("z", listOf("x", "y")) { values -> values.getValue("x") + values.getValue("y") },
            "x" to SolveRule("x", listOf("y", "z")) { values -> values.getValue("z") - values.getValue("y") },
            "y" to SolveRule("y", listOf("x", "z")) { values -> values.getValue("z") - values.getValue("x") },
        ),
    )
}
