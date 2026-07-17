package com.majortomman.school.ui

import com.majortomman.school.data.Lesson

enum class InteractiveLessonKind {
    LINEAR_FUNCTION,
    NEWTON_FIRST_LAW,
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
            subjectId == "math" && isLinearFunctionLesson(title) -> InteractiveLessonSpec(
                kind = InteractiveLessonKind.LINEAR_FUNCTION,
                badge = "数学可视化实验 · V1",
                title = "一次函数",
                subtitle = "让公式、数据表和函数图像一起变化",
                formula = "y = kx + b",
                sourceSummary = "教材从气温、弹簧伸长等变量关系出发，归纳出形如 y=kx+b（k≠0）的函数；当 b=0 时，它是正比例函数。",
                derivationTitle = "从变化量看懂斜率",
                derivationSteps = listOf(
                    "在直线上任选两点，横坐标变化记为 Δx，纵坐标变化记为 Δy。",
                    "同一条直线上，Δy/Δx 保持不变，这个固定比值就是斜率 k。",
                    "当 x 增加 1 时，y 改变 k；因此从截距 b 出发可以写成 y=kx+b。",
                    "k 决定直线倾斜方向和快慢，b 决定直线与 y 轴的交点。",
                ),
                background = listOf(
                    "一次函数是描述“均匀变化”的最基本模型。匀速路程、固定单价总价、稳定温度梯度都可以转成直线关系。",
                    "图像不是公式的装饰。图像上的每一个点 (x,y) 都是一组满足函数关系的真实数据。",
                ),
                misconception = "k=0 时图像仍是一条水平直线，但按本册教材的定义，它退化为常值函数，不属于 k≠0 的一次函数。",
                sourcePage = firstPage,
                sourcePageEnd = lastPage,
            )

            subjectId == "physics" && title.contains("牛顿第一定律") -> InteractiveLessonSpec(
                kind = InteractiveLessonKind.NEWTON_FIRST_LAW,
                badge = "物理思想实验 · V1",
                title = "牛顿第一定律",
                subtitle = "把摩擦逐渐拿掉，观察运动真正需要什么",
                formula = "ΣF = 0  ⇒  a = 0  ⇒  v = 常量",
                sourceSummary = "教材先用伽利略斜面理想实验排除摩擦干扰，再指出：力不是维持运动的原因，而是改变物体运动状态的原因。",
                derivationTitle = "它不是代数推导，而是理想实验",
                derivationSteps = listOf(
                    "球从斜面滚下时加速，滚上另一斜面时减速，并趋向回到原来的高度。",
                    "第二个斜面越平缓，球为了达到原高度就要运动得越远。",
                    "把斜面想象成完全水平并忽略摩擦，球将不再有停下来的理由。",
                    "由此抽象出惯性：合力为零时，物体保持静止或匀速直线运动。",
                ),
                background = listOf(
                    "日常物体会停下，通常是因为摩擦力和空气阻力一直在改变它的速度。",
                    "牛顿第一定律描述的是理想状态，也是判断一个参考系是否为惯性系的基础。",
                ),
                misconception = "“物体向前运动，所以一定受到向前的力”是常见误区。速度说明运动状态，合力决定速度是否改变。",
                sourcePage = firstPage,
                sourcePageEnd = lastPage,
            )

            else -> null
        }
    }

    private fun isLinearFunctionLesson(title: String): Boolean =
        title == "一次函数" ||
            title.contains("一次函数的概念") ||
            title.contains("一次函数的图象和性质")
}
