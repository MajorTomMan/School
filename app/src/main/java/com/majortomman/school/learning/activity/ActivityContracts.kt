package com.majortomman.school.learning.activity

import com.majortomman.school.learning.capability.CapabilityKey

@JvmInline
value class ActivityId(val value: String) {
    init {
        require(ID.matches(value)) { "activity id 格式无效：$value" }
    }

    override fun toString(): String = value

    private companion object {
        val ID = Regex("^[A-Za-z0-9._:-]+$")
    }
}

interface ActivitySpec {
    val id: ActivityId
    val capability: CapabilityKey
    val schemaVersion: Int
}

object ActivityCapabilityKeys {
    val TEXT_ANSWER = CapabilityKey("core.text-answer")
    val PLACE_ON_NUMBER_LINE = CapabilityKey("mathematics.place-on-number-line")
}

data class TextAnswerActivitySpec(
    override val id: ActivityId,
    val placeholder: String? = null,
) : ActivitySpec {
    override val capability: CapabilityKey = ActivityCapabilityKeys.TEXT_ANSWER
    override val schemaVersion: Int = 1

    init {
        require(placeholder == null || placeholder.isNotBlank()) { "activity placeholder 不能为空字符串" }
    }
}

data class PlaceOnNumberLineActivitySpec(
    override val id: ActivityId,
    val min: Double,
    val max: Double,
    val step: Double,
    val initialValue: Double = 0.0,
) : ActivitySpec {
    override val capability: CapabilityKey = ActivityCapabilityKeys.PLACE_ON_NUMBER_LINE
    override val schemaVersion: Int = 1

    init {
        require(min.isFinite() && max.isFinite() && step.isFinite() && initialValue.isFinite()) {
            "number-line activity 参数必须是有限数"
        }
        require(max > min) { "number-line activity max 必须大于 min" }
        require(step > 0.0) { "number-line activity step 必须大于 0" }
        require((max - min) / step <= 80.0 + 1e-9) { "number-line activity 刻度数量不能超过 80" }
        require(initialValue in min..max) { "number-line activity initialValue 必须位于 min..max" }
    }
}
