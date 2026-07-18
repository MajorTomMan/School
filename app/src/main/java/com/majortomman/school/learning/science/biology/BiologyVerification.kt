package com.majortomman.school.learning.science.biology

import com.majortomman.school.learning.course.BiologyCourseCategory
import kotlin.math.abs

enum class BiologyRelationId(
    val display: String,
    val target: String,
    val variables: List<String>,
    val condition: String,
) {
    MAGNIFICATION("放大倍数=图像大小/实际大小", "magnification", listOf("image", "actual"), "图像大小与实际大小必须使用同一长度单位，实际大小大于 0"),
    POPULATION_DENSITY("种群密度=个体数/面积", "density", listOf("count", "area"), "调查区域具有代表性，面积大于 0"),
    GROWTH_RATE("增长率=(末数量-初数量)/初数量×100%", "rate", listOf("final", "initial"), "初始数量大于 0，前后统计口径一致"),
    ENERGY_EFFICIENCY("能量传递效率=下一营养级能量/上一营养级能量×100%", "efficiency", listOf("next", "previous"), "两级能量采用相同时间和能量单位，上一营养级能量大于 0"),
    CARDIAC_OUTPUT("心输出量=心率×每搏输出量", "output", listOf("heartRate", "strokeVolume"), "心率和每搏输出量对应同一状态和时间单位"),
    RESPIRATION_RATE("呼吸速率=气体变化量/时间", "rate", listOf("gas", "time"), "气体变化量、时间和样品量的定义保持一致，时间大于 0"),
    PHOTOSYNTHESIS_RATE("光合速率=产物或气体变化量/时间", "rate", listOf("product", "time"), "明确测得的是净光合还是总光合，时间大于 0"),
    GENETIC_PROBABILITY("遗传概率=符合条件组合数/全部等可能组合数", "probability", listOf("favorable", "total"), "配子组合等可能且全部组合不重不漏，总组合数大于 0"),
    SURVIVAL_RATE("成活率=成活个体数/总个体数×100%", "rate", listOf("survived", "total"), "总个体数大于 0，统计时间与标准一致"),
}

enum class BiologyVerificationStatus {
    CORRECT,
    INCORRECT,
    MISSING_VALUES,
    INVALID_MODEL,
    NOT_ALLOWED,
}

data class BiologyVerificationResult(
    val relation: BiologyRelationId,
    val status: BiologyVerificationStatus,
    val expected: Double? = null,
    val submitted: Double? = null,
    val unit: String = "",
    val steps: List<String> = emptyList(),
    val message: String,
)

object BiologyRelationVerifier {
    private const val EPSILON = 1e-7

    fun allowedRelations(category: BiologyCourseCategory): List<BiologyRelationId> = when (category) {
        BiologyCourseCategory.CELL -> listOf(BiologyRelationId.MAGNIFICATION)
        BiologyCourseCategory.METABOLISM -> listOf(BiologyRelationId.RESPIRATION_RATE, BiologyRelationId.PHOTOSYNTHESIS_RATE)
        BiologyCourseCategory.PLANT -> listOf(BiologyRelationId.PHOTOSYNTHESIS_RATE, BiologyRelationId.SURVIVAL_RATE)
        BiologyCourseCategory.HUMAN -> listOf(BiologyRelationId.CARDIAC_OUTPUT, BiologyRelationId.RESPIRATION_RATE)
        BiologyCourseCategory.GENETICS -> listOf(BiologyRelationId.GENETIC_PROBABILITY)
        BiologyCourseCategory.REPRODUCTION -> listOf(BiologyRelationId.SURVIVAL_RATE, BiologyRelationId.GENETIC_PROBABILITY)
        BiologyCourseCategory.ECOLOGY -> listOf(BiologyRelationId.POPULATION_DENSITY, BiologyRelationId.GROWTH_RATE, BiologyRelationId.ENERGY_EFFICIENCY)
        BiologyCourseCategory.EXPERIMENT -> BiologyRelationId.entries
        BiologyCourseCategory.CLASSIFICATION,
        BiologyCourseCategory.EVOLUTION,
        BiologyCourseCategory.GENERAL,
        -> emptyList()
    }

    fun verify(
        category: BiologyCourseCategory,
        relation: BiologyRelationId,
        values: Map<String, Double>,
        submitted: Double?,
        unit: String,
        conditionsAccepted: Boolean,
    ): BiologyVerificationResult {
        if (relation !in allowedRelations(category)) {
            return BiologyVerificationResult(
                relation = relation,
                status = BiologyVerificationStatus.NOT_ALLOWED,
                message = "${relation.display} 不属于当前生物主题允许的数量关系。",
            )
        }
        if (!conditionsAccepted) {
            return BiologyVerificationResult(
                relation = relation,
                status = BiologyVerificationStatus.INVALID_MODEL,
                message = "请先确认模型条件：${relation.condition}。",
            )
        }
        val missing = relation.variables.filterNot(values::containsKey)
        if (missing.isNotEmpty() || submitted == null) {
            return BiologyVerificationResult(
                relation = relation,
                status = BiologyVerificationStatus.MISSING_VALUES,
                message = "请填写 ${(missing + relation.target.takeIf { submitted == null }).filterNotNull().joinToString("、")}。",
            )
        }
        return runCatching {
            val expected = calculate(relation, values)
            require(expected.isFinite()) { "计算结果不是有限数。" }
            val correct = nearlyEqual(expected, submitted)
            BiologyVerificationResult(
                relation = relation,
                status = if (correct) BiologyVerificationStatus.CORRECT else BiologyVerificationStatus.INCORRECT,
                expected = expected,
                submitted = submitted,
                unit = unit,
                steps = listOf(
                    "选用关系：${relation.display}",
                    "适用条件：${relation.condition}",
                    "代入：${relation.variables.joinToString { "$it=${format(values.getValue(it))}" }}",
                    "计算：${relation.target}=${format(expected)} $unit",
                ),
                message = if (correct) {
                    "数值与当前生物学数量关系一致。"
                } else {
                    "结果不一致，请检查统计口径、单位、分母和百分数换算。"
                },
            )
        }.getOrElse { error ->
            BiologyVerificationResult(
                relation = relation,
                status = BiologyVerificationStatus.INVALID_MODEL,
                message = error.message ?: "当前生物模型无法计算。",
            )
        }
    }

    fun calculate(relation: BiologyRelationId, values: Map<String, Double>): Double = when (relation) {
        BiologyRelationId.MAGNIFICATION -> values.required("image") / values.positive("actual")
        BiologyRelationId.POPULATION_DENSITY -> values.nonNegative("count") / values.positive("area")
        BiologyRelationId.GROWTH_RATE -> (values.nonNegative("final") - values.positive("initial")) / values.positive("initial") * 100.0
        BiologyRelationId.ENERGY_EFFICIENCY -> values.nonNegative("next") / values.positive("previous") * 100.0
        BiologyRelationId.CARDIAC_OUTPUT -> values.positive("heartRate") * values.positive("strokeVolume")
        BiologyRelationId.RESPIRATION_RATE -> values.required("gas") / values.positive("time")
        BiologyRelationId.PHOTOSYNTHESIS_RATE -> values.required("product") / values.positive("time")
        BiologyRelationId.GENETIC_PROBABILITY -> values.nonNegative("favorable") / values.positive("total")
        BiologyRelationId.SURVIVAL_RATE -> values.nonNegative("survived") / values.positive("total") * 100.0
    }

    private fun Map<String, Double>.required(key: String): Double =
        get(key)?.takeIf(Double::isFinite) ?: error("缺少有效数据 $key。")

    private fun Map<String, Double>.positive(key: String): Double =
        required(key).also { require(it > 0.0) { "$key 必须大于 0。" } }

    private fun Map<String, Double>.nonNegative(key: String): Double =
        required(key).also { require(it >= 0.0) { "$key 不能小于 0。" } }

    private fun nearlyEqual(left: Double, right: Double): Boolean {
        val scale = maxOf(1.0, abs(left), abs(right))
        return abs(left - right) <= EPSILON * scale
    }

    private fun format(value: Double): String {
        val rounded = kotlin.math.round(value * 1_000_000.0) / 1_000_000.0
        return if (abs(rounded - rounded.toLong()) < 1e-9) rounded.toLong().toString()
        else rounded.toString().trimEnd('0').trimEnd('.')
    }
}
