package com.majortomman.school.learning.assessment.contract

import com.majortomman.school.learning.assessment.domain.KnowledgePointId
import com.majortomman.school.learning.assessment.domain.QuestionSetId
import com.majortomman.school.learning.content.ContentAssetId
import com.majortomman.school.learning.course.CourseDocument

object AssessmentPackageContract {
    fun validate(
        course: CourseDocument,
        assessments: AssessmentDocument,
        knowledgePoints: KnowledgePointDocument,
    ) {
        val courseId = course.textbook.id
        require(assessments.courseId == courseId) {
            "assessments.courseId 与 course.json 教材 ID 不一致：${assessments.courseId} != $courseId"
        }
        require(knowledgePoints.courseId == courseId) {
            "knowledge-points.courseId 与 course.json 教材 ID 不一致：${knowledgePoints.courseId} != $courseId"
        }

        val sectionIds = buildSet {
            course.chapters.forEach { chapter ->
                chapter.sections.forEach { add(it.id) }
                chapter.review?.let { add(it.id) }
            }
        }
        validateKnowledgePoints(knowledgePoints, sectionIds)
        validatePlacements(assessments, sectionIds)
        validateQuestionBindings(assessments, knowledgePoints)
        validateAssets(assessments)
    }

    private fun validateKnowledgePoints(
        document: KnowledgePointDocument,
        sectionIds: Set<String>,
    ) {
        val knownIds = document.knowledgePoints.map { it.id }.toSet()
        document.knowledgePoints.forEach { point ->
            val unknownPrerequisites = point.prerequisiteIds.toSet() - knownIds
            require(unknownPrerequisites.isEmpty()) {
                "知识点 ${point.id} 引用了不存在的前置知识：${unknownPrerequisites.joinToString()}"
            }
            val unknownSections = point.sectionIds.toSet() - sectionIds
            require(unknownSections.isEmpty()) {
                "知识点 ${point.id} 引用了不存在的小节：${unknownSections.joinToString()}"
            }
        }
        requireAcyclicKnowledgeGraph(document)
    }

    private fun requireAcyclicKnowledgeGraph(document: KnowledgePointDocument) {
        val prerequisites = document.knowledgePoints.associate { it.id to it.prerequisiteIds }
        val visiting = linkedSetOf<KnowledgePointId>()
        val visited = linkedSetOf<KnowledgePointId>()

        fun visit(id: KnowledgePointId) {
            if (id in visited) return
            require(id !in visiting) {
                "知识点前置关系形成循环：${(visiting + id).joinToString(" -> ")}"
            }
            visiting += id
            prerequisites.getValue(id).forEach(::visit)
            visiting -= id
            visited += id
        }

        prerequisites.keys.forEach(::visit)
    }

    private fun validatePlacements(
        assessments: AssessmentDocument,
        sectionIds: Set<String>,
    ) {
        val knownQuestionSets = assessments.questionSets.map { it.id }.toSet()
        assessments.placements.forEach { placement ->
            require(placement.sectionId in sectionIds) {
                "题组位置引用了不存在的小节：${placement.sectionId}"
            }
            val unknownSets = placement.questionSetIds.toSet() - knownQuestionSets
            require(unknownSets.isEmpty()) {
                "小节 ${placement.sectionId} 引用了不存在的题组：${unknownSets.joinToString()}"
            }
        }

        val placedIds = assessments.placements.flatMap { it.questionSetIds }
        require(placedIds.distinct().size == placedIds.size) { "同一题组不能放置到多个小节" }
        val unplaced = knownQuestionSets - placedIds.toSet()
        require(unplaced.isEmpty()) { "存在未放置到课程小节的题组：${unplaced.joinToString()}" }
    }

    private fun validateQuestionBindings(
        assessments: AssessmentDocument,
        knowledgePoints: KnowledgePointDocument,
    ) {
        val knownKnowledgePoints = knowledgePoints.knowledgePoints.map { it.id }.toSet()
        assessments.questionSets.forEach { questionSet ->
            questionSet.questions.forEach { question ->
                val referenced = question.definition.knowledgeBindings.map { it.knowledgePointId }.toSet()
                val unknown = referenced - knownKnowledgePoints
                require(unknown.isEmpty()) {
                    "题目 ${question.definition.key.id} 引用了不存在的知识点：${unknown.joinToString()}"
                }
            }
        }
    }

    private fun validateAssets(assessments: AssessmentDocument) {
        val declared = assessments.assets.map { it.id }.toSet()
        val referenced = assessments.questionSets
            .flatMap { it.questions }
            .flatMapTo(linkedSetOf()) { it.referencedAssetIds() }
        val missing = referenced - declared
        require(missing.isEmpty()) { "题目内容引用了未声明资产：${missing.joinToString()}" }
        val unused = declared - referenced
        require(unused.isEmpty()) { "assessments 声明了未使用资产：${unused.joinToString()}" }
    }
}

@Suppress("unused")
private fun contractIdAnchors(questionSetId: QuestionSetId, assetId: ContentAssetId) =
    questionSetId to assetId
