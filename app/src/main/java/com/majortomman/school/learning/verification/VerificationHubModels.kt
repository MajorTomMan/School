package com.majortomman.school.learning.verification

enum class VerificationSubject(
    val label: String,
    val subtitle: String,
) {
    MATHEMATICS(
        label = "数学",
        subtitle = "表达式 · 计算 · 函数图像",
    ),
    PHYSICS(
        label = "物理",
        subtitle = "公式 · 关系 · 单位 · 图像",
    ),
    CHEMISTRY(
        label = "化学",
        subtitle = "化学式 · 方程式 · 结构",
    ),
    BIOLOGY(
        label = "生物",
        subtitle = "数量关系 · 结构 · 过程",
    ),
    ENGLISH(
        label = "英语",
        subtitle = "词形 · 语序 · 语法结构",
    ),
    JAPANESE(
        label = "日语",
        subtitle = "助词 · 活用 · 语体结构",
    ),
}
