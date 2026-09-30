package com.majortomman.school.learning.activity

sealed interface ActivityEvent {
    data class TextChanged(val value: String) : ActivityEvent
    data object Submit : ActivityEvent
    data object Reset : ActivityEvent
}

sealed interface ActivityState {
    val spec: ActivitySpec

    data class TextAnswer(
        override val spec: TextAnswerActivitySpec,
        val draft: String = "",
    ) : ActivityState
}

sealed interface ActivityResult {
    val activityId: ActivityId

    data class TextAnswer(
        override val activityId: ActivityId,
        val value: String,
    ) : ActivityResult
}

data class ActivityTransition(
    val state: ActivityState,
    val result: ActivityResult? = null,
)

class ActivityRuntime(private val spec: ActivitySpec) {
    var state: ActivityState = initialState(spec)
        private set

    fun dispatch(event: ActivityEvent): ActivityTransition {
        val transition = when (val current = state) {
            is ActivityState.TextAnswer -> reduceTextAnswer(current, event)
        }
        state = transition.state
        return transition
    }

    private fun reduceTextAnswer(state: ActivityState.TextAnswer, event: ActivityEvent): ActivityTransition = when (event) {
        is ActivityEvent.TextChanged -> ActivityTransition(state.copy(draft = event.value.take(MAX_TEXT_LENGTH)))
        ActivityEvent.Submit -> {
            val value = state.draft.trim()
            ActivityTransition(state, if (value.isBlank()) null else ActivityResult.TextAnswer(state.spec.id, value))
        }
        ActivityEvent.Reset -> ActivityTransition(state.copy(draft = ""))
    }

    private companion object {
        const val MAX_TEXT_LENGTH = 2_000

        fun initialState(spec: ActivitySpec): ActivityState = when (spec) {
            is TextAnswerActivitySpec -> ActivityState.TextAnswer(spec)
        }
    }
}
