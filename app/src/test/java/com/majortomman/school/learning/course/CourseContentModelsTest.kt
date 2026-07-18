package com.majortomman.school.learning.course

import com.majortomman.school.learning.capability.ConceptId
import com.majortomman.school.learning.capability.ContentOrigin
import com.majortomman.school.learning.capability.ExtensionPolicy
import com.majortomman.school.learning.capability.LessonCapability
import com.majortomman.school.learning.capability.NumberDomain
import com.majortomman.school.learning.capability.OperationId
import com.majortomman.school.learning.capability.WidgetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseContentModelsTest {
    @Test
    fun textbookSummaryMustStayInsideLessonPages() {
        val note = CourseNote(
            origin = ContentOrigin.TEXTBOOK_SUMMARY,
            title = "教材内容依据",
            body = "根据本课页面整理的定义与例题顺序。",
            sourcePageStart = 20,
            sourcePageEnd = 24,
        )

        val errors = note.validationErrors(21..23)

        assertTrue(errors.any { it.contains("超出本课") })
    }

    @Test
    fun optionalExtensionMustBeExplicitlyLabelled() {
        val enrichment = LessonEnrichment(
            extensions = listOf(
                CourseNote(
                    origin = ContentOrigin.OPTIONAL_EXTENSION,
                    title = "历史背景",
                    body = "帮助理解概念来源，但不作为本课必会内容。",
                ),
            ),
        )

        val errors = enrichment.validationErrors(10..12)

        assertTrue(errors.any { it.contains("明确包含“扩展”") })
    }

    @Test
    fun parameterRejectsOutOfRangeDefault() {
        val parameter = CourseParameterSpec(
            id = "voltage",
            label = "电源电压",
            kind = CourseParameterKind.NUMBER,
            defaultValue = "15",
            unit = "V",
            minimum = 0.0,
            maximum = 12.0,
            step = 0.5,
        )

        assertTrue(parameter.validationErrors().any { it.contains("大于最大值") })
    }

    @Test
    fun visualizationCannotUseConceptBeforeChapterAllowsIt() {
        val capability = LessonCapability(
            allowedConcepts = setOf(ConceptId.VARIABLE),
            enabledOperations = setOf(OperationId.SUBSTITUTE),
            enabledWidgets = setOf(WidgetType.RELATION_CALCULATOR),
            numberDomain = NumberDomain.REAL,
            extensionPolicy = ExtensionPolicy.NONE,
        )
        val visualization = CourseVisualizationSpec(
            kind = CourseVisualizationKind.CARTESIAN_GRAPH,
            title = "函数图像",
            description = "调节参数并观察曲线。",
            requiredConcepts = setOf(ConceptId.FUNCTION_GRAPH),
            requiredOperations = setOf(OperationId.PLOT_2D),
            requiredWidgets = setOf(WidgetType.COORDINATE_GRAPH_2D),
        )

        val errors = visualization.validationErrors(capability)

        assertTrue(errors.any { it.contains("未允许的概念") })
        assertTrue(errors.any { it.contains("未允许的操作") })
        assertTrue(errors.any { it.contains("未允许的组件") })
    }

    @Test
    fun enrichmentJsonRoundTripKeepsParametersAndVerification() {
        val original = LessonEnrichment(
            background = listOf(
                CourseNote(
                    origin = ContentOrigin.SCHOOL_EXPLANATION,
                    title = "背景知识",
                    body = "说明本课概念和先前知识的联系。",
                ),
            ),
            extensions = listOf(
                CourseNote(
                    origin = ContentOrigin.OPTIONAL_EXTENSION,
                    title = "扩展：进一步观察",
                    body = "只在完成教材核心内容后阅读。",
                ),
            ),
            visualization = CourseVisualizationSpec(
                kind = CourseVisualizationKind.CARTESIAN_GRAPH,
                title = "参数变化",
                description = "改变参数并观察图像。",
                parameters = listOf(
                    CourseParameterSpec(
                        id = "k",
                        label = "比例系数",
                        kind = CourseParameterKind.NUMBER,
                        defaultValue = "1",
                        minimum = -5.0,
                        maximum = 5.0,
                        step = 0.5,
                    ),
                ),
                requiredConcepts = setOf(ConceptId.FUNCTION_GRAPH),
                requiredOperations = setOf(OperationId.PLOT_2D),
                requiredWidgets = setOf(WidgetType.COORDINATE_GRAPH_2D),
            ),
            verification = CourseVerificationSpec(
                kind = CourseVerificationKind.MATH_EXPRESSION,
                title = "公式验证",
                prompt = "输入等式并给出变量值。",
                inputHint = "例如 y=2x+1",
                examples = listOf("y=2x+1"),
                requiredOperations = setOf(OperationId.VERIFY_EQUALITY),
            ),
        )

        val restored = LessonEnrichment.fromJson(original.toJson())

        assertEquals(original, restored)
        assertTrue(restored.validationErrors(1..5).isEmpty())
    }
}
