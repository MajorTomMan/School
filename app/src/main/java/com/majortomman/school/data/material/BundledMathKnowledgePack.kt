package com.majortomman.school.data.material

import android.content.Context
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

internal data class BundledMathKnowledgeBook(
    val assetPath: String,
    val sha256: String,
    val title: String,
    val stage: EducationStage,
    val grade: Int,
    val volume: TextbookVolume,
    val pageCount: Int,
    val pageIndexOffset: Int,
    val publisher: String,
    val edition: String,
    val lessons: List<BundledMathLesson>,
) {
    fun validate(slot: TextbookSlot, actualPageCount: Int) {
        require(slot.subjectId == "math") { "预制知识包只适用于数学教材" }
        require(slot.stage == stage && slot.grade == grade && slot.volume == volume) {
            "识别到$title，请从${stage.label} · ${gradeLabel(grade)} · ${volume.labelFor(stage)}导入"
        }
        require(actualPageCount == pageCount) {
            "教材页数与预制知识包不一致：预期 $pageCount 页，实际 $actualPageCount 页"
        }
    }

    fun toScanResult(): DirectPdfScanResult = DirectPdfScanResult(
        title = title,
        pageIndexOffset = pageIndexOffset,
        catalog = TextbookCatalog(
            book = CatalogBook(
                id = "prebuilt-${sha256.take(16)}",
                title = title,
                subject = "数学",
                grade = grade,
                volume = volume,
                publisher = publisher,
                edition = edition,
            ),
            lessons = lessons.map { lesson ->
                CatalogLesson(
                    id = lesson.sourceId,
                    title = lesson.title,
                    pageStart = lesson.pageStart,
                    pageEnd = lesson.pageEnd,
                )
            },
        ),
        scannedPages = 0,
        evidence = "内置数学知识包 · SHA-256 精确匹配 · ${lessons.size} 个知识点",
    )

    fun writeAnalyses(textbookRoot: File) {
        lessons.forEach { lesson ->
            LessonAnalysisStore.write(
                textbookRoot,
                LessonAnalysis.fromJson(lesson.analysis, LessonAnalysisSource.PACK),
            )
        }
    }
}

internal data class BundledMathLesson(
    val sourceId: String,
    val title: String,
    val pageStart: Int,
    val pageEnd: Int,
    val analysis: JSONObject,
)

internal object BundledMathKnowledgePack {
    private const val INDEX_ASSET = "prebuilt/math/index.json"
    private var cachedIndex: List<IndexEntry>? = null
    private val cachedBooks = mutableMapOf<String, BundledMathKnowledgeBook>()

    fun find(
        context: Context,
        sha256: String,
        sourceName: String,
    ): BundledMathKnowledgeBook? {
        val entries = loadIndex(context)
        val normalizedName = normalizeTitle(sourceName)
        val entry = entries.firstOrNull { it.sha256.equals(sha256, ignoreCase = true) }
            ?: entries.firstOrNull { candidate ->
                candidate.aliases.any { alias -> normalizeTitle(alias) == normalizedName }
            }
            ?: return null
        return synchronized(cachedBooks) {
            cachedBooks[entry.assetPath] ?: loadBook(context, entry.assetPath).also {
                cachedBooks[entry.assetPath] = it
            }
        }
    }

    fun count(context: Context): Int = loadIndex(context).size

    private fun loadIndex(context: Context): List<IndexEntry> = synchronized(this) {
        cachedIndex ?: readAsset(context, INDEX_ASSET).let { root ->
            val books = root.optJSONArray("books") ?: JSONArray()
            buildList {
                for (index in 0 until books.length()) {
                    val item = books.getJSONObject(index)
                    add(
                        IndexEntry(
                            sha256 = item.getString("sha256"),
                            assetPath = item.getString("asset"),
                            aliases = item.optJSONArray("aliases").toStringList(),
                        ),
                    )
                }
            }.also { cachedIndex = it }
        }
    }

    private fun loadBook(context: Context, assetPath: String): BundledMathKnowledgeBook {
        val root = readAsset(context, assetPath)
        val lessons = root.optJSONArray("lessons") ?: JSONArray()
        return BundledMathKnowledgeBook(
            assetPath = assetPath,
            sha256 = root.getString("sha256"),
            title = root.getString("title"),
            stage = EducationStage.fromId(root.getString("stage"))
                ?: throw IllegalArgumentException("预制教材缺少教育阶段"),
            grade = root.getInt("grade"),
            volume = TextbookVolume.fromId(root.getInt("volume")),
            pageCount = root.getInt("pageCount"),
            pageIndexOffset = root.getInt("pageIndexOffset"),
            publisher = root.optString("publisher", "人民教育出版社"),
            edition = root.optString("edition", "预制数学知识包"),
            lessons = buildList {
                for (index in 0 until lessons.length()) {
                    val lesson = lessons.getJSONObject(index)
                    add(
                        BundledMathLesson(
                            sourceId = lesson.getString("sourceId"),
                            title = lesson.getString("title"),
                            pageStart = lesson.getInt("pageStart"),
                            pageEnd = lesson.getInt("pageEnd"),
                            analysis = lesson.getJSONObject("analysis"),
                        ),
                    )
                }
            },
        )
    }

    private fun readAsset(context: Context, path: String): JSONObject =
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { reader ->
            JSONObject(reader.readText())
        }

    private fun normalizeTitle(value: String): String = value
        .substringAfterLast('/')
        .removeSuffix(".pdf")
        .removeSuffix(".PDF")
        .replace("·", "")
        .replace("（", "(")
        .replace("）", ")")
        .replace(Regex("[\\s_\\-]+"), "")
        .lowercase()

    private data class IndexEntry(
        val sha256: String,
        val assetPath: String,
        val aliases: List<String>,
    )
}

private fun JSONArray?.toStringList(): List<String> = buildList {
    val source = this@toStringList ?: return@buildList
    for (index in 0 until source.length()) {
        source.optString(index).trim().takeIf { it.isNotBlank() }?.let(::add)
    }
}
