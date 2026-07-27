package com.majortomman.school.learning.assessment.contract

import com.majortomman.school.learning.content.ContentAssetId
import com.majortomman.school.learning.content.LearningContent
import com.majortomman.school.learning.content.LearningTextStyle
import com.majortomman.school.learning.course.CourseScene
import com.majortomman.school.learning.course.CourseSceneData
import com.majortomman.school.learning.course.CourseSceneTemplate
import org.json.JSONArray
import org.json.JSONObject

internal object LearningContentParser {
    fun decodeArray(array: JSONArray, location: String, allowEmpty: Boolean): List<LearningContent> {
        val result = array.contractObjects(location).mapIndexed { index, json ->
            decode(json, "$location[$index]")
        }
        if (!allowEmpty) require(result.isNotEmpty()) { "$location 不能为空" }
        return result
    }

    private fun decode(json: JSONObject, location: String): LearningContent =
        when (val type = json.requireContractText("type", location)) {
            "heading" -> {
                json.requireContractShape(location, required = setOf("type", "text"))
                LearningContent.Heading(json.requireContractText("text", location))
            }

            "text" -> {
                json.requireContractShape(location, required = setOf("type", "text", "style"))
                val style = json.requireContractText("style", location).uppercase().let { wire ->
                    LearningTextStyle.entries.firstOrNull { it.name == wire }
                        ?: error("$location.style 不受支持：$wire")
                }
                LearningContent.Text(json.requireContractText("text", location), style)
            }

            "formula" -> {
                json.requireContractShape(
                    location,
                    required = setOf("type", "expression", "conditions"),
                )
                LearningContent.Formula(
                    expression = json.requireContractText("expression", location),
                    conditions = json.requireContractStrings("conditions", location),
                )
            }

            "list" -> {
                json.requireContractShape(location, required = setOf("type", "items"))
                LearningContent.ItemList(json.requireContractStrings("items", location))
            }

            "image" -> {
                json.requireContractShape(
                    location,
                    required = setOf("type", "assetId", "altText"),
                    optional = setOf("caption"),
                )
                LearningContent.Image(
                    assetId = ContentAssetId(json.requireContractText("assetId", location)),
                    altText = json.requireContractText("altText", location),
                    caption = json.optionalContractText("caption", location),
                )
            }

            "table" -> {
                json.requireContractShape(
                    location,
                    required = setOf("type", "columns", "rows"),
                    optional = setOf("caption", "sourceAssetId"),
                )
                val columns = json.requireContractStrings("columns", location)
                val rowsArray = json.requireContractArray("rows", location)
                val rows = buildList {
                    for (index in 0 until rowsArray.length()) {
                        val row = rowsArray.get(index)
                        require(row is JSONArray) { "$location.rows[$index] 必须是数组" }
                        add(row.contractStrings("$location.rows[$index]"))
                    }
                }
                LearningContent.Table(
                    columns = columns,
                    rows = rows,
                    caption = json.optionalContractText("caption", location),
                    sourceAssetId = json.optionalContractText("sourceAssetId", location)?.let(::ContentAssetId),
                )
            }

            "scene" -> {
                json.requireContractShape(location, required = setOf("type", "template", "data"))
                val templateId = json.requireContractText("template", location)
                val template = CourseSceneTemplate.fromId(templateId)
                    ?: error("$location.template 不受支持：$templateId")
                LearningContent.Scene(
                    CourseScene(
                        template = template,
                        data = decodeSceneData(
                            template,
                            json.requireContractObject("data", location),
                            location,
                        ),
                    ),
                )
            }

            else -> error("$location.type 不受支持：$type")
        }

    private fun decodeSceneData(
        template: CourseSceneTemplate,
        json: JSONObject,
        location: String,
    ): CourseSceneData {
        val allowed = when (template) {
            CourseSceneTemplate.OPPOSITE_QUANTITIES -> setOf("title", "scene", "scenes")
            CourseSceneTemplate.RATIONAL_CLASSIFICATION -> setOf("title", "mode")
            CourseSceneTemplate.NUMBER_LINE -> setOf("title", "mode", "signed", "initial")
            CourseSceneTemplate.SUBTRACTION_TRANSFORM,
            CourseSceneTemplate.DIVISION_TRANSFORM,
            -> setOf("title", "expression")
            CourseSceneTemplate.ALGEBRA_PROCESS,
            CourseSceneTemplate.EQUATION_BALANCE,
            -> setOf("title", "left", "right", "note")
            CourseSceneTemplate.FUNCTION_GRAPH -> setOf("title", "function", "note")
            CourseSceneTemplate.GEOMETRY -> setOf("title", "shape", "note")
            CourseSceneTemplate.TRANSFORMATION -> setOf("title", "mode", "note")
            CourseSceneTemplate.RIGHT_TRIANGLE -> setOf("title", "formula", "note")
            CourseSceneTemplate.DATA_CHART -> setOf("title", "mode", "note")
            CourseSceneTemplate.ROOT_NUMBER_LINE,
            CourseSceneTemplate.CARTESIAN_PLANE,
            CourseSceneTemplate.PROBABILITY,
            CourseSceneTemplate.PROJECTION,
            -> setOf("title", "note")
            CourseSceneTemplate.DECLARATIVE_DIAGRAM -> setOf("height", "elements")
            else -> setOf("title")
        }
        json.requireContractShape("$location.data", required = emptySet(), optional = allowed)
        val values = linkedMapOf<String, Any?>()
        json.keys().forEach { key ->
            values[key] = decodeJsonValue(json.get(key), "$location.data.$key", depth = 0)
        }

        fun optionalString(key: String, allowedValues: Set<String>? = null) {
            if (!values.containsKey(key)) return
            val value = values[key]
            require(value is String && value.isNotBlank()) { "$location.data.$key 必须是非空字符串" }
            if (allowedValues != null) {
                require(value in allowedValues) { "$location.data.$key 的值不受支持：$value" }
            }
        }

        listOf("title", "left", "right", "note", "expression", "formula").forEach(::optionalString)
        when (template) {
            CourseSceneTemplate.OPPOSITE_QUANTITIES -> {
                val allowedScenes = setOf("temperature", "account", "elevation", "change", "tolerance", "deviation")
                optionalString("scene", allowedScenes)
                values["scenes"]?.let { raw ->
                    require(raw is List<*> && raw.isNotEmpty() && raw.all { it is String && it in allowedScenes }) {
                        "$location.data.scenes 必须是受支持的非空场景数组"
                    }
                }
            }
            CourseSceneTemplate.RATIONAL_CLASSIFICATION -> optionalString("mode", setOf("definition", "fraction_form"))
            CourseSceneTemplate.NUMBER_LINE -> {
                optionalString("mode", setOf("road", "construction", "value", "example", "read_points"))
                values["signed"]?.let { require(it is Boolean) { "$location.data.signed 必须是布尔值" } }
                values["initial"]?.let {
                    require(it is Number && it.toDouble().isFinite()) { "$location.data.initial 必须是有限数" }
                }
            }
            CourseSceneTemplate.FUNCTION_GRAPH -> optionalString("function", setOf("linear", "quadratic", "inverse"))
            CourseSceneTemplate.GEOMETRY -> optionalString("shape", setOf("triangle", "parallel", "circle"))
            CourseSceneTemplate.TRANSFORMATION -> optionalString("mode", setOf("translation", "rotation", "symmetry"))
            CourseSceneTemplate.DATA_CHART -> optionalString("mode", setOf("bar", "line"))
            CourseSceneTemplate.DECLARATIVE_DIAGRAM -> validateDeclarativeDiagram(values, location)
            else -> Unit
        }
        return CourseSceneData(values)
    }

    private fun validateDeclarativeDiagram(values: Map<String, Any?>, location: String) {
        values["height"]?.let {
            require(it is Number && it.toDouble() in 120.0..1000.0) { "$location.data.height 超出允许范围" }
        }
        val elements = values["elements"]
        require(elements is List<*> && elements.isNotEmpty()) { "$location.data.elements 必须是非空数组" }
        require(elements.size <= MAX_DIAGRAM_ELEMENTS) { "$location.data.elements 数量超过限制" }
        elements.forEachIndexed { index, raw ->
            require(raw is Map<*, *>) { "$location.data.elements[$index] 必须是对象" }
            @Suppress("UNCHECKED_CAST")
            validateDiagramElement(raw as Map<String, Any?>, "$location.data.elements[$index]")
        }
    }

    private fun validateDiagramElement(element: Map<String, Any?>, location: String) {
        val type = element["type"] as? String ?: error("$location 缺少 type")
        val common = setOf("type", "color", "stroke")
        val allowed = common + when (type) {
            "line", "arrow" -> setOf("x1", "y1", "x2", "y2")
            "point", "circle" -> setOf("x", "y", "radius")
            "rectangle" -> setOf("x", "y", "width", "height")
            "text" -> setOf("x", "y", "text", "size")
            "polyline" -> setOf("points")
            "number_line" -> setOf("x1", "x2", "y", "min", "max", "step")
            else -> error("$location 使用了不支持的图元：$type")
        }
        val unknown = element.keys - allowed
        require(unknown.isEmpty()) { "$location 包含未知字段：${unknown.sorted().joinToString()}" }
        element["color"]?.let {
            require(it in setOf("blue", "yellow", "muted", "white")) { "$location.color 不受支持" }
        }
        element["stroke"]?.let { requireNumber(it, "$location.stroke", 0.5..20.0) }

        fun ratio(key: String, required: Boolean = true) {
            val value = element[key]
            if (required || value != null) requireNumber(value, "$location.$key", 0.0..1.0)
        }

        when (type) {
            "line", "arrow" -> listOf("x1", "y1", "x2", "y2").forEach(::ratio)
            "point", "circle" -> {
                ratio("x"); ratio("y"); requireNumber(element["radius"], "$location.radius", 0.001..1.0)
            }
            "rectangle" -> {
                ratio("x"); ratio("y")
                requireNumber(element["width"], "$location.width", 0.001..1.0)
                requireNumber(element["height"], "$location.height", 0.001..1.0)
            }
            "text" -> {
                ratio("x"); ratio("y")
                require((element["text"] as? String)?.isNotBlank() == true) { "$location.text 必须是非空字符串" }
                element["size"]?.let { requireNumber(it, "$location.size", 8.0..72.0) }
            }
            "polyline" -> {
                val points = element["points"]
                require(points is List<*> && points.size in 2..MAX_POLYLINE_POINTS) {
                    "$location.points 必须包含 2 到 $MAX_POLYLINE_POINTS 个点"
                }
                points.forEachIndexed { index, point ->
                    require(point is Map<*, *> && point.keys == setOf("x", "y")) {
                        "$location.points[$index] 格式无效"
                    }
                    requireNumber(point["x"], "$location.points[$index].x", 0.0..1.0)
                    requireNumber(point["y"], "$location.points[$index].y", 0.0..1.0)
                }
            }
            "number_line" -> {
                ratio("x1", false); ratio("x2", false); ratio("y", false)
                val minimum = (element["min"] as? Number)?.toDouble() ?: -5.0
                val maximum = (element["max"] as? Number)?.toDouble() ?: 5.0
                val step = (element["step"] as? Number)?.toDouble() ?: 1.0
                require(minimum.isFinite() && maximum.isFinite() && minimum < maximum) {
                    "$location 的数轴范围无效"
                }
                require(step.isFinite() && step > 0.0 && (maximum - minimum) / step <= 200.0) {
                    "$location.step 无效"
                }
            }
        }
    }

    private fun requireNumber(value: Any?, location: String, range: ClosedFloatingPointRange<Double>) {
        require(value is Number && value.toDouble().isFinite() && value.toDouble() in range) {
            "$location 必须是 ${range.start}..${range.endInclusive} 范围内的数"
        }
    }

    private fun decodeJsonValue(value: Any?, location: String, depth: Int): Any {
        require(depth <= MAX_JSON_DEPTH) { "$location 嵌套层级超过限制" }
        return when (value) {
            JSONObject.NULL, null -> error("$location 不允许 null")
            is String -> value.also { require(it.length <= MAX_TEXT_LENGTH) { "$location 文本过长" } }
            is Boolean, is Int, is Long -> value
            is Number -> value.toDouble().also { require(it.isFinite()) { "$location 必须是有限数" } }
            is JSONObject -> {
                require(value.length() <= MAX_OBJECT_KEYS) { "$location 对象字段过多" }
                buildMap {
                    value.keys().forEach { key ->
                        put(key, decodeJsonValue(value.get(key), "$location.$key", depth + 1))
                    }
                }
            }
            is JSONArray -> {
                require(value.length() <= MAX_ARRAY_ITEMS) { "$location 数组元素过多" }
                buildList {
                    for (index in 0 until value.length()) {
                        add(decodeJsonValue(value.get(index), "$location[$index]", depth + 1))
                    }
                }
            }
            else -> error("$location 包含不支持的数据类型")
        }
    }

    private const val MAX_JSON_DEPTH = 8
    private const val MAX_OBJECT_KEYS = 100
    private const val MAX_ARRAY_ITEMS = 500
    private const val MAX_TEXT_LENGTH = 20_000
    private const val MAX_DIAGRAM_ELEMENTS = 200
    private const val MAX_POLYLINE_POINTS = 500
}
