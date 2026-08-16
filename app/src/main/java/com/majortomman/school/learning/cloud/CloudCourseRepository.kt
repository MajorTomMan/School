package com.majortomman.school.learning.cloud

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
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

object CloudCourseRepository {
    private val mutableRevision = MutableStateFlow(0L)
    private val revisionCounter = AtomicLong(0L)
    private var course: CourseDocument? = null

    val revision: StateFlow<Long> = mutableRevision.asStateFlow()

    fun load(file: File) {
        val parsed = CourseDocumentParser.decode(file.readText(Charsets.UTF_8))
        course = parsed
        mutableRevision.value = revisionCounter.incrementAndGet()
    }

    fun clear() {
        course = null
        mutableRevision.value = revisionCounter.incrementAndGet()
    }

    fun current(): CourseDocument? = course

    fun lessonFor(id: String, title: String): CourseLesson? {
        val document = course ?: return null
        val lessons = document.chapters.flatMap(CourseChapter::sections).flatMap(CourseSection::lessons)
        return lessons.firstOrNull { it.id == id } ?: lessons.firstOrNull { lesson -> lesson.title == title || title in lesson.aliases }
    }
}

internal object CourseDocumentParser {
    private val identifierPattern = Regex("[a-z0-9][a-z0-9._-]{0,95}")
    private val latexForbidden = Regex("[²³⁴⁵⁶⁷⁸⁹⁰¹₀₁₂₃₄₅₆₇₈₉×÷√≤≥≠≈∞∑∏∫]")

    fun decode(raw: String): CourseDocument {
        val root = JSONObject(raw)
        root.requireKeys(setOf("textbook", "knowledgePoints", "chapters"))
        val textbook = decodeTextbook(root.objectValue("textbook"))
        val knowledgePoints = decodeKnowledgePoints(root.arrayValue("knowledgePoints"))
        val knowledgeIds = knowledgePoints.map(CourseKnowledgePoint::id).toSet()
        val chapters = decodeChapters(root.arrayValue("chapters"), knowledgeIds)
        val document = CourseDocument(textbook, knowledgePoints, chapters)
        validateDocument(document)
        return document
    }

    private fun decodeTextbook(json: JSONObject): CourseTextbook {
        json.requireKeys(setOf("id", "title", "publisher", "edition", "grade", "semester", "subject", "pdf"))
        val pdf = json.objectValue("pdf")
        pdf.requireKeys(setOf("path", "pageCount", "pageIndexOffset"))
        return CourseTextbook(
            id = json.identifier("id"),
            title = json.text("title"),
            publisher = json.text("publisher"),
            edition = json.text("edition"),
            grade = json.text("grade"),
            semester = json.text("semester"),
            subject = json.text("subject"),
            pdf = CoursePdf(pdf.text("path"), pdf.strictInt("pageCount"), pdf.strictInt("pageIndexOffset")),
        )
    }

    private fun decodeKnowledgePoints(array: JSONArray): List<CourseKnowledgePoint> {
        val ids = mutableSetOf<String>()
        return array.objects("knowledgePoints").mapIndexed { index, json ->
            json.requireKeys(setOf("id", "name", "description", "prerequisiteIds"))
            val id = json.identifier("id")
            require(ids.add(id)) { "知识点 ID 重复：$id" }
            CourseKnowledgePoint(id, json.text("name"), json.text("description"), json.stringArray("prerequisiteIds"))
        }.also { points ->
            val all = points.map(CourseKnowledgePoint::id).toSet()
            points.forEach { point -> require(point.prerequisiteIds.all { it in all && it != point.id }) { "知识点 ${point.id} 的前置关系无效" } }
        }
    }

    private fun decodeChapters(array: JSONArray, knowledgeIds: Set<String>): List<CourseChapter> {
        val chapterIds = mutableSetOf<String>()
        val sectionIds = mutableSetOf<String>()
        val lessonIds = mutableSetOf<String>()
        val practiceIds = mutableSetOf<String>()
        return array.objects("chapters").map { chapter ->
            chapter.requireKeys(setOf("id", "title", "sections"))
            val chapterId = chapter.identifier("id")
            require(chapterIds.add(chapterId)) { "章节 ID 重复：$chapterId" }
            val sections = chapter.arrayValue("sections").objects("sections").map { section ->
                section.requireKeys(setOf("id", "title", "lessons"))
                val sectionId = section.identifier("id")
                require(sectionIds.add(sectionId)) { "小节 ID 重复：$sectionId" }
                val lessons = section.arrayValue("lessons").objects("lessons").map { lesson ->
                    decodeLesson(lesson, lessonIds, practiceIds, knowledgeIds)
                }
                require(lessons.isNotEmpty()) { "小节 $sectionId 不能没有课时" }
                CourseSection(sectionId, section.text("title"), lessons)
            }
            require(sections.isNotEmpty()) { "章节 $chapterId 不能没有小节" }
            CourseChapter(chapterId, chapter.text("title"), sections)
        }.also { require(it.isNotEmpty()) { "课程必须至少包含一个章节" } }
    }

    private fun decodeLesson(json: JSONObject, lessonIds: MutableSet<String>, practiceIds: MutableSet<String>, knowledgeIds: Set<String>): CourseLesson {
        json.requireKeys(setOf("id", "title", "aliases", "goals", "knowledgePointIds", "prerequisiteLessonIds", "references", "steps", "practice", "summary"))
        val id = json.identifier("id")
        require(lessonIds.add(id)) { "课时 ID 重复：$id" }
        val pointIds = json.stringArray("knowledgePointIds")
        require(pointIds.isNotEmpty() && pointIds.all { it in knowledgeIds }) { "课时 $id 的知识点绑定无效" }
        val steps = json.arrayValue("steps").objects("steps").mapIndexed { index, step -> decodeStep(step, "$id.steps[$index]") }
        require(steps.isNotEmpty()) { "课时 $id 必须包含教学步骤" }
        val practices = json.arrayValue("practice").objects("practice").map { decodePractice(it, id, knowledgeIds, practiceIds) }
        val summary = json.stringArray("summary")
        require(summary.isNotEmpty()) { "课时 $id 必须包含总结" }
        return CourseLesson(
            id = id,
            title = json.text("title"),
            aliases = json.stringArray("aliases"),
            goals = json.stringArray("goals"),
            knowledgePointIds = pointIds,
            prerequisiteLessonIds = json.stringArray("prerequisiteLessonIds"),
            references = json.arrayValue("references").objects("references").map { reference ->
                reference.requireKeys(setOf("label", "pageStart", "pageEnd"))
                CourseSourceReference(reference.text("label"), reference.strictInt("pageStart"), reference.strictInt("pageEnd"))
            },
            steps = steps,
            practice = practices,
            summary = summary,
        )
    }

    private fun decodeStep(json: JSONObject, location: String): CourseStep = when (val type = json.text("type")) {
        "question" -> {
            json.requireKeys(setOf("type", "prompt", "hint"))
            CourseQuestion(json.text("prompt"), json.optionalText("hint"))
        }
        "explanation" -> {
            json.requireKeys(setOf("type", "title", "text"))
            CourseExplanation(json.optionalText("title"), json.text("text"))
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
                        require(item is Number && item !is Boolean) { "$location.parameters.$key 只能是数值列表" }
                        item.toDouble()
                    }
                    VisualizationParameterValue.NumberListValue(numbers)
                }
                is String -> VisualizationParameterValue.MathExpressionValue.parse(raw)
                else -> error("$location.parameters.$key 只接受 number、boolean、number[] 或受限数学表达式")
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
        val difficulty = json.strictInt("difficulty")
        require(difficulty in 1..5) { "练习难度必须在 1..5" }
        return CoursePractice(id, json.text("prompt"), json.text("answer"), analysis, ids, difficulty)
    }

    private fun validateDocument(document: CourseDocument) {
        val knowledgeIds = document.knowledgePoints.map(CourseKnowledgePoint::id).toSet()
        document.knowledgePoints.forEach { point -> require(point.prerequisiteIds.all { it in knowledgeIds }) }
        requireAcyclic(document.knowledgePoints.associate { it.id to it.prerequisiteIds }, "知识点")
        val lessons = document.chapters.flatMap(CourseChapter::sections).flatMap(CourseSection::lessons)
        val lessonIds = lessons.map(CourseLesson::id).toSet()
        lessons.forEach { lesson -> require(lesson.prerequisiteLessonIds.all { it in lessonIds && it != lesson.id }) { "课时 ${lesson.id} 的前置课时无效" } }
        requireAcyclic(lessons.associate { it.id to it.prerequisiteLessonIds }, "课时")
    }

    private fun requireAcyclic(graph: Map<String, List<String>>, label: String) {
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        fun visit(node: String) {
            if (node in visited) return
            require(visiting.add(node)) { "$label 前置关系存在循环：$node" }
            graph[node].orEmpty().forEach(::visit)
            visiting.remove(node)
            visited.add(node)
        }
        graph.keys.forEach(::visit)
    }

    private fun requirePureLatex(expression: String, location: String) {
        require(!latexForbidden.containsMatchIn(expression)) { "$location.expression 必须使用标准 LaTeX，不能混用 Unicode 数学符号" }
    }

    private fun JSONObject.requireKeys(expected: Set<String>) {
        val actual = keys().asSequence().toSet()
        require(actual == expected) { "课程字段不符合当前契约：expected=${expected.sorted()} actual=${actual.sorted()}" }
    }

    private fun JSONObject.objectValue(name: String): JSONObject = get(name) as? JSONObject ?: error("$name 必须是对象")
    private fun JSONObject.arrayValue(name: String): JSONArray = get(name) as? JSONArray ?: error("$name 必须是数组")
    private fun JSONObject.text(name: String): String = (get(name) as? String)?.trim()?.takeIf(String::isNotBlank) ?: error("$name 必须是非空字符串")
    private fun JSONObject.optionalText(name: String): String? = if (isNull(name)) null else (get(name) as? String)?.trim()?.takeIf(String::isNotBlank)
    private fun JSONObject.identifier(name: String): String = text(name).also { require(identifierPattern.matches(it)) { "$name 格式无效：$it" } }
    private fun JSONObject.strictInt(name: String): Int {
        val value = get(name)
        require(value is Int || value is Long) { "$name 必须是 JSON 整数" }
        val longValue = (value as Number).toLong()
        require(longValue in Int.MIN_VALUE..Int.MAX_VALUE) { "$name 超出 Int 范围" }
        return longValue.toInt()
    }
    private fun JSONObject.stringArray(name: String): List<String> = arrayValue(name).let { array ->
        List(array.length()) { index -> (array.get(index) as? String)?.trim()?.takeIf(String::isNotBlank) ?: error("$name[$index] 必须是非空字符串") }
    }
    private fun JSONArray.objects(name: String): List<JSONObject> = List(length()) { index -> get(index) as? JSONObject ?: error("$name[$index] 必须是对象") }
}
