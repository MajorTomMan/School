package com.majortomman.school.learning.knowledge

@JvmInline
value class KnowledgePointId(val value: String) {
    init {
        require(value.isNotBlank()) { "knowledgePointId 不能为空" }
    }

    override fun toString(): String = value
}
