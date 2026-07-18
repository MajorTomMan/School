package com.majortomman.school.learning.course

import com.majortomman.school.learning.capability.ConceptId
import com.majortomman.school.learning.capability.ContentOrigin
import com.majortomman.school.learning.capability.ExtensionPolicy
import com.majortomman.school.learning.capability.LessonCapability
import com.majortomman.school.learning.capability.OperationId
import com.majortomman.school.learning.capability.WidgetType
import org.json.JSONArray
import org.json.JSONObject

enum class CourseVisualizationKind {
    NUMBER_LINE,
    CARTESIAN_GRAPH,
    GEOMETRY_2D,
    GEOMETRY_3D,
    VECTOR,
    MOTION,
    FORCE_DIAGRAM,
    CIRCUIT,
    WAVE,
    PARTICLE_MODEL,
    CHEMICAL_EQUATION,
    MOLECULE,
    CELL,
    BIOLOGICAL_PROCESS,
    DATA_TABLE,
    TEXT_STRUCTURE,
    LANGUAGE_STRUCTURE,
    PROCESS,
}

enum class CourseParameterKind {
    NUMBER,
    INTEGER,
    BOOLEAN,
    CHOICE,
    TEXT,
}

data class CourseParameterSpec(
    val id: String,
    val label: String,
    val kind: CourseParameterKind,
    val defaultValue: String,
    val unit: String = "",
    val minimum: Double? = null,
    val maximum: Double? = null,
    val step: Double? = null,
    val choices: List<String> = emptyList(),
    val explanation: String = "",
) {
    fun validationErrors(): List<String> = buildList {
        if (!PARAMETER_ID.matches(id)) add("参数 id 只能使用小写字母、数字、下划线和短横线：$id")
        if (label.isBlank()) add("参数 $id 缺少显示名称")
        if (defaultValue.isBlank() && kind != CourseParameterKind.TEXT) add("参数 $id 缺少默认值")
        if (minimum != null && !minimum.isFinite()) add("参数 $id 的最小值不是有限数")
        if (maximum != null && !maximum.isFinite()) add("参数 $id 的最大值不是有限数")
        if (minimum != null && maximum != null && minimum > maximum) add("参数 $id 的最小值大于最大值")
        if (step != null && (!step.isFinite() || step <= 0.0)) add("参数 $id 的步长必须为正有限数")
        when (kind) {
            CourseParameterKind.NUMBER,
            CourseParameterKind.INTEGER,
            -> {
                val value = defaultValue.toDoubleOrNull()
                if (value == null || !value.isFinite()) {
                    add("参数 $id 的默认值不是数字")
                } else {
                    if (minimum != null && value < minimum) add("参数 $id 的默认值小于最小值")
                    if (maximum != null && value > maximum) add("参数 $id 的默认值大于最大值")
                    if (kind == CourseParameterKind.INTEGER && value % 1.0 != 0.0) {
                        add("整数参数 $id 的默认值不是整数")
                    }
                }
            }
            CourseParameterKind.BOOLEAN -> if (defaultValue !in setOf("true", "false")) {
                add("布尔参数 $id 只能使用 true 或 false")
            }
            CourseParameterKind.CHOICE -> {
                if (choices.isEmpty()) add("选项参数 $id 没有候选项")
                if (choices.distinct().size != choices.size) add("选项参数 $id 包含重复候选项")
                if (defaultValue !in choices) add("选项参数 $id 的默认值不在候选项中")
            }
            CourseParameterKind.TEXT -> Unit
        }
    }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("label", label)
        .put("kind", kind.name)
        .put("defaultValue", defaultValue)
        .put("unit", unit)
        .put("minimum", minimum)
        .put("maximum", maximum)
        .put("step", step)
        .put("choices", JSONArray(choices))
        .put("explanation", explanation)

    companion object {
        private val PARAMETER_ID = Regex("^[a-z][a-z0-9_-]{0,47}$")

        fun fromJson(root: JSONObject): CourseParameterSpec = CourseParameterSpec(
            id = root.optString("id").trim(),
            label = root.optString("label").trim(),
            kind = root.optString("kind")
                .enumOrNull<CourseParameterKind>()
                ?: CourseParameterKind.NUMBER,
            defaultValue = root.optString("defaultValue").trim(),
            unit = root.optString("unit").trim(),
            minimum = root.optionalFiniteDouble("minimum"),
            maximum = root.optionalFiniteDouble("maximum"),
            step = root.optionalFiniteDouble("step"),
            choices = root.optJSONArray("choices").stringList(),
            explanation = root.optString("explanation").trim(),
        )
    }
}

data class CourseNote(
    val origin: ContentOrigin,
    val title: String,
    val body: String,
    val sourcePageStart: Int? = null,
    val sourcePageEnd: Int? = null,
) {
    val displayLabel: String
        get() = when (origin) {
            ContentOrigin.TEXTBOOK_QUOTE -> "教材原文"
            ContentOrigin.TEXTBOOK_SUMMARY -> "教材内容依据"
            ContentOrigin.SCHOOL_EXPLANATION -> "School 解释"
            ContentOrigin.OPTIONAL_EXTENSION -> "扩展知识"
            ContentOrigin.SIMULATION_DESCRIPTION -> "模拟说明"
        }

    fun validationErrors(sourcePages: IntRange): List<String> = buildList {
        if (title.isBlank()) add("课程补充内容缺少标题")
        if (body.isBlank()) add("课程补充内容“$title”缺少正文")
        val pageAnchored = origin == ContentOrigin.TEXTBOOK_QUOTE || origin == ContentOrigin.TEXTBOOK_SUMMARY
        if (pageAnchored && sourcePageStart == null) add("$displayLabel“$title”缺少教材页码")
        if (sourcePageStart != null) {
            val end = sourcePageEnd ?: sourcePageStart
            if (sourcePageStart <= 0 || end < sourcePageStart) add("课程补充内容“$title”的页码范围无效")
            if (sourcePageStart !in sourcePages || end !in sourcePages) {
                add("课程补充内容“$title”的页码超出本课 ${sourcePages.first}—${sourcePages.last} 页")
            }
        }
        if (origin == ContentOrigin.OPTIONAL_EXTENSION && !title.contains("扩展")) {
            add("扩展内容标题必须明确包含“扩展”")
        }
    }

    fun toJson(): JSONObject = JSONObject()
        .put("origin", origin.name)
        .put("title", title)
        .put("body", body)
        .put("sourcePageStart", sourcePageStart)
        .put("sourcePageEnd", sourcePageEnd)

    companion object {
        fun fromJson(root: JSONObject): CourseNote = CourseNote(
            origin = root.optString("origin")
                .enumOrNull<ContentOrigin>()
                ?: ContentOrigin.SCHOOL_EXPLANATION,
            title = root.optString("title").trim(),
            body = root.optString("body").trim(),
            sourcePageStart = root.optionalPositiveInt("sourcePageStart"),
            sourcePageEnd = root.optionalPositiveInt("sourcePageEnd"),
        )
    }
}

data class CourseVisualizationSpec(
    val kind: CourseVisualizationKind,
    val title: String,
    val description: String,
    val parameters: List<CourseParameterSpec> = emptyList(),
    val requiredConcepts: Set<ConceptId> = emptySet(),
    val requiredOperations: Set<OperationId> = emptySet(),
    val requiredWidgets: Set<WidgetType> = emptySet(),
) {
    fun validationErrors(capability: LessonCapability? = null): List<String> = buildList {
        if (title.isBlank()) add("可视化缺少标题")
        if (description.isBlank()) add("可视化“$title”缺少说明")
        if (parameters.map { it.id }.distinct().size != parameters.size) add("可视化“$title”包含重复参数 id")
        parameters.forEach { addAll(it.validationErrors()) }
        capability?.validate(requiredConcepts, requiredOperations, requiredWidgets)?.let { validation ->
            if (!validation.allowed) {
                if (validation.blockedConcepts.isNotEmpty()) add("可视化使用了本章未允许的概念：${validation.blockedConcepts.joinToString()}")
                if (validation.blockedOperations.isNotEmpty()) add("可视化使用了本章未允许的操作：${validation.blockedOperations.joinToString()}")
                if (validation.blockedWidgets.isNotEmpty()) add("可视化使用了本章未允许的组件：${validation.blockedWidgets.joinToString()}")
            }
        }
    }

    fun toJson(): JSONObject = JSONObject()
        .put("kind", kind.name)
        .put("title", title)
        .put("description", description)
        .put("parameters", JSONArray().apply { parameters.forEach { put(it.toJson()) } })
        .put("requiredConcepts", JSONArray(requiredConcepts.map { it.name }.sorted()))
        .put("requiredOperations", JSONArray(requiredOperations.map { it.name }.sorted()))
        .put("requiredWidgets", JSONArray(requiredWidgets.map { it.name }.sorted()))

    companion object {
        fun fromJson(root: JSONObject): CourseVisualizationSpec = CourseVisualizationSpec(
            kind = root.optString("kind")
                .enumOrNull<CourseVisualizationKind>()
                ?: CourseVisualizationKind.PROCESS,
            title = root.optString("title").trim(),
            description = root.optString("description").trim(),
            parameters = root.optJSONArray("parameters").objectList().map(CourseParameterSpec::fromJson),
            requiredConcepts = root.optJSONArray("requiredConcepts").enumSet<ConceptId>(),
            requiredOperations = root.optJSONArray("requiredOperations").enumSet<OperationId>(),
            requiredWidgets = root.optJSONArray("requiredWidgets").enumSet<WidgetType>(),
        )
    }
}

enum class CourseVerificationKind {
    MATH_EXPRESSION,
    MATH_RELATION,
    PHYSICAL_RELATION,
    PHYSICAL_MODEL,
    CHEMICAL_FORMULA,
    CHEMICAL_EQUATION,
    ORGANIC_STRUCTURE,
    BIOLOGICAL_RELATION,
    BIOLOGICAL_PROCESS,
    ENGLISH_SENTENCE,
    JAPANESE_SENTENCE,
    DIAGRAM_LABEL,
    PROCESS_ORDER,
}

data class CourseVerificationSpec(
    val kind: CourseVerificationKind,
    val title: String,
    val prompt: String,
    val inputHint: String,
    val examples: List<String> = emptyList(),
    val requiredConcepts: Set<ConceptId> = emptySet(),
    val requiredOperations: Set<OperationId> = emptySet(),
) {
    fun validationErrors(capability: LessonCapability? = null): List<String> = buildList {
        if (title.isBlank()) add("验证任务缺少标题")
        if (prompt.isBlank()) add("验证任务“$title”缺少问题")
        if (inputHint.isBlank()) add("验证任务“$title”缺少输入说明")
        capability?.validate(requiredConcepts, requiredOperations)?.let { validation ->
            if (!validation.allowed) {
                if (validation.blockedConcepts.isNotEmpty()) add("验证任务使用了本章未允许的概念：${validation.blockedConcepts.joinToString()}")
                if (validation.blockedOperations.isNotEmpty()) add("验证任务使用了本章未允许的操作：${validation.blockedOperations.joinToString()}")
            }
        }
    }

    fun toJson(): JSONObject = JSONObject()
        .put("kind", kind.name)
        .put("title", title)
        .put("prompt", prompt)
        .put("inputHint", inputHint)
        .put("examples", JSONArray(examples))
        .put("requiredConcepts", JSONArray(requiredConcepts.map { it.name }.sorted()))
        .put("requiredOperations", JSONArray(requiredOperations.map { it.name }.sorted()))

    companion object {
        fun fromJson(root: JSONObject): CourseVerificationSpec = CourseVerificationSpec(
            kind = root.optString("kind")
                .enumOrNull<CourseVerificationKind>()
                ?: CourseVerificationKind.PROCESS_ORDER,
            title = root.optString("title").trim(),
            prompt = root.optString("prompt").trim(),
            inputHint = root.optString("inputHint").trim(),
            examples = root.optJSONArray("examples").stringList(),
            requiredConcepts = root.optJSONArray("requiredConcepts").enumSet<ConceptId>(),
            requiredOperations = root.optJSONArray("requiredOperations").enumSet<OperationId>(),
        )
    }
}

data class LessonEnrichment(
    val background: List<CourseNote> = emptyList(),
    val extensions: List<CourseNote> = emptyList(),
    val visualization: CourseVisualizationSpec? = null,
    val verification: CourseVerificationSpec? = null,
) {
    fun validationErrors(
        sourcePages: IntRange,
        capability: LessonCapability? = null,
    ): List<String> = buildList {
        background.forEach { note ->
            addAll(note.validationErrors(sourcePages))
            if (note.origin == ContentOrigin.OPTIONAL_EXTENSION) {
                add("背景知识“${note.title}”不能标记为扩展内容")
            }
        }
        extensions.forEach { note ->
            addAll(note.validationErrors(sourcePages))
            if (note.origin != ContentOrigin.OPTIONAL_EXTENSION) {
                add("扩展知识“${note.title}”必须标记为 OPTIONAL_EXTENSION")
            }
        }
        if (capability?.extensionPolicy == ExtensionPolicy.NONE && extensions.isNotEmpty()) {
            add("本章禁止扩展知识，但课程仍配置了扩展内容")
        }
        visualization?.let { addAll(it.validationErrors(capability)) }
        verification?.let { addAll(it.validationErrors(capability)) }
    }

    fun toJson(): JSONObject = JSONObject()
        .put("background", JSONArray().apply { background.forEach { put(it.toJson()) } })
        .put("extensions", JSONArray().apply { extensions.forEach { put(it.toJson()) } })
        .put("visualization", visualization?.toJson())
        .put("verification", verification?.toJson())

    companion object {
        fun fromJson(root: JSONObject?): LessonEnrichment {
            if (root == null) return LessonEnrichment()
            return LessonEnrichment(
                background = root.optJSONArray("background").objectList().map(CourseNote::fromJson),
                extensions = root.optJSONArray("extensions").objectList().map(CourseNote::fromJson),
                visualization = root.optJSONObject("visualization")?.let(CourseVisualizationSpec::fromJson),
                verification = root.optJSONObject("verification")?.let(CourseVerificationSpec::fromJson),
            )
        }
    }
}

private inline fun <reified T : Enum<T>> String.enumOrNull(): T? =
    trim().uppercase().takeIf(String::isNotEmpty)?.let { value -> enumValues<T>().firstOrNull { it.name == value } }

private fun JSONObject.optionalFiniteDouble(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    return optDouble(key).takeIf(Double::isFinite)
}

private fun JSONObject.optionalPositiveInt(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return optInt(key).takeIf { it > 0 }
}

private fun JSONArray?.stringList(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optString(index).trim().takeIf(String::isNotEmpty)?.let(::add)
        }
    }
}

private fun JSONArray?.objectList(): List<JSONObject> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) optJSONObject(index)?.let(::add)
    }
}

private inline fun <reified T : Enum<T>> JSONArray?.enumSet(): Set<T> =
    stringList().mapNotNull { it.enumOrNull<T>() }.toSet()
