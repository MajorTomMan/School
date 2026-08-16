package com.majortomman.school.learning.assessment.contract

import com.majortomman.school.learning.content.ContentAssetId
import com.majortomman.school.learning.content.LearningContent
import com.majortomman.school.learning.content.LearningTextStyle
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
        val result = array.contractObjects(location).mapIndexed { index, json -> decode(json, "$location[$index]") }
        if (!allowEmpty) require(result.isNotEmpty()) { "$location 不能为空" }
        return result
    }

    private fun decode(json: JSONObject, location: String): LearningContent = when (val type = json.requireContractText("type", location)) {
        "heading" -> {
            json.requireContractShape(location, required = setOf("type", "text"))
            LearningContent.Heading(json.requireContractText("text", location))
        }
        "text" -> {
            json.requireContractShape(location, required = setOf("type", "text", "style"))
            val style = json.requireContractText("style", location).uppercase().let { wire ->
                LearningTextStyle.entries.firstOrNull { it.name == wire } ?: error("$location.style 不受支持：$wire")
            }
            LearningContent.Text(json.requireContractText("text", location), style)
        }
        "formula" -> {
            json.requireContractShape(location, required = setOf("type", "expression", "conditions"))
            LearningContent.Formula(expression = json.requireContractText("expression", location), conditions = json.requireContractStrings("conditions", location))
        }
        "list" -> {
            json.requireContractShape(location, required = setOf("type", "items"))
            LearningContent.ItemList(json.requireContractStrings("items", location))
        }
        "image" -> {
            json.requireContractShape(location, required = setOf("type", "assetId", "altText"), optional = setOf("caption"))
            LearningContent.Image(
                assetId = ContentAssetId(json.requireContractText("assetId", location)),
                altText = json.requireContractText("altText", location),
                caption = json.optionalContractText("caption", location),
            )
        }
        "table" -> {
            json.requireContractShape(location, required = setOf("type", "columns", "rows"), optional = setOf("caption", "sourceAssetId"))
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
        "visualization" -> decodeVisualization(json, location)
        else -> error("$location.type 不受支持：$type")
    }

    private fun decodeVisualization(json: JSONObject, location: String): LearningContent.Visualization {
        json.requireContractShape(location, required = setOf("type", "renderer", "parameters", "texts"))
        val invocation = VisualizationInvocation(
            renderer = VisualizationKey(json.requireContractText("renderer", location)),
            parameters = decodeVisualizationParameters(json.requireContractObject("parameters", location), location),
            texts = decodeVisualizationTexts(json.requireContractObject("texts", location), location),
        )
        val issues = SchoolVisualizationCatalog.validate(invocation)
        require(issues.isEmpty()) { "$location 可视化参数无效：${issues.joinToString("；")}" }
        return LearningContent.Visualization(invocation)
    }

    private fun decodeVisualizationParameters(json: JSONObject, location: String): VisualizationParameters {
        val values = linkedMapOf<String, VisualizationParameterValue>()
        json.keys().forEach { key ->
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
        json.keys().forEach { key ->
            val raw = json.get(key)
            require(raw is String) { "$location.texts.$key 只能是字符串" }
            values[key] = raw
        }
        return VisualizationTexts.of(values)
    }
}
