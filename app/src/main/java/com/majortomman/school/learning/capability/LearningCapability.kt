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
    fun supports(key: CapabilityKey, schemaVersion: Int = 1): Boolean =
        capabilitiesByKey[key]?.schemaVersion == schemaVersion

    fun capabilities(subject: SubjectId? = null, kind: CapabilityKind? = null): List<CapabilityDescriptor> =
        capabilitiesByKey.values.filter { descriptor ->
            (subject == null || descriptor.subject == subject) && (kind == null || descriptor.kind == kind)
        }
}
