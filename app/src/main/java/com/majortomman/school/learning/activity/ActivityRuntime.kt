package com.majortomman.school.learning.activity

import com.majortomman.school.learning.capability.CapabilityKey

interface ActivityEvent

data class TextChanged(val value: String) : ActivityEvent

data object SubmitActivity : ActivityEvent

data object ResetActivity : ActivityEvent

interface ActivityState {
    val spec: ActivitySpec
}

data class TextAnswerActivityState(
    override val spec: TextAnswerActivitySpec,
    val draft: String = "",
) : ActivityState

interface ActivityResult {
    val activityId: ActivityId
}

interface TextualActivityResult : ActivityResult {
    val value: String
}

interface NumericActivityResult : ActivityResult {
    val value: Double
}

data class TextAnswerActivityResult(
    override val activityId: ActivityId,
    override val value: String,
) : TextualActivityResult

data class ActivityTransition(
    val state: ActivityState,
    val result: ActivityResult? = null,
)

interface ActivityRuntimeHandler {
    val capability: CapabilityKey
    val schemaVersion: Int

    fun initialState(spec: ActivitySpec): ActivityState

    fun reduce(state: ActivityState, event: ActivityEvent): ActivityTransition
}

object SchoolActivityRuntimeCatalog {
    private val handlers = linkedMapOf<CapabilityKey, ActivityRuntimeHandler>()

    fun install(handler: ActivityRuntimeHandler) {
        synchronized(this) {
            val existing = handlers[handler.capability]
            if (existing != null) {
                require(existing.schemaVersion == handler.schemaVersion) {
                    "activity runtime ${handler.capability} 已注册 schemaVersion=${existing.schemaVersion}"
                }
                return
            }
            handlers[handler.capability] = handler
        }
    }

    fun requireHandler(spec: ActivitySpec): ActivityRuntimeHandler {
        val handler = synchronized(this) { handlers[spec.capability] }
            ?: error("未安装 Activity capability：${spec.capability}")
        require(handler.schemaVersion == spec.schemaVersion) {
            "Activity capability ${spec.capability} schemaVersion 不兼容：course=${spec.schemaVersion}, app=${handler.schemaVersion}"
        }
        return handler
    }
}

object CoreTextAnswerActivityHandler : ActivityRuntimeHandler {
    override val capability: CapabilityKey = ActivityCapabilityKeys.TEXT_ANSWER
    override val schemaVersion: Int = 1

    override fun initialState(spec: ActivitySpec): ActivityState {
        require(spec is TextAnswerActivitySpec) { "core.text-answer 收到错误 spec：${spec::class.simpleName}" }
        return TextAnswerActivityState(spec)
    }

    override fun reduce(state: ActivityState, event: ActivityEvent): ActivityTransition {
        require(state is TextAnswerActivityState) { "core.text-answer 收到错误 state：${state::class.simpleName}" }
        return when (event) {
            is TextChanged -> ActivityTransition(state.copy(draft = event.value.take(MAX_TEXT_LENGTH)))
            SubmitActivity -> {
                val value = state.draft.trim()
                ActivityTransition(
                    state = state,
                    result = if (value.isBlank()) null else TextAnswerActivityResult(state.spec.id, value),
                )
            }
            ResetActivity -> ActivityTransition(state.copy(draft = ""))
            else -> ActivityTransition(state)
        }
    }

    private const val MAX_TEXT_LENGTH = 2_000
}

class ActivityRuntime(spec: ActivitySpec) {
    private val handler = SchoolActivityRuntimeCatalog.requireHandler(spec)

    var state: ActivityState = handler.initialState(spec)
        private set

    fun dispatch(event: ActivityEvent): ActivityTransition {
        val transition = handler.reduce(state, event)
        require(transition.state.spec.id == state.spec.id) { "Activity handler 不能替换 activity id" }
        require(transition.state.spec.capability == state.spec.capability) { "Activity handler 不能替换 capability" }
        state = transition.state
        return transition
    }
}
