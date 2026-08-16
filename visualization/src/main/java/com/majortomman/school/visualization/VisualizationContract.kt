package com.majortomman.school.visualization

@JvmInline
value class VisualizationKey(val value: String) {
    init {
        require(KEY_PATTERN.matches(value)) { "可视化 renderer key 无效：$value" }
    }

    companion object {
        private val KEY_PATTERN = Regex("[a-z0-9][a-z0-9._-]*")
    }
}

sealed interface VisualizationParameterValue {
    data class NumberValue(val value: Double) : VisualizationParameterValue {
        init {
            require(value.isFinite() && value.toFloat().isFinite()) { "可视化数值参数必须是可安全绘制的有限数" }
        }
    }

    data class BooleanValue(val value: Boolean) : VisualizationParameterValue

    class NumberListValue(values: List<Double>) : VisualizationParameterValue {
        val values: List<Double> = values.toList()

        init {
            require(this.values.all { it.isFinite() && it.toFloat().isFinite() }) { "可视化数值列表只能包含可安全绘制的有限数" }
        }

        override fun equals(other: Any?): Boolean = other is NumberListValue && values == other.values
        override fun hashCode(): Int = values.hashCode()
        override fun toString(): String = "NumberListValue(values=$values)"
    }
}

class VisualizationParameters private constructor(private val values: Map<String, VisualizationParameterValue>) {
    val keys: Set<String> get() = values.keys

    fun value(name: String): VisualizationParameterValue? = values[name]
    fun number(name: String, default: Double = 0.0): Double = (values[name] as? VisualizationParameterValue.NumberValue)?.value ?: default
    fun boolean(name: String, default: Boolean = false): Boolean = (values[name] as? VisualizationParameterValue.BooleanValue)?.value ?: default
    fun numberList(name: String): List<Double> = (values[name] as? VisualizationParameterValue.NumberListValue)?.values.orEmpty()

    override fun equals(other: Any?): Boolean = other is VisualizationParameters && values == other.values
    override fun hashCode(): Int = values.hashCode()
    override fun toString(): String = "VisualizationParameters(values=$values)"

    companion object {
        val Empty = VisualizationParameters(emptyMap())

        fun of(values: Map<String, VisualizationParameterValue>): VisualizationParameters {
            require(values.keys.all(FIELD_PATTERN::matches)) { "可视化参数名无效" }
            return VisualizationParameters(values.toMap())
        }

        fun of(vararg values: Pair<String, VisualizationParameterValue>): VisualizationParameters = of(linkedMapOf(*values))
    }
}

class VisualizationTexts private constructor(private val values: Map<String, String>) {
    val keys: Set<String> get() = values.keys

    fun text(name: String, default: String = ""): String = values[name] ?: default

    override fun equals(other: Any?): Boolean = other is VisualizationTexts && values == other.values
    override fun hashCode(): Int = values.hashCode()
    override fun toString(): String = "VisualizationTexts(values=$values)"

    companion object {
        val Empty = VisualizationTexts(emptyMap())

        fun of(values: Map<String, String>): VisualizationTexts {
            require(values.keys.all(FIELD_PATTERN::matches)) { "可视化文本名无效" }
            return VisualizationTexts(values.toMap())
        }

        fun of(vararg values: Pair<String, String>): VisualizationTexts = of(linkedMapOf(*values))
    }
}

data class VisualizationInvocation(
    val renderer: VisualizationKey,
    val parameters: VisualizationParameters = VisualizationParameters.Empty,
    val texts: VisualizationTexts = VisualizationTexts.Empty,
)

enum class VisualizationParameterType {
    NUMBER,
    BOOLEAN,
    NUMBER_LIST,
}

data class VisualizationParameterSpec(
    val name: String,
    val type: VisualizationParameterType,
    val required: Boolean = true,
) {
    init {
        require(FIELD_PATTERN.matches(name)) { "可视化参数字段名无效：$name" }
    }
}

data class VisualizationTextSpec(
    val name: String,
    val required: Boolean = true,
    val allowBlank: Boolean = false,
) {
    init {
        require(FIELD_PATTERN.matches(name)) { "可视化文本字段名无效：$name" }
    }
}

data class VisualizationSchema(
    val parameters: List<VisualizationParameterSpec> = emptyList(),
    val texts: List<VisualizationTextSpec> = emptyList(),
) {
    init {
        require(parameters.map { it.name }.distinct().size == parameters.size) { "可视化参数 schema 存在重复字段" }
        require(texts.map { it.name }.distinct().size == texts.size) { "可视化文本 schema 存在重复字段" }
    }

    fun validate(invocation: VisualizationInvocation): List<String> = buildList {
        val parameterSpecs = parameters.associateBy { it.name }
        val textSpecs = texts.associateBy { it.name }

        val unknownParameters = invocation.parameters.keys - parameterSpecs.keys
        val unknownTexts = invocation.texts.keys - textSpecs.keys
        unknownParameters.sorted().forEach { add("不接受参数 $it") }
        unknownTexts.sorted().forEach { add("不接受文本 $it") }

        parameters.forEach { spec ->
            val value = invocation.parameters.value(spec.name)
            if (value == null) {
                if (spec.required) add("缺少参数 ${spec.name}")
            } else if (!spec.type.accepts(value)) {
                add("参数 ${spec.name} 类型应为 ${spec.type}")
            }
        }

        texts.forEach { spec ->
            val present = spec.name in invocation.texts.keys
            if (!present) {
                if (spec.required) add("缺少文本 ${spec.name}")
            } else if (!spec.allowBlank && invocation.texts.text(spec.name).isBlank()) {
                add("文本 ${spec.name} 不能为空")
            }
        }
    }

    private fun VisualizationParameterType.accepts(value: VisualizationParameterValue): Boolean = when (this) {
        VisualizationParameterType.NUMBER -> value is VisualizationParameterValue.NumberValue
        VisualizationParameterType.BOOLEAN -> value is VisualizationParameterValue.BooleanValue
        VisualizationParameterType.NUMBER_LIST -> value is VisualizationParameterValue.NumberListValue
    }
}

private val FIELD_PATTERN = Regex("[a-z][A-Za-z0-9_]*")
