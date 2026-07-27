package com.majortomman.school.learning.assessment.contract

import com.majortomman.school.learning.assessment.domain.AnswerInputSpec
import com.majortomman.school.learning.assessment.domain.KnowledgePointId
import com.majortomman.school.learning.assessment.domain.QuestionDefinition
import com.majortomman.school.learning.assessment.domain.QuestionSetDefinition
import com.majortomman.school.learning.assessment.domain.QuestionSetId
import com.majortomman.school.learning.content.ContentAssetId
import com.majortomman.school.learning.content.LearningContent
import com.majortomman.school.learning.content.referencedAssetIds

internal const val ASSESSMENTS_FILE_NAME = "assessments.json"
internal const val KNOWLEDGE_POINTS_FILE_NAME = "knowledge-points.json"

private val COURSE_IDENTIFIER = Regex("^[a-z][a-z0-9_-]{0,95}$")
private val SECTION_IDENTIFIER = Regex("^[a-z0-9][a-z0-9._-]{0,95}$")

internal fun requireCourseIdentifier(value: String, label: String): String = value.trim().also {
    require(COURSE_IDENTIFIER.matches(it)) { "$label 格式无效：$value" }
}

internal fun requireSectionIdentifier(value: String, label: String): String = value.trim().also {
    require(SECTION_IDENTIFIER.matches(it)) { "$label 格式无效：$value" }
}

internal fun requireSafeAssetPath(value: String): String = value.trim().also { path ->
    require(path.isNotBlank()) { "asset path 不能为空" }
    require(path.startsWith("assets/")) { "asset path 必须位于 assets/：$path" }
    require(!path.startsWith("/") && '\\' !in path) { "asset path 必须是正斜杠相对路径：$path" }
    require(path.split('/').none { it.isBlank() || it == "." || it == ".." }) { "asset path 包含非法路径段：$path" }
    require('?' !in path && '#' !in path && '%' !in path) { "asset path 包含不允许的字符：$path" }
}

enum class ContentAssetMediaType(
    val wireValue: String,
    val extensions: Set<String>,
) {
    PNG("image/png", setOf("png")),
    WEBP("image/webp", setOf("webp")),
    JPEG("image/jpeg", setOf("jpg", "jpeg")),
    ;

    companion object {
        fun fromWireValue(value: String): ContentAssetMediaType? = entries.firstOrNull { it.wireValue == value }
    }
}

data class ContentAssetDefinition(
    val id: ContentAssetId,
    val path: String,
    val mediaType: ContentAssetMediaType,
    val width: Int,
    val height: Int,
) {
    init {
        requireSafeAssetPath(path)
        require(width in 1..MAX_IMAGE_DIMENSION) { "asset width 超出允许范围：$width" }
        require(height in 1..MAX_IMAGE_DIMENSION) { "asset height 超出允许范围：$height" }
        require(width.toLong() * height.toLong() <= MAX_IMAGE_PIXELS) { "asset 像素数量超过限制" }
        val extension = path.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        require(extension in mediaType.extensions) { "asset 扩展名与 mediaType 不一致：$path" }
    }

    private companion object {
        const val MAX_IMAGE_DIMENSION = 16_384
        const val MAX_IMAGE_PIXELS = 40_000_000L
    }
}

data class QuestionChoiceContent(
    val id: String,
    val content: List<LearningContent>,
) {
    init {
        require(id.isNotBlank()) { "choice id 不能为空" }
        require(content.isNotEmpty()) { "choice content 不能为空" }
    }
}

data class CourseAssessmentQuestion(
    val definition: QuestionDefinition,
    val stem: List<LearningContent>,
    val choices: List<QuestionChoiceContent> = emptyList(),
    val explanation: List<LearningContent> = emptyList(),
) {
    init {
        require(stem.isNotEmpty()) { "题目 ${definition.key.id} 的 stem 不能为空" }
        require(choices.map(QuestionChoiceContent::id).distinct().size == choices.size) {
            "题目 ${definition.key.id} 的 choice id 不能重复"
        }
        val input = definition.inputSpec
        if (input is AnswerInputSpec.SingleChoice) {
            require(choices.map(QuestionChoiceContent::id) == input.optionIds) {
                "题目 ${definition.key.id} 的 choices 必须与 optionIds 同序一致"
            }
        } else {
            require(choices.isEmpty()) { "非单选题 ${definition.key.id} 不能声明 choices" }
        }
    }

    fun referencedAssetIds(): Set<ContentAssetId> = buildSet {
        stem.forEach { addAll(it.referencedAssetIds()) }
        choices.flatMap(QuestionChoiceContent::content).forEach { addAll(it.referencedAssetIds()) }
        explanation.forEach { addAll(it.referencedAssetIds()) }
    }
}

data class CourseAssessmentQuestionSet(
    val id: QuestionSetId,
    val title: String,
    val questions: List<CourseAssessmentQuestion>,
    val allowSkip: Boolean,
    val allowReviewBeforeFinish: Boolean,
) {
    init {
        require(title.isNotBlank()) { "题组标题不能为空" }
        require(questions.isNotEmpty()) { "题组至少需要一道题" }
        require(questions.map { it.definition.key }.distinct().size == questions.size) {
            "题组 $id 内 question key 不能重复"
        }
    }

    fun toDomainDefinition(): QuestionSetDefinition = QuestionSetDefinition(
        id = id,
        title = title,
        questions = questions.map(CourseAssessmentQuestion::definition),
        allowSkip = allowSkip,
        allowReviewBeforeFinish = allowReviewBeforeFinish,
    )
}

data class AssessmentPlacement(
    val sectionId: String,
    val questionSetIds: List<QuestionSetId>,
) {
    init {
        requireSectionIdentifier(sectionId, "placement.sectionId")
        require(questionSetIds.isNotEmpty()) { "placement.questionSetIds 不能为空" }
        require(questionSetIds.distinct().size == questionSetIds.size) { "placement.questionSetIds 不能重复" }
    }
}

data class AssessmentDocument(
    val courseId: String,
    val assets: List<ContentAssetDefinition>,
    val questionSets: List<CourseAssessmentQuestionSet>,
    val placements: List<AssessmentPlacement>,
) {
    init {
        requireCourseIdentifier(courseId, "assessments.courseId")
        require(questionSets.isNotEmpty()) { "assessments 至少需要一个题组" }
        require(questionSets.map(CourseAssessmentQuestionSet::id).distinct().size == questionSets.size) {
            "questionSet id 不能重复"
        }
        require(questionSets.flatMap(CourseAssessmentQuestionSet::questions).map { it.definition.key }.distinct().size ==
            questionSets.sumOf { it.questions.size }) { "question key 在一本教材内必须唯一" }
        require(assets.map(ContentAssetDefinition::id).distinct().size == assets.size) { "asset id 不能重复" }
        require(assets.map(ContentAssetDefinition::path).distinct().size == assets.size) { "asset path 不能重复" }
        require(placements.map(AssessmentPlacement::sectionId).distinct().size == placements.size) {
            "同一 section 只能声明一个 placement"
        }
    }
}

data class KnowledgePointDefinition(
    val id: KnowledgePointId,
    val title: String,
    val description: String,
    val prerequisiteIds: List<KnowledgePointId>,
    val sectionIds: List<String>,
) {
    init {
        require(title.isNotBlank()) { "knowledge point title 不能为空" }
        require(description.isNotBlank()) { "knowledge point description 不能为空" }
        require(prerequisiteIds.distinct().size == prerequisiteIds.size) { "prerequisiteIds 不能重复" }
        require(id !in prerequisiteIds) { "知识点不能把自己作为前置知识" }
        require(sectionIds.isNotEmpty()) { "知识点至少需要关联一个 section" }
        sectionIds.forEach { requireSectionIdentifier(it, "knowledgePoint.sectionId") }
        require(sectionIds.distinct().size == sectionIds.size) { "knowledgePoint.sectionIds 不能重复" }
    }
}

data class KnowledgePointDocument(
    val courseId: String,
    val knowledgePoints: List<KnowledgePointDefinition>,
) {
    init {
        requireCourseIdentifier(courseId, "knowledge-points.courseId")
        require(knowledgePoints.isNotEmpty()) { "knowledge-points 至少需要一个知识点" }
        require(knowledgePoints.map(KnowledgePointDefinition::id).distinct().size == knowledgePoints.size) {
            "knowledge point id 不能重复"
        }
    }
}
