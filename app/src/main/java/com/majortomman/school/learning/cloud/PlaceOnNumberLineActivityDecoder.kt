package com.majortomman.school.learning.cloud

import com.majortomman.school.learning.activity.ActivityCapabilityKeys
import com.majortomman.school.learning.activity.ActivityId
import com.majortomman.school.learning.activity.ActivitySpec
import com.majortomman.school.learning.activity.PlaceOnNumberLineActivitySpec
import com.majortomman.school.learning.capability.CapabilityKey
import org.json.JSONObject

internal object PlaceOnNumberLineActivityDecoder : CourseActivitySpecDecoder {
    override val capability: CapabilityKey = ActivityCapabilityKeys.PLACE_ON_NUMBER_LINE
    override val schemaVersion: Int = 1

    override fun decode(id: ActivityId, parameters: JSONObject): ActivitySpec {
        parameters.requireActivityShape(required = setOf("min", "max", "step", "initialValue"))
        return PlaceOnNumberLineActivitySpec(
            id = id,
            min = parameters.activityNumber("min"),
            max = parameters.activityNumber("max"),
            step = parameters.activityNumber("step"),
            initialValue = parameters.activityNumber("initialValue"),
        )
    }
}

internal fun JSONObject.requireActivityShape(
    required: Set<String> = emptySet(),
    optional: Set<String> = emptySet(),
) {
    val actual = keys().asSequence().toSet()
    val unknown = actual - required - optional
    val missing = required - actual
    require(unknown.isEmpty()) { "activity parameters 包含未知字段：${unknown.sorted()}" }
    require(missing.isEmpty()) { "activity parameters 缺少字段：${missing.sorted()}" }
}

internal fun JSONObject.optionalActivityText(key: String): String? {
    if (!has(key) || isNull(key)) return null
    require(get(key) is String) { "activity parameter $key 必须是字符串" }
    return getString(key).trim().also { require(it.isNotEmpty()) { "activity parameter $key 不能为空" } }
}

internal fun JSONObject.activityNumber(key: String): Double {
    val raw = get(key)
    require(raw is Number && raw !is Boolean) { "activity parameter $key 必须是 number" }
    return raw.toDouble().also { require(it.isFinite()) { "activity parameter $key 必须是有限数" } }
}
