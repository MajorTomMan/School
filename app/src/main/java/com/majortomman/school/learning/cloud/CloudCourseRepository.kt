package com.majortomman.school.learning.cloud

import android.content.Context
import com.majortomman.school.learning.course.CourseChapter
import com.majortomman.school.learning.course.CourseCheckpoint
import com.majortomman.school.learning.course.CourseDocument
import com.majortomman.school.learning.course.CourseExample
import com.majortomman.school.learning.course.CourseExplanation
import com.majortomman.school.learning.course.CourseFormula
import com.majortomman.school.learning.course.CourseKeyIdea
import com.majortomman.school.learning.course.CourseKnowledgePoint
import com.majortomman.school.learning.course.CourseLesson
import com.majortomman.school.learning.course.CoursePdf
import com.majortomman.school.learning.course.CoursePractice
import com.majortomman.school.learning.course.CourseQuestion
import com.majortomman.school.learning.course.CourseScene
import com.majortomman.school.learning.course.CourseSceneData
import com.majortomman.school.learning.course.CourseSceneStep
import com.majortomman.school.learning.course.CourseSceneTemplate
import com.majortomman.school.learning.course.CourseSection
import com.majortomman.school.learning.course.CourseSourceLink
import com.majortomman.school.learning.course.CourseSourceReference
import com.majortomman.school.learning.course.CourseStep
import com.majortomman.school.learning.course.CourseSummaryStep
import com.majortomman.school.learning.course.CourseTextbook
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

object CloudCourseRepository {
    private const val ACTIVE_DIRECTORY = "course-packs/active"
    private const val COURSE_FILE_NAME = "course.json"
    @Volatile private var appContext: Context? = null
    private val mutableRevision = MutableStateFlow(0L)
    val revision = mutableRevision.asStateFlow()
    private val cacheLock = Any()
    private val parsedCache = mutableMapOf<String, CachedCourseDocument>()

    fun initialize(context: Context) { appContext = context.applicationContext }
    fun hasInstalledCourseContent(): Boolean = courseDocuments().isNotEmpty()

    fun lessonFor(id: String, title: String): CourseLesson? {
        val normalizedTitle = normalize(title)
        return courseDocuments().asSequence().flatMap { it.document.chapters.asSequence() }
            .flatMap { it.sections.asSequence() }.flatMap { it.lessons.asSequence() }
            .firstOrNull { lesson -> lesson.id == id || normalize(lesson.title) == normalizedTitle || lesson.aliases.any { normalize(it) == normalizedTitle } }
    }

    internal fun markContentChanged() {
        synchronized(cacheLock) { parsedCache.clear() }
        mutableRevision.value += 1L
    }

    private fun courseDocuments(): List<CachedCourseDocument> {
        val context = appContext ?: return emptyList()
        val activeRoot = File(context.filesDir, ACTIVE_DIRECTORY)
        return activeRoot.listFiles().orEmpty().filter(File::isDirectory).map { File(it, COURSE_FILE_NAME) }.filter(File::isFile).mapNotNull { file ->
            synchronized(cacheLock) {
                val key = file.absolutePath
                val cached = parsedCache[key]
                if (cached != null && cached.lastModified == file.lastModified() && cached.length == file.length()) cached else runCatching {
                    val document = CourseDocumentParser.decode(file.readText(Charsets.UTF_8))
                    val root = file.parentFile ?: error("课程文件缺少安装目录")
                    require(File(root, document.textbook.pdf.path).isFile) { "课程包缺少教材 PDF" }
                    CachedCourseDocument(file.lastModified(), file.length(), document)
                }.getOrNull()?.also { parsedCache[key] = it }
            }
        }
    }

    private data class CachedCourseDocument(val lastModified: Long, val length: Long, val document: CourseDocument)
    private fun normalize(value: String): String = value.replace(" ", "").replace("　", "").trim()
}

internal object CourseDocumentParser {
    fun decode(raw: String): CourseDocument = decode(JSONObject(raw))

    fun decode(root: JSONObject): CourseDocument {
        root.requireKeys(setOf("textbook", "knowledgePoints", "chapters"))
        val textbook = decodeTextbook(root.objectValue("textbook"))
        val knowledgePoints = root.arrayValue("knowledgePoints").objects().map(::decodeKnowledgePoint)
        require(knowledgePoints.isNotEmpty()) { "课程必须声明知识点" }
        require(knowledgePoints.map { it.id }.size == knowledgePoints.map { it.id }.toSet().size) { "知识点 ID 不能重复" }
        requireKnowledgeGraph(knowledgePoints)
        val knowledgeIds = knowledgePoints.map { it.id }.toSet()
        val lessonIds = linkedSetOf<String>()
        val chapters = root.arrayValue("chapters").objects().map { decodeChapter(it, textbook.pdf, knowledgeIds, lessonIds) }
        require(chapters.isNotEmpty()) { "课程必须包含章节" }
        val allLessons = chapters.flatMap { it.sections }.flatMap { it.lessons }
        val missingPrerequisites = allLessons.flatMap { lesson -> lesson.prerequisiteLessonIds.map { lesson.id to it } }.filter { (_, prerequisite) -> prerequisite !in lessonIds }
        require(missingPrerequisites.isEmpty()) { "课程包含不存在的前置课时：$missingPrerequisites" }
        return CourseDocument(textbook, knowledgePoints, chapters)
    }

    private fun decodeTextbook(json: JSONObject): CourseTextbook {
        json.requireKeys(setOf("id", "title", "publisher", "edition", "grade", "semester", "subject", "pdf"))
        val pdfJson = json.objectValue("pdf")
        pdfJson.requireKeys(setOf("path", "pageCount", "pageIndexOffset"))
        val path = pdfJson.text("path")
        require(path.endsWith(".pdf", true) && !path.startsWith('/') && ".." !in path.split('/')) { "教材 PDF 路径无效" }
        return CourseTextbook(json.identifier("id"), json.text("title"), json.text("publisher"), json.text("edition"), json.text("grade"), json.text("semester"), json.text("subject"), CoursePdf(path, pdfJson.positiveInt("pageCount"), pdfJson.getInt("pageIndexOffset")))
    }

    private fun decodeKnowledgePoint(json: JSONObject): CourseKnowledgePoint {
        json.requireKeys(setOf("id", "name", "description", "prerequisiteIds"))
        return CourseKnowledgePoint(json.identifier("id"), json.text("name"), json.text("description"), json.stringArray("prerequisiteIds"))
    }

    private fun decodeChapter(json: JSONObject, pdf: CoursePdf, knowledgeIds: Set<String>, lessonIds: MutableSet<String>): CourseChapter {
        json.requireKeys(setOf("id", "title", "sections"))
        val sections = json.arrayValue("sections").objects().map { decodeSection(it, pdf, knowledgeIds, lessonIds) }
        require(sections.isNotEmpty()) { "章节 ${json.optString("id")} 不包含小节" }
        return CourseChapter(json.identifier("id"), json.text("title"), sections)
    }

    private fun decodeSection(json: JSONObject, pdf: CoursePdf, knowledgeIds: Set<String>, lessonIds: MutableSet<String>): CourseSection {
        json.requireKeys(setOf("id", "title", "lessons"))
        val lessons = json.arrayValue("lessons").objects().map { decodeLesson(it, pdf, knowledgeIds, lessonIds) }
        require(lessons.isNotEmpty()) { "小节 ${json.optString("id")} 不包含课时" }
        return CourseSection(json.identifier("id"), json.text("title"), lessons)
    }

    private fun decodeLesson(json: JSONObject, pdf: CoursePdf, knowledgeIds: Set<String>, lessonIds: MutableSet<String>): CourseLesson {
        json.requireKeys(setOf("id", "title", "aliases", "goals", "knowledgePointIds", "prerequisiteLessonIds", "references", "steps", "practice", "summary"))
        val id = json.identifier("id")
        require(lessonIds.add(id)) { "课时 ID 重复：$id" }
        val lessonKnowledge = json.stringArray("knowledgePointIds")
        require(lessonKnowledge.isNotEmpty() && lessonKnowledge.all { it in knowledgeIds }) { "课时 $id 的知识点绑定无效" }
        val references = json.arrayValue("references").objects().map { decodeReference(it, pdf, id) }
        val steps = json.arrayValue("steps").objects().mapIndexed { index, item -> decodeStep(item, "$id.steps[$index]", references.size) }
        require(steps.isNotEmpty()) { "课时 $id 不包含教学步骤" }
        val practice = json.arrayValue("practice").objects().map { decodePractice(it, id, knowledgeIds) }
        return CourseLesson(id, json.text("title"), json.stringArray("aliases"), json.stringArray("goals").also { require(it.isNotEmpty()) { "课时 $id 必须声明教学目标" } }, lessonKnowledge, json.stringArray("prerequisiteLessonIds"), references, steps, practice, json.stringArray("summary").also { require(it.isNotEmpty()) { "课时 $id 必须有总结" } })
    }

    private fun decodeReference(json: JSONObject, pdf: CoursePdf, lessonId: String): CourseSourceReference {
        json.requireKeys(setOf("label", "pageStart", "pageEnd"))
        val start = json.positiveInt("pageStart")
        val end = json.positiveInt("pageEnd")
        require(start <= end && end <= pdf.pageCount) { "课时 $lessonId 的教材引用页码无效" }
        return CourseSourceReference(json.text("label"), start, end)
    }

    private fun decodeStep(json: JSONObject, location: String, referenceCount: Int): CourseStep = when (val type = json.text("type")) {
        "explanation" -> CourseExplanation(json.optionalText("title"), json.text("text"))
        "question" -> CourseQuestion(json.text("prompt"), json.optionalText("hint"))
        "keyIdea" -> CourseKeyIdea(json.optionalText("title"), json.text("text"))
        "formula" -> CourseFormula(json.text("expression"), json.optionalText("note"))
        "example" -> CourseExample(json.text("title"), json.text("prompt"), json.stringArray("steps"), json.text("answer"))
        "scene" -> {
            val templateId = json.text("template")
            val template = CourseSceneTemplate.fromId(templateId) ?: error("$location 使用了不支持的场景：$templateId")
            CourseSceneStep(CourseScene(template, CourseSceneData(json.objectValue("data").toMap())))
        }
        "checkpoint" -> CourseCheckpoint(json.text("prompt"), json.text("expectedAnswer"), json.text("explanation"))
        "sourceLink" -> CourseSourceLink(json.getInt("referenceIndex").also { require(it in 0 until referenceCount) { "$location 引用索引越界" } })
        "summary" -> CourseSummaryStep(json.text("text"))
        else -> error("$location 使用了不支持的教学步骤：$type")
    }

    private fun decodePractice(json: JSONObject, lessonId: String, knowledgeIds: Set<String>): CoursePractice {
        json.requireKeys(setOf("id", "prompt", "answer", "analysis", "knowledgePointIds", "difficulty"))
        val ids = json.stringArray("knowledgePointIds")
        require(ids.isNotEmpty() && ids.all { it in knowledgeIds }) { "课时 $lessonId 的练习知识点绑定无效" }
        val difficulty = json.getInt("difficulty")
        require(difficulty in 1..5) { "练习难度必须在 1..5" }
        return CoursePractice(json.identifier("id"), json.text("prompt"), json.text("answer"), json.stringArray("analysis"), ids, difficulty)
    }

    private fun requireKnowledgeGraph(points: List<CourseKnowledgePoint>) {
        val ids = points.map { it.id }.toSet()
        points.forEach { point -> require(point.prerequisiteIds.all { it in ids }) { "知识点 ${point.id} 引用了不存在的前置知识" } }
        val prerequisites = points.associate { it.id to it.prerequisiteIds }
        val visiting = linkedSetOf<String>()
        val visited = linkedSetOf<String>()
        fun visit(id: String) {
            if (id in visited) return
            require(id !in visiting) { "知识点前置关系形成循环：${(visiting + id).joinToString(" -> ")}" }
            visiting += id
            prerequisites.getValue(id).forEach(::visit)
            visiting -= id
            visited += id
        }
        ids.forEach(::visit)
    }
}

private val IDENTIFIER = Regex("^[A-Za-z0-9._:-]+$")
private fun JSONObject.text(key: String): String = getString(key).trim().also { require(it.isNotEmpty()) { "$key 不能为空" } }
private fun JSONObject.optionalText(key: String): String? = optString(key).trim().takeIf(String::isNotEmpty)
private fun JSONObject.identifier(key: String): String = text(key).also { require(IDENTIFIER.matches(it)) { "$key 不是合法 ID：$it" } }
private fun JSONObject.positiveInt(key: String): Int = getInt(key).also { require(it > 0) { "$key 必须是正整数" } }
private fun JSONObject.objectValue(key: String): JSONObject = getJSONObject(key)
private fun JSONObject.arrayValue(key: String): JSONArray = getJSONArray(key)
private fun JSONObject.stringArray(key: String): List<String> = arrayValue(key).let { array -> List(array.length()) { array.getString(it).trim() }.also { values -> require(values.all(String::isNotEmpty)) { "$key 包含空字符串" } } }
private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }
private fun JSONObject.requireKeys(required: Set<String>) {
    val actual = keys().asSequence().toSet()
    require(actual == required) { "字段不匹配：expected=${required.sorted()} actual=${actual.sorted()}" }
}
private fun JSONObject.toMap(): Map<String, Any?> = keys().asSequence().associateWith { key ->
    when (val value = get(key)) {
        JSONObject.NULL -> null
        is JSONObject -> value.toMap()
        is JSONArray -> List(value.length()) { index -> when (val item = value.get(index)) { JSONObject.NULL -> null; is JSONObject -> item.toMap(); else -> item } }
        else -> value
    }
}
