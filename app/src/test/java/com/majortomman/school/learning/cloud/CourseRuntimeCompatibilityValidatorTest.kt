package com.majortomman.school.learning.cloud

import com.majortomman.school.learning.activity.ActivityEvent
import com.majortomman.school.learning.activity.ActivityId
import com.majortomman.school.learning.activity.ActivityRuntimeHandler
import com.majortomman.school.learning.activity.ActivitySpec
import com.majortomman.school.learning.activity.ActivityState
import com.majortomman.school.learning.activity.ActivityTransition
import com.majortomman.school.learning.activity.SchoolActivityRuntimeCatalog
import com.majortomman.school.learning.capability.CapabilityKey
import com.majortomman.school.learning.content.LearningContent
import com.majortomman.school.learning.course.CourseChapter
import com.majortomman.school.learning.course.CourseDocument
import com.majortomman.school.learning.course.CourseKnowledgePoint
import com.majortomman.school.learning.course.CourseLesson
import com.majortomman.school.learning.course.CoursePdf
import com.majortomman.school.learning.course.CourseSection
import com.majortomman.school.learning.course.CourseStep
import com.majortomman.school.learning.course.CourseStepRole
import com.majortomman.school.learning.course.CourseTextbook
import org.junit.Assert.assertThrows
import org.junit.Test

class CourseRuntimeCompatibilityValidatorTest {
    @Test
    fun runtimeOnlyActivityCapabilityIsRejectedWithoutUiHost() {
        SchoolActivityRuntimeCatalog.install(RuntimeOnlyHandler)

        assertThrows(IllegalStateException::class.java) {
            CourseRuntimeCompatibilityValidator.validate(courseWith(RuntimeOnlySpec))
        }
    }

    private fun courseWith(activity: ActivitySpec): CourseDocument = CourseDocument(
        textbook = CourseTextbook(
            id = "test-course",
            title = "Test",
            publisher = "Test",
            edition = "1",
            grade = "1",
            semester = "1",
            subject = "test",
            pdf = CoursePdf("assets/textbook.pdf", 1, 0),
        ),
        knowledgePoints = listOf(
            CourseKnowledgePoint(
                id = "kp",
                name = "Knowledge",
                description = "Knowledge",
                prerequisiteIds = emptyList(),
            ),
        ),
        chapters = listOf(
            CourseChapter(
                id = "chapter",
                title = "Chapter",
                sections = listOf(
                    CourseSection(
                        id = "section",
                        title = "Section",
                        lessons = listOf(
                            CourseLesson(
                                id = "lesson",
                                title = "Lesson",
                                aliases = emptyList(),
                                goals = listOf("Goal"),
                                knowledgePointIds = listOf("kp"),
                                prerequisiteLessonIds = emptyList(),
                                references = emptyList(),
                                steps = listOf(
                                    CourseStep(
                                        id = "step",
                                        role = CourseStepRole.PRACTICE,
                                        title = null,
                                        content = listOf(LearningContent.Text("Prompt")),
                                        activity = activity,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        ),
    )

    private data object RuntimeOnlySpec : ActivitySpec {
        override val id = ActivityId("runtime-only")
        override val capability = CapabilityKey("test.runtime-only")
        override val schemaVersion: Int = 1
    }

    private data class RuntimeOnlyState(
        override val spec: ActivitySpec,
    ) : ActivityState

    private object RuntimeOnlyHandler : ActivityRuntimeHandler {
        override val capability = CapabilityKey("test.runtime-only")
        override val schemaVersion: Int = 1

        override fun initialState(spec: ActivitySpec): ActivityState = RuntimeOnlyState(spec)

        override fun reduce(state: ActivityState, event: ActivityEvent): ActivityTransition =
            ActivityTransition(state)
    }
}
