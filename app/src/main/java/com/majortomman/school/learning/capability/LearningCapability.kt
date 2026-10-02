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
