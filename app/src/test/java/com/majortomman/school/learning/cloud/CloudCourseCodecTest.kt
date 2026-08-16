package com.majortomman.school.learning.cloud

import com.majortomman.school.learning.course.CourseQuestion
import com.majortomman.school.learning.course.CourseVisualizationStep
import com.majortomman.school.visualization.VisualizationKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudCourseCodecTest {
    @Test
    fun authoredCourseDecodesKnowledgeLessonsStepsAndPractice() {
        val document = CourseDocumentParser.decode(SAMPLE_COURSE)
        val lesson = document.chapters.single().sections.single().lessons.single()

        assertEquals("为什么需要负数", lesson.title)
        assertEquals("positive-negative", lesson.knowledgePointIds.single())
        assertTrue(lesson.steps[0] is CourseQuestion)
        val visualization = (lesson.steps[1] as CourseVisualizationStep).visualization
        assertEquals(VisualizationKey("mathematics.number-line.basic"), visualization.renderer)
        assertEquals(-3.0, visualization.parameters.number("value"), 0.0)
        assertEquals("在数轴上观察位置", visualization.texts.text("title"))
        assertEquals(1, lesson.practice.size)
        assertEquals(2, lesson.references.single().pageEnd)
    }

    @Test
    fun jsonNullOptionalTeachingTextStaysNull() {
        val document = CourseDocumentParser.decode(SAMPLE_COURSE.replace("\"hint\":\"想想方向\"", "\"hint\":null"))
        val question = document.chapters.single().sections.single().lessons.single().steps.first() as CourseQuestion
        assertNull(question.hint)
    }

    @Test
    fun oldPageContractIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            CourseDocumentParser.decode(SAMPLE_COURSE.replace("\"knowledgePoints\":", "\"pages\":[] ,\"knowledgePoints\":"))
        }
    }

    @Test
    fun legacySceneStepIsRejectedWithoutCompatibility() {
        val legacy = SAMPLE_COURSE.replace(
            "{\"type\":\"visualization\",\"renderer\":\"mathematics.number-line.basic\",\"parameters\":{\"value\":-3,\"min\":-8,\"max\":8,\"step\":1},\"texts\":{\"title\":\"在数轴上观察位置\",\"note\":\"0 是正负方向的共同基准\"}}",
            "{\"type\":\"scene\",\"template\":\"number_line\",\"data\":{\"mode\":\"value\",\"initial\":-3}}",
        )
        assertThrows(IllegalStateException::class.java) { CourseDocumentParser.decode(legacy) }
    }

    @Test
    fun visualizationRejectsUnknownRenderer() {
        val invalid = SAMPLE_COURSE.replace("mathematics.number-line.basic", "mathematics.number-line.missing")
        assertThrows(IllegalArgumentException::class.java) { CourseDocumentParser.decode(invalid) }
    }

    @Test
    fun visualizationRejectsUnknownParameter() {
        val invalid = SAMPLE_COURSE.replace("\"step\":1", "\"step\":1,\"remoteUrl\":1")
        assertThrows(IllegalArgumentException::class.java) { CourseDocumentParser.decode(invalid) }
    }

    @Test
    fun visualizationParametersRejectStringsAndObjects() {
        val stringParameter = SAMPLE_COURSE.replace("\"value\":-3", "\"value\":\"-3\"")
        assertThrows(IllegalStateException::class.java) { CourseDocumentParser.decode(stringParameter) }

        val objectParameter = SAMPLE_COURSE.replace("\"value\":-3", "\"value\":{\"nested\":-3}")
        assertThrows(IllegalStateException::class.java) { CourseDocumentParser.decode(objectParameter) }
    }

    @Test
    fun visualizationTextsRejectNonStrings() {
        val invalid = SAMPLE_COURSE.replace("\"title\":\"在数轴上观察位置\"", "\"title\":123")
        assertThrows(IllegalArgumentException::class.java) { CourseDocumentParser.decode(invalid) }
    }

    @Test
    fun unknownKnowledgePointIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            CourseDocumentParser.decode(SAMPLE_COURSE.replace("\"positive-negative\"]", "\"missing\"]"))
        }
    }

    @Test
    fun cyclicKnowledgeGraphIsRejected() {
        val cyclic = SAMPLE_COURSE.replace("\"prerequisiteIds\":[]", "\"prerequisiteIds\":[\"positive-negative\"]")
        assertThrows(IllegalArgumentException::class.java) { CourseDocumentParser.decode(cyclic) }
    }

    @Test
    fun googleDriveShareLinkBecomesDirectDownloadLink() {
        assertEquals("https://drive.google.com/uc?export=download&id=abcDEF123", CourseSyncManager.normalizeGoogleDriveDownloadUrl("https://drive.google.com/file/d/abcDEF123/view?usp=sharing"))
    }

    private companion object {
        val SAMPLE_COURSE = """
            {
              "textbook":{"id":"pep-math-7-1","title":"数学七年级上册","publisher":"人民教育出版社","edition":"2024","grade":"七年级","semester":"上册","subject":"数学","pdf":{"path":"assets/textbook.pdf","pageCount":202,"pageIndexOffset":7}},
              "knowledgePoints":[{"id":"positive-negative","name":"正数和负数","description":"表示相反意义的量","prerequisiteIds":[]}],
              "chapters":[{"id":"chapter-01","title":"有理数","sections":[{"id":"section-01","title":"正数和负数","lessons":[{
                "id":"positive-negative-intro","title":"为什么需要负数","aliases":["正数和负数"],"goals":["理解相反意义的量"],"knowledgePointIds":["positive-negative"],"prerequisiteLessonIds":[],
                "references":[{"label":"教材1—2页","pageStart":1,"pageEnd":2}],
                "steps":[{"type":"question","prompt":"低于0℃怎么表示？","hint":"想想方向"},{"type":"visualization","renderer":"mathematics.number-line.basic","parameters":{"value":-3,"min":-8,"max":8,"step":1},"texts":{"title":"在数轴上观察位置","note":"0 是正负方向的共同基准"}}],
                "practice":[{"id":"practice-01","prompt":"向西8米怎么表示？","answer":"-8米","analysis":["方向相反使用负号"],"knowledgePointIds":["positive-negative"],"difficulty":1}],
                "summary":["正负号用于区分相反方向"]
              }]}]}]
            }
        """.trimIndent()
    }
}
