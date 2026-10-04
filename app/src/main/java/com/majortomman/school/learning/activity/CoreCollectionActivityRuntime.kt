package com.majortomman.school.learning.activity

import com.majortomman.school.learning.capability.CapabilityKey

data class SelectOption(val optionId: String) : ActivityEvent

enum class MoveDirection {
    UP,
    DOWN,
}

data class MoveOrderItem(
    val itemId: String,
    val direction: MoveDirection,
) : ActivityEvent

data class SelectMatchLeft(val itemId: String) : ActivityEvent

data class SelectMatchRight(val itemId: String) : ActivityEvent

data class SelectOneActivityState(
    override val spec: SelectOneActivitySpec,
    val selectedId: String? = null,
) : ActivityState

data class OrderActivityState(
    override val spec: OrderActivitySpec,
    val orderedIds: List<String>,
) : ActivityState

data class MatchActivityState(
    override val spec: MatchActivitySpec,
    val selectedLeftId: String? = null,
    val matches: Map<String, String> = emptyMap(),
) : ActivityState

data class CanonicalTextActivityResult(
    override val activityId: ActivityId,
    override val value: String,
) : TextualActivityResult

object SelectOneActivityHandler : ActivityRuntimeHandler {
    override val capability: CapabilityKey = ActivityCapabilityKeys.SELECT_ONE
    override val schemaVersion: Int = 1

    override fun initialState(spec: ActivitySpec): ActivityState {
        require(spec is SelectOneActivitySpec) { "core.select-one 收到错误 spec" }
        return SelectOneActivityState(spec)
    }

    override fun reduce(state: ActivityState, event: ActivityEvent): ActivityTransition {
        require(state is SelectOneActivityState) { "core.select-one 收到错误 state" }
        return when (event) {
            is SelectOption -> {
                val option = state.spec.options.firstOrNull { it.id == event.optionId }
                    ?: return ActivityTransition(state)
                ActivityTransition(state.copy(selectedId = option.id))
            }
            SubmitActivity -> {
                val selectedId = state.selectedId
                ActivityTransition(
                    state = state,
                    result = selectedId?.let { CanonicalTextActivityResult(state.spec.id, it) },
                )
            }
            ResetActivity -> ActivityTransition(state.copy(selectedId = null))
            else -> ActivityTransition(state)
        }
    }
}

object OrderActivityHandler : ActivityRuntimeHandler {
    override val capability: CapabilityKey = ActivityCapabilityKeys.ORDER
    override val schemaVersion: Int = 1

    override fun initialState(spec: ActivitySpec): ActivityState {
        require(spec is OrderActivitySpec) { "core.order 收到错误 spec" }
        return OrderActivityState(spec, spec.items.map(ActivityOption::id))
    }

    override fun reduce(state: ActivityState, event: ActivityEvent): ActivityTransition {
        require(state is OrderActivityState) { "core.order 收到错误 state" }
        return when (event) {
            is MoveOrderItem -> ActivityTransition(move(state, event))
            SubmitActivity -> ActivityTransition(
                state = state,
                result = CanonicalTextActivityResult(state.spec.id, state.orderedIds.joinToString("|")),
            )
            ResetActivity -> ActivityTransition(state.copy(orderedIds = state.spec.items.map(ActivityOption::id)))
            else -> ActivityTransition(state)
        }
    }

    private fun move(state: OrderActivityState, event: MoveOrderItem): OrderActivityState {
        val from = state.orderedIds.indexOf(event.itemId)
        if (from < 0) return state
        val to = when (event.direction) {
            MoveDirection.UP -> from - 1
            MoveDirection.DOWN -> from + 1
        }
        if (to !in state.orderedIds.indices) return state
        val changed = state.orderedIds.toMutableList()
        val item = changed.removeAt(from)
        changed.add(to, item)
        return state.copy(orderedIds = changed)
    }
}

object MatchActivityHandler : ActivityRuntimeHandler {
    override val capability: CapabilityKey = ActivityCapabilityKeys.MATCH
    override val schemaVersion: Int = 1

    override fun initialState(spec: ActivitySpec): ActivityState {
        require(spec is MatchActivitySpec) { "core.match 收到错误 spec" }
        return MatchActivityState(spec)
    }

    override fun reduce(state: ActivityState, event: ActivityEvent): ActivityTransition {
        require(state is MatchActivityState) { "core.match 收到错误 state" }
        return when (event) {
            is SelectMatchLeft -> {
                val exists = state.spec.leftItems.any { it.id == event.itemId }
                if (!exists) ActivityTransition(state)
                else ActivityTransition(state.copy(selectedLeftId = event.itemId))
            }
            is SelectMatchRight -> pair(state, event.itemId)
            SubmitActivity -> submit(state)
            ResetActivity -> ActivityTransition(state.copy(selectedLeftId = null, matches = emptyMap()))
            else -> ActivityTransition(state)
        }
    }

    private fun pair(state: MatchActivityState, rightId: String): ActivityTransition {
        val leftId = state.selectedLeftId ?: return ActivityTransition(state)
        if (state.spec.rightItems.none { it.id == rightId }) return ActivityTransition(state)
        val changed = state.matches
            .filterValues { it != rightId }
            .toMutableMap()
        changed[leftId] = rightId
        return ActivityTransition(state.copy(selectedLeftId = null, matches = changed))
    }

    private fun submit(state: MatchActivityState): ActivityTransition {
        val leftIds = state.spec.leftItems.map(ActivityOption::id)
        if (!leftIds.all(state.matches::containsKey)) return ActivityTransition(state)
        val value = leftIds.joinToString("|") { leftId -> "$leftId=${state.matches.getValue(leftId)}" }
        return ActivityTransition(state, CanonicalTextActivityResult(state.spec.id, value))
    }
}
