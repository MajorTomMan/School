package com.majortomman.school.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StrictAiGradingProtocolTest {
    @Test
    fun parsesCompletedCorrectResult() {
        val result = StrictAiGradingProtocol.parse(
            """{"completed":true,"answer_correct":true,"feedback":"答案正确。","explanation":"两个数关于原点对称。","mistake_type":"无"}""",
        )

        assertTrue(result.completed)
        assertTrue(result.answerCorrect)
        assertEquals("答案正确。", result.feedback)
        assertEquals(null, result.mistakeType)
    }

    @Test
    fun preservesCompletedIncorrectResult() {
        val result = StrictAiGradingProtocol.parse(
            """{"completed":true,"answer_correct":false,"feedback":"符号写反了。","explanation":"相反数位于原点两侧且距离相等。","mistake_type":"概念错误"}""",
        )

        assertTrue(result.completed)
        assertFalse(result.answerCorrect)
        assertEquals("概念错误", result.mistakeType)
    }

    @Test
    fun preservesIncompleteFlagForCallerToReject() {
        val result = StrictAiGradingProtocol.parse(
            """{"completed":false,"answer_correct":true,"feedback":"尚未完成。","explanation":"仍需核对标准答案。","mistake_type":"无"}""",
        )

        assertFalse(result.completed)
    }

    @Test
    fun rejectsMissingCompletionFlag() {
        assertFailsWith<IllegalArgumentException> {
            StrictAiGradingProtocol.parse(
                """{"answer_correct":true,"feedback":"正确。","explanation":"解释。"}""",
            )
        }
    }

    @Test
    fun rejectsMissingCorrectnessFlag() {
        assertFailsWith<IllegalArgumentException> {
            StrictAiGradingProtocol.parse(
                """{"completed":true,"feedback":"正确。","explanation":"解释。"}""",
            )
        }
    }

    @Test
    fun rejectsStringBoolean() {
        assertFailsWith<IllegalArgumentException> {
            StrictAiGradingProtocol.parse(
                """{"completed":"true","answer_correct":true,"feedback":"正确。","explanation":"解释。"}""",
            )
        }
    }

    @Test
    fun rejectsMarkdownWrappedJson() {
        assertFailsWith<IllegalArgumentException> {
            StrictAiGradingProtocol.parse(
                """```json
                {"completed":true,"answer_correct":true,"feedback":"正确。","explanation":"解释。"}
                ```""".trimIndent(),
            )
        }
    }

    @Test
    fun rejectsEmptyExplanation() {
        assertFailsWith<IllegalArgumentException> {
            StrictAiGradingProtocol.parse(
                """{"completed":true,"answer_correct":true,"feedback":"正确。","explanation":""}""",
            )
        }
    }

    @Test
    fun rejectsUnknownFields() {
        assertFailsWith<IllegalArgumentException> {
            StrictAiGradingProtocol.parse(
                """{"completed":true,"answer_correct":true,"feedback":"正确。","explanation":"解释。","pass":true}""",
            )
        }
    }
}
