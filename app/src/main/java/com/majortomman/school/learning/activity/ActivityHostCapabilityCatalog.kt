package com.majortomman.school.learning.activity

import com.majortomman.school.learning.capability.CapabilityKey

object SchoolActivityHostCapabilityCatalog {
    private val schemaVersions = linkedMapOf<CapabilityKey, Int>()

    internal fun install(capability: CapabilityKey, schemaVersion: Int) {
        require(schemaVersion > 0) { "Activity UI host schemaVersion 必须大于 0" }
        synchronized(this) {
            val existing = schemaVersions[capability]
            if (existing != null) {
                require(existing == schemaVersion) {
                    "Activity UI host $capability 已注册 schemaVersion=$existing"
                }
                return
            }
            schemaVersions[capability] = schemaVersion
        }
    }

    fun requireHost(spec: ActivitySpec) {
        val installed = synchronized(this) { schemaVersions[spec.capability] }
            ?: error("未安装 Activity UI host：${spec.capability}")
        require(installed == spec.schemaVersion) {
            "Activity UI host ${spec.capability} schemaVersion 不兼容：course=${spec.schemaVersion}, app=$installed"
        }
    }
}
