package com.majortomman.school.learning.assessment.application

import com.majortomman.school.learning.assessment.domain.QuestionSetDefinition

class AssessmentSessionControllerFactory(
    private val gateway: AssessmentSessionGateway,
) {
    fun create(
        courseId: String,
        contentRevision: String,
        questionSet: QuestionSetDefinition,
    ): AssessmentSessionController = AssessmentSessionController(
        courseId = courseId,
        contentRevision = contentRevision,
        questionSet = questionSet,
        gateway = gateway,
    )
}
