package com.majortomman.school.learning.capability

@JvmInline
value class CapabilityKey(val value: String) {
    init {
        require(KEY.matches(value)) { "capability key 格式无效：$value" }
    }

    override fun toString(): String = value

    private companion object {
        val KEY = Regex("^[a-z][a-z0-9]*(?:[.-][a-z0-9]+)+$")
    }
}

@JvmInline
value class SubjectId(val value: String) {
    init {
        require(ID.matches(value)) { "subject id 格式无效：$value" }
    }

    override fun toString(): String = value

    private companion object {
        val ID = Regex("^[a-z][a-z0-9_-]*$")
    }
}

enum class CapabilityKind {
    VISUALIZATION,
    ACTIVITY,
    VERIFICATION,
    NOTATION,
    PRESENTATION,
}

data class CapabilityDescriptor(
    val key: CapabilityKey,
    val subject: SubjectId,
    val kind: CapabilityKind,
    val schemaVersion: Int = 1,
) {
    init {
        require(schemaVersion > 0) { "capability schemaVersion 必须大于 0" }
    }
}

interface SubjectModule {
    val id: SubjectId
    val capabilities: List<CapabilityDescriptor>
}

class CapabilityRegistry(modules: List<SubjectModule>) {
    private val modulesById: Map<SubjectId, SubjectModule>
    private val capabilitiesByKey: Map<CapabilityKey, CapabilityDescriptor>

    init {
        val duplicateModules = modules.groupBy(SubjectModule::id).filterValues { it.size > 1 }.keys
        require(duplicateModules.isEmpty()) { "subject module 重复：${duplicateModules.joinToString()}" }

        val descriptors = modules.flatMap { module ->
            module.capabilities.onEach { descriptor ->
                require(descriptor.subject == module.id) {
                    "capability ${descriptor.key} 的 subject ${descriptor.subject} 与 module ${module.id} 不一致"
                }
            }
        }
        val duplicateCapabilities = descriptors.groupBy(CapabilityDescriptor::key).filterValues { it.size > 1 }.keys
        require(duplicateCapabilities.isEmpty()) { "capability key 重复：${duplicateCapabilities.joinToString()}" }

        modulesById = modules.associateBy(SubjectModule::id)
        capabilitiesByKey = descriptors.associateBy(CapabilityDescriptor::key)
    }

    fun module(id: SubjectId): SubjectModule? = modulesById[id]

    fun capability(key: CapabilityKey): CapabilityDescriptor? = capabilitiesByKey[key]

    fun supports(key: CapabilityKey, kind: CapabilityKind, schemaVersion: Int = 1): Boolean =
        capabilitiesByKey[key]?.let { it.kind == kind && it.schemaVersion == schemaVersion } == true

    fun capabilities(subject: SubjectId? = null, kind: CapabilityKind? = null): List<CapabilityDescriptor> =
        capabilitiesByKey.values.filter { descriptor ->
            (subject == null || descriptor.subject == subject) && (kind == null || descriptor.kind == kind)
        }
}

object SchoolCapabilityCatalog {
    private val modules = linkedMapOf<SubjectId, SubjectModule>()

    @Volatile
    private var registry = CapabilityRegistry(emptyList())

    fun install(module: SubjectModule) {
        synchronized(this) {
            val existing = modules[module.id]
            if (existing != null) {
                require(existing.capabilities == module.capabilities) {
                    "subject module ${module.id} 已使用不同 capability 集合注册"
                }
                return
            }
            val next = modules.toMutableMap().apply { put(module.id, module) }
            registry = CapabilityRegistry(next.values.toList())
            modules[module.id] = module
        }
    }

    fun requireSupported(key: CapabilityKey, kind: CapabilityKind, schemaVersion: Int) {
        require(registry.supports(key, kind, schemaVersion)) {
            "未安装或不兼容的 capability：$key kind=$kind schemaVersion=$schemaVersion"
        }
    }

    fun supports(key: CapabilityKey, kind: CapabilityKind, schemaVersion: Int = 1): Boolean =
        registry.supports(key, kind, schemaVersion)
}
