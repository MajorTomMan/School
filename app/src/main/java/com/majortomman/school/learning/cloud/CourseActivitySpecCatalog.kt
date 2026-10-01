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
