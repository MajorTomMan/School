package com.majortomman.school.learning.science

import com.majortomman.school.learning.course.PhysicsCourseCategory
import com.majortomman.school.learning.science.physics.PhysicsRelationId
import com.majortomman.school.learning.science.physics.PhysicsRelationVerifier
import com.majortomman.school.learning.science.physics.PhysicsVariableValue
import com.majortomman.school.learning.science.physics.PhysicsVerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhysicsCourseAndVerifierTest {
    @Test
    fun verifiesOhmsLawInsideElectricityPermission() {
        val result = PhysicsRelationVerifier.verify(
            category = PhysicsCourseCategory.ELECTRICITY,
            relation = PhysicsRelationId.OHM,
            values = mapOf(
                "I" to PhysicsVariableValue("I", 2.0, "A"),
                "R" to PhysicsVariableValue("R", 3.0, "Ω"),
            ),
            submittedTarget = PhysicsVariableValue("U", 6.0, "V"),
            conditionsAccepted = true,
        )

        assertEquals(PhysicsVerificationStatus.CORRECT, result.status)
        assertEquals(6.0, result.expected ?: 0.0, 1e-9)
    }

    @Test
    fun rejectsRelationOutsidePackagePermission() {
        val result = PhysicsRelationVerifier.verify(
            category = PhysicsCourseCategory.KINEMATICS,
            relation = PhysicsRelationId.OHM,
            values = emptyMap(),
            submittedTarget = null,
            conditionsAccepted = true,
        )

        assertEquals(PhysicsVerificationStatus.NOT_ALLOWED, result.status)
    }

    @Test
    fun requiresModelConditionsBeforeCalculation() {
        val result = PhysicsRelationVerifier.verify(
            category = PhysicsCourseCategory.ENERGY,
            relation = PhysicsRelationId.KINETIC_ENERGY,
            values = mapOf(
                "m" to PhysicsVariableValue("m", 2.0, "kg"),
                "v" to PhysicsVariableValue("v", 3.0, "m/s"),
            ),
            submittedTarget = PhysicsVariableValue("Ek", 9.0, "J"),
            conditionsAccepted = false,
        )

        assertEquals(PhysicsVerificationStatus.INVALID_MODEL, result.status)
        assertTrue(result.message.contains("模型条件"))
    }

    @Test
    fun invalidDenominatorReturnsModelError() {
        val result = PhysicsRelationVerifier.verify(
            category = PhysicsCourseCategory.KINEMATICS,
            relation = PhysicsRelationId.SPEED,
            values = mapOf(
                "s" to PhysicsVariableValue("s", 10.0, "m"),
                "t" to PhysicsVariableValue("t", 0.0, "s"),
            ),
            submittedTarget = PhysicsVariableValue("v", 0.0, "m/s"),
            conditionsAccepted = true,
        )

        assertEquals(PhysicsVerificationStatus.INVALID_MODEL, result.status)
    }
}
