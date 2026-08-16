package com.majortomman.school.visualization

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Small deterministic math-expression language for course-controlled visualization parameters.
 *
 * It deliberately has no access to reflection, classes, files, network, callbacks or host APIs.
 * The language only contains numeric literals, named variables, constants, arithmetic operators,
 * parentheses and a fixed function allowlist.
 */
internal class SafeMathExpression private constructor(
    val source: String,
    private val root: Node,
    val variables: Set<String>,
) {
    fun evaluate(values: Map<String, Double>): Double {
        require(variables.all(values::containsKey)) { "数学表达式缺少变量：${(variables - values.keys).sorted().joinToString()}" }
        return root.evaluate(values)
    }

    companion object {
        private const val MAX_SOURCE_LENGTH = 256
        private const val MAX_TOKEN_COUNT = 160
        private const val MAX_AST_DEPTH = 32

        fun parse(raw: String): SafeMathExpression {
            val source = raw.trim()
            require(source.isNotBlank()) { "数学表达式不能为空" }
            require(source.length <= MAX_SOURCE_LENGTH) { "数学表达式不能超过 $MAX_SOURCE_LENGTH 个字符" }
            val parser = Parser(source, MAX_TOKEN_COUNT, MAX_AST_DEPTH)
            val root = parser.parse()
            return SafeMathExpression(source, root, parser.variables.toSet())
        }
    }

    private sealed interface Node {
        fun evaluate(values: Map<String, Double>): Double
    }

    private data class ConstantNode(val value: Double) : Node {
        override fun evaluate(values: Map<String, Double>): Double = value
    }

    private data class VariableNode(val name: String) : Node {
        override fun evaluate(values: Map<String, Double>): Double = values.getValue(name)
    }

    private data class UnaryNode(val operator: Char, val operand: Node) : Node {
        override fun evaluate(values: Map<String, Double>): Double {
            val value = operand.evaluate(values)
            return when (operator) {
                '+' -> value
                '-' -> -value
                else -> error("不支持的一元运算符：$operator")
            }
        }
    }

    private data class BinaryNode(val left: Node, val operator: Char, val right: Node) : Node {
        override fun evaluate(values: Map<String, Double>): Double {
            val leftValue = left.evaluate(values)
            val rightValue = right.evaluate(values)
            return when (operator) {
                '+' -> leftValue + rightValue
                '-' -> leftValue - rightValue
                '*' -> leftValue * rightValue
                '/' -> if (abs(rightValue) < 1e-15) Double.NaN else leftValue / rightValue
                '^' -> leftValue.pow(rightValue)
                else -> error("不支持的二元运算符：$operator")
            }
        }
    }

    private data class FunctionNode(val name: String, val argument: Node) : Node {
        override fun evaluate(values: Map<String, Double>): Double {
            val value = argument.evaluate(values)
            return when (name) {
                "abs" -> abs(value)
                "sqrt" -> if (value < 0.0) Double.NaN else sqrt(value)
                "sin" -> sin(value)
                "cos" -> cos(value)
                "tan" -> tan(value)
                "ln" -> if (value <= 0.0) Double.NaN else ln(value)
                "log" -> if (value <= 0.0) Double.NaN else log10(value)
                "exp" -> exp(value)
                else -> error("不支持的数学函数：$name")
            }
        }
    }

    private class Parser(
        private val source: String,
        private val maxTokenCount: Int,
        private val maxDepth: Int,
    ) {
        private var index = 0
        private var tokenCount = 0
        val variables = linkedSetOf<String>()

        fun parse(): Node {
            val node = parseExpression(0)
            skipWhitespace()
            require(index == source.length) { "数学表达式在位置 $index 存在无法识别的内容" }
            return node
        }

        private fun parseExpression(depth: Int): Node {
            checkDepth(depth)
            var node = parseTerm(depth + 1)
            while (true) {
                skipWhitespace()
                val operator = peek()
                if (operator != '+' && operator != '-') return node
                consume()
                node = BinaryNode(node, operator, parseTerm(depth + 1))
            }
        }

        private fun parseTerm(depth: Int): Node {
            checkDepth(depth)
            var node = parsePower(depth + 1)
            while (true) {
                skipWhitespace()
                val operator = peek()
                if (operator != '*' && operator != '/') return node
                consume()
                node = BinaryNode(node, operator, parsePower(depth + 1))
            }
        }

        private fun parsePower(depth: Int): Node {
            checkDepth(depth)
            val left = parseUnary(depth + 1)
            skipWhitespace()
            if (peek() != '^') return left
            consume()
            return BinaryNode(left, '^', parsePower(depth + 1))
        }

        private fun parseUnary(depth: Int): Node {
            checkDepth(depth)
            skipWhitespace()
            val operator = peek()
            return if (operator == '+' || operator == '-') {
                consume()
                UnaryNode(operator, parseUnary(depth + 1))
            } else {
                parsePrimary(depth + 1)
            }
        }

        private fun parsePrimary(depth: Int): Node {
            checkDepth(depth)
            skipWhitespace()
            val char = peek()
            if (char == '(') {
                consume()
                val node = parseExpression(depth + 1)
                skipWhitespace()
                require(peek() == ')') { "数学表达式缺少右括号" }
                consume()
                return node
            }
            if (char.isDigit() || char == '.') return ConstantNode(parseNumber())
            if (char.isLetter()) {
                val name = parseIdentifier().lowercase()
                skipWhitespace()
                if (peek() == '(') {
                    require(name in FUNCTIONS) { "不支持的数学函数：$name" }
                    consume()
                    val argument = parseExpression(depth + 1)
                    skipWhitespace()
                    require(peek() == ')') { "函数 $name 缺少右括号" }
                    consume()
                    return FunctionNode(name, argument)
                }
                return when (name) {
                    "pi" -> ConstantNode(Math.PI)
                    "e" -> ConstantNode(Math.E)
                    else -> {
                        require(VARIABLE_PATTERN.matches(name)) { "变量名无效：$name" }
                        variables += name
                        VariableNode(name)
                    }
                }
            }
            throw IllegalArgumentException("数学表达式在位置 $index 缺少数值、变量或括号")
        }

        private fun parseNumber(): Double {
            val start = index
            var hasDigit = false
            var hasDot = false
            while (index < source.length) {
                val char = source[index]
                when {
                    char.isDigit() -> {
                        hasDigit = true
                        index += 1
                    }
                    char == '.' && !hasDot -> {
                        hasDot = true
                        index += 1
                    }
                    else -> break
                }
            }
            require(hasDigit) { "数学表达式在位置 $start 的数值无效" }
            countToken()
            val value = source.substring(start, index).toDoubleOrNull()
                ?: throw IllegalArgumentException("数学表达式数值无效：${source.substring(start, index)}")
            require(value.isFinite() && value.toFloat().isFinite()) { "数学表达式数值超出绘制范围" }
            return value
        }

        private fun parseIdentifier(): String {
            val start = index
            while (index < source.length && (source[index].isLetterOrDigit() || source[index] == '_')) index += 1
            countToken()
            return source.substring(start, index)
        }

        private fun consume(): Char {
            require(index < source.length) { "数学表达式意外结束" }
            countToken()
            return source[index++]
        }

        private fun peek(): Char {
            skipWhitespace()
            return source.getOrNull(index) ?: '\u0000'
        }

        private fun skipWhitespace() {
            while (index < source.length && source[index].isWhitespace()) index += 1
        }

        private fun countToken() {
            tokenCount += 1
            require(tokenCount <= maxTokenCount) { "数学表达式过于复杂" }
        }

        private fun checkDepth(depth: Int) {
            require(depth <= maxDepth) { "数学表达式嵌套层级过深" }
        }

        companion object {
            private val FUNCTIONS = setOf("abs", "sqrt", "sin", "cos", "tan", "ln", "log", "exp")
            private val VARIABLE_PATTERN = Regex("[a-z][a-z0-9_]{0,15}")
        }
    }
}
