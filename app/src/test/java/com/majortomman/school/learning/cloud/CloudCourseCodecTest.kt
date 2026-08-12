package com.majortomman.school.learning.cloud

import com.majortomman.school.learning.course.CourseQuestion
import com.majortomman.school.learning.course.CourseSceneStep
import com.majortomman.school.learning.course.CourseSceneTemplate
import org.junit.Assert.assertEquals
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
        val scene = (lesson.steps[1] as CourseSceneStep).scene
        assertEquals(CourseSceneTemplate.NUMBER_LINE, scene.template)
        assertEquals(1, lesson.practice.size)
        assertEquals(2, lesson.references.single().pageEnd)
    }

    @Test
    fun oldPageContractIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            CourseDocumentParser.decode(SAMPLE_COURSE.replace("\"knowledgePoints\":", "\"pages\":[] ,\"knowledgePoints\":"))
        }
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
                "steps":[{"type":"question","prompt":"低于0℃怎么表示？","hint":"想想方向"},{"type":"scene","template":"number_line","data":{"mode":"value","signed":true,"initial":-3}}],
                "practice":[{"id":"practice-01","prompt":"向西8米怎么表示？","answer":"-8米","analysis":["方向相反使用负号"],"knowledgePointIds":["positive-negative"],"difficulty":1}],
                "summary":["正负号用于区分相反方向"]
              }]}]}]
            }
        """.trimIndent()
    }
}
