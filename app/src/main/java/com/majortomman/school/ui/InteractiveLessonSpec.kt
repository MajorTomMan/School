package com.majortomman.school.ui

import com.majortomman.school.data.Lesson
import com.majortomman.school.learning.course.LessonEnrichment
import com.majortomman.school.learning.course.MathCourseContentFactory

enum class InteractiveLessonKind {
    LINEAR_FUNCTION,
    NEWTON_FIRST_LAW,
    MATH_GENERAL,
}

data class InteractiveLessonSpec(
    val kind: InteractiveLessonKind,
    val badge: String,
    val title: String,
    val subtitle: String,
    val formula: String,
    val sourceSummary: String,
    val derivationTitle: String,
    val derivationSteps: List<String>,
    val background: List<String>,
    val misconception: String,
    val sourcePage: Int,
    val sourcePageEnd: Int,
    val enrichment: LessonEnrichment = LessonEnrichment(),
)

object InteractiveLessonCatalog {
    fun resolve(subjectId: String, lesson: Lesson): InteractiveLessonSpec? {
        val title = lesson.title
            .replace(" ", "")
            .replace("　", "")
            .replace("（", "(")
            .replace("）", ")")
        val firstPage = lesson.textbookPages.first.coerceAtLeast(1)
        val lastPage = lesson.textbookPages.last.coerceAtLeast(firstPage)

        return when {
            subjectId == "math" && isLinearFunctionLesson(title) -> {
                val content = MathCourseContentFactory.create(lesson)
                InteractiveLessonSpec(
                    kind = InteractiveLessonKind.LINEAR_FUNCTION,
                    badge = "数学可视化课程 · 教材顺序版",
                    title = "一次函数",
                    subtitle = "先看对应值怎样变化，再把点画到坐标系中",
                    formula = "y = kx + b",
                    sourceSummary = "教材原文（第114页）：“形如 y=kx+b 的函数，叫作一次函数。”\n\n教材先从实际数量关系写出解析式，再观察共同形式；斜率、截距等后续语言不在概念刚出现时提前作为前提。",
                    derivationTitle = "教材怎样引出一次函数",
                    derivationSteps = listOf(
                        "从气温随海拔变化等实际问题识别两个变量。",
                        "根据每升高一定高度时温度的变化，先写出具体数量关系。",
                        "比较多个关系式的共同结构，再抽象为 y=kx+b。",
                        "列表计算对应值，描点并观察图像。",
                    ),
                    background = content.background,
                    misconception = "不要把一次函数理解成先有一条直线再调参数；教材顺序是先得到变量关系，再用表格和图像观察。",
                    sourcePage = firstPage,
                    sourcePageEnd = lastPage,
                    enrichment = content.enrichment,
                )
            }
            subjectId == "physics" && title.contains("牛顿第一定律") -> InteractiveLessonSpec(
                kind = InteractiveLessonKind.NEWTON_FIRST_LAW,
                badge = "物理思想实验 · 教材顺序版",
                title = "牛顿第一定律",
                subtitle = "先看小球为什么停下，再想象把阻力逐渐减小",
                formula = "若没有摩擦，球将永远运动下去。",
                sourceSummary = "教材原文（第84页）：“若没有摩擦，球将永远运动下去。”\n\n教材从日常运动受阻现象出发，通过伽利略斜面理想实验逐渐排除阻力，说明力不是维持运动的原因。",
                derivationTitle = "教材怎样完成这个思想实验",
                derivationSteps = listOf(
                    "观察小球沿两个斜面运动并趋向回到原高度。",
                    "第二个斜面越平缓，小球运动距离越远。",
                    "把现实中的停止归因于摩擦等阻力。",
                    "继续理想化到无阻力水平面，得到保持运动的结论。",
                ),
                background = listOf(
                    "理想实验从真实观察出发，逐步去掉干扰因素，再用逻辑推理抓住本质。",
                    "本节不提前使用摩擦系数或牛顿第二定律公式。",
                ),
                misconception = "现实小球停下说明阻力改变了运动状态，不说明运动必须由力维持。",
                sourcePage = firstPage,
                sourcePageEnd = lastPage,
            )
            subjectId == "math" -> {
                val content = MathCourseContentFactory.create(lesson)
                InteractiveLessonSpec(
                    kind = InteractiveLessonKind.MATH_GENERAL,
                    badge = content.badge,
                    title = lesson.title,
                    subtitle = content.subtitle,
                    formula = content.representativeExpression,
                    sourceSummary = content.sourceSummary,
                    derivationTitle = content.reasoningTitle,
                    derivationSteps = content.reasoningSteps,
                    background = content.background,
                    misconception = content.misconception,
                    sourcePage = firstPage,
                    sourcePageEnd = lastPage,
                    enrichment = content.enrichment,
                )
            }
            else -> null
        }
    }

    private fun isLinearFunctionLesson(title: String): Boolean =
        title == "一次函数" ||
            title.contains("一次函数的概念") ||
            title.contains("一次函数的图象和性质")
}
