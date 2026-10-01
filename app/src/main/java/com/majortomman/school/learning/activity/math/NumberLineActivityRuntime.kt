package com.majortomman.school.learning.activity.math

import com.majortomman.school.learning.activity.ActivityCapabilityKeys
import com.majortomman.school.learning.activity.ActivityEvent
import com.majortomman.school.learning.activity.ActivityId
import com.majortomman.school.learning.activity.ActivityResult
import com.majortomman.school.learning.activity.ActivityRuntimeHandler
import com.majortomman.school.learning.activity.ActivitySpec
import com.majortomman.school.learning.activity.ActivityState
import com.majortomman.school.learning.activity.ActivityTransition
import com.majortomman.school.learning.activity.PlaceOnNumberLineActivitySpec
import com.majortomman.school.learning.activity.ResetActivity
import com.majortomman.school.learning.activity.SubmitActivity
import com.majortomman.school.learning.capability.CapabilityKey
import kotlin.math.round

data class PositionSelected(val value: Double) : ActivityEvent {
    init {
        require(value.isFinite()) { "position value 必须是有限数" }
    }
}

data class NumberLinePositionState(
    override val spec: PlaceOnNumberLineActivitySpec,
    val selectedValue: Double,
) : ActivityState

data class NumberPositionResult(
    override val activityId: ActivityId,
    val value: Double,
) : ActivityResult

object PlaceOnNumberLineActivityHandler : ActivityRuntimeHandler {
    override val capability: CapabilityKey = ActivityCapabilityKeys.PLACE_ON_NUMBER_LINE
    override val schemaVersion: Int = 1

    override fun initialState(spec: ActivitySpec): ActivityState {
        require(spec is PlaceOnNumberLineActivitySpec) { "place-on-number-line 收到错误 spec" }
        return NumberLinePositionState(spec, snap(spec, spec.initialValue))
    }

    override fun reduce(state: ActivityState, event: ActivityEvent): ActivityTransition {
        require(state is NumberLinePositionState) { "place-on-number-line 收到错误 state" }
        return when (event) {
            is PositionSelected -> ActivityTransition(state.copy(selectedValue = snap(state.spec, event.value)))
            SubmitActivity -> ActivityTransition(state, NumberPositionResult(state.spec.id, state.selectedValue))
            ResetActivity -> ActivityTransition(state.copy(selectedValue = snap(state.spec, state.spec.initialValue)))
            else -> ActivityTransition(state)
        }
    }

    private fun snap(spec: PlaceOnNumberLineActivitySpec, value: Double): Double {
        val snapped = spec.min + round((value - spec.min) / spec.step) * spec.step
        return snapped.coerceIn(spec.min, spec.max)
    }
}
