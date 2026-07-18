package com.majortomman.school.learning.science

import com.majortomman.school.learning.course.ChemistryCourseCategory
import com.majortomman.school.learning.course.ChemistryCourseContentFactory
import com.majortomman.school.learning.science.chemistry.ChemistryVerificationMode
import com.majortomman.school.learning.science.chemistry.ChemistryVerificationStatus
import com.majortomman.school.learning.science.chemistry.ChemistryVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChemistryCourseAndVerifierTest {
    @Test
    fun classifiesCommonChemistryTextbookThemes() {
        assertEquals(ChemistryCourseCategory.FORMULA, ChemistryCourseContentFactory.classify("化学式的书写"))
        assertEquals(ChemistryCourseCategory.ION_REDOX, ChemistryCourseContentFactory.classify("化合价与氧化还原"))
        assertEquals(ChemistryCourseCategory.EQUATION, ChemistryCourseContentFactory.classify("如何正确书写化学方程式"))
        assertEquals(ChemistryCourseCategory.STOICHIOMETRY, ChemistryCourseContentFactory.classify("物质的量在化学反应中的应用"))
        assertEquals(ChemistryCourseCategory.ORGANIC, ChemistryCourseContentFactory.classify("乙醇与乙酸"))
    }

    @Test
    fun parsesNestedFormulaAndCountsAtoms() {
        val result = ChemistryVerifier.verify(
            ChemistryVerificationMode.FORMULA,
            "Al2(SO4)3",
        )

        assertEquals(ChemistryVerificationStatus.VALID, result.status)
        assertTrue(result.rows.contains("Al" to "2"))
        assertTrue(result.rows.contains("S" to "3"))
        assertTrue(result.rows.contains("O" to "12"))
    }

    @Test
    fun balancesCompleteChemicalEquation() {
        val result = ChemistryVerifier.verify(
            ChemistryVerificationMode.EQUATION_BALANCE,
            "Fe+O2->Fe2O3",
        )

        assertEquals(ChemistryVerificationStatus.BALANCED_RESULT, result.status)
        assertEquals("4Fe + 3O2 → 2Fe2O3", result.normalized)
    }

    @Test
    fun reportsUnbalancedEquationWithoutChangingIt() {
        val result = ChemistryVerifier.verify(
            ChemistryVerificationMode.EQUATION_CHECK,
            "H2+O2->H2O",
        )

        assertEquals(ChemistryVerificationStatus.UNBALANCED, result.status)
        assertTrue(result.rows.any { it.first == "O" && it.second == "2 → 1" })
    }

    @Test
    fun refusesToPredictUnknownProducts() {
        val result = ChemistryVerifier.verify(
            ChemistryVerificationMode.EQUATION_BALANCE,
            "H2+O2",
        )

        assertEquals(ChemistryVerificationStatus.INVALID, result.status)
        assertTrue(result.message.contains("不会只根据反应物猜测"))
    }

    @Test
    fun analyzesOrganicFunctionalGroup() {
        val result = ChemistryVerifier.verify(
            ChemistryVerificationMode.ORGANIC_STRUCTURE,
            "CCO",
        )

        assertEquals(ChemistryVerificationStatus.VALID, result.status)
        assertTrue(result.rows.contains("分子式" to "C2H6O"))
        assertTrue(result.rows.any { it.first == "官能团" && it.second.contains("羟基") })
    }

    @Test
    fun comparesConstitutionalIsomers() {
        val result = ChemistryVerifier.verify(
            ChemistryVerificationMode.ORGANIC_ISOMER,
            "CCO",
            "COC",
        )

        assertEquals(ChemistryVerificationStatus.VALID, result.status)
        assertTrue(result.rows.contains("分子式相同" to "是"))
        assertTrue(result.rows.contains("构造异构体" to "是"))
    }
}
