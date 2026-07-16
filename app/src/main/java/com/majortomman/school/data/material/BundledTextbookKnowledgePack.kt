package com.majortomman.school.data.material

import android.content.Context
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.json.JSONArray
import org.json.JSONObject

internal data class BundledTextbookBook(
    val sha256: String,
    val title: String,
    val aliases: List<String>,
    val subjectId: String,
    val subjectTitle: String,
    val stage: EducationStage,
    val grade: Int,
    val volume: TextbookVolume,
    val courseCode: String,
    val pageCount: Int,
    val pageIndexOffset: Int,
    val publisher: String,
    val edition: String,
    val lessons: List<CatalogLesson>,
) {
    val slot: TextbookSlot = TextbookSlot(subjectId, subjectTitle, grade, volume, stage)

    fun installUnbound(context: Context): InstalledTextbook = install(
        context = context,
        root = File(context.filesDir, "materials/prebuilt/$courseCode"),
        pdfPath = UNBOUND_PDF_PATH,
        boundPdf = false,
    )

    fun installBound(context: Context, textbook: InstalledTextbook): InstalledTextbook {
        require(textbook.slot.subjectId == subjectId) { "识别到$title，与当前${textbook.slot.subjectTitle}不一致" }
        require(textbook.pageCount == pageCount) {
            "教材页数与预制课程不一致：预期 $pageCount 页，实际 ${textbook.pageCount} 页"
        }
        return install(
            context = context,
            root = File(textbook.pack.rootPath),
            pdfPath = textbook.pack.manifest.pdf.path,
            boundPdf = true,
            installedAt = textbook.pack.installedAt,
        )
    }

    private fun install(
        context: Context,
        root: File,
        pdfPath: String,
        boundPdf: Boolean,
        installedAt: Long = System.currentTimeMillis(),
    ): InstalledTextbook {
        root.mkdirs()
        val manifest = MaterialPackManifest(
            schemaVersion = MATERIAL_PACK_SCHEMA_VERSION,
            packId = "prebuilt-${sha256.take(16)}",
            version = PACK_VERSION,
            title = title,
            subject = subjectTitle,
            catalogPath = "catalog.json",
            pdf = MaterialPdfAsset(pdfPath, sha256, pageIndexOffset),
        )
        val catalog = TextbookCatalog(
            book = CatalogBook(
                id = courseCode,
                title = title,
                subject = subjectTitle,
                grade = grade,
                volume = volume,
                publisher = publisher,
                edition = edition,
            ),
            lessons = lessons,
        )
        val generated = TextbookCatalogParser.generateLessons(slot, catalog)
        writeJson(File(root, "manifest.json"), MaterialPackManifestParser.toJson(manifest))
        writeJson(File(root, manifest.catalogPath), DirectPdfImportScanner.catalogToJson(catalog))
        writeJson(
            File(root, "generated/lessons.json"),
            JSONObject().put("lessons", JSONArray().apply { generated.forEach { put(it.toJson()) } }),
        )
        File(root, "generated/analysis").deleteRecursively()
        generated.forEach { lesson ->
            val analysis = if (subjectId == "math") {
                PrebuiltMathAnalysisFactory.create(slot, lesson)
            } else {
                PrebuiltSubjectAnalysisFactory.create(slot, lesson)
            }
            LessonAnalysisStore.write(root, analysis)
        }
        writeJson(
            File(root, "generated/identity.json"),
            JSONObject()
                .put("sourceMode", if (boundPdf) "PREBUILT_BOUND" else "PREBUILT_UNBOUND")
                .put("courseCode", courseCode)
                .put("title", title)
                .put("subjectId", subjectId)
                .put("subject", subjectTitle)
                .put("stage", stage.id)
                .put("grade", grade)
                .put("volume", volume.id)
                .put("pageIndexOffset", pageIndexOffset)
                .put("knowledgePointCount", generated.size)
                .put("pdfBound", boundPdf),
        )
        return InstalledTextbook(
            slot = slot,
            pack = InstalledMaterialPack(
                manifest = manifest,
                rootPath = root.absolutePath,
                installedAt = installedAt,
                sizeBytes = MaterialLibraryStore.directorySize(root),
            ),
            pageCount = pageCount,
            lessons = generated,
        )
    }
}

internal object BundledTextbookKnowledgePack {
    const val PACK_VERSION = "prebuilt-all-subjects-v1"
    private const val MATH_INDEX = "prebuilt/math/index.json"
    private const val ALL_INDEX = "prebuilt/all/index.json"
    private var cachedBooks: List<BundledTextbookBook>? = null

    fun books(context: Context): List<BundledTextbookBook> = synchronized(this) {
        cachedBooks ?: (readMathBooks(context) + readGenericBooks(context))
            .distinctBy { it.courseCode }
            .also { cachedBooks = it }
    }

    fun find(context: Context, sha256: String, sourceName: String): BundledTextbookBook? {
        val normalized = normalizeTitle(sourceName)
        return books(context).firstOrNull { it.sha256.equals(sha256, ignoreCase = true) }
            ?: books(context).firstOrNull { book ->
                book.aliases.any { normalizeTitle(it) == normalized }
            }
    }

    fun upgradeIfMatched(context: Context, textbook: InstalledTextbook): InstalledTextbook {
        if (textbook.pack.manifest.version == PACK_VERSION) return textbook
        val book = find(context, textbook.pack.manifest.pdf.sha256, textbook.pack.manifest.title) ?: return textbook
        return book.installBound(context, textbook).also { MaterialLibraryStore.upsert(context, it) }
    }

    private fun readMathBooks(context: Context): List<BundledTextbookBook> = readAsset(context, MATH_INDEX)
        .optJSONArray("books").toBooks { root, lessons ->
            val stage = EducationStage.fromId(root.getString("stage")) ?: return@toBooks null
            val grade = root.getInt("grade")
            val volume = TextbookVolume.fromId(root.getInt("volume"))
            BundledTextbookBook(
                sha256 = root.getString("sha256"),
                title = root.getString("title"),
                aliases = root.optJSONArray("aliases").toStringList(),
                subjectId = "math",
                subjectTitle = "数学",
                stage = stage,
                grade = grade,
                volume = volume,
                courseCode = "math-${stage.id}-g$grade-v${volume.id}",
                pageCount = root.getInt("pageCount"),
                pageIndexOffset = root.getInt("pageIndexOffset"),
                publisher = root.optString("publisher", "人民教育出版社"),
                edition = root.optString("edition", "预制数学知识包"),
                lessons = buildList {
                    for (index in 0 until lessons.length()) {
                        val row = lessons.getJSONArray(index)
                        add(CatalogLesson(row.getString(0), row.getString(1), row.getInt(2), row.getInt(3)))
                    }
                },
            )
        }

    private fun readGenericBooks(context: Context): List<BundledTextbookBook> = readAsset(context, ALL_INDEX)
        .optJSONArray("books").toBooks { root, lessons ->
            val stage = EducationStage.fromId(root.getString("stage")) ?: return@toBooks null
            BundledTextbookBook(
                sha256 = root.getString("sha256"),
                title = root.getString("title"),
                aliases = root.optJSONArray("aliases").toStringList(),
                subjectId = root.getString("subjectId"),
                subjectTitle = root.getString("subjectTitle"),
                stage = stage,
                grade = root.getInt("grade"),
                volume = TextbookVolume.fromId(root.getInt("volume")),
                courseCode = root.getString("courseCode"),
                pageCount = root.getInt("pageCount"),
                pageIndexOffset = root.optInt("pageIndexOffset", 0),
                publisher = root.optString("publisher", "人民教育出版社"),
                edition = root.optString("edition", "全学科预制课程"),
                lessons = buildList {
                    for (index in 0 until lessons.length()) {
                        val row = lessons.getJSONObject(index)
                        add(
                            CatalogLesson(
                                id = row.getString("id"),
                                title = row.getString("title"),
                                pageStart = row.getInt("pageStart"),
                                pageEnd = row.getInt("pageEnd"),
                                path = row.optJSONArray("path").toStringList(),
                            ),
                        )
                    }
                },
            )
        }

    private fun readAsset(context: Context, path: String): JSONObject =
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }

    private inline fun JSONArray?.toBooks(
        transform: (JSONObject, JSONArray) -> BundledTextbookBook?,
    ): List<BundledTextbookBook> = buildList {
        val source = this@toBooks ?: return@buildList
        for (index in 0 until source.length()) {
            val root = source.getJSONObject(index)
            transform(root, root.optJSONArray("lessons") ?: JSONArray())?.let(::add)
        }
    }

    private fun normalizeTitle(value: String): String = value
        .substringAfterLast('/')
        .removeSuffix(".pdf")
        .removeSuffix(".PDF")
        .replace("（根据2022年版课程标准修订）", "")
        .replace("·", "")
        .replace(Regex("[\\s_\\-]+"), "")
        .lowercase()
}

private const val UNBOUND_PDF_PATH = "binding/textbook.pdf"

private fun writeJson(file: File, json: JSONObject) {
    file.parentFile?.mkdirs()
    writeTextAtomically(file, json.toString(2), "无法保存预制教材数据：${file.name}")
}

private fun writeTextAtomically(file: File, text: String, message: String) {
    val parent = file.parentFile ?: throw IOException(message)
    require(parent.mkdirs() || parent.isDirectory) { "无法创建 ${parent.absolutePath}" }
    val temporary = File(parent, ".${file.name}.${System.nanoTime()}.tmp")
    try {
        temporary.writeText(text, Charsets.UTF_8)
        try {
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    } catch (error: Throwable) {
        temporary.delete()
        throw IOException(message, error)
    }
}

private fun JSONArray?.toStringList(): List<String> = buildList {
    val source = this@toStringList ?: return@buildList
    for (index in 0 until source.length()) source.optString(index).trim().takeIf(String::isNotBlank)?.let(::add)
}
