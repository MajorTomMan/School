package com.majortomman.school.learning.assessment.contract

import com.majortomman.school.learning.assessment.domain.AnswerInputSpec
import com.majortomman.school.learning.assessment.domain.AnswerRule
import com.majortomman.school.learning.assessment.domain.Difficulty
import com.majortomman.school.learning.assessment.domain.KnowledgeBinding
import com.majortomman.school.learning.assessment.domain.KnowledgePointId
import com.majortomman.school.learning.assessment.domain.QuestionDefinition
import com.majortomman.school.learning.assessment.domain.QuestionHint
import com.majortomman.school.learning.assessment.domain.QuestionId
import com.majortomman.school.learning.assessment.domain.QuestionKey
import com.majortomman.school.learning.assessment.domain.QuestionSetId
import com.majortomman.school.learning.assessment.domain.RationalValue
import com.majortomman.school.learning.content.ContentAssetId
import java.math.BigInteger
import org.json.JSONObject

object AssessmentDocumentParser {
    fun decode(raw: String): AssessmentDocument = decode(JSONObject(raw))

    fun decode(root: JSONObject): AssessmentDocument {
        val location = "assessments"
        root.requireContractShape(
            location,
            required = setOf("courseId", "assets", "questionSets", "placements"),
        )
        return AssessmentDocument(
            courseId = requireCourseIdentifier(root.requireContractText("courseId", location), "$location.courseId"),
            assets = root.requireContractObjects("assets", location).mapIndexed(::decodeAsset),
            questionSets = root.requireContractObjects("questionSets", location).mapIndexed(::decodeQuestionSet),
            placements = root.requireContractObjects("placements", location).mapIndexed(::decodePlacement),
        )
    }

    private fun decodeAsset(index: Int, json: JSONObject): ContentAssetDefinition {
        val location = "assessments.assets[$index]"
        json.requireContractShape(
            location,
            required = setOf("id", "path", "mediaType", "width", "height"),
        )
        val mediaTypeWire = json.requireContractText("mediaType", location)
        return ContentAssetDefinition(
            id = ContentAssetId(json.requireContractText("id", location)),
            path = requireSafeAssetPath(json.requireContractText("path", location)),
            mediaType = ContentAssetMediaType.fromWireValue(mediaTypeWire)
                ?: error("$location.mediaType 不受支持：$mediaTypeWire"),
            width = json.requireContractPositiveInt("width", location),
            height = json.requireContractPositiveInt("height", location),
        )
    }

    private fun decodeQuestionSet(index: Int, json: JSONObject): CourseAssessmentQuestionSet {
        val location = "assessments.questionSets[$index]"
        json.requireContractShape(
            location,
            required = setOf(
                "id",
                "title",
                "allowSkip",
                "allowReviewBeforeFinish",
                "questions",
            ),
        )
        return CourseAssessmentQuestionSet(
            id = QuestionSetId(requireSectionIdentifier(json.requireContractText("id", location), "$location.id")),
            title = json.requireContractText("title", location),
            allowSkip = json.requireContractBoolean("allowSkip", location),
            allowReviewBeforeFinish = json.requireContractBoolean("allowReviewBeforeFinish", location),
            questions = json.requireContractObjects("questions", location).mapIndexed { questionIndex, question ->
                decodeQuestion(question, "$location.questions[$questionIndex]")
            },
        )
    }

    private fun decodeQuestion(json: JSONObject, location: String): CourseAssessmentQuestion {
        json.requireContractShape(
            location,
            required = setOf(
                "id",
                "revision",
                "number",
                "stem",
                "input",
                "answer",
                "knowledgeBindings",
                "difficulty",
                "hints",
                "choices",
                "explanation",
            ),
        )
        val key = QuestionKey(
            id = QuestionId(requireSectionIdentifier(json.requireContractText("id", location), "$location.id")),
            revision = json.requireContractPositiveInt("revision", location),
        )
        val input = decodeInput(json.requireContractObject("input", location), "$location.input")
        val answer = decodeAnswer(json.requireContractObject("answer", location), "$location.answer")
        val bindings = json.requireContractObjects("knowledgeBindings", location).mapIndexed { index, binding ->
            decodeKnowledgeBinding(binding, "$location.knowledgeBindings[$index]")
        }
        val hints = json.requireContractObjects("hints", location).mapIndexed { index, hint ->
            decodeHint(hint, "$location.hints[$index]")
        }
        val choices = json.requireContractObjects("choices", location).mapIndexed { index, choice ->
            decodeChoice(choice, "$location.choices[$index]")
        }
        return CourseAssessmentQuestion(
            definition = QuestionDefinition(
                key = key,
                number = json.requireContractText("number", location),
                inputSpec = input,
                answerRule = answer,
                knowledgeBindings = bindings,
                difficulty = Difficulty(json.requireContractDouble("difficulty", location)),
                hints = hints,
            ),
            stem = LearningContentParser.decodeArray(
                json.requireContractArray("stem", location),
                "$location.stem",
                allowEmpty = false,
            ),
            choices = choices,
            explanation = LearningContentParser.decodeArray(
                json.requireContractArray("explanation", location),
                "$location.explanation",
                allowEmpty = true,
            ),
        )
    }

    private fun decodeInput(json: JSONObject, location: String): AnswerInputSpec =
        when (val type = json.requireContractText("type", location)) {
            "integer" -> {
                json.requireContractShape(location, required = setOf("type"))
                AnswerInputSpec.Integer
            }
            "decimal" -> {
                json.requireContractShape(location, required = setOf("type", "allowFraction"))
                AnswerInputSpec.Decimal(json.requireContractBoolean("allowFraction", location))
            }
            "rational" -> {
                json.requireContractShape(location, required = setOf("type", "allowDecimal"))
                AnswerInputSpec.Rational(json.requireContractBoolean("allowDecimal", location))
            }
            "single_choice" -> {
                json.requireContractShape(location, required = setOf("type", "optionIds"))
                AnswerInputSpec.SingleChoice(json.requireContractStrings("optionIds", location))
            }
            "coordinate" -> {
                json.requireContractShape(location, required = setOf("type"))
                AnswerInputSpec.Coordinate
            }
            else -> error("$location.type 不受支持：$type")
        }

    private fun decodeAnswer(json: JSONObject, location: String): AnswerRule =
        when (val type = json.requireContractText("type", location)) {
            "exact_integer" -> {
                json.requireContractShape(location, required = setOf("type", "expected"))
                AnswerRule.ExactInteger(BigInteger.valueOf(json.requireContractLong("expected", location)))
            }
            "decimal" -> {
                json.requireContractShape(location, required = setOf("type", "expected", "tolerance"))
                AnswerRule.Decimal(
                    expected = json.requireContractDouble("expected", location),
                    tolerance = json.requireContractDouble("tolerance", location),
                )
            }
            "rational_equivalent" -> {
                json.requireContractShape(location, required = setOf("type", "expected"))
                AnswerRule.RationalEquivalent(
                    decodeRational(json.requireContractObject("expected", location), "$location.expected"),
                )
            }
            "single_choice" -> {
                json.requireContractShape(location, required = setOf("type", "expectedOptionId"))
                AnswerRule.SingleChoice(json.requireContractText("expectedOptionId", location))
            }
            "coordinate" -> {
                json.requireContractShape(location, required = setOf("type", "expected"))
                val expected = json.requireContractObject("expected", location)
                expected.requireContractShape("$location.expected", required = setOf("x", "y"))
                AnswerRule.Coordinate(
                    expectedX = decodeRational(expected.requireContractObject("x", "$location.expected"), "$location.expected.x"),
                    expectedY = decodeRational(expected.requireContractObject("y", "$location.expected"), "$location.expected.y"),
                )
            }
            else -> error("$location.type 不受支持：$type")
        }

    private fun decodeRational(json: JSONObject, location: String): RationalValue {
        json.requireContractShape(location, required = setOf("numerator", "denominator"))
        return RationalValue.of(
            numerator = json.requireContractLong("numerator", location),
            denominator = json.requireContractLong("denominator", location),
        )
    }

    private fun decodeKnowledgeBinding(json: JSONObject, location: String): KnowledgeBinding {
        json.requireContractShape(location, required = setOf("knowledgePointId", "weight"))
        return KnowledgeBinding(
            knowledgePointId = KnowledgePointId(
                requireSectionIdentifier(
                    json.requireContractText("knowledgePointId", location),
                    "$location.knowledgePointId",
                ),
            ),
            weight = json.requireContractDouble("weight", location),
        )
    }

    private fun decodeHint(json: JSONObject, location: String): QuestionHint {
        json.requireContractShape(location, required = setOf("id", "text"))
        return QuestionHint(
            id = requireSectionIdentifier(json.requireContractText("id", location), "$location.id"),
            text = json.requireContractText("text", location),
        )
    }

    private fun decodeChoice(json: JSONObject, location: String): QuestionChoiceContent {
        json.requireContractShape(location, required = setOf("id", "content"))
        return QuestionChoiceContent(
            id = requireSectionIdentifier(json.requireContractText("id", location), "$location.id"),
            content = LearningContentParser.decodeArray(
                json.requireContractArray("content", location),
                "$location.content",
                allowEmpty = false,
            ),
        )
    }

    private fun decodePlacement(index: Int, json: JSONObject): AssessmentPlacement {
        val location = "assessments.placements[$index]"
        json.requireContractShape(location, required = setOf("sectionId", "questionSetIds"))
        return AssessmentPlacement(
            sectionId = requireSectionIdentifier(json.requireContractText("sectionId", location), "$location.sectionId"),
            questionSetIds = json.requireContractStrings("questionSetIds", location).map { raw ->
                QuestionSetId(requireSectionIdentifier(raw, "$location.questionSetIds"))
            },
        )
    }
}
