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
import com.majortomman.school.learning.course.CourseSection
import com.majortomman.school.learning.course.CourseSourceReference
import com.majortomman.school.learning.course.CourseStep
import com.majortomman.school.learning.course.CourseSummaryStep
import com.majortomman.school.learning.course.CourseTextbook
import com.majortomman.school.learning.course.CourseVisualizationStep
import com.majortomman.school.visualization.SchoolVisualizationCatalog
import com.majortomman.school.visualization.VisualizationInvocation
import com.majortomman.school.visualization.VisualizationKey
import com.majortomman.school.visualization.VisualizationParameterValue
import com.majortomman.school.visualization.VisualizationParameters
import com.majortomman.school.visualization.VisualizationTexts
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
        val practiceIds = linkedSetOf<String>()
        val chapters = root.arrayValue("chapters").objects().map { decodeChapter(it, textbook.pdf, knowledgeIds, lessonIds, practiceIds) }
        require(chapters.isNotEmpty()) { "课程必须包含章节" }
        val allLessons = chapters.flatMap { it.sections }.flatMap { it.lessons }
        val missingPrerequisites = allLessons.flatMap { lesson -> lesson.prerequisiteLessonIds.map { lesson.id to it } }.filter { (_, prerequisite) -> prerequisite !in lessonIds }
        require(missingPrerequisites.isEmpty()) { "课程包含不存在的前置课时：$missingPrerequisites" }
        requireLessonGraph(allLessons)
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

    private fun decodeChapter(json: JSONObject, pdf: CoursePdf, knowledgeIds: Set<String>, lessonIds: MutableSet<String>, practiceIds: MutableSet<String>): CourseChapter {
        json.requireKeys(setOf("id", "title", "sections"))
        val sections = json.arrayValue("sections").objects().map { decodeSection(it, pdf, knowledgeIds, lessonIds, practiceIds) }
        require(sections.isNotEmpty()) { "章节 ${json.optString("id")} 不包含小节" }
        return CourseChapter(json.identifier("id"), json.text("title"), sections)
    }

    private fun decodeSection(json: JSONObject, pdf: CoursePdf, knowledgeIds: Set<String>, lessonIds: MutableSet<String>, practiceIds: MutableSet<String>): CourseSection {
        json.requireKeys(setOf("id", "title", "lessons"))
        val lessons = json.arrayValue("lessons").objects().map { decodeLesson(it, pdf, knowledgeIds, lessonIds, practiceIds) }
        require(lessons.isNotEmpty()) { "小节 ${json.optString("id")} 不包含课时" }
        return CourseSection(json.identifier("id"), json.text("title"), lessons)
    }

    private fun decodeLesson(json: JSONObject, pdf: CoursePdf, knowledgeIds: Set<String>, lessonIds: MutableSet<String>, practiceIds: MutableSet<String>): CourseLesson {
        json.requireKeys(setOf("id", "title", "aliases", "goals", "knowledgePointIds", "prerequisiteLessonIds", "references", "steps", "practice", "summary"))
        val id = json.identifier("id")
        require(lessonIds.add(id)) { "课时 ID 重复：$id" }
        val lessonKnowledge = json.stringArray("knowledgePointIds")
        require(lessonKnowledge.isNotEmpty() && lessonKnowledge.all { it in knowledgeIds }) { "课时 $id 的知识点绑定无效" }
        val references = json.arrayValue("references").objects().map { decodeReference(it, pdf, id) }
        val steps = json.arrayValue("steps").objects().mapIndexed { index, item -> decodeStep(item, "$id.steps[$index]") }
        require(steps.isNotEmpty()) { "课时 $id 不包含教学步骤" }
        val practice = json.arrayValue("practice").objects().map { decodePractice(it, id, knowledgeIds, practiceIds) }
        return CourseLesson(id, json.text("title"), json.stringArray("aliases"), json.stringArray("goals").also { require(it.isNotEmpty()) { "课时 $id 必须声明教学目标" } }, lessonKnowledge, json.stringArray("prerequisiteLessonIds"), references, steps, practice, json.stringArray("summary").also { require(it.isNotEmpty()) { "课时 $id 必须有总结" } })
    }

    private fun decodeReference(json: JSONObject, pdf: CoursePdf, lessonId: String): CourseSourceReference {
        json.requireKeys(setOf("label", "pageStart", "pageEnd"))
        val start = json.positiveInt("pageStart")
        val end = json.positiveInt("pageEnd")
        require(start <= end && end <= pdf.pageCount) { "课时 $lessonId 的教材引用页码无效" }
        return CourseSourceReference(json.text("label"), start, end)
    }

    private fun decodeStep(json: JSONObject, location: String): CourseStep = when (val type = json.text("type")) {
        "explanation" -> {
            json.requireKeys(setOf("type", "title", "text"))
            CourseExplanation(json.optionalText("title"), json.text("text"))
        }
        "question" -> {
            json.requireKeys(setOf("type", "prompt", "hint"))
            CourseQuestion(json.text("prompt"), json.optionalText("hint"))
        }
        "keyIdea" -> {
            json.requireKeys(setOf("type", "title", "text"))
            CourseKeyIdea(json.optionalText("title"), json.text("text"))
        }
        "formula" -> {
            json.requireKeys(setOf("type", "expression", "note"))
            val expression = json.text("expression")
            requirePureLatex(expression, location)
            CourseFormula(expression, json.optionalText("note"))
        }
        "example" -> {
            json.requireKeys(setOf("type", "title", "prompt", "steps", "answer"))
            val steps = json.stringArray("steps")
            require(steps.isNotEmpty()) { "$location.steps 不能为空" }
            CourseExample(json.text("title"), json.text("prompt"), steps, json.text("answer"))
        }
        "visualization" -> decodeVisualization(json, location)
        "checkpoint" -> {
            json.requireKeys(setOf("type", "prompt", "expectedAnswer", "explanation"))
            CourseCheckpoint(json.text("prompt"), json.text("expectedAnswer"), json.text("explanation"))
        }
        "summary" -> {
            json.requireKeys(setOf("type", "text"))
            CourseSummaryStep(json.text("text"))
        }
        else -> error("$location 使用了不支持的教学步骤：$type")
    }

    private fun decodeVisualization(json: JSONObject, location: String): CourseVisualizationStep {
        json.requireKeys(setOf("type", "renderer", "parameters", "texts"))
        val renderer = VisualizationKey(json.text("renderer"))
        val parameters = decodeVisualizationParameters(json.objectValue("parameters"), location)
        val texts = decodeVisualizationTexts(json.objectValue("texts"), location)
        val invocation = VisualizationInvocation(renderer, parameters, texts)
        val issues = SchoolVisualizationCatalog.validate(invocation)
        require(issues.isEmpty()) { "$location 可视化参数无效：${issues.joinToString("；")}" }
        return CourseVisualizationStep(invocation)
    }

    private fun decodeVisualizationParameters(json: JSONObject, location: String): VisualizationParameters {
        val values = linkedMapOf<String, VisualizationParameterValue>()
        json.keys().asSequence().forEach { key ->
            val raw = json.get(key)
            values[key] = when (raw) {
                is Number -> VisualizationParameterValue.NumberValue(raw.toDouble())
                is Boolean -> VisualizationParameterValue.BooleanValue(raw)
                is JSONArray -> {
                    val numbers = List(raw.length()) { index ->
                        val item = raw.get(index)
                        require(item is Number) { "$location.parameters.$key 只能是数值列表" }
                        item.toDouble()
                    }
                    VisualizationParameterValue.NumberListValue(numbers)
                }
                else -> error("$location.parameters.$key 只接受 number、boolean 或 number[]")
            }
        }
        return VisualizationParameters.of(values)
    }

    private fun decodeVisualizationTexts(json: JSONObject, location: String): VisualizationTexts {
        val values = linkedMapOf<String, String>()
        json.keys().asSequence().forEach { key ->
            val raw = json.get(key)
            require(raw is String) { "$location.texts.$key 只能是字符串" }
            values[key] = raw
        }
        return VisualizationTexts.of(values)
    }

    private fun decodePractice(json: JSONObject, lessonId: String, knowledgeIds: Set<String>, practiceIds: MutableSet<String>): CoursePractice {
        json.requireKeys(setOf("id", "prompt", "answer", "analysis", "knowledgePointIds", "difficulty"))
        val id = json.identifier("id")
        require(practiceIds.add(id)) { "练习 ID 重复：$id" }
        val analysis = json.stringArray("analysis")
        require(analysis.isNotEmpty()) { "课时 $lessonId 的练习 $id 必须包含解析" }
        val ids = json.stringArray("knowledgePointIds")
        require(ids.isNotEmpty() && ids.all { it in knowledgeIds }) { "课时 $lessonId 的练习知识点绑定无效" }
        val difficulty = json.getInt("difficulty")
        require(difficulty in 1..5) { "练习难度必须在 1..5" }
        return CoursePractice(id, json.text("prompt"), json.text("answer"), analysis, ids, difficulty)
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

    private fun requireLessonGraph(lessons: List<CourseLesson>) {
        val prerequisites = lessons.associate { it.id to it.prerequisiteLessonIds }
        val visiting = linkedSetOf<String>()
        val visited = linkedSetOf<String>()
        fun visit(id: String) {
            if (id in visited) return
            require(id !in visiting) { "课时前置关系形成循环：${(visiting + id).joinToString(" -> ")}" }
            visiting += id
            prerequisites.getValue(id).forEach(::visit)
            visiting -= id
            visited += id
        }
        prerequisites.keys.forEach(::visit)
    }

    private fun requirePureLatex(expression: String, location: String) {
        require('$' !in expression && "\\(" !in expression && "\\)" !in expression && "\\[" !in expression && "\\]" !in expression) { "$location.expression 必须保存不带定界符的纯 LaTeX 数学表达式" }
        require(!CJK.containsMatchIn(expression)) { "$location.expression 不能包含中文说明文字" }
        require(expression.none { it in NON_LATEX_MATH }) { "$location.expression 必须使用 LaTeX 命令而不是 Unicode 数学符号" }
    }
}

private val IDENTIFIER = Regex("^[A-Za-z0-9._:-]+$")
private val CJK = Regex("[\\u3400-\\u9fff]")
private val NON_LATEX_MATH = "²³⁴⁵⁶⁷⁸⁹₀₁₂₃₄₅₆₇₈₉−×÷≤≥≠Σαβγθπ°′″".toSet()
private fun JSONObject.text(key: String): String = getString(key).trim().also { require(it.isNotEmpty()) { "$key 不能为空" } }
private fun JSONObject.optionalText(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return getString(key).trim().takeIf(String::isNotEmpty)
}
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
