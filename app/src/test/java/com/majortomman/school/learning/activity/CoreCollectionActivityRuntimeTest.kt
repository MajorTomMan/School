package com.majortomman.school.learning.activity

import com.majortomman.school.learning.assessment.domain.InlineAssessmentEvaluator
import com.majortomman.school.learning.assessment.domain.InlineAssessmentOutcome
import com.majortomman.school.learning.assessment.domain.InlineAssessmentRule
import com.majortomman.school.learning.assessment.domain.InlineAssessmentSpec
import com.majortomman.school.learning.assessment.domain.Difficulty
import com.majortomman.school.learning.content.LearningContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoreCollectionActivityRuntimeTest {
    @Test
    fun selectOneReturnsSelectedOptionId() {
        val spec = SelectOneActivitySpec(
            id = ActivityId("select-1"),
            options = listOf(
                ActivityOption("a", "甲"),
                ActivityOption("b", "乙"),
            ),
        )
        var state = SelectOneActivityHandler.initialState(spec)
        state = SelectOneActivityHandler.reduce(state, SelectOption("b")).state

        val result = SelectOneActivityHandler.reduce(state, SubmitActivity).result as CanonicalTextActivityResult

        assertEquals("b", result.value)
    }

    @Test
    fun orderReturnsCanonicalItemOrder() {
        val spec = OrderActivitySpec(
            id = ActivityId("order-1"),
            items = listOf(
                ActivityOption("a", "甲"),
                ActivityOption("b", "乙"),
                ActivityOption("c", "丙"),
            ),
        )
        var state = OrderActivityHandler.initialState(spec)
        state = OrderActivityHandler.reduce(state, MoveOrderItem("c", MoveDirection.UP)).state

        val result = OrderActivityHandler.reduce(state, SubmitActivity).result as CanonicalTextActivityResult

        assertEquals("a|c|b", result.value)
    }

    @Test
    fun matchRequiresCompleteOneToOneMappingAndUsesLeftOrder() {
        val spec = MatchActivitySpec(
            id = ActivityId("match-1"),
            leftItems = listOf(
                ActivityOption("l1", "一"),
                ActivityOption("l2", "二"),
            ),
            rightItems = listOf(
                ActivityOption("r1", "A"),
                ActivityOption("r2", "B"),
            ),
        )
        var state = MatchActivityHandler.initialState(spec)
        state = MatchActivityHandler.reduce(state, SelectMatchLeft("l1")).state
        state = MatchActivityHandler.reduce(state, SelectMatchRight("r2")).state
        assertNull(MatchActivityHandler.reduce(state, SubmitActivity).result)

        state = MatchActivityHandler.reduce(state, SelectMatchLeft("l2")).state
        state = MatchActivityHandler.reduce(state, SelectMatchRight("r1")).state
        val result = MatchActivityHandler.reduce(state, SubmitActivity).result as CanonicalTextActivityResult

        assertEquals("l1=r2|l2=r1", result.value)
    }

    @Test
    fun canonicalTextResultWorksWithExistingInlineAssessment() {
        val result = CanonicalTextActivityResult(ActivityId("order-2"), "a|c|b")
        val assessment = InlineAssessmentSpec(
            rule = InlineAssessmentRule.ExactText("a|c|b"),
            explanation = listOf(LearningContent.Text("顺序正确")),
            knowledgePointIds = listOf("ordering"),
            difficulty = Difficulty(0.5),
        )

        assertEquals(
            InlineAssessmentOutcome.CORRECT,
            InlineAssessmentEvaluator.evaluate(assessment, result).outcome,
        )
    }
}
