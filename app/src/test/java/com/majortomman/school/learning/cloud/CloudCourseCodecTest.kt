package com.majortomman.school.learning.cloud

import com.majortomman.school.learning.activity.TextAnswerActivitySpec
import com.majortomman.school.learning.assessment.domain.InlineAssessmentRule
import com.majortomman.school.learning.content.LearningContent
import com.majortomman.school.learning.course.CourseStepRole
import com.majortomman.school.visualization.VisualizationKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudCourseCodecTest {
    @Test
    fun authoredCourseDecodesRoleContentActivityAndAssessment() {
        val lesson = CourseDocumentParser.decode(SAMPLE_COURSE).chapters.single().sections.single().lessons.single()

        assertEquals("为什么需要负数", lesson.title)
        assertEquals("positive-negative", lesson.knowledgePointIds.single())
        assertEquals(CourseStepRole.INQUIRY, lesson.steps[0].role)
        assertTrue(lesson.steps[0].content.single() is LearningContent.Text)

        val visualization = (lesson.steps[1].content.single() as LearningContent.Visualization).visualization
        assertEquals(VisualizationKey("mathematics.number-line.basic"), visualization.renderer)
        assertEquals(-3.0, visualization.parameters.number("value"), 0.0)

        val practice = lesson.steps[2]
        assertEquals(CourseStepRole.PRACTICE, practice.role)
        assertTrue(practice.activity is TextAnswerActivitySpec)
        assertTrue(practice.assessment?.rule is InlineAssessmentRule.ExactText)
        assertEquals(2, lesson.references.single().pageEnd)
    }

    @Test
    fun nullableStepTitleStaysNull() {
        val lesson = CourseDocumentParser.decode(SAMPLE_COURSE).chapters.single().sections.single().lessons.single()
        assertNull(lesson.steps.first().title)
    }

    @Test
    fun blankOptionalStepTitleIsRejected() {
        val invalid = SAMPLE_COURSE.replace(
            "\"role\":\"inquiry\"",
            "\"role\":\"inquiry\",\"title\":\"   \"",
        )
        assertThrows(IllegalArgumentException::class.java) { CourseDocumentParser.decode(invalid) }
    }

    @Test
    fun integerFieldsRejectStringAndDecimalCoercion() {
        assertThrows(IllegalArgumentException::class.java) {
            CourseDocumentParser.decode(SAMPLE_COURSE.replace("\"pageIndexOffset\":7", "\"pageIndexOffset\":\"7\""))
        }
        assertThrows(IllegalArgumentException::class.java) {
            CourseDocumentParser.decode(SAMPLE_COURSE.replace("\"pageIndexOffset\":7", "\"pageIndexOffset\":7.0"))
        }
    }

    @Test
    fun legacyLessonAndTypedStepContractsAreRejected() {
        val legacyLesson = SAMPLE_COURSE.replace("\"steps\":[", "\"practice\":[],\"summary\":[],\"steps\":[")
        assertThrows(IllegalArgumentException::class.java) { CourseDocumentParser.decode(legacyLesson) }

        val legacyStep = SAMPLE_COURSE.replace(
            INQUIRY_STEP,
            "{\"type\":\"question\",\"prompt\":\"低于0℃怎么表示？\",\"hint\":\"想想方向\"}",
        )
        assertThrows(IllegalArgumentException::class.java) { CourseDocumentParser.decode(legacyStep) }
    }

    @Test
    fun stepRejectsUnknownFields() {
        val invalid = SAMPLE_COURSE.replace(
            "\"role\":\"inquiry\"",
            "\"role\":\"inquiry\",\"remoteUrl\":\"https://example.invalid\"",
        )
        assertThrows(IllegalArgumentException::class.java) { CourseDocumentParser.decode(invalid) }
    }

    @Test
    fun formulaContentRequiresPureLatex() {
        val delimited = SAMPLE_COURSE.replace(
            INQUIRY_STEP,
            "{\"id\":\"formula\",\"role\":\"explanation\",\"content\":[{\"type\":\"formula\",\"expression\":\"\$x+1\$\",\"conditions\":[]}]}",
        )
        assertThrows(IllegalArgumentException::class.java) { CourseDocumentParser.decode(delimited) }

        val unicode = SAMPLE_COURSE.replace(
            INQUIRY_STEP,
            "{\"id\":\"formula\",\"role\":\"explanation\",\"content\":[{\"type\":\"formula\",\"expression\":\"x²\",\"conditions\":[]}]}",
        )
        assertThrows(IllegalArgumentException::class.java) { CourseDocumentParser.decode(unicode) }
    }

    @Test
    fun practiceRequiresActivityAndAssessmentExplanation() {
        val missingActivity = SAMPLE_COURSE.replace(ACTIVITY, "")
        assertThrows(IllegalArgumentException::class.java) { CourseDocumentParser.decode(missingActivity) }

        val emptyExplanation = SAMPLE_COURSE.replace(EXPLANATION, "\"explanation\":[]")
        assertThrows(IllegalArgumentException::class.java) { CourseDocumentParser.decode(emptyExplanation) }
    }

    @Test
    fun stepAndActivityIdsMustBeUnique() {
        val duplicateStep = SAMPLE_COURSE.replace(
            "\"id\":\"observe-number-line\"",
            "\"id\":\"inquiry-temperature\"",
        )
        assertThrows(IllegalArgumentException::class.java) { CourseDocumentParser.decode(duplicateStep) }

        val secondActivity = SAMPLE_COURSE.replace(
            "{\"id\":\"summary\",\"role\":\"summary\",\"content\":[",
            "{\"id\":\"summary\",\"role\":\"summary\",\"activity\":{\"type\":\"textAnswer\",\"id\":\"activity-west\"},\"content\":[",
        )
        assertThrows(IllegalArgumentException::class.java) { CourseDocumentParser.decode(secondActivity) }
    }

    @Test
    fun lessonPrerequisiteCycleIsRejected() {
        val cyclic = SAMPLE_COURSE.replace(
            "\"prerequisiteLessonIds\":[]",
            "\"prerequisiteLessonIds\":[\"positive-negative-intro\"]",
        )
        assertThrows(IllegalArgumentException::class.java) { CourseDocumentParser.decode(cyclic) }
    }

    @Test
    fun visualizationRejectsUnknownRendererAndParameter() {
        val unknownRenderer = SAMPLE_COURSE.replace(
            "mathematics.number-line.basic",
            "mathematics.number-line.missing",
        )
        assertThrows(IllegalArgumentException::class.java) { CourseDocumentParser.decode(unknownRenderer) }

        val unknownParameter = SAMPLE_COURSE.replace(
            "\"step\":1",
            "\"step\":1,\"remoteUrl\":1",
        )
        assertThrows(IllegalArgumentException::class.java) { CourseDocumentParser.decode(unknownParameter) }
    }

    @Test
    fun unknownKnowledgePointAndCyclesAreRejected() {
        val missing = SAMPLE_COURSE.replace(
            "\"knowledgePointIds\":[\"positive-negative\"]",
            "\"knowledgePointIds\":[\"missing\"]",
        )
        assertThrows(IllegalArgumentException::class.java) { CourseDocumentParser.decode(missing) }

        val cyclic = SAMPLE_COURSE.replace(
            "\"prerequisiteIds\":[]",
            "\"prerequisiteIds\":[\"positive-negative\"]",
        )
        assertThrows(IllegalArgumentException::class.java) { CourseDocumentParser.decode(cyclic) }
    }

    @Test
    fun googleDriveShareLinkBecomesDirectDownloadLink() {
        assertEquals(
            "https://drive.google.com/uc?export=download&id=abcDEF123",
            CourseSyncManager.normalizeGoogleDriveDownloadUrl(
                "https://drive.google.com/file/d/abcDEF123/view?usp=sharing",
            ),
        )
    }

    private companion object {
        const val INQUIRY_STEP =
            "{\"id\":\"inquiry-temperature\",\"role\":\"inquiry\",\"content\":[{\"type\":\"text\",\"style\":\"prompt\",\"text\":\"低于0℃怎么表示？\"},{\"type\":\"text\",\"style\":\"caption\",\"text\":\"提示：想想方向\"}]}"
        const val ACTIVITY =
            ",\"activity\":{\"type\":\"textAnswer\",\"id\":\"activity-west\",\"placeholder\":\"最终答案\"}"
        const val EXPLANATION =
            "\"explanation\":[{\"type\":\"text\",\"style\":\"explanation\",\"text\":\"方向相反使用负号\"}]"

        val SAMPLE_COURSE = """
            {
              "textbook":{"id":"pep-math-7-1","title":"数学七年级上册","publisher":"人民教育出版社","edition":"2024","grade":"七年级","semester":"上册","subject":"数学","pdf":{"path":"assets/textbook.pdf","pageCount":202,"pageIndexOffset":7}},
              "knowledgePoints":[{"id":"positive-negative","name":"正数和负数","description":"表示相反意义的量","prerequisiteIds":[]}],
              "chapters":[{"id":"chapter-01","title":"有理数","sections":[{"id":"section-01","title":"正数和负数","lessons":[{
                "id":"positive-negative-intro","title":"为什么需要负数","aliases":["正数和负数"],"goals":["理解相反意义的量"],"knowledgePointIds":["positive-negative"],"prerequisiteLessonIds":[],
                "references":[{"label":"教材1—2页","pageStart":1,"pageEnd":2}],
                "steps":[
                  $INQUIRY_STEP,
                  {"id":"observe-number-line","role":"explanation","title":"观察","content":[{"type":"visualization","renderer":"mathematics.number-line.basic","parameters":{"value":-3,"min":-8,"max":8,"step":1},"texts":{"title":"在数轴上观察位置","note":"0 是正负方向的共同基准"}}]},
                  {"id":"practice-west","role":"practice","content":[{"type":"text","style":"prompt","text":"向西8米怎么表示？"}]$ACTIVITY,"assessment":{"type":"exactText","expected":"-8米","ignoreCase":false,$EXPLANATION,"knowledgePointIds":["positive-negative"],"difficulty":0.2}},
                  {"id":"summary","role":"summary","content":[{"type":"text","style":"body","text":"正负号用于区分相反方向"}]}
                ]
              }]}]}]
            }
        """.trimIndent()
    }
}
