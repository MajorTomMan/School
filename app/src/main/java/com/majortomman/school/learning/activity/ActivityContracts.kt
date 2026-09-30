package com.majortomman.school.learning.activity

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

sealed interface ActivitySpec {
    val id: ActivityId
}

data class TextAnswerActivitySpec(
    override val id: ActivityId,
    val placeholder: String? = null,
) : ActivitySpec {
    init {
        require(placeholder == null || placeholder.isNotBlank()) { "activity placeholder 不能为空字符串" }
    }
}
