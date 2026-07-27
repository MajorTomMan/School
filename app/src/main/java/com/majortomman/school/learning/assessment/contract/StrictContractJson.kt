package com.majortomman.school.learning.assessment.contract

import org.json.JSONArray
import org.json.JSONObject

internal fun JSONObject.requireContractShape(
    location: String,
    required: Set<String>,
    optional: Set<String> = emptySet(),
) {
    val actual = keys().asSequence().toSet()
    val unknown = actual - required - optional
    val missing = required - actual
    require(unknown.isEmpty()) { "$location 包含未知字段：${unknown.sorted().joinToString()}" }
    require(missing.isEmpty()) { "$location 缺少必需字段：${missing.sorted().joinToString()}" }
}

internal fun JSONObject.requireContractObject(key: String, location: String): JSONObject =
    optJSONObject(key) ?: error("$location.$key 必须是对象")

internal fun JSONObject.requireContractArray(key: String, location: String): JSONArray =
    optJSONArray(key) ?: error("$location.$key 必须是数组")

internal fun JSONObject.requireContractText(key: String, location: String): String {
    require(has(key) && get(key) is String) { "$location.$key 必须是字符串" }
    return getString(key).trim().also { require(it.isNotBlank()) { "$location.$key 不能为空" } }
}

internal fun JSONObject.optionalContractText(key: String, location: String): String? {
    if (!has(key)) return null
    require(get(key) is String) { "$location.$key 必须是字符串" }
    return getString(key).trim().also { require(it.isNotBlank()) { "$location.$key 不能为空" } }
}

internal fun JSONObject.requireContractBoolean(key: String, location: String): Boolean {
    require(has(key) && get(key) is Boolean) { "$location.$key 必须是布尔值" }
    return getBoolean(key)
}

internal fun JSONObject.requireContractInt(key: String, location: String): Int {
    require(has(key)) { "$location 缺少 $key" }
    val value = get(key)
    require(value is Int || value is Long) { "$location.$key 必须是整数" }
    return value.toString().toIntOrNull() ?: error("$location.$key 超出 Int 范围")
}

internal fun JSONObject.requireContractPositiveInt(key: String, location: String): Int =
    requireContractInt(key, location).also { require(it > 0) { "$location.$key 必须大于 0" } }

internal fun JSONObject.requireContractLong(key: String, location: String): Long {
    require(has(key)) { "$location 缺少 $key" }
    val value = get(key)
    require(value is Int || value is Long) { "$location.$key 必须是整数" }
    return value.toString().toLongOrNull() ?: error("$location.$key 超出 Long 范围")
}

internal fun JSONObject.requireContractDouble(key: String, location: String): Double {
    require(has(key)) { "$location 缺少 $key" }
    val value = get(key)
    require(value is Number) { "$location.$key 必须是数字" }
    return value.toDouble().also { require(it.isFinite()) { "$location.$key 必须是有限数" } }
}

internal fun JSONArray.contractObjects(location: String): List<JSONObject> {
    val source = this
    return buildList(source.length()) {
        for (index in 0 until source.length()) {
            val value = source.get(index)
            require(value is JSONObject) { "$location[$index] 必须是对象" }
            add(value)
        }
    }
}

internal fun JSONArray.contractStrings(location: String): List<String> {
    val source = this
    return buildList(source.length()) {
        for (index in 0 until source.length()) {
            val value = source.get(index)
            require(value is String) { "$location[$index] 必须是字符串" }
            add(value.trim().also { require(it.isNotBlank()) { "$location[$index] 不能为空" } })
        }
    }
}

internal fun JSONObject.requireContractStrings(key: String, location: String): List<String> =
    requireContractArray(key, location).contractStrings("$location.$key")

internal fun JSONObject.requireContractObjects(key: String, location: String): List<JSONObject> =
    requireContractArray(key, location).contractObjects("$location.$key")
