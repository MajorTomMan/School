package com.majortomman.school.learning.verification.math

import com.majortomman.school.learning.science.expression.BigRational
import com.majortomman.school.learning.science.expression.ScienceExpression
import com.majortomman.school.learning.science.expression.ScienceExpressionEvaluator
import com.majortomman.school.learning.science.expression.ScienceExpressionParser
import com.majortomman.school.learning.science.expression.ScienceExpressionRenderer
import com.majortomman.school.learning.science.expression.ScienceExpressionSimplifier
import com.majortomman.school.learning.science.expression.ScienceNumberDomain
import com.majortomman.school.learning.science.math.AlgebraSolver
import com.majortomman.school.learning.science.math.AlgebraStep
import com.majortomman.school.learning.science.math.EquationSolution
import com.majortomman.school.learning.science.math.Polynomial
import com.majortomman.school.learning.verification.VerificationSubject
import com.majortomman.school.learning.verification.core.SubjectEngine
import com.majortomman.school.learning.verification.core.VerificationArtifact
import com.majortomman.school.learning.verification.core.VerificationMode
import com.majortomman.school.learning.verification.core.VerificationProblemType
import com.majortomman.school.learning.verification.core.VerificationRequest
import com.majortomman.school.learning.verification.core.VerificationResult
import com.majortomman.school.learning.verification.core.VerificationRuleKey
import com.majortomman.school.learning.verification.core.VerificationStatus
import com.majortomman.school.learning.verification.core.VerificationStep
import com.majortomman.school.learning.verification.core.VerificationVisualizationRequest
import com.majortomman.school.learning.verification.core.VerificationVisualizationValue
import com.majortomman.school.learning.verification.core.VerificationWarning
import com.majortomman.school.visualization.VisualizationParameterValue
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

object MathVerificationEngine : SubjectEngine {
    override val subject: VerificationSubject = VerificationSubject.MATHEMATICS
    override val mode: VerificationMode = VerificationMode.DETERMINISTIC

    override fun verify(request: VerificationRequest): VerificationResult {
        val source = request.input.trim()
        if (source.isBlank()) return invalid(source, "请输入数学表达式或方程。")
        val basic = normalizeBasicSymbols(source)
        if (containsHigherMath(basic)) {
            return unsupported(
                basic,
                "当前本地数学引擎限定为初高中数学，不支持极限、导数、微分、积分等高等数学内容。",
            )
        }
        return try {
            when {
                basic.startsWith("f(x)=") -> verifyFunction(normalizeExpressionBody(basic.removePrefix("f(x)=")), declared = true)
                basic.startsWith("y=") -> verifyFunction(normalizeExpressionBody(basic.removePrefix("y=")), declared = true)
                basic.count { it == '=' } == 1 -> verifyEquation(basic)
                '=' in basic -> invalid(basic, "一个方程中只能出现一个等号。")
                basic.any { it == '<' || it == '>' || it == '≤' || it == '≥' } -> unsupported(basic, "当前数学验证页先接入表达式、函数和一元一次/二次方程；不等式求解器会在同一 Math Engine 中继续接入。")
                else -> verifyExpression(normalizeExpressionBody(basic))
            }
        } catch (error: UnsupportedMathOperation) {
            unsupported(basic, error.message ?: "当前本地数学引擎暂不支持这个题型。")
        } catch (error: IllegalArgumentException) {
            invalid(basic, error.message ?: "无法解析这个数学输入。")
        } catch (error: IllegalStateException) {
            invalid(basic, error.message ?: "无法解析这个数学输入。")
        }
    }

    private fun verifyExpression(expressionText: String): VerificationResult {
        if (hasGeneralFunctionCall(expressionText)) return verifySafeExpression(expressionText)
        val expression = runCatching { ScienceExpressionParser.parse(expressionText) }.getOrElse {
            return verifySafeExpression(expressionText)
        }
        val variables = variablesOf(expression)
        if (variables.isEmpty()) return verifyConstantExpression(expressionText, expression)
        if (variables.size > 1) throw UnsupportedMathOperation("当前符号化简先支持单变量表达式；检测到变量：${variables.sorted().joinToString()}。")

        val variable = variables.first()
        val polynomial = runCatching { toPolynomial(expression, variable) }.getOrNull()
        if (polynomial != null) return verifyPolynomialExpression(expressionText, variable, polynomial)
        if (variable == "x") return verifyFunction(expressionText, declared = false)
        throw UnsupportedMathOperation("这个表达式包含当前多项式内核尚未支持的结构；函数图像目前只接受自变量 x。")
    }

    private fun verifySafeExpression(expressionText: String): VerificationResult {
        val parsed = VisualizationParameterValue.MathExpressionValue.parse(expressionText)
        if (parsed.variables.isEmpty()) return verifyApproximateConstantExpression(expressionText, parsed)
        if (parsed.variables.all { it == "x" }) return verifyFunction(expressionText, declared = false)
        throw UnsupportedMathOperation("当前函数表达式只支持自变量 x；检测到变量：${parsed.variables.sorted().joinToString()}。")
    }

    private fun verifyApproximateConstantExpression(expressionText: String, expression: VisualizationParameterValue.MathExpressionValue): VerificationResult {
        val value = expression.evaluate(emptyMap())
        require(value.isFinite()) { "计算结果不是有限数值。" }
        val answer = VerificationArtifact.MathSolution(formatApproximate(value))
        return success(
            type = TYPE_NUMERIC_EXPRESSION,
            normalizedInput = expressionText,
            answer = answer,
            steps = listOf(
                VerificationStep(
                    rule = VerificationRuleKey("PARSE_EXPRESSION"),
                    title = "识别表达式",
                    before = VerificationArtifact.MathExpression(expressionText),
                    after = VerificationArtifact.MathExpression(expressionText),
                    explanation = "将输入解析成安全的数学表达式结构；函数名必须来自本地白名单。",
                ),
                VerificationStep(
                    rule = VerificationRuleKey("EVALUATE_EXPRESSION"),
                    title = "计算函数值",
                    before = VerificationArtifact.MathExpression(expressionText),
                    after = answer,
                    explanation = "按本地数学函数定义计算结果，不调用网络、AI 或脚本执行环境。",
                ),
            ),
        )
    }

    private fun verifyConstantExpression(expressionText: String, expression: ScienceExpression): VerificationResult {
        val simplified = ScienceExpressionSimplifier.simplify(expression, ScienceNumberDomain.COMPLEX)
        val answerText = ScienceExpressionRenderer.render(simplified)
        val value = ScienceExpressionEvaluator.evaluate(simplified)
        require(value.real.isFinite() && value.imaginary.isFinite()) { "计算结果不是有限数值。" }

        val original = VerificationArtifact.MathExpression(expressionText)
        val answer = VerificationArtifact.MathSolution(answerText)
        val steps = mutableListOf(
            VerificationStep(
                rule = VerificationRuleKey("PARSE_EXPRESSION"),
                title = "识别表达式",
                before = VerificationArtifact.MathExpression(expressionText),
                after = original,
                explanation = "将输入解析成安全的数学表达式结构，不执行任何脚本或外部代码。",
            ),
        )
        if (answerText != expressionText) {
            steps += VerificationStep(
                rule = VerificationRuleKey("SIMPLIFY_EXPRESSION"),
                title = "按运算规则化简",
                before = original,
                after = answer,
                explanation = "按括号、乘方、乘除和加减的数学规则计算，并尽量保留分数、根式、π 等精确形式。",
            )
        } else {
            steps += VerificationStep(
                rule = VerificationRuleKey("EVALUATE_EXPRESSION"),
                title = "确认结果",
                before = original,
                after = answer,
                explanation = "表达式已经处于当前内核可保留的精确形式。",
            )
        }
        return success(
            type = TYPE_NUMERIC_EXPRESSION,
            normalizedInput = expressionText,
            answer = answer,
            steps = steps,
        )
    }

    private fun verifyPolynomialExpression(expressionText: String, variable: String, polynomial: Polynomial): VerificationResult {
        val expanded = polynomial.render(variable)
        val before = VerificationArtifact.MathExpression(expressionText)
        val after = VerificationArtifact.MathExpression(expanded)
        val steps = mutableListOf(
            VerificationStep(
                rule = VerificationRuleKey("PARSE_EXPRESSION"),
                title = "识别代数结构",
                before = before,
                after = before,
                explanation = "识别变量、系数、括号、乘方和运算关系。",
            ),
        )
        if (expanded != expressionText) {
            steps += VerificationStep(
                rule = VerificationRuleKey("EXPAND_AND_COMBINE"),
                title = "展开并合并同类项",
                before = before,
                after = after,
                explanation = "应用分配律展开乘积，再把相同次数的同类项合并。",
            )
        }
        val visualizations = if (variable == "x") listOf(buildFunctionGraphRequest(expanded)) else emptyList()
        val warnings = if (variable != "x") {
            listOf(VerificationWarning("GRAPH_VARIABLE_NOT_X", "当前函数图像坐标系只使用自变量 x，因此本次只显示代数化简结果。"))
        } else {
            emptyList()
        }
        return success(
            type = TYPE_POLYNOMIAL_EXPRESSION,
            normalizedInput = expressionText,
            answer = after,
            steps = steps,
            warnings = warnings,
            visualizations = visualizations,
        )
    }

    private fun verifyFunction(expressionText: String, declared: Boolean): VerificationResult {
        val parsed = VisualizationParameterValue.MathExpressionValue.parse(expressionText)
        if (parsed.variables.any { it != "x" }) {
            throw UnsupportedMathOperation("函数图像目前只支持自变量 x；检测到变量：${parsed.variables.sorted().joinToString()}。")
        }
        val answerText = "y = $expressionText"
        val before = VerificationArtifact.MathExpression(if (declared) answerText else expressionText)
        val function = VerificationArtifact.MathFunction(answerText)
        val steps = listOf(
            VerificationStep(
                rule = VerificationRuleKey("IDENTIFY_FUNCTION"),
                title = "识别函数",
                before = before,
                after = function,
                explanation = if (parsed.variables.isEmpty()) "表达式不含自变量，因此识别为常值函数。" else "检测到唯一自变量 x，可按 y=f(x) 解释并进行本地采样。",
            ),
            VerificationStep(
                rule = VerificationRuleKey("PREPARE_FUNCTION_GRAPH"),
                title = "准备函数图像",
                before = function,
                after = function,
                explanation = "把经过安全解析的函数表达式作为语义参数交给统一可视化基础设施绘图。",
            ),
        )
        return success(
            type = TYPE_FUNCTION,
            normalizedInput = answerText,
            answer = function,
            steps = steps,
            visualizations = listOf(buildFunctionGraphRequest(expressionText, parsed)),
        )
    }

    private fun verifyEquation(source: String): VerificationResult {
        if (hasGeneralFunctionCall(source)) throw UnsupportedMathOperation("当前方程求解器先支持初高中一元一次和一元二次代数方程，暂不求解三角、指数或对数方程。")
        val sides = source.split('=')
        require(sides.size == 2 && sides.all(String::isNotBlank)) { "等号两边都需要有数学表达式。" }
        val leftText = normalizeExpressionBody(sides[0])
        val rightText = normalizeExpressionBody(sides[1])
        val left = ScienceExpressionParser.parse(leftText)
        val right = ScienceExpressionParser.parse(rightText)
        val variables = (variablesOf(left) + variablesOf(right)).toSortedSet()
        val normalizedEquation = "$leftText = $rightText"
        if (variables.isEmpty()) return verifyConstantEquation(normalizedEquation, left, right)
        if (variables.size != 1) throw UnsupportedMathOperation("当前方程求解先支持一元方程；检测到变量：${variables.joinToString()}。")

        val variable = variables.first()
        val polynomial = toPolynomial(left, variable) - toPolynomial(right, variable)
        if (polynomial.degree > 2) throw UnsupportedMathOperation("当前本地求解器限定到一元二次方程；检测到 ${polynomial.degree} 次方程。")
        val canonical = "${polynomial.render(variable)} = 0"
        if (polynomial.degree <= 0) return verifyConstantPolynomialEquation(normalizedEquation, canonical, polynomial)

        val algebra = if (polynomial.degree == 1) {
            AlgebraSolver.solveLinear(polynomial.coefficient(1), polynomial.coefficient(0), BigRational.ZERO)
        } else {
            AlgebraSolver.solveQuadratic(
                polynomial.coefficient(2),
                polynomial.coefficient(1),
                polynomial.coefficient(0),
                ScienceNumberDomain.COMPLEX,
            )
        }
        val type = if (polynomial.degree == 1) TYPE_LINEAR_EQUATION else TYPE_QUADRATIC_EQUATION
        val answerText = renderEquationSolution(algebra.solution, variable)
        val answer = VerificationArtifact.MathSolution(answerText)
        val steps = mutableListOf(
            VerificationStep(
                rule = VerificationRuleKey("STANDARDIZE_EQUATION"),
                title = "整理为标准形式",
                before = VerificationArtifact.MathEquation(normalizedEquation),
                after = VerificationArtifact.MathEquation(canonical),
                explanation = "把等号右边的项移到左边，并展开、合并同类项，得到与原方程同解的标准形式。",
            ),
        )
        var previous: VerificationArtifact = VerificationArtifact.MathEquation(canonical)
        algebra.steps.drop(1).forEach { algebraStep ->
            val mapped = mapAlgebraStep(algebraStep, previous, variable)
            steps += mapped
            previous = mapped.after ?: previous
        }
        if (previous.display != answer.display) {
            steps += VerificationStep(
                rule = VerificationRuleKey("COLLECT_SOLUTIONS"),
                title = "写出解集",
                before = previous,
                after = answer,
                explanation = "汇总所有满足原方程的根。",
            )
        }
        return success(
            type = type,
            normalizedInput = normalizedEquation,
            answer = answer,
            steps = steps,
        )
    }

    private fun verifyConstantEquation(source: String, left: ScienceExpression, right: ScienceExpression): VerificationResult {
        val leftSimplified = ScienceExpressionSimplifier.simplify(left, ScienceNumberDomain.COMPLEX)
        val rightSimplified = ScienceExpressionSimplifier.simplify(right, ScienceNumberDomain.COMPLEX)
        val leftValue = ScienceExpressionEvaluator.evaluate(leftSimplified)
        val rightValue = ScienceExpressionEvaluator.evaluate(rightSimplified)
        val equal = abs(leftValue.real - rightValue.real) <= 1e-10 && abs(leftValue.imaginary - rightValue.imaginary) <= 1e-10
        val answer = VerificationArtifact.MathSolution(if (equal) "等式成立" else "等式不成立")
        return success(
            type = TYPE_CONSTANT_EQUATION,
            normalizedInput = source,
            answer = answer,
            steps = listOf(
                VerificationStep(
                    rule = VerificationRuleKey("EVALUATE_BOTH_SIDES"),
                    title = "分别计算等号两边",
                    before = VerificationArtifact.MathEquation(source),
                    after = VerificationArtifact.MathEquation("${ScienceExpressionRenderer.render(leftSimplified)} ${if (equal) "=" else "≠"} ${ScienceExpressionRenderer.render(rightSimplified)}"),
                    explanation = "分别化简左右两边，再比较得到的数值。",
                ),
                VerificationStep(
                    rule = VerificationRuleKey("CHECK_EQUALITY"),
                    title = "判断等式",
                    before = VerificationArtifact.MathEquation(source),
                    after = answer,
                    explanation = if (equal) "左右两边表示同一个数，因此等式成立。" else "左右两边表示不同的数，因此等式不成立。",
                ),
            ),
        )
    }

    private fun verifyConstantPolynomialEquation(source: String, canonical: String, polynomial: Polynomial): VerificationResult {
        val allValues = polynomial.isZero
        val answer = VerificationArtifact.MathSolution(if (allValues) "任意数都是解" else "无解")
        return success(
            type = TYPE_CONSTANT_EQUATION,
            normalizedInput = source,
            answer = answer,
            steps = listOf(
                VerificationStep(
                    rule = VerificationRuleKey("STANDARDIZE_EQUATION"),
                    title = "整理方程",
                    before = VerificationArtifact.MathEquation(source),
                    after = VerificationArtifact.MathEquation(canonical),
                    explanation = "将两边移到同一侧并合并同类项。",
                ),
                VerificationStep(
                    rule = VerificationRuleKey("CHECK_CONSTANT_EQUATION"),
                    title = "判断常量关系",
                    before = VerificationArtifact.MathEquation(canonical),
                    after = answer,
                    explanation = if (allValues) "整理后得到恒等式 0=0，所以任意数都满足原方程。" else "整理后得到非零常数等于 0 的矛盾式，所以原方程无解。",
                ),
            ),
        )
    }

    private fun mapAlgebraStep(step: AlgebraStep, before: VerificationArtifact, variable: String): VerificationStep {
        val expression = step.expression.replace("x", variable)
        val after = if (step.title == "结果" || step.title == "重根" || step.title.endsWith("结论")) {
            VerificationArtifact.MathSolution(expression)
        } else {
            VerificationArtifact.MathEquation(expression)
        }
        return VerificationStep(
            rule = VerificationRuleKey(ruleFor(step.title)),
            title = step.title,
            before = before,
            after = after,
            explanation = normalizeAlgebraReason(step.reason),
            conditions = step.conditions,
        )
    }

    private fun ruleFor(title: String): String = when (title) {
        "移项" -> "TRANSPOSE_TERMS"
        "除以系数", "除以正数", "除以负数" -> "DIVIDE_BOTH_SIDES"
        "判别式" -> "CALCULATE_DISCRIMINANT"
        "求根公式" -> "APPLY_QUADRATIC_FORMULA"
        "重根" -> "IDENTIFY_DOUBLE_ROOT"
        "结果" -> "COLLECT_SOLUTIONS"
        "检查常量", "常量判断" -> "CHECK_CONSTANT_EQUATION"
        else -> "ALGEBRA_TRANSFORM"
    }

    private fun normalizeAlgebraReason(reason: String): String {
        val match = Regex("等式两边同时减去 (-[0-9]+(?:/[0-9]+)?)").find(reason) ?: return reason
        return "等式两边同时加上 ${match.groupValues[1].removePrefix("-")}"
    }

    private fun renderEquationSolution(solution: EquationSolution, variable: String): String = when (solution) {
        EquationSolution.AllValues -> "任意数都是解"
        EquationSolution.NoSolution -> "无解"
        is EquationSolution.Roots -> solution.values.joinToString("，") { "$variable = ${it.text}" }
    }

    private fun toPolynomial(expression: ScienceExpression, variable: String): Polynomial = when (expression) {
        is ScienceExpression.RationalLiteral -> Polynomial.constant(expression.value)
        is ScienceExpression.Variable -> {
            if (expression.name != variable) throw UnsupportedMathOperation("当前多项式内核只处理一个变量。")
            Polynomial.monomial(BigRational.ONE, 1)
        }
        is ScienceExpression.Sum -> expression.terms.fold(Polynomial.ZERO) { result, term -> result + toPolynomial(term, variable) }
        is ScienceExpression.Product -> expression.factors.fold(Polynomial.ONE) { result, factor -> result * toPolynomial(factor, variable) }
        is ScienceExpression.Quotient -> {
            val denominator = ScienceExpressionSimplifier.simplify(expression.denominator, ScienceNumberDomain.REAL)
            val rational = (denominator as? ScienceExpression.RationalLiteral)?.value
                ?: throw UnsupportedMathOperation("当前方程求解暂不支持含变量或无理式的分母。")
            require(!rational.isZero) { "分母不能为 0。" }
            toPolynomial(expression.numerator, variable).scale(rational.reciprocal())
        }
        is ScienceExpression.Power -> {
            if (expression.exponent < 0) throw UnsupportedMathOperation("当前多项式方程暂不处理负指数。")
            polynomialPower(toPolynomial(expression.base, variable), expression.exponent)
        }
        ScienceExpression.Pi,
        ScienceExpression.E,
        ScienceExpression.ImaginaryUnit,
        is ScienceExpression.Radical,
        -> throw UnsupportedMathOperation("当前多项式系数先限定为有理数。")
    }

    private fun polynomialPower(base: Polynomial, exponent: Int): Polynomial {
        var result = Polynomial.ONE
        repeat(exponent) { result = result * base }
        return result
    }

    private fun variablesOf(expression: ScienceExpression): Set<String> = when (expression) {
        is ScienceExpression.Variable -> setOf(expression.name)
        is ScienceExpression.Sum -> expression.terms.flatMapTo(linkedSetOf()) { variablesOf(it) }
        is ScienceExpression.Product -> expression.factors.flatMapTo(linkedSetOf()) { variablesOf(it) }
        is ScienceExpression.Quotient -> variablesOf(expression.numerator) + variablesOf(expression.denominator)
        is ScienceExpression.Power -> variablesOf(expression.base)
        is ScienceExpression.Radical -> variablesOf(expression.radicand)
        else -> emptySet()
    }

    private fun buildFunctionGraphRequest(expressionText: String): VerificationVisualizationRequest {
        val parsed = VisualizationParameterValue.MathExpressionValue.parse(expressionText)
        return buildFunctionGraphRequest(expressionText, parsed)
    }

    private fun buildFunctionGraphRequest(expressionText: String, parsed: VisualizationParameterValue.MathExpressionValue): VerificationVisualizationRequest {
        val bounds = deriveGraphBounds(parsed)
        return VerificationVisualizationRequest(
            renderer = "mathematics.function.graph",
            parameters = mapOf(
                "expression" to VerificationVisualizationValue.MathExpressionValue(expressionText),
                "xMin" to VerificationVisualizationValue.NumberValue(bounds.xMin),
                "xMax" to VerificationVisualizationValue.NumberValue(bounds.xMax),
                "yMin" to VerificationVisualizationValue.NumberValue(bounds.yMin),
                "yMax" to VerificationVisualizationValue.NumberValue(bounds.yMax),
            ),
            texts = mapOf(
                "title" to "y = $expressionText",
                "note" to "本地解析 · 双指缩放 / 双击复位",
            ),
        )
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
            yMin -= 2.0
            yMax += 2.0
        } else {
            val padding = max(0.8, (yMax - yMin) * 0.12)
            yMin -= padding
            yMax += padding
        }
        yMin = max(-50.0, yMin)
        yMax = min(50.0, yMax)
        return GraphBounds(xMin, xMax, yMin, yMax)
    }

    private fun normalizeBasicSymbols(raw: String): String = raw.trim().lowercase()
        .replace('−', '-')
        .replace('—', '-')
        .replace('×', '*')
        .replace('·', '*')
        .replace('÷', '/')
        .replace('（', '(')
        .replace('）', ')')
        .replace("π", "pi")
        .replace(" ", "")

    private fun normalizeExpressionBody(raw: String): String {
        var text = raw
        text = text.replace(Regex("√\\(([^()]*)\\)"), "sqrt($1)")
        text = text.replace(Regex("√([a-z0-9.]+)"), "sqrt($1)")
        text = text.replace(Regex("(?<=[0-9x)])(?=[x(])"), "*")
        text = text.replace(Regex("(?<=\\))(?=[0-9x(])"), "*")
        text = text.replace(Regex("(?<=[0-9x)])(?=pi(?:\\b|\\())"), "*")
        text = text.replace(Regex("(?<=pi)(?=[0-9x(])"), "*")
        text = text.replace(Regex("(?<=[0-9x)])(?=(?:abs|sqrt|sin|cos|tan|ln|log|exp)\\()"), "*")
        return text
    }

    private fun hasGeneralFunctionCall(text: String): Boolean =
        Regex("(?:abs|sin|cos|tan|ln|log|exp)\\(").containsMatchIn(text)

    private fun containsHigherMath(text: String): Boolean {
        val keywords = listOf("limit", "lim(", "derivative", "diff(", "integral", "int(", "d/dx", "∫", "极限", "导数", "微分", "积分")
        return keywords.any(text::contains)
    }

    private fun formatApproximate(value: Double): String {
        val integer = value.roundToLong()
        if (abs(value - integer.toDouble()) <= 1e-10) return integer.toString()
        return BigDecimal.valueOf(value).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    }

    private fun success(
        type: VerificationProblemType,
        normalizedInput: String,
        answer: VerificationArtifact,
        steps: List<VerificationStep>,
        warnings: List<VerificationWarning> = emptyList(),
        visualizations: List<VerificationVisualizationRequest> = emptyList(),
    ): VerificationResult = VerificationResult(
        subject = subject,
        mode = mode,
        status = VerificationStatus.SUCCESS,
        problemType = type,
        normalizedInput = normalizedInput,
        answer = answer,
        steps = steps,
        warnings = warnings,
        visualizations = visualizations,
    )

    private fun unsupported(input: String, message: String): VerificationResult = VerificationResult(
        subject = subject,
        mode = mode,
        status = VerificationStatus.UNSUPPORTED,
        problemType = TYPE_UNSUPPORTED,
        normalizedInput = input,
        warnings = listOf(VerificationWarning("MATH_SCOPE_UNSUPPORTED", message)),
    )

    private fun invalid(input: String, message: String): VerificationResult = VerificationResult(
        subject = subject,
        mode = mode,
        status = VerificationStatus.INVALID,
        problemType = TYPE_INVALID,
        normalizedInput = input,
        warnings = listOf(VerificationWarning("MATH_INPUT_INVALID", message)),
    )

    private class UnsupportedMathOperation(message: String) : IllegalArgumentException(message)

    private data class GraphBounds(val xMin: Double, val xMax: Double, val yMin: Double, val yMax: Double)

    private val TYPE_NUMERIC_EXPRESSION = VerificationProblemType("math.numeric-expression", "数值表达式")
    private val TYPE_POLYNOMIAL_EXPRESSION = VerificationProblemType("math.polynomial-expression", "代数式化简")
    private val TYPE_FUNCTION = VerificationProblemType("math.function", "函数")
    private val TYPE_LINEAR_EQUATION = VerificationProblemType("math.linear-equation", "一元一次方程")
    private val TYPE_QUADRATIC_EQUATION = VerificationProblemType("math.quadratic-equation", "一元二次方程")
    private val TYPE_CONSTANT_EQUATION = VerificationProblemType("math.constant-equation", "常量等式")
    private val TYPE_UNSUPPORTED = VerificationProblemType("math.unsupported", "超出当前范围")
    private val TYPE_INVALID = VerificationProblemType("math.invalid", "无法识别")
}
