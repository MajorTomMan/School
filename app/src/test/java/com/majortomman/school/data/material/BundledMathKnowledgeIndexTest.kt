package com.majortomman.school.data.material

import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BundledMathKnowledgeIndexTest {
    private val expectedHashes = mapOf(
        "义务教育教科书·数学七年级上册" to "11b6f1fbfa46eee4158953ef745ae1e6fbe6b9527a1423d55cbe75729e8210b9",
        "义务教育教科书·数学七年级下册" to "a58d058dd4f6a0855558a90a24640d9cf3ff8430112fb4d1f434bd881f836210",
        "义务教育教科书·数学九年级上册" to "b6d43c0601c07a6a6f68d3be065c863b41529745e739b4496154c90ecbe7140d",
        "义务教育教科书·数学九年级下册" to "a71d30eb32b0e43737c88ada99a76ae6b5959050dc53c491b5206bae2d31b0ce",
        "义务教育教科书·数学八年级上册" to "e21e7094a470f74a5be947e5fa8a9eee57bd6a4deaaaba3dbd09b811590838ed",
        "义务教育教科书·数学八年级下册" to "4979e490f518912473519aafc2b7e016aa804741ac7efbaa4156d230d9dc8b3c",
        "普通高中教科书·数学（A版）必修 第一册" to "c6be83eea122bd50025c0b05019a94d7e95fb7a351fe9560e24f1b65ff51c748",
        "普通高中教科书·数学（A版）必修 第二册" to "c8f50482b6788bf2642fb13ced24b253c4656d73259f204f96e0e8b4829cc4a7",
        "普通高中教科书·数学（A版）选择性必修 第一册" to "cbb6a7f2b610f36275e6b71752dbd7dfd9d0af8143b06e35a98021e3a51210fd",
        "普通高中教科书·数学（A版）选择性必修 第三册" to "05b3ce22aa155fdd938248e976afdeef5b7147583b51d1e49bde0493285e18c5",
        "普通高中教科书·数学（A版）选择性必修 第二册" to "d58fef0e5ad887e700cdeeac6cdc24a42bae8b7f3431b4c661871c3a55a0406b",
    )

    @Test
    fun `prebuilt math index contains eleven verified books and 280 compilable lessons`() {
        val file = File("src/main/assets/prebuilt/math/index.json")
        assertTrue("预制数学目录文件不存在", file.isFile)
        val root = JSONObject(file.readText(Charsets.UTF_8))
        val books = root.getJSONArray("books")
        val hashes = mutableSetOf<String>()
        var lessonCount = 0

        assertEquals(1, root.getInt("schemaVersion"))
        assertEquals(11, books.length())
        assertEquals(11, expectedHashes.size)

        for (bookIndex in 0 until books.length()) {
            val book = books.getJSONObject(bookIndex)
            val title = book.getString("title")
            val pageCount = book.getInt("pageCount")
            val offset = book.getInt("pageIndexOffset")
            val lessons = book.getJSONArray("lessons")
            val hash = book.getString("sha256")
            val stage = EducationStage.fromId(book.getString("stage"))
                ?: error("教材阶段无法识别：$title")
            val volume = TextbookVolume.fromId(book.getInt("volume"))
            val slot = TextbookSlot(
                subjectId = "math",
                subjectTitle = "数学",
                grade = book.getInt("grade"),
                volume = volume,
                stage = stage,
            )

            assertEquals("教材指纹与扫描结果不一致：$title", expectedHashes[title], hash)
            assertEquals(64, hash.length)
            assertTrue("教材指纹重复", hashes.add(hash))
            assertTrue(pageCount > 100)
            assertTrue(offset in 0..20)
            assertTrue(lessons.length() > 0)

            var previousStart = 0
            for (lessonIndex in 0 until lessons.length()) {
                val lesson = lessons.getJSONArray(lessonIndex)
                val sourceId = lesson.getString(0)
                val lessonTitle = lesson.getString(1)
                val start = lesson.getInt(2)
                val end = lesson.getInt(3)

                assertTrue(sourceId.isNotBlank())
                assertTrue(lessonTitle.isNotBlank())
                assertTrue(start >= previousStart)
                assertTrue(end >= start)
                assertTrue(start - 1 + offset in 0 until pageCount)
                assertTrue(end - 1 + offset in 0 until pageCount)

                val generated = GeneratedLesson(
                    id = "${slot.key}:$sourceId",
                    sourceId = sourceId,
                    title = lessonTitle,
                    subtitle = "教材第 $start—$end 页",
                    estimatedMinutes = 18,
                    pageStart = start,
                    pageEnd = end,
                    objectives = emptyList(),
                    explanation = "",
                    commonMistake = "",
                )
                val analysis = PrebuiltMathAnalysisFactory.create(slot, generated)
                assertEquals(LessonAnalysisSource.PACK, analysis.source)
                assertEquals(start..end, analysis.sourcePages)
                assertTrue(analysis.summary.isNotBlank())
                assertTrue(analysis.scene.title.isNotBlank())
                assertTrue(analysis.scene.conclusion.isNotBlank())

                previousStart = start
                lessonCount += 1
            }
        }

        assertEquals(280, lessonCount)
    }
}
