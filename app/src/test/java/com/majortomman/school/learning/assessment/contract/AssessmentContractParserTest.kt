package com.majortomman.school.learning.assessment.contract

import com.majortomman.school.learning.assessment.domain.AnswerInputSpec
import com.majortomman.school.learning.assessment.domain.AnswerRule
import com.majortomman.school.learning.content.LearningContent
import com.majortomman.school.learning.course.CourseChapter
import com.majortomman.school.learning.course.CourseDocument
import com.majortomman.school.learning.course.CourseExplanation
import com.majortomman.school.learning.course.CourseKnowledgePoint
import com.majortomman.school.learning.course.CourseLesson
import com.majortomman.school.learning.course.CoursePdf
import com.majortomman.school.learning.course.CoursePractice
import com.majortomman.school.learning.course.CourseSection
import com.majortomman.school.learning.course.CourseSourceReference
import com.majortomman.school.learning.course.CourseTextbook
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssessmentContractParserTest {
    @Test
    fun parsesAllInitialQuestionTypesAndContentNodes() {
        val assessments = AssessmentDocumentParser.decode(validAssessmentsJson())
        val knowledge = KnowledgePointDocumentParser.decode(validKnowledgeJson())

        AssessmentPackageContract.validate(validCourse(), assessments, knowledge)

        assertEquals(5, assessments.questionSets.single().questions.size)
        assertTrue(assessments.questionSets.single().questions[0].definition.inputSpec is AnswerInputSpec.Integer)
        assertTrue(assessments.questionSets.single().questions[1].definition.inputSpec is AnswerInputSpec.Decimal)
        assertTrue(assessments.questionSets.single().questions[2].definition.inputSpec is AnswerInputSpec.Rational)
        assertTrue(assessments.questionSets.single().questions[3].definition.inputSpec is AnswerInputSpec.SingleChoice)
        assertTrue(assessments.questionSets.single().questions[4].definition.inputSpec is AnswerInputSpec.Coordinate)
        assertTrue(assessments.questionSets.single().questions[0].definition.answerRule is AnswerRule.ExactInteger)

        val firstStem = assessments.questionSets.single().questions.first().stem
        assertTrue(firstStem.any { it is LearningContent.Image })
        assertTrue(firstStem.any { it is LearningContent.Table })
        assertTrue(firstStem.any { it is LearningContent.Scene })
    }

    @Test
    fun rejectsUnknownQuestionField() {
        val root = JSONObject(validAssessmentsJson())
        root.getJSONArray("questionSets").getJSONObject(0).getJSONArray("questions").getJSONObject(0).put("legacyAnswer", 2)
        val error = runCatching { AssessmentDocumentParser.decode(root) }.exceptionOrNull()
        assertTrue(error?.message.orEmpty().contains("未知字段"))
    }

    @Test
    fun rejectsUnknownSceneDataField() {
        val root = JSONObject(validAssessmentsJson())
        val scene = root.getJSONArray("questionSets").getJSONObject(0).getJSONArray("questions").getJSONObject(0).getJSONArray("stem").getJSONObject(3)
        scene.getJSONObject("data").put("script", "alert(1)")
        val error = runCatching { AssessmentDocumentParser.decode(root) }.exceptionOrNull()
        assertTrue(error?.message.orEmpty().contains("未知字段"))
    }

    @Test
    fun rejectsMissingKnowledgePointReference() {
        val assessmentsRoot = JSONObject(validAssessmentsJson())
        assessmentsRoot.getJSONArray("questionSets").getJSONObject(0).getJSONArray("questions").getJSONObject(0).getJSONArray("knowledgeBindings").getJSONObject(0).put("knowledgePointId", "missing-point")
        val error = runCatching {
            AssessmentPackageContract.validate(validCourse(), AssessmentDocumentParser.decode(assessmentsRoot), KnowledgePointDocumentParser.decode(validKnowledgeJson()))
        }.exceptionOrNull()
        assertTrue(error?.message.orEmpty().contains("不存在的知识点"))
    }

    @Test
    fun rejectsKnowledgePointCycle() {
        val root = JSONObject(validKnowledgeJson())
        root.getJSONArray("knowledgePoints").getJSONObject(0).getJSONArray("prerequisiteIds").put("number-line")
        val error = runCatching {
            AssessmentPackageContract.validate(validCourse(), AssessmentDocumentParser.decode(validAssessmentsJson()), KnowledgePointDocumentParser.decode(root))
        }.exceptionOrNull()
        assertTrue(error?.message.orEmpty().contains("形成循环"))
    }

    @Test
    fun rejectsUnknownPlacementSection() {
        val root = JSONObject(validAssessmentsJson())
        root.getJSONArray("placements").getJSONObject(0).put("sectionId", "section-missing")
        val error = runCatching {
            AssessmentPackageContract.validate(validCourse(), AssessmentDocumentParser.decode(root), KnowledgePointDocumentParser.decode(validKnowledgeJson()))
        }.exceptionOrNull()
        assertTrue(error?.message.orEmpty().contains("不存在的小节"))
    }

    @Test
    fun rejectsUndeclaredAssetReference() {
        val root = JSONObject(validAssessmentsJson())
        root.getJSONArray("questionSets").getJSONObject(0).getJSONArray("questions").getJSONObject(0).getJSONArray("stem").getJSONObject(1).put("assetId", "missing-image")
        val error = runCatching {
            AssessmentPackageContract.validate(validCourse(), AssessmentDocumentParser.decode(root), KnowledgePointDocumentParser.decode(validKnowledgeJson()))
        }.exceptionOrNull()
        assertTrue(error?.message.orEmpty().contains("未声明资产"))
    }

    @Test
    fun rejectsMismatchedInputAndAnswerRule() {
        val root = JSONObject(validAssessmentsJson())
        val firstQuestion = root.getJSONArray("questionSets").getJSONObject(0).getJSONArray("questions").getJSONObject(0)
        firstQuestion.put("answer", JSONObject().put("type", "decimal").put("expected", 2.0).put("tolerance", 0.0))
        val error = runCatching { AssessmentDocumentParser.decode(root) }.exceptionOrNull()
        assertTrue(error?.message.orEmpty().contains("整数输入必须使用 ExactInteger"))
    }

    private fun validCourse(): CourseDocument = CourseDocument(
        textbook = CourseTextbook(
            id = "pep-math-7-1",
            title = "数学七年级上册",
            publisher = "人民教育出版社",
            edition = "人教版",
            grade = "七年级",
            semester = "上册",
            subject = "数学",
            pdf = CoursePdf("assets/textbook.pdf", pageCount = 100, pageIndexOffset = 0),
        ),
        knowledgePoints = listOf(
            CourseKnowledgePoint(id = "signed-number", name = "正数和负数", description = "理解正负数表示相反意义的量。", prerequisiteIds = emptyList()),
            CourseKnowledgePoint(id = "number-line", name = "数轴", description = "理解数轴三要素并读取点的位置。", prerequisiteIds = listOf("signed-number")),
        ),
        chapters = listOf(
            CourseChapter(
                id = "chapter-1",
                title = "有理数",
                sections = listOf(
                    CourseSection(
                        id = "section-1",
                        title = "数轴",
                        lessons = listOf(
                            CourseLesson(
                                id = "number-line-intro",
                                title = "认识数轴",
                                aliases = emptyList(),
                                goals = listOf("理解数轴的原点、正方向和单位长度"),
                                knowledgePointIds = listOf("number-line"),
                                prerequisiteLessonIds = emptyList(),
                                references = listOf(CourseSourceReference("教材第8—9页", 8, 9)),
                                steps = listOf(CourseExplanation(null, "数轴把数和直线上的位置对应起来。")),
                                practice = listOf(CoursePractice("number-line-practice", "数轴原点表示什么数？", "0", listOf("原点表示0。"), listOf("number-line"), 1)),
                                summary = listOf("数轴包含原点、正方向和单位长度。"),
                            ),
                        ),
                    ),
                ),
            ),
        ),
    )

    private fun validAssessmentsJson(): String = """
        {
          "courseId": "pep-math-7-1",
          "assets": [
            {
              "id": "number-line-source",
              "path": "assets/figures/number-line-source.webp",
              "mediaType": "image/webp",
              "width": 1200,
              "height": 600
            }
          ],
          "questionSets": [
            {
              "id": "number-line-examples",
              "title": "数轴例题",
              "allowSkip": true,
              "allowReviewBeforeFinish": true,
              "questions": [
                ${integerQuestion()},
                ${decimalQuestion()},
                ${rationalQuestion()},
                ${choiceQuestion()},
                ${coordinateQuestion()}
              ]
            }
          ],
          "placements": [
            {
              "sectionId": "section-1",
              "questionSetIds": ["number-line-examples"]
            }
          ]
        }
    """.trimIndent()

    private fun integerQuestion(): String = """
        {
          "id": "integer-1",
          "revision": 1,
          "number": "例1",
          "stem": [
            {"type": "text", "style": "prompt", "text": "点 A 表示什么数？"},
            {"type": "image", "assetId": "number-line-source", "altText": "教材中的数轴图", "caption": "图 1"},
            {
              "type": "table",
              "caption": "点与数",
              "sourceAssetId": "number-line-source",
              "columns": ["点", "数"],
              "rows": [["A", "-3"], ["B", "2"]]
            },
            {
              "type": "scene",
              "template": "number_line",
              "data": {"title": "读取数轴", "mode": "read_points", "signed": true, "initial": -3}
            }
          ],
          "input": {"type": "integer"},
          "answer": {"type": "exact_integer", "expected": -3},
          "knowledgeBindings": [{"knowledgePointId": "number-line", "weight": 1.0}],
          "difficulty": 0.3,
          "hints": [{"id": "hint-1", "text": "先找到原点。"}],
          "choices": [],
          "explanation": [{"type": "text", "style": "explanation", "text": "A 在原点左侧三个单位。"}]
        }
    """.trimIndent()

    private fun decimalQuestion(): String = baseQuestion(
        id = "decimal-1",
        number = "例2",
        input = "{\"type\":\"decimal\",\"allowFraction\":false}",
        answer = "{\"type\":\"decimal\",\"expected\":1.5,\"tolerance\":0.001}",
    )

    private fun rationalQuestion(): String = baseQuestion(
        id = "rational-1",
        number = "例3",
        input = "{\"type\":\"rational\",\"allowDecimal\":true}",
        answer = "{\"type\":\"rational_equivalent\",\"expected\":{\"numerator\":-3,\"denominator\":2}}",
    )

    private fun choiceQuestion(): String = """
        {
          "id": "choice-1",
          "revision": 1,
          "number": "例4",
          "stem": [{"type": "text", "style": "prompt", "text": "选择原点。"}],
          "input": {"type": "single_choice", "optionIds": ["a", "b"]},
          "answer": {"type": "single_choice", "expectedOptionId": "a"},
          "knowledgeBindings": [{"knowledgePointId": "number-line", "weight": 1.0}],
          "difficulty": 0.2,
          "hints": [],
          "choices": [
            {"id": "a", "content": [{"type": "text", "style": "body", "text": "0"}]},
            {"id": "b", "content": [{"type": "text", "style": "body", "text": "1"}]}
          ],
          "explanation": []
        }
    """.trimIndent()

    private fun coordinateQuestion(): String = baseQuestion(
        id = "coordinate-1",
        number = "例5",
        input = "{\"type\":\"coordinate\"}",
        answer = "{\"type\":\"coordinate\",\"expected\":{\"x\":{\"numerator\":-1,\"denominator\":1},\"y\":{\"numerator\":2,\"denominator\":1}}}",
    )

    private fun baseQuestion(id: String, number: String, input: String, answer: String): String = """
        {
          "id": "$id",
          "revision": 1,
          "number": "$number",
          "stem": [{"type": "text", "style": "prompt", "text": "完成计算。"}],
          "input": $input,
          "answer": $answer,
          "knowledgeBindings": [{"knowledgePointId": "number-line", "weight": 1.0}],
          "difficulty": 0.4,
          "hints": [],
          "choices": [],
          "explanation": []
        }
    """.trimIndent()

    private fun validKnowledgeJson(): String = """
        {
          "courseId": "pep-math-7-1",
          "knowledgePoints": [
            {
              "id": "signed-number",
              "title": "正数和负数",
              "description": "理解正负数表示相反意义的量。",
              "prerequisiteIds": [],
              "sectionIds": ["section-1"]
            },
            {
              "id": "number-line",
              "title": "数轴",
              "description": "理解数轴三要素并读取点的位置。",
              "prerequisiteIds": ["signed-number"],
              "sectionIds": ["section-1"]
            }
          ]
        }
    """.trimIndent()
}
