package com.majortomman.school.learning.verification

import com.majortomman.school.visualization.VisualizationInvocation
import com.majortomman.school.visualization.VisualizationKey
import com.majortomman.school.visualization.VisualizationParameterValue
import com.majortomman.school.visualization.VisualizationParameters
import com.majortomman.school.visualization.VisualizationTexts
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

enum class LocalMathExpressionKind {
    VALUE,
    FUNCTION,
}

data class LocalMathExpressionResult(
    val kind: LocalMathExpressionKind,
    val normalizedExpression: String,
    val displayAnswer: String,
    val graph: VisualizationInvocation? = null,
)

object LocalMathExpressionVerifier {
    fun verify(raw: String): LocalMathExpressionResult {
        val normalized = normalizeInput(raw)
        val expression = VisualizationParameterValue.MathExpressionValue.parse(normalized)
        return when {
            expression.variables.isEmpty() -> verifyValue(normalized, expression)
            expression.variables == setOf("x") -> verifyFunction(normalized, expression)
            else -> throw IllegalArgumentException("当前数学验证只支持常量表达式，或仅含自变量 x 的函数。检测到：${expression.variables.sorted().joinToString()}。")
        }
    }

    private fun verifyValue(expressionText: String, expression: VisualizationParameterValue.MathExpressionValue): LocalMathExpressionResult {
        val value = expression.evaluate(emptyMap())
        require(value.isFinite()) { "这个表达式在实数范围内没有可显示的有限结果。" }
        return LocalMathExpressionResult(
            kind = LocalMathExpressionKind.VALUE,
            normalizedExpression = expressionText,
            displayAnswer = formatNumber(value),
        )
    }

    private fun verifyFunction(expressionText: String, expression: VisualizationParameterValue.MathExpressionValue): LocalMathExpressionResult {
        val bounds = deriveGraphBounds(expression)
        val graph = VisualizationInvocation(
            renderer = VisualizationKey("mathematics.function.graph"),
            parameters = VisualizationParameters.of(
                "expression" to expression,
                "xMin" to VisualizationParameterValue.NumberValue(bounds.xMin),
                "xMax" to VisualizationParameterValue.NumberValue(bounds.xMax),
                "yMin" to VisualizationParameterValue.NumberValue(bounds.yMin),
                "yMax" to VisualizationParameterValue.NumberValue(bounds.yMax),
            ),
            texts = VisualizationTexts.of(
                "title" to "y = $expressionText",
                "note" to "本地解析 · 双指缩放 / 双击复位",
            ),
        )
        return LocalMathExpressionResult(
            kind = LocalMathExpressionKind.FUNCTION,
            normalizedExpression = expressionText,
            displayAnswer = "y = $expressionText",
            graph = graph,
        )
    }

    private fun normalizeInput(raw: String): String {
        var text = raw.trim().lowercase()
        require(text.isNotBlank()) { "请输入数学表达式。" }
        text = text
            .replace('−', '-')
            .replace('—', '-')
            .replace('×', '*')
            .replace('·', '*')
            .replace('÷', '/')
            .replace('（', '(')
            .replace('）', ')')
            .replace('π', 'p')
            .replace(" ", "")
        text = text.replace("p", "pi")
        text = text.replace(Regex("√\\(([^()]*)\\)"), "sqrt($1)")
        text = text.replace(Regex("√([a-z0-9.]+)"), "sqrt($1)")
        text = when {
            text.startsWith("f(x)=") -> text.removePrefix("f(x)=")
            text.startsWith("y=") -> text.removePrefix("y=")
            else -> text
        }
        require('=' !in text) { "当前先支持数学表达式和 y=f(x) 形式；方程验证会作为后续本地能力单独接入。" }
        text = text.replace(Regex("(?<=[0-9x)])(?=[x(])"), "*")
        text = text.replace(Regex("(?<=\\))(?=[0-9x(])"), "*")
        text = text.replace(Regex("(?<=[0-9x)])(?=pi(?:\\b|\\())"), "*")
        text = text.replace(Regex("(?<=pi)(?=[0-9x(])"), "*")
        return text
    }

    private fun deriveGraphBounds(expression: VisualizationParameterValue.MathExpressionValue): GraphBounds {
        val xMin = -6.0
        val xMax = 6.0
        val values = mutableListOf<Double>()
        val sampleCount = 240
        for (index in 0..sampleCount) {
            val x = xMin + (xMax - xMin) * index / sampleCount.toDouble()
            val y = runCatching { expression.evaluate(mapOf("x" to x)) }.getOrNull()
            if (y != null && y.isFinite() && abs(y) <= 50.0) values += y
        }
        if (values.isEmpty()) return GraphBounds(xMin, xMax, -6.0, 6.0)

        var yMin = values.minOrNull() ?: -6.0
        var yMax = values.maxOrNull() ?: 6.0
        if (abs(yMax - yMin) < 1e-9) {
            val center = yMin
            yMin = center - 2.0
            yMax = center + 2.0
        } else {
            val padding = max(0.8, (yMax - yMin) * 0.12)
            yMin -= padding
            yMax += padding
        }
        yMin = max(-50.0, yMin)
        yMax = min(50.0, yMax)
        if (yMax - yMin < 4.0) {
            val center = (yMin + yMax) / 2.0
            yMin = max(-50.0, center - 2.0)
            yMax = min(50.0, center + 2.0)
        }
        return GraphBounds(xMin, xMax, yMin, yMax)
    }

    private fun formatNumber(value: Double): String {
        val normalized = if (abs(value) < 1e-12) 0.0 else value
        val rounded = round(normalized)
        if (abs(normalized - rounded) < 1e-10 && rounded >= Long.MIN_VALUE.toDouble() && rounded <= Long.MAX_VALUE.toDouble()) {
            return rounded.toLong().toString()
        }
        return BigDecimal.valueOf(normalized).setScale(10, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    }

    private data class GraphBounds(
        val xMin: Double,
        val xMax: Double,
        val yMin: Double,
        val yMax: Double,
    )
}
