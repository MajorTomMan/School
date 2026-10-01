package com.majortomman.school.learning.cloud

import com.majortomman.school.learning.activity.ActivityId
import com.majortomman.school.learning.activity.ActivitySpec
import com.majortomman.school.learning.capability.CapabilityKey
import com.majortomman.school.learning.assessment.domain.Difficulty
import com.majortomman.school.learning.assessment.domain.InlineAssessmentRule
import com.majortomman.school.learning.assessment.domain.InlineAssessmentSpec
import com.majortomman.school.learning.content.LearningContent
import com.majortomman.school.learning.content.LearningContentParser
import com.majortomman.school.learning.course.CourseChapter
import com.majortomman.school.learning.course.CourseDocument
import com.majortomman.school.learning.course.CourseKnowledgePoint
import com.majortomman.school.learning.course.CourseLesson
import com.majortomman.school.learning.course.CoursePdf
import com.majortomman.school.learning.course.CourseSection
import com.majortomman.school.learning.course.CourseSourceReference
import com.majortomman.school.learning.course.CourseStep
import com.majortomman.school.learning.course.CourseStepRole
import com.majortomman.school.learning.course.CourseTextbook
import org.json.JSONArray
import org.json.JSONObject

internal object CourseDocumentParser {
    fun decode(raw: String): CourseDocument = decode(JSONObject(raw))

    fun decode(root: JSONObject): CourseDocument {
        root.requireShape(required = setOf("textbook", "knowledgePoints", "chapters"))
        val textbook = decodeTextbook(root.objectValue("textbook"))
        val knowledgePoints = root.arrayValue("knowledgePoints").objects().map(::decodeKnowledgePoint)
        require(knowledgePoints.isNotEmpty()) { "课程必须声明知识点" }
        require(knowledgePoints.map { it.id }.distinct().size == knowledgePoints.size) { "知识点 ID 不能重复" }
        requireKnowledgeGraph(knowledgePoints)

        val knowledgeIds = knowledgePoints.map { it.id }.toSet()
        val lessonIds = linkedSetOf<String>()
        val stepIds = linkedSetOf<String>()
        val activityIds = linkedSetOf<String>()
        val chapters = root.arrayValue("chapters").objects().map {
            decodeChapter(it, textbook.pdf, knowledgeIds, lessonIds, stepIds, activityIds)
        }
        require(chapters.isNotEmpty()) { "课程必须包含章节" }

        val allLessons = chapters.flatMap { it.sections }.flatMap { it.lessons }
        val missingPrerequisites = allLessons.flatMap { lesson ->
            lesson.prerequisiteLessonIds.map { lesson.id to it }
        }.filter { (_, prerequisite) -> prerequisite !in lessonIds }
        require(missingPrerequisites.isEmpty()) { "课程包含不存在的前置课时：$missingPrerequisites" }
        requireLessonGraph(allLessons)
        return CourseDocument(textbook, knowledgePoints, chapters)
    }

    private fun decodeTextbook(json: JSONObject): CourseTextbook {
        json.requireShape(required = setOf("id", "title", "publisher", "edition", "grade", "semester", "subject", "pdf"))
        val pdfJson = json.objectValue("pdf")
        pdfJson.requireShape(required = setOf("path", "pageCount", "pageIndexOffset"))
        val path = pdfJson.text("path")
        require(path.endsWith(".pdf", true) && !path.startsWith('/') && ".." !in path.split('/')) { "教材 PDF 路径无效" }
        return CourseTextbook(
            json.identifier("id"),
            json.text("title"),
            json.text("publisher"),
            json.text("edition"),
            json.text("grade"),
            json.text("semester"),
            json.text("subject"),
            CoursePdf(path, pdfJson.positiveInt("pageCount"), pdfJson.strictInt("pageIndexOffset")),
        )
    }

    private fun decodeKnowledgePoint(json: JSONObject): CourseKnowledgePoint {
        json.requireShape(required = setOf("id", "name", "description", "prerequisiteIds"))
        return CourseKnowledgePoint(json.identifier("id"), json.text("name"), json.text("description"), json.stringArray("prerequisiteIds"))
    }

    private fun decodeChapter(
        json: JSONObject,
        pdf: CoursePdf,
        knowledgeIds: Set<String>,
        lessonIds: MutableSet<String>,
        stepIds: MutableSet<String>,
        activityIds: MutableSet<String>,
    ): CourseChapter {
        json.requireShape(required = setOf("id", "title", "sections"))
        val sections = json.arrayValue("sections").objects().map {
            decodeSection(it, pdf, knowledgeIds, lessonIds, stepIds, activityIds)
        }
        require(sections.isNotEmpty()) { "章节 ${json.optString("id")} 不包含小节" }
        return CourseChapter(json.identifier("id"), json.text("title"), sections)
    }

    private fun decodeSection(
        json: JSONObject,
        pdf: CoursePdf,
        knowledgeIds: Set<String>,
        lessonIds: MutableSet<String>,
        stepIds: MutableSet<String>,
        activityIds: MutableSet<String>,
    ): CourseSection {
        json.requireShape(required = setOf("id", "title", "lessons"))
        val lessons = json.arrayValue("lessons").objects().map {
            decodeLesson(it, pdf, knowledgeIds, lessonIds, stepIds, activityIds)
        }
        require(lessons.isNotEmpty()) { "小节 ${json.optString("id")} 不包含课时" }
        return CourseSection(json.identifier("id"), json.text("title"), lessons)
    }

    private fun decodeLesson(
        json: JSONObject,
        pdf: CoursePdf,
        knowledgeIds: Set<String>,
        lessonIds: MutableSet<String>,
        stepIds: MutableSet<String>,
        activityIds: MutableSet<String>,
    ): CourseLesson {
        json.requireShape(
            required = setOf("id", "title", "aliases", "goals", "knowledgePointIds", "prerequisiteLessonIds", "references", "steps"),
        )
        val id = json.identifier("id")
        require(lessonIds.add(id)) { "课时 ID 重复：$id" }
        val lessonKnowledge = json.stringArray("knowledgePointIds")
        require(lessonKnowledge.isNotEmpty() && lessonKnowledge.all { it in knowledgeIds }) { "课时 $id 的知识点绑定无效" }
        val references = json.arrayValue("references").objects().map { decodeReference(it, pdf, id) }
        val steps = json.arrayValue("steps").objects().mapIndexed { index, item ->
            decodeStep(item, "$id.steps[$index]", knowledgeIds, stepIds, activityIds)
        }
        require(steps.isNotEmpty()) { "课时 $id 不包含教学步骤" }
        val goals = json.stringArray("goals")
        require(goals.isNotEmpty()) { "课时 $id 必须声明教学目标" }
        return CourseLesson(
            id = id,
            title = json.text("title"),
            aliases = json.stringArray("aliases"),
            goals = goals,
            knowledgePointIds = lessonKnowledge,
            prerequisiteLessonIds = json.stringArray("prerequisiteLessonIds"),
            references = references,
            steps = steps,
        )
    }

    private fun decodeReference(json: JSONObject, pdf: CoursePdf, lessonId: String): CourseSourceReference {
        json.requireShape(required = setOf("label", "pageStart", "pageEnd"))
        val start = json.positiveInt("pageStart")
        val end = json.positiveInt("pageEnd")
        require(start <= end && end <= pdf.pageCount) { "课时 $lessonId 的教材引用页码无效" }
        return CourseSourceReference(json.text("label"), start, end)
    }

    private fun decodeStep(
        json: JSONObject,
        location: String,
        knowledgeIds: Set<String>,
        stepIds: MutableSet<String>,
        activityIds: MutableSet<String>,
    ): CourseStep {
        json.requireShape(
            required = setOf("id", "role", "content"),
            optional = setOf("title", "activity", "assessment"),
        )
        val id = json.identifier("id")
        require(stepIds.add(id)) { "step ID 重复：$id" }
        val role = when (val wire = json.text("role")) {
            "explanation" -> CourseStepRole.EXPLANATION
            "inquiry" -> CourseStepRole.INQUIRY
            "example" -> CourseStepRole.EXAMPLE
            "keyIdea" -> CourseStepRole.KEY_IDEA
            "practice" -> CourseStepRole.PRACTICE
            "checkpoint" -> CourseStepRole.CHECKPOINT
            "summary" -> CourseStepRole.SUMMARY
            else -> error("$location.role 不受支持：$wire")
        }
        val content = LearningContentParser.decodeArray(json.arrayValue("content"), "$location.content", allowEmpty = true)
        content.filterIsInstance<LearningContent.Formula>().forEach { requirePureLatex(it.expression, "$location.content") }
        require(content.none { it is LearningContent.Image }) { "$location 暂不允许 image；课程图片需要正式 asset catalog 后再启用" }
        val activity = json.optionalObject("activity")?.let { decodeActivity(it, "$location.activity", activityIds) }
        val assessment = json.optionalObject("assessment")?.let {
            decodeInlineAssessment(it, "$location.assessment", knowledgeIds)
        }
        return CourseStep(id, role, json.optionalText("title"), content, activity, assessment)
    }

    private fun decodeActivity(json: JSONObject, location: String, activityIds: MutableSet<String>): ActivitySpec {
        json.requireShape(required = setOf("id", "capability", "schemaVersion", "parameters"))
        val id = json.identifier("id")
        require(activityIds.add(id)) { "activity ID 重复：$id" }
        return CourseActivitySpecCatalog.decode(
            id = ActivityId(id),
            capability = CapabilityKey(json.text("capability")),
            schemaVersion = json.positiveInt("schemaVersion"),
            parameters = json.objectValue("parameters"),
        )
    }

    private fun decodeInlineAssessment(
        json: JSONObject,
        location: String,
        knowledgeIds: Set<String>,
    ): InlineAssessmentSpec {
        val type = json.text("type")
        val commonRequired = setOf("type", "explanation", "knowledgePointIds", "difficulty")
        val rule = when (type) {
            "exactText" -> {
                json.requireShape(required = commonRequired + setOf("expected", "ignoreCase"))
                InlineAssessmentRule.ExactText(json.text("expected"), json.booleanValue("ignoreCase"))
            }
            "exactNumber" -> {
                json.requireShape(required = commonRequired + setOf("expected", "tolerance"))
                InlineAssessmentRule.ExactNumber(
                    expected = json.doubleValue("expected"),
                    tolerance = json.doubleValue("tolerance"),
                )
            }
            else -> error("$location.type 不受支持：$type")
        }
        val ids = json.stringArray("knowledgePointIds")
        require(ids.all { it in knowledgeIds }) { "$location.knowledgePointIds 包含不存在的知识点" }
        return InlineAssessmentSpec(
            rule = rule,
            explanation = LearningContentParser.decodeArray(json.arrayValue("explanation"), "$location.explanation", allowEmpty = false),
            knowledgePointIds = ids,
            difficulty = Difficulty(json.doubleValue("difficulty")),
        )
    }

    private fun requireKnowledgeGraph(points: List<CourseKnowledgePoint>) {
        val ids = points.map { it.id }.toSet()
        points.forEach { point -> require(point.prerequisiteIds.all { it in ids }) { "知识点 ${point.id} 引用了不存在的前置知识" } }
        val prerequisites = points.associate { it.id to it.prerequisiteIds }
        requireAcyclic(ids, prerequisites, "知识点前置关系形成循环")
    }

    private fun requireLessonGraph(lessons: List<CourseLesson>) {
        val prerequisites = lessons.associate { it.id to it.prerequisiteLessonIds }
        requireAcyclic(prerequisites.keys, prerequisites, "课时前置关系形成循环")
    }

    private fun requireAcyclic(ids: Set<String>, prerequisites: Map<String, List<String>>, message: String) {
        val visiting = linkedSetOf<String>()
        val visited = linkedSetOf<String>()
        fun visit(id: String) {
            if (id in visited) return
            require(id !in visiting) { "$message：${(visiting + id).joinToString(" -> ")}" }
            visiting += id
            prerequisites.getValue(id).forEach(::visit)
            visiting -= id
            visited += id
        }
        ids.forEach(::visit)
    }

    private fun requirePureLatex(expression: String, location: String) {
        require('$' !in expression && "\\(" !in expression && "\\)" !in expression && "\\[" !in expression && "\\]" !in expression) {
            "$location formula 必须保存不带定界符的纯 LaTeX 数学表达式"
        }
        require(!CJK.containsMatchIn(expression)) { "$location formula 不能包含中文说明文字" }
        require(expression.none { it in NON_LATEX_MATH }) { "$location formula 必须使用 LaTeX 命令而不是 Unicode 数学符号" }
    }
}

private val IDENTIFIER = Regex("^[A-Za-z0-9._:-]+$")
private val CJK = Regex("[\\u3400-\\u9fff]")
private val NON_LATEX_MATH = "²³⁴⁵⁶⁷⁸⁹₀₁₂₃₄₅₆₇₈₉−×÷≤≥≠Σαβγθπ°′″".toSet()

private fun JSONObject.text(key: String): String = getString(key).trim().also { require(it.isNotEmpty()) { "$key 不能为空" } }

private fun JSONObject.optionalText(key: String): String? {
    if (!has(key) || isNull(key)) return null
    require(get(key) is String) { "$key 必须是字符串" }
    return getString(key).trim().also { require(it.isNotEmpty()) { "$key 不能是空字符串" } }
}

private fun JSONObject.identifier(key: String): String = text(key).also { require(IDENTIFIER.matches(it)) { "$key 不是合法 ID：$it" } }

private fun JSONObject.strictInt(key: String): Int {
    val raw = get(key)
    require(raw is Byte || raw is Short || raw is Int || raw is Long) { "$key 必须是 JSON 整数" }
    val value = (raw as Number).toLong()
    require(value in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) { "$key 超出 Int 范围" }
    return value.toInt()
}

private fun JSONObject.positiveInt(key: String): Int = strictInt(key).also { require(it > 0) { "$key 必须是正整数" } }

private fun JSONObject.doubleValue(key: String): Double {
    val raw = get(key)
    require(raw is Number && raw !is Boolean) { "$key 必须是 JSON number" }
    return raw.toDouble().also { require(it.isFinite()) { "$key 必须是有限数" } }
}

private fun JSONObject.booleanValue(key: String): Boolean {
    val raw = get(key)
    require(raw is Boolean) { "$key 必须是布尔值" }
    return raw
}

private fun JSONObject.objectValue(key: String): JSONObject = getJSONObject(key)
private fun JSONObject.optionalObject(key: String): JSONObject? = if (!has(key) || isNull(key)) null else getJSONObject(key)
private fun JSONObject.arrayValue(key: String): JSONArray = getJSONArray(key)
private fun JSONObject.stringArray(key: String): List<String> = arrayValue(key).let { array ->
    List(array.length()) { array.getString(it).trim() }.also { values ->
        require(values.all(String::isNotEmpty)) { "$key 包含空字符串" }
    }
}
private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }

private fun JSONObject.requireShape(required: Set<String>, optional: Set<String> = emptySet()) {
    val actual = keys().asSequence().toSet()
    val unknown = actual - required - optional
    val missing = required - actual
    require(unknown.isEmpty()) { "包含未知字段：${unknown.sorted()}" }
    require(missing.isEmpty()) { "缺少字段：${missing.sorted()}" }
}
