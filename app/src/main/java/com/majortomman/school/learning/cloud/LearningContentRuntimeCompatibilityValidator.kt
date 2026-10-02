package com.majortomman.school.learning.cloud

import com.majortomman.school.learning.assessment.contract.AssessmentDocument
import com.majortomman.school.learning.content.LearningContent
import com.majortomman.school.visualization.SchoolVisualizationCatalog

internal object LearningContentRuntimeCompatibilityValidator {
    fun validate(contents: Iterable<LearningContent>) {
        contents.filterIsInstance<LearningContent.Visualization>().forEach { content ->
            SchoolVisualizationCatalog.requireValid(content.visualization)
        }
    }

    fun validate(assessments: AssessmentDocument) {
        assessments.questionSets
            .flatMap { it.questions }
            .forEach { question ->
                validate(question.stem)
                question.choices.forEach { choice -> validate(choice.content) }
                validate(question.explanation)
            }
    }
}
