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
    val SELECT_ONE = CapabilityKey("core.select-one")
    val ORDER = CapabilityKey("core.order")
    val MATCH = CapabilityKey("core.match")
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

data class ActivityOption(
    val id: String,
    val label: String,
) {
    init {
        require(ID.matches(id)) { "activity option id 格式无效：$id" }
        require(label.isNotBlank()) { "activity option label 不能为空" }
    }

    private companion object {
        val ID = Regex("^[A-Za-z0-9._:-]+$")
    }
}

data class SelectOneActivitySpec(
    override val id: ActivityId,
    val options: List<ActivityOption>,
) : ActivitySpec {
    override val capability: CapabilityKey = ActivityCapabilityKeys.SELECT_ONE
    override val schemaVersion: Int = 1

    init {
        require(options.size in 2..12) { "select-one options 数量必须在 2..12" }
        require(options.map(ActivityOption::id).distinct().size == options.size) { "select-one option id 不能重复" }
    }
}

data class OrderActivitySpec(
    override val id: ActivityId,
    val items: List<ActivityOption>,
) : ActivitySpec {
    override val capability: CapabilityKey = ActivityCapabilityKeys.ORDER
    override val schemaVersion: Int = 1

    init {
        require(items.size in 2..12) { "order items 数量必须在 2..12" }
        require(items.map(ActivityOption::id).distinct().size == items.size) { "order item id 不能重复" }
    }
}

data class MatchActivitySpec(
    override val id: ActivityId,
    val leftItems: List<ActivityOption>,
    val rightItems: List<ActivityOption>,
) : ActivitySpec {
    override val capability: CapabilityKey = ActivityCapabilityKeys.MATCH
    override val schemaVersion: Int = 1

    init {
        require(leftItems.size in 2..12) { "match leftItems 数量必须在 2..12" }
        require(rightItems.size == leftItems.size) { "match 左右项目数量必须一致" }
        require(leftItems.map(ActivityOption::id).distinct().size == leftItems.size) { "match left item id 不能重复" }
        require(rightItems.map(ActivityOption::id).distinct().size == rightItems.size) { "match right item id 不能重复" }
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
        val minFloat = min.toFloat()
        val maxFloat = max.toFloat()
        val stepFloat = step.toFloat()
        require(minFloat.isFinite() && maxFloat.isFinite() && maxFloat > minFloat) {
            "number-line activity 范围超出 Float 绘制精度"
        }
        require(stepFloat.isFinite() && stepFloat > 0f) { "number-line activity step 超出 Float 绘制精度" }
        require((max - min) / step <= 80.0 + 1e-9) { "number-line activity 刻度数量不能超过 80" }
        require(initialValue in min..max) { "number-line activity initialValue 必须位于 min..max" }
    }
}
