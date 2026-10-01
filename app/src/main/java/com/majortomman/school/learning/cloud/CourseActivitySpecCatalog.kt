package com.majortomman.school.learning.cloud

import com.majortomman.school.learning.activity.ActivityId
import com.majortomman.school.learning.activity.ActivitySpec
import com.majortomman.school.learning.capability.CapabilityKey
import org.json.JSONObject

internal interface CourseActivitySpecDecoder {
    val capability: CapabilityKey
    val schemaVersion: Int

    fun decode(id: ActivityId, parameters: JSONObject): ActivitySpec
}

internal object CourseActivitySpecCatalog {
    private val decoders = linkedMapOf<CapabilityKey, CourseActivitySpecDecoder>()

    fun install(decoder: CourseActivitySpecDecoder) {
        synchronized(this) {
            val existing = decoders[decoder.capability]
            if (existing != null) {
                require(existing.schemaVersion == decoder.schemaVersion) {
                    "activity decoder ${decoder.capability} 已注册 schemaVersion=${existing.schemaVersion}"
                }
                return
            }
            decoders[decoder.capability] = decoder
        }
    }

    fun decode(
        id: ActivityId,
        capability: CapabilityKey,
        schemaVersion: Int,
        parameters: JSONObject,
    ): ActivitySpec {
        val decoder = synchronized(this) { decoders[capability] }
            ?: error("未安装 Activity decoder：$capability")
        require(decoder.schemaVersion == schemaVersion) {
            "Activity decoder $capability schemaVersion 不兼容：course=$schemaVersion, app=${decoder.schemaVersion}"
        }
        return decoder.decode(id, parameters).also { spec ->
            require(spec.capability == capability && spec.schemaVersion == schemaVersion) {
                "Activity decoder $capability 返回了不一致的 spec"
            }
        }
    }
}


internal fun JSONObject.requireActivityShape(
    required: Set<String> = emptySet(),
    optional: Set<String> = emptySet(),
) {
    val actual = keys().asSequence().toSet()
    val unknown = actual - required - optional
    val missing = required - actual
    require(unknown.isEmpty()) { "activity parameters 包含未知字段：" + unknown.sorted() }
    require(missing.isEmpty()) { "activity parameters 缺少字段：" + missing.sorted() }
}

internal fun JSONObject.optionalActivityText(key: String): String? {
    if (!has(key) || isNull(key)) return null
    require(get(key) is String) { "activity parameter " + key + " 必须是字符串" }
    return getString(key).trim().also { require(it.isNotEmpty()) { "activity parameter " + key + " 不能为空" } }
}

internal fun JSONObject.activityNumber(key: String): Double {
    val raw = get(key)
    require(raw is Number && raw !is Boolean) { "activity parameter " + key + " 必须是 number" }
    return raw.toDouble().also { require(it.isFinite()) { "activity parameter " + key + " 必须是有限数" } }
}
