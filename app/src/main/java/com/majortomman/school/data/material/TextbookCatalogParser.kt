package com.majortomman.school.data.material

import org.json.JSONArray
import org.json.JSONObject

object TextbookCatalogParser {
    fun parse(
        json: String,
        manifest: MaterialPackManifest,
        selectedSlot: TextbookSlot,
    ): TextbookCatalog {
        val root = JSONObject(json)
        require(root.getInt("schemaVersion") == MATERIAL_PACK_SCHEMA_VERSION) {
            "不支持的教材目录格式版本：${root.getInt("schemaVersion")}"
        }
        val bookObject = root.getJSONObject("book")
        val subject = bookObject.getString("subject").trim()
        val grade = bookObject.getInt("grade")
        val volume = TextbookVolume.fromId(bookObject.getInt("volume"))
        val book = CatalogBook(
            id = bookObject.getString("id").trim(),
            title = bookObject.getString("title").trim(),
            subject = subject,
            grade = grade,
            volume = volume,
            publisher = bookObject.optString("publisher").trim(),
            edition = bookObject.optString("edition").trim(),
        )
        require(book.id.isNotBlank() && book.title.isNotBlank() && book.subject.isNotBlank()) {
            "catalog.json 的 book 缺少 id、title 或 subject"
        }
        require(book.grade == selectedSlot.grade) {
            "所选教材属于${gradeLabel(book.grade)}，与当前${gradeLabel(selectedSlot.grade)}不一致"
        }
        require(book.volume == selectedSlot.volume) {
            "所选教材为${book.volume.labelFor(selectedSlot.stage)}，与当前${selectedSlot.volumeLabel}不一致"
        }
        require(book.subject == selectedSlot.subjectTitle && manifest.subject == selectedSlot.subjectTitle) {
            "所选教材科目为${book.subject}，与当前${selectedSlot.subjectTitle}不一致"
        }

        val lessons = parseLessons(root.getJSONArray("lessons"))
        require(lessons.isNotEmpty()) { "catalog.json 中没有可生成的课程" }
        return TextbookCatalog(book, lessons)
    }

    private fun parseLessons(source: JSONArray): List<CatalogLesson> = buildList {
        for (index in 0 until source.length()) {
            val lesson = source.getJSONObject(index)
            val id = lesson.getString("id").trim()
            val title = lesson.getString("title").trim()
            require(id.isNotBlank() && title.isNotBlank()) { "catalog.json 中课程缺少 id 或 title" }
            val start = lesson.getInt("pageStart")
            val end = lesson.optInt("pageEnd", start)
            require(start > 0 && end >= start) { "课程 $title 的页码范围无效" }
            add(
                CatalogLesson(
                    id = id,
                    title = title,
                    pageStart = start,
                    pageEnd = end,
                    role = lesson.optString("role", "CORE"),
                    path = lesson.optJSONArray("path").toPathNodes(),
                    orderIndex = lesson.optInt("orderIndex", index),
                ),
            )
        }
    }

    fun generateLessons(slot: TextbookSlot, catalog: TextbookCatalog): List<GeneratedLesson> =
        catalog.lessons.mapIndexed { index, source ->
            val pageLabel = if (source.pageStart == source.pageEnd) {
                "教材第 ${source.pageStart} 页"
            } else {
                "教材第 ${source.pageStart}—${source.pageEnd} 页"
            }
            GeneratedLesson(
                id = "${slot.key}:${source.id}",
                sourceId = source.id,
                title = source.title,
                subtitle = "$pageLabel · 依据教材原文",
                estimatedMinutes = ((source.pageEnd - source.pageStart + 1) * 3).coerceIn(12, 36),
                pageStart = source.pageStart,
                pageEnd = source.pageEnd,
                objectives = listOf(
                    "理解${source.title}的核心概念",
                    "能说明教材中的定义与例题依据",
                    "完成${source.title}的独立练习",
                ),
                explanation = "本节课程依据《${catalog.book.title}》$pageLabel 的原文与教学顺序编写。学习时可以随时回到教材原页核对定义、图形和例题。",
                commonMistake = "不要只记结论。遇到不确定的条件时，先返回$pageLabel，确认教材原文和例题中的适用范围。",
                role = source.role,
                path = source.path,
                orderIndex = source.orderIndex.takeIf { it != 0 } ?: index,
            )
        }
}

private fun JSONArray?.toPathNodes(): List<CatalogPathNode> = buildList {
    val source = this@toPathNodes ?: return@buildList
    for (index in 0 until source.length()) {
        val node = CatalogPathNode.fromJson(source.getJSONObject(index))
        if (node.id.isNotBlank() && node.title.isNotBlank()) add(node)
    }
}
