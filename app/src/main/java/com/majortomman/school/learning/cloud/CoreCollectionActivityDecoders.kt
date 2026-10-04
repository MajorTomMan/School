package com.majortomman.school.learning.cloud

import com.majortomman.school.learning.activity.ActivityCapabilityKeys
import com.majortomman.school.learning.activity.ActivityId
import com.majortomman.school.learning.activity.ActivityOption
import com.majortomman.school.learning.activity.ActivitySpec
import com.majortomman.school.learning.activity.MatchActivitySpec
import com.majortomman.school.learning.activity.OrderActivitySpec
import com.majortomman.school.learning.activity.SelectOneActivitySpec
import com.majortomman.school.learning.capability.CapabilityKey
import org.json.JSONArray
import org.json.JSONObject

internal object SelectOneActivityDecoder : CourseActivitySpecDecoder {
    override val capability: CapabilityKey = ActivityCapabilityKeys.SELECT_ONE
    override val schemaVersion: Int = 1

    override fun decode(id: ActivityId, parameters: JSONObject): ActivitySpec {
        parameters.requireActivityShape(required = setOf("options"))
        return SelectOneActivitySpec(id, parameters.activityOptions("options"))
    }
}

internal object OrderActivityDecoder : CourseActivitySpecDecoder {
    override val capability: CapabilityKey = ActivityCapabilityKeys.ORDER
    override val schemaVersion: Int = 1

    override fun decode(id: ActivityId, parameters: JSONObject): ActivitySpec {
        parameters.requireActivityShape(required = setOf("items"))
        return OrderActivitySpec(id, parameters.activityOptions("items"))
    }
}

internal object MatchActivityDecoder : CourseActivitySpecDecoder {
    override val capability: CapabilityKey = ActivityCapabilityKeys.MATCH
    override val schemaVersion: Int = 1

    override fun decode(id: ActivityId, parameters: JSONObject): ActivitySpec {
        parameters.requireActivityShape(required = setOf("left", "right"))
        return MatchActivitySpec(
            id = id,
            leftItems = parameters.activityOptions("left"),
            rightItems = parameters.activityOptions("right"),
        )
    }
}

private fun JSONObject.activityOptions(key: String): List<ActivityOption> {
    val raw = get(key)
    require(raw is JSONArray) { "activity parameter $key 必须是数组" }
    val result = ArrayList<ActivityOption>(raw.length())
    for (index in 0 until raw.length()) {
        val item = raw.get(index)
        require(item is JSONObject) { "activity parameter $key[$index] 必须是对象" }
        val actual = item.keys().asSequence().toSet()
        require(actual == setOf("id", "label")) {
            "activity parameter $key[$index] 只允许 id/label"
        }
        val id = item.get("id")
        val label = item.get("label")
        require(id is String) { "activity parameter $key[$index].id 必须是字符串" }
        require(label is String) { "activity parameter $key[$index].label 必须是字符串" }
        result += ActivityOption(id.trim(), label.trim())
    }
    return result
}
