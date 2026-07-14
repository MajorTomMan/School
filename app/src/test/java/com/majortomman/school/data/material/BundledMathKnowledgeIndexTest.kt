package com.majortomman.school.data.material

import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BundledMathKnowledgeIndexTest {
    @Test
    fun `prebuilt math index contains eleven valid books and 280 lessons`() {
        val file = File("src/main/assets/prebuilt/math/index.json")
        assertTrue("预制数学目录文件不存在", file.isFile)
        val root = JSONObject(file.readText(Charsets.UTF_8))
        val books = root.getJSONArray("books")
        val hashes = mutableSetOf<String>()
        var lessonCount = 0

        assertEquals(1, root.getInt("schemaVersion"))
        assertEquals(11, books.length())

        for (bookIndex in 0 until books.length()) {
            val book = books.getJSONObject(bookIndex)
            val pageCount = book.getInt("pageCount")
            val offset = book.getInt("pageIndexOffset")
            val lessons = book.getJSONArray("lessons")
            val hash = book.getString("sha256")

            assertEquals(64, hash.length)
            assertTrue("教材指纹重复", hashes.add(hash))
            assertTrue(pageCount > 100)
            assertTrue(offset in 0..20)
            assertTrue(lessons.length() > 0)

            var previousStart = 0
            for (lessonIndex in 0 until lessons.length()) {
                val lesson = lessons.getJSONArray(lessonIndex)
                val sourceId = lesson.getString(0)
                val title = lesson.getString(1)
                val start = lesson.getInt(2)
                val end = lesson.getInt(3)

                assertTrue(sourceId.isNotBlank())
                assertTrue(title.isNotBlank())
                assertTrue(start >= previousStart)
                assertTrue(end >= start)
                assertTrue(start - 1 + offset in 0 until pageCount)
                assertTrue(end - 1 + offset in 0 until pageCount)
                previousStart = start
                lessonCount += 1
            }
        }

        assertEquals(280, lessonCount)
    }
}
