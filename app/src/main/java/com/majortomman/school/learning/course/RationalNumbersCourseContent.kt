package com.majortomman.school.learning.course

enum class RationalVisualizationKind {
    OPPOSITE_QUANTITIES,
    RATIONAL_CLASSIFICATION,
    NUMBER_LINE,
    OPPOSITE_NUMBERS,
    ABSOLUTE_VALUE,
    NUMBER_COMPARISON,
    ADDITION_PROCESS,
    SUBTRACTION_TRANSFORM,
    MULTIPLICATION_SIGN,
    DIVISION_TRANSFORM,
    POWER_PROCESS,
    HISTORY,
}

data class RationalLessonPage(
    val id: String,
    val section: String,
    val title: String,
    val paragraphs: List<String>,
    val sourcePage: Int,
    val visualization: RationalVisualizationKind,
    val formula: String? = null,
    val conclusion: String? = null,
)

object RationalNumbersCourseFactory {
    fun supports(title: String): Boolean {
        val normalized = normalize(title)
        return normalized.contains("正数和负数") ||
            normalized.contains("允许偏差") ||
            normalized.contains("有理数及其大小比较") ||
            normalized.contains("数轴") ||
            normalized.contains("相反数") ||
            normalized.contains("绝对值") ||
            normalized.contains("漫漫长路识负数") ||
            normalized.contains("有理数的加法与减法") ||
            normalized.contains("正负术") ||
            normalized.contains("有理数的乘法与除法") ||
            normalized.contains("数系扩充看乘法法则") ||
            normalized.contains("有理数的乘方") ||
            normalized == "有理数" ||
            normalized == "第一章有理数" ||
            normalized == "有理数的运算" ||
            normalized == "第二章有理数的运算"
    }

    fun pagesFor(title: String, sourcePages: IntRange): List<RationalLessonPage> {
        require(!sourcePages.isEmpty()) { "教材页码范围不能为空" }
        val normalized = normalize(title)
        return when {
            normalized.contains("允许偏差") -> allowedDeviationPages(sourcePages)
            normalized.contains("漫漫长路识负数") -> negativeNumberHistoryPages(sourcePages)
            normalized.contains("正负术") -> signedArithmeticHistoryPages(sourcePages)
            normalized.contains("数系扩充看乘法法则") -> numberSystemExtensionPages(sourcePages)
            normalized.contains("有理数的加法与减法") -> additionAndSubtractionPages(sourcePages)
            normalized.contains("有理数的乘法与除法") -> multiplicationAndDivisionPages(sourcePages)
            normalized.contains("有理数的乘方") -> powerPages(sourcePages)
            normalized == "有理数的运算" || normalized == "第二章有理数的运算" -> operationChapterPages(sourcePages)
            normalized.contains("正数和负数") -> positiveAndNegativePages(sourcePages)
            normalized.contains("数轴") -> numberLinePages(sourcePages)
            normalized.contains("相反数") -> oppositeNumberPages(sourcePages)
            normalized.contains("绝对值") -> absoluteValuePages(sourcePages)
            normalized.contains("有理数及其大小比较") -> rationalNumberAndComparisonPages(sourcePages)
            normalized == "有理数" || normalized == "第一章有理数" -> rationalNumberChapterPages(sourcePages)
            else -> emptyList()
        }
    }

    private fun positiveAndNegativePages(source: IntRange) = listOf(
        page(
            id = "opposite-quantities",
            section = "1.1 正数和负数",
            title = "具有相反意义的量",
            source = source,
            offset = 0,
            visualization = RationalVisualizationKind.OPPOSITE_QUANTITIES,
            paragraphs = listOf(
                "在同一问题中，零上与零下、增加与减少、收入与支出等，分别表示意义相反的两个量。",
                "表示这类量时，应先规定其中一种意义为正，另一种意义为负。正、负的规定必须在同一问题中保持一致。",
            ),
            conclusion = "正数和负数可以表示具有相反意义的量。",
        ),
        page(
            id = "positive-negative-definition",
            section = "1.1 正数和负数",
            title = "正数和负数",
            source = source,
            offset = 1,
            visualization = RationalVisualizationKind.OPPOSITE_QUANTITIES,
            paragraphs = listOf(
                "像 3，50，7.8% 这样大于 0 的数叫作正数。正数前面的正号通常省略不写。",
                "像 −3，−10，−0.7% 这样在正数前加上符号“−”的数叫作负数。负号不能省略。",
            ),
            formula = "+3=3，−3<0",
            conclusion = "正数大于 0，负数小于 0。",
        ),
        page(
            id = "zero-boundary",
            section = "1.1 正数和负数",
            title = "0 的意义",
            source = source,
            offset = 2,
            visualization = RationalVisualizationKind.OPPOSITE_QUANTITIES,
            paragraphs = listOf(
                "0 既不是正数，也不是负数。",
                "0 不仅可以表示“没有”，还常作为确定正、负的基准。例如，海拔 0 m 是规定的高度基准，温度 0 ℃ 是摄氏温标中的一个确定刻度。",
            ),
            formula = "负数 < 0 < 正数",
            conclusion = "0 是正数与负数的分界。",
        ),
    )

    private fun allowedDeviationPages(source: IntRange) = listOf(
        page(
            id = "allowed-deviation",
            section = "阅读与思考",
            title = "用正负数表示允许偏差",
            source = source,
            offset = 0,
            visualization = RationalVisualizationKind.OPPOSITE_QUANTITIES,
            paragraphs = listOf(
                "以规定的标准值为基准，超过标准值的部分记为正，低于标准值的部分记为负。",
                "偏差的正负表示方向，偏差的绝对值表示与标准值相差的大小。判断实际数值时，应把标准值与偏差合并考虑。",
            ),
            formula = "实际值 = 标准值 + 偏差",
            conclusion = "正负号说明偏差方向，绝对值说明偏差大小。",
        ),
    )

    private fun rationalNumberAndComparisonPages(source: IntRange) = listOf(
        page(
            id = "rational-definition",
            section = "1.2 有理数及其大小比较",
            title = "有理数",
            source = source,
            offset = 0,
            visualization = RationalVisualizationKind.RATIONAL_CLASSIFICATION,
            paragraphs = listOf(
                "整数可以写成分母为 1 的分数。有限小数和百分数也可以写成分数形式。",
                "可以写成分数形式的数称为有理数。其中，可以写成正分数形式的数为正有理数，可以写成负分数形式的数为负有理数。",
            ),
            formula = "a/b（a，b 为整数，b≠0）",
            conclusion = "整数和分数统称有理数。",
        ),
        page(
            id = "rational-classification",
            section = "1.2 有理数及其大小比较",
            title = "有理数的分类",
            source = source,
            offset = 1,
            visualization = RationalVisualizationKind.RATIONAL_CLASSIFICATION,
            paragraphs = listOf(
                "按符号分类，有理数分为正有理数、0 和负有理数。",
                "按表示形式分类，有理数分为整数和分数。正整数、0、负整数都是整数；正分数和负分数都是分数。",
            ),
            conclusion = "分类标准不同，所得分类形式也不同。",
        ),
        page(
            id = "number-line-definition",
            section = "1.2 有理数及其大小比较",
            title = "数轴",
            source = source,
            offset = 2,
            visualization = RationalVisualizationKind.NUMBER_LINE,
            paragraphs = listOf(
                "画一条直线，在直线上任取一点表示数 0，这个点叫作原点。规定直线上从原点向右的方向为正方向，并选取适当长度作为单位长度。",
                "规定了原点、正方向和单位长度的直线叫作数轴。",
            ),
            conclusion = "原点、正方向和单位长度是数轴的三个要素。",
        ),
        page(
            id = "rational-on-line",
            section = "1.2 有理数及其大小比较",
            title = "用数轴表示有理数",
            source = source,
            offset = 3,
            visualization = RationalVisualizationKind.NUMBER_LINE,
            paragraphs = listOf(
                "正数用原点右边的点表示，负数用原点左边的点表示，0 用原点表示。",
                "任何一个有理数都可以用数轴上的一个点表示。表示分数时，应按分母把相应单位长度等分。",
            ),
            conclusion = "数轴把数与直线上的位置联系起来。",
        ),
        page(
            id = "opposite-number",
            section = "1.2 有理数及其大小比较",
            title = "相反数",
            source = source,
            offset = 5,
            visualization = RationalVisualizationKind.OPPOSITE_NUMBERS,
            paragraphs = listOf(
                "在数轴上，表示 3 和 −3 的点分别位于原点两侧，并且与原点的距离相等。",
                "像 3 和 −3 这样，只有符号不同的两个数互为相反数。0 的相反数是 0。",
            ),
            formula = "a 的相反数是 −a",
            conclusion = "互为相反数的两个点关于原点对称。",
        ),
        page(
            id = "absolute-value",
            section = "1.2 有理数及其大小比较",
            title = "绝对值",
            source = source,
            offset = 7,
            visualization = RationalVisualizationKind.ABSOLUTE_VALUE,
            paragraphs = listOf(
                "数轴上表示数 a 的点与原点的距离，叫作数 a 的绝对值，记作 |a|。",
                "正数的绝对值是它本身；负数的绝对值是它的相反数；0 的绝对值是 0。绝对值表示距离，因此绝对值不可能是负数。",
            ),
            formula = "|a|≥0",
            conclusion = "绝对值反映数在数轴上与原点的距离。",
        ),
        page(
            id = "rational-comparison",
            section = "1.2 有理数及其大小比较",
            title = "有理数的大小比较",
            source = source,
            offset = 9,
            visualization = RationalVisualizationKind.NUMBER_COMPARISON,
            paragraphs = listOf(
                "在数轴上表示的两个数，右边的数总比左边的数大。",
                "正数大于 0，0 大于负数，正数大于负数。两个负数比较大小，绝对值大的反而小。",
            ),
            formula = "若 |a|>|b| 且 a<0，b<0，则 a<b",
            conclusion = "比较有理数大小，既可以利用数轴，也可以利用符号和绝对值。",
        ),
    )

    private fun numberLinePages(source: IntRange) = rationalNumberAndComparisonPages(source)
        .filter { it.visualization == RationalVisualizationKind.NUMBER_LINE }

    private fun oppositeNumberPages(source: IntRange) = rationalNumberAndComparisonPages(source)
        .filter { it.visualization == RationalVisualizationKind.OPPOSITE_NUMBERS }

    private fun absoluteValuePages(source: IntRange) = rationalNumberAndComparisonPages(source)
        .filter { it.visualization == RationalVisualizationKind.ABSOLUTE_VALUE }

    private fun additionAndSubtractionPages(source: IntRange) = listOf(
        page(
            id = "addition-meaning",
            section = "2.1 有理数的加法与减法",
            title = "有理数加法",
            source = source,
            offset = 0,
            visualization = RationalVisualizationKind.ADDITION_PROCESS,
            paragraphs = listOf(
                "有理数相加时，既要确定和的符号，又要确定和的绝对值。",
                "同号两数相加，符号保持不变，绝对值相加；异号两数相加，先比较绝对值，再用较大的绝对值减去较小的绝对值。",
            ),
            conclusion = "符号与绝对值应分别判断。",
        ),
        page(
            id = "addition-rule",
            section = "2.1 有理数的加法与减法",
            title = "有理数加法法则",
            source = source,
            offset = 3,
            visualization = RationalVisualizationKind.ADDITION_PROCESS,
            paragraphs = listOf(
                "同号两数相加，取相同的符号，并把绝对值相加。",
                "绝对值不相等的异号两数相加，取绝对值较大的加数的符号，并用较大的绝对值减去较小的绝对值。互为相反数的两个数相加得 0。一个数同 0 相加，仍得这个数。",
            ),
            formula = "3+(−5)=−(5−3)=−2",
            conclusion = "先定符号，再算绝对值。",
        ),
        page(
            id = "addition-process",
            section = "2.1 有理数的加法与减法",
            title = "异号两数相加",
            source = source,
            offset = 5,
            visualization = RationalVisualizationKind.ADDITION_PROCESS,
            paragraphs = listOf(
                "计算 3+(−5) 时，3 个正单位与 3 个负单位相互抵消，剩下 2 个负单位。",
                "抵消过程对应绝对值相减；剩余单位的符号由绝对值较大的加数决定。",
            ),
            formula = "3+(−5)=−2",
            conclusion = "异号相加的实质是相反单位先抵消。",
        ),
        page(
            id = "subtraction-rule",
            section = "2.1 有理数的加法与减法",
            title = "有理数减法法则",
            source = source,
            offset = 8,
            visualization = RationalVisualizationKind.SUBTRACTION_TRANSFORM,
            paragraphs = listOf(
                "有理数的减法可以转化为加法来进行。",
                "减去一个数，等于加这个数的相反数。转化后，按照有理数加法法则计算。",
            ),
            formula = "a−b=a+(−b)",
            conclusion = "减法转化为加法时，减数变为它的相反数。",
        ),
        page(
            id = "subtraction-process",
            section = "2.1 有理数的加法与减法",
            title = "减法的转化过程",
            source = source,
            offset = 10,
            visualization = RationalVisualizationKind.SUBTRACTION_TRANSFORM,
            paragraphs = listOf(
                "计算 9−(−8) 时，先把减去 −8 转化为加上 −8 的相反数 8。",
                "转化只改变运算形式，不改变原式的值。完成转化后，再计算 9+8。",
            ),
            formula = "9−(−8)=9+8=17",
            conclusion = "括号内数的相反数必须连同符号一起确定。",
        ),
    )

    private fun multiplicationAndDivisionPages(source: IntRange) = listOf(
        page(
            id = "multiplication-rule",
            section = "2.2 有理数的乘法与除法",
            title = "有理数乘法法则",
            source = source,
            offset = 0,
            visualization = RationalVisualizationKind.MULTIPLICATION_SIGN,
            paragraphs = listOf(
                "两数相乘，同号得正，异号得负，且积的绝对值等于乘数的绝对值的积。",
                "任何数与 0 相乘，都得 0。计算时，应先确定积的符号，再计算绝对值的积。",
            ),
            formula = "(−3)×(−4)=+(3×4)=12",
            conclusion = "积的符号由两个乘数的符号决定。",
        ),
        page(
            id = "multiplication-sign",
            section = "2.2 有理数的乘法与除法",
            title = "符号与绝对值",
            source = source,
            offset = 3,
            visualization = RationalVisualizationKind.MULTIPLICATION_SIGN,
            paragraphs = listOf(
                "计算有理数乘法，可以把符号判断和绝对值计算分成两个步骤。",
                "例如 (−2)×3 中，异号决定积为负，2×3 决定积的绝对值为 6，因此结果为 −6。",
            ),
            formula = "符号：(−)×(+)=(−)；绝对值：2×3=6",
            conclusion = "符号判断与数值计算相互独立，最后合并。",
        ),
        page(
            id = "reciprocal",
            section = "2.2 有理数的乘法与除法",
            title = "倒数",
            source = source,
            offset = 6,
            visualization = RationalVisualizationKind.DIVISION_TRANSFORM,
            paragraphs = listOf(
                "乘积是 1 的两个数互为倒数。",
                "非零有理数 a 的倒数是 1/a。0 没有倒数，因为不存在一个数与 0 相乘得到 1。",
            ),
            formula = "a·(1/a)=1（a≠0）",
            conclusion = "只有非零有理数才有倒数。",
        ),
        page(
            id = "division-rule",
            section = "2.2 有理数的乘法与除法",
            title = "有理数除法法则",
            source = source,
            offset = 8,
            visualization = RationalVisualizationKind.DIVISION_TRANSFORM,
            paragraphs = listOf(
                "除以一个不等于 0 的数，等于乘这个数的倒数。",
                "两数相除，同号得正，异号得负，且商的绝对值等于被除数的绝对值除以除数的绝对值的商。0 除以任何一个不等于 0 的数，都得 0。",
            ),
            formula = "a÷b=a·(1/b)（b≠0）",
            conclusion = "除法可以转化为乘法，除数不能为 0。",
        ),
        page(
            id = "division-process",
            section = "2.2 有理数的乘法与除法",
            title = "除法的转化过程",
            source = source,
            offset = 10,
            visualization = RationalVisualizationKind.DIVISION_TRANSFORM,
            paragraphs = listOf(
                "计算 (−12)÷3 时，把除以 3 转化为乘 1/3。",
                "转化后，按照有理数乘法法则确定符号并计算绝对值。",
            ),
            formula = "(−12)÷3=(−12)×1/3=−4",
            conclusion = "转化的关键是把除数改为它的倒数。",
        ),
    )

    private fun powerPages(source: IntRange) = listOf(
        page(
            id = "power-definition",
            section = "2.3 有理数的乘方",
            title = "乘方",
            source = source,
            offset = 0,
            visualization = RationalVisualizationKind.POWER_PROCESS,
            paragraphs = listOf(
                "求 n 个相同因数的积的运算，叫作乘方。乘方的结果叫作幂。",
                "在 aⁿ 中，a 叫作底数，n 叫作指数。aⁿ 表示 n 个 a 相乘，不表示 a 与 n 相乘。",
            ),
            formula = "aⁿ=a·a·…·a（n 个 a）",
            conclusion = "指数表示相同因数的个数。",
        ),
        page(
            id = "negative-base-power",
            section = "2.3 有理数的乘方",
            title = "负数的乘方",
            source = source,
            offset = 2,
            visualization = RationalVisualizationKind.POWER_PROCESS,
            paragraphs = listOf(
                "负数的奇次幂是负数，负数的偶次幂是正数。",
                "判断符号时，应先看底数是否为负，再看指数的奇偶性。括号决定负号是否属于底数。",
            ),
            formula = "(−2)³=−8，(−2)⁴=16",
            conclusion = "负因数的个数决定幂的符号。",
        ),
        page(
            id = "power-parentheses",
            section = "2.3 有理数的乘方",
            title = "底数与括号",
            source = source,
            offset = 4,
            visualization = RationalVisualizationKind.POWER_PROCESS,
            paragraphs = listOf(
                "(−2)² 的底数是 −2，表示 (−2)×(−2)，结果为 4。",
                "−2² 中指数只作用于 2，表示 −(2²)，结果为 −4。书写负数的乘方时，负数应放在括号内。",
            ),
            formula = "(−2)²=4，−2²=−4",
            conclusion = "括号明确指数作用的范围。",
        ),
        page(
            id = "mixed-operation-order",
            section = "2.3 有理数的乘方",
            title = "有理数混合运算的顺序",
            source = source,
            offset = 6,
            visualization = RationalVisualizationKind.POWER_PROCESS,
            paragraphs = listOf(
                "有理数混合运算中，先算乘方，再算乘除，最后算加减；同级运算按照从左到右的顺序进行。",
                "有括号时，先算括号内的运算。每一步都应写清运算依据，避免同时改变多个符号。",
            ),
            formula = "括号 → 乘方 → 乘除 → 加减",
            conclusion = "运算顺序决定每一步的对象和结果。",
        ),
    )

    private fun negativeNumberHistoryPages(source: IntRange) = listOf(
        page(
            id = "negative-number-history",
            section = "图说数学史",
            title = "负数认识的发展",
            source = source,
            offset = 0,
            visualization = RationalVisualizationKind.HISTORY,
            paragraphs = listOf(
                "负数的形成经历了较长过程。实际计算中的收入与支出、盈与亏，使人们逐步需要表示小于 0 的量。",
                "中国古代数学使用不同颜色的算筹区分正数和负数，并形成正负数的运算方法。此后，负数才逐渐被作为数接受。",
            ),
            conclusion = "数学概念常在解决实际问题和完善运算体系的过程中逐步形成。",
        ),
    )

    private fun signedArithmeticHistoryPages(source: IntRange) = listOf(
        page(
            id = "positive-negative-method",
            section = "阅读与思考",
            title = "正负术",
            source = source,
            offset = 0,
            visualization = RationalVisualizationKind.HISTORY,
            paragraphs = listOf(
                "中国古代数学在筹算中用不同颜色的算筹表示正数和负数，并给出正负数加减运算的规则。",
                "这些规则把具有相反意义的量纳入统一计算，体现了有理数运算从具体情境走向一般法则的过程。",
            ),
            conclusion = "正负数运算法则使相反意义的量能够统一计算。",
        ),
    )

    private fun numberSystemExtensionPages(source: IntRange) = listOf(
        page(
            id = "number-system-extension",
            section = "探究与发现",
            title = "数系扩充与乘法法则",
            source = source,
            offset = 0,
            visualization = RationalVisualizationKind.MULTIPLICATION_SIGN,
            paragraphs = listOf(
                "把非负有理数扩充到有理数后，原有运算律应继续成立。负数乘负数的符号规则不是孤立规定，而要与分配律等已有规律相容。",
                "数系扩充既要引入新的数，也要保持原有运算结构的一致性。",
            ),
            conclusion = "运算法则必须与已经成立的运算律保持一致。",
        ),
    )

    private fun rationalNumberChapterPages(source: IntRange): List<RationalLessonPage> {
        val positive = positiveAndNegativePages(subRange(source, 1, 5))
        val rational = rationalNumberAndComparisonPages(subRange(source, 6, 16))
        return positive + rational
    }

    private fun operationChapterPages(source: IntRange): List<RationalLessonPage> {
        val addSub = additionAndSubtractionPages(subRange(source, 1, 13))
        val mulDiv = multiplicationAndDivisionPages(subRange(source, 14, 26))
        val power = powerPages(subRange(source, 27, 34))
        return addSub + mulDiv + power
    }

    private fun page(
        id: String,
        section: String,
        title: String,
        source: IntRange,
        offset: Int,
        visualization: RationalVisualizationKind,
        paragraphs: List<String>,
        formula: String? = null,
        conclusion: String? = null,
    ) = RationalLessonPage(
        id = id,
        section = section,
        title = title,
        paragraphs = paragraphs,
        sourcePage = (source.first + offset).coerceIn(source.first, source.last),
        visualization = visualization,
        formula = formula,
        conclusion = conclusion,
    )

    private fun subRange(source: IntRange, startOffset: Int, endOffset: Int): IntRange {
        val start = (source.first + startOffset).coerceIn(source.first, source.last)
        val end = (source.first + endOffset).coerceIn(start, source.last)
        return start..end
    }

    private fun normalize(title: String): String = title
        .replace(" ", "")
        .replace("　", "")
        .replace("（", "(")
        .replace("）", ")")
        .replace(Regex("^[第0-9一二三四五六七八九十.、]+"), "")
}
