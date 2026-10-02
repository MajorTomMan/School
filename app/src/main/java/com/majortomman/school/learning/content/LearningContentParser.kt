package com.majortomman.school.learning.content

import com.majortomman.school.visualization.SchoolVisualizationCatalog
import com.majortomman.school.visualization.VisualizationInvocation
import com.majortomman.school.visualization.VisualizationKey
import com.majortomman.school.visualization.VisualizationParameterValue
import com.majortomman.school.visualization.VisualizationParameters
import com.majortomman.school.visualization.VisualizationTexts
import org.json.JSONArray
import org.json.JSONObject

internal object LearningContentParser {
    fun decodeArray(array: JSONArray, location: String, allowEmpty: Boolean): List<LearningContent> {
        val result = array.objects(location).mapIndexed { index, json -> decode(json, "$location[$index]") }
        if (!allowEmpty) require(result.isNotEmpty()) { "$location 不能为空" }
        return result
    }

    private fun decode(json: JSONObject, location: String): LearningContent = when (val type = json.text("type", location)) {
        "heading" -> {
            json.shape(location, required = setOf("type", "text"))
            LearningContent.Heading(json.text("text", location))
        }
        "text" -> {
            json.shape(location, required = setOf("type", "text", "style"))
            val style = json.text("style", location).uppercase().let { wire ->
                LearningTextStyle.entries.firstOrNull { it.name == wire } ?: error("$location.style 不受支持：$wire")
            }
            LearningContent.Text(json.text("text", location), style)
        }
        "formula" -> {
            json.shape(location, required = setOf("type", "expression", "conditions"))
            LearningContent.Formula(json.text("expression", location), json.strings("conditions", location))
        }
        "list" -> {
            json.shape(location, required = setOf("type", "items"))
            LearningContent.ItemList(json.strings("items", location))
        }
        "image" -> {
            json.shape(location, required = setOf("type", "assetId", "altText"), optional = setOf("caption"))
            LearningContent.Image(
                ContentAssetId(json.text("assetId", location)),
                json.text("altText", location),
                json.optionalText("caption", location),
            )
        }
        "table" -> {
            json.shape(location, required = setOf("type", "columns", "rows"), optional = setOf("caption", "sourceAssetId"))
            val columns = json.strings("columns", location)
            val rows = json.array("rows", location).let { array ->
                List(array.length()) { index ->
                    val row = array.get(index)
                    require(row is JSONArray) { "$location.rows[$index] 必须是数组" }
                    row.strings("$location.rows[$index]")
                }
            }
            LearningContent.Table(
                columns = columns,
                rows = rows,
                caption = json.optionalText("caption", location),
                sourceAssetId = json.optionalText("sourceAssetId", location)?.let(::ContentAssetId),
            )
        }
        "visualization" -> decodeVisualization(json, location)
        else -> error("$location.type 不受支持：$type")
    }

    private fun decodeVisualization(json: JSONObject, location: String): LearningContent.Visualization {
        json.shape(location, required = setOf("type", "renderer", "schemaVersion", "parameters", "texts"))
        val invocation = VisualizationInvocation(
            renderer = VisualizationKey(json.text("renderer", location)),
            schemaVersion = json.positiveInt("schemaVersion", location),
            parameters = decodeParameters(json.objectValue("parameters", location), location),
            texts = decodeTexts(json.objectValue("texts", location), location),
        )
        val issues = SchoolVisualizationCatalog.validate(invocation)
        require(issues.isEmpty()) { "$location 可视化参数无效：${issues.joinToString("；")}" }
        return LearningContent.Visualization(invocation)
    }

    private fun decodeParameters(json: JSONObject, location: String): VisualizationParameters {
        val values = linkedMapOf<String, VisualizationParameterValue>()
        json.keys().forEach { key ->
            val raw = json.get(key)
            values[key] = when (raw) {
                is Number -> VisualizationParameterValue.NumberValue(raw.toDouble())
                is Boolean -> VisualizationParameterValue.BooleanValue(raw)
                is JSONArray -> VisualizationParameterValue.NumberListValue(
                    List(raw.length()) { index ->
                        val item = raw.get(index)
                        require(item is Number) { "$location.parameters.$key 只能是数值列表" }
                        item.toDouble()
                    },
                )
                is String -> VisualizationParameterValue.MathExpressionValue.parse(raw)
                else -> error("$location.parameters.$key 只接受 number、boolean、number[] 或受限数学表达式")
            }
        }
        return VisualizationParameters.of(values)
    }

    private fun decodeTexts(json: JSONObject, location: String): VisualizationTexts {
        val values = linkedMapOf<String, String>()
        json.keys().forEach { key ->
            val raw = json.get(key)
            require(raw is String) { "$location.texts.$key 只能是字符串" }
            values[key] = raw
        }
        return VisualizationTexts.of(values)
    }
}

private fun JSONObject.shape(location: String, required: Set<String>, optional: Set<String> = emptySet()) {
    val actual = keys().asSequence().toSet()
    val unknown = actual - required - optional
    val missing = required - actual
    require(unknown.isEmpty()) { "$location 包含未知字段：${unknown.sorted().joinToString()}" }
    require(missing.isEmpty()) { "$location 缺少必需字段：${missing.sorted().joinToString()}" }
}

private fun JSONObject.text(key: String, location: String): String {
    require(has(key) && get(key) is String) { "$location.$key 必须是字符串" }
    return getString(key).trim().also { require(it.isNotBlank()) { "$location.$key 不能为空" } }
}

private fun JSONObject.optionalText(key: String, location: String): String? {
    if (!has(key) || isNull(key)) return null
    require(get(key) is String) { "$location.$key 必须是字符串" }
    return getString(key).trim().also { require(it.isNotBlank()) { "$location.$key 不能为空" } }
}

private fun JSONObject.objectValue(key: String, location: String): JSONObject =
    optJSONObject(key) ?: error("$location.$key 必须是对象")

private fun JSONObject.array(key: String, location: String): JSONArray =
    optJSONArray(key) ?: error("$location.$key 必须是数组")

private fun JSONObject.strings(key: String, location: String): List<String> = array(key, location).strings("$location.$key")

private fun JSONArray.strings(location: String): List<String> = List(length()) { index ->
    val value = get(index)
    require(value is String) { "$location[$index] 必须是字符串" }
    value.trim().also { require(it.isNotBlank()) { "$location[$index] 不能为空" } }
}

private fun JSONArray.objects(location: String): List<JSONObject> = List(length()) { index ->
    val value = get(index)
    require(value is JSONObject) { "$location[$index] 必须是对象" }
    value
}


private fun JSONObject.positiveInt(key: String, location: String): Int {
    val raw = get(key)
    require(raw is Byte || raw is Short || raw is Int || raw is Long) { "$location.$key 必须是 JSON 整数" }
    val value = (raw as Number).toLong()
    require(value in 1..Int.MAX_VALUE.toLong()) { "$location.$key 必须是正整数" }
    return value.toInt()
}
