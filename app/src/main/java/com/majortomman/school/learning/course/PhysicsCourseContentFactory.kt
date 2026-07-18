package com.majortomman.school.learning.course

import com.majortomman.school.data.Lesson
import com.majortomman.school.learning.capability.ConceptId
import com.majortomman.school.learning.capability.ContentOrigin
import com.majortomman.school.learning.capability.OperationId
import com.majortomman.school.learning.capability.WidgetType

enum class PhysicsCourseCategory {
    MEASUREMENT,
    KINEMATICS,
    FORCE,
    ENERGY,
    MOMENTUM,
    GRAVITY,
    THERMAL,
    WAVE,
    SOUND,
    LIGHT,
    ELECTRICITY,
    MAGNETISM,
    EXPERIMENT,
    GENERAL,
}

data class PhysicsCourseContent(
    val category: PhysicsCourseCategory,
    val subtitle: String,
    val formula: String,
    val sourceSummary: String,
    val steps: List<String>,
    val background: List<String>,
    val misconception: String,
    val enrichment: LessonEnrichment,
)

object PhysicsCourseContentFactory {
    fun create(lesson: Lesson): PhysicsCourseContent {
        val category = classify(lesson.title)
        val profile = profile(category)
        val pages = lesson.textbookPages
        val pageLabel = if (pages.first == pages.last) "第 ${pages.first} 页" else "第 ${pages.first}—${pages.last} 页"
        return PhysicsCourseContent(
            category = category,
            subtitle = profile.subtitle,
            formula = profile.formula,
            sourceSummary = "本课程依据教材目录中的“${lesson.title}”及$pageLabel 组织。仓库没有保存教材正文，因此不生成未经核对的原文；School 解释只覆盖该主题的基础模型、条件和常见教材顺序。绑定 PDF 后可核对原页。",
            steps = profile.steps,
            background = profile.background,
            misconception = profile.misconception,
            enrichment = LessonEnrichment(
                background = profile.background.mapIndexed { index, text ->
                    CourseNote(
                        origin = ContentOrigin.SCHOOL_EXPLANATION,
                        title = if (index == 0) "物理背景" else "模型边界",
                        body = text,
                    )
                },
                extensions = listOf(
                    CourseNote(
                        origin = ContentOrigin.OPTIONAL_EXTENSION,
                        title = "扩展：改变条件继续观察",
                        body = profile.extension,
                    ),
                ),
                visualization = CourseVisualizationSpec(
                    kind = profile.visualization,
                    title = profile.visualTitle,
                    description = profile.visualDescription,
                    parameters = profile.parameters,
                    requiredConcepts = profile.concepts,
                    requiredOperations = profile.operations,
                    requiredWidgets = profile.widgets,
                ),
                verification = CourseVerificationSpec(
                    kind = CourseVerificationKind.PHYSICAL_RELATION,
                    title = "教材物理模型验证",
                    prompt = "选择当前主题允许的物理关系，确认模型条件，统一单位并填写已知量和目标结果。",
                    inputHint = profile.formula,
                    examples = profile.examples,
                    requiredConcepts = profile.concepts,
                    requiredOperations = profile.operations,
                ),
            ),
        )
    }

    fun classify(title: String): PhysicsCourseCategory {
        val t = title.replace(" ", "").replace("　", "")
        return when {
            t.contains("实验") || t.contains("探究") || t.contains("测量") && t.contains("方法") -> PhysicsCourseCategory.EXPERIMENT
            t.contains("电流") || t.contains("电压") || t.contains("电阻") || t.contains("欧姆") ||
                t.contains("电路") || t.contains("电功") || t.contains("电能") || t.contains("电热") || t.contains("家庭电路") -> PhysicsCourseCategory.ELECTRICITY
            t.contains("磁") || t.contains("电磁") || t.contains("发电机") || t.contains("电动机") -> PhysicsCourseCategory.MAGNETISM
            t.contains("光") || t.contains("透镜") || t.contains("成像") || t.contains("反射") || t.contains("折射") -> PhysicsCourseCategory.LIGHT
            t.contains("声音") || t.contains("声") -> PhysicsCourseCategory.SOUND
            t.contains("波") || t.contains("振动") || t.contains("周期") || t.contains("频率") -> PhysicsCourseCategory.WAVE
            t.contains("热") || t.contains("温度") || t.contains("内能") || t.contains("比热") || t.contains("物态") -> PhysicsCourseCategory.THERMAL
            t.contains("万有引力") || t.contains("重力") || t.contains("天体") || t.contains("宇宙") -> PhysicsCourseCategory.GRAVITY
            t.contains("动量") || t.contains("冲量") || t.contains("碰撞") -> PhysicsCourseCategory.MOMENTUM
            t.contains("功") || t.contains("能") || t.contains("机械效率") || t.contains("功率") -> PhysicsCourseCategory.ENERGY
            t.contains("力") || t.contains("压强") || t.contains("浮力") || t.contains("平衡") || t.contains("弹簧") -> PhysicsCourseCategory.FORCE
            t.contains("运动") || t.contains("速度") || t.contains("加速度") || t.contains("位移") || t.contains("路程") -> PhysicsCourseCategory.KINEMATICS
            t.contains("长度") || t.contains("质量") || t.contains("密度") || t.contains("单位") || t.contains("误差") -> PhysicsCourseCategory.MEASUREMENT
            else -> PhysicsCourseCategory.GENERAL
        }
    }

    private fun profile(category: PhysicsCourseCategory): PhysicsProfile = when (category) {
        PhysicsCourseCategory.MEASUREMENT -> profile(
            "从测量对象、仪器、单位和误差建立可信数据",
            "ρ=m/V",
            CourseVisualizationKind.DATA_TABLE,
            "测量值与单位换算",
            "改变质量、体积或测量分度值，观察密度、有效数字和误差表达。",
            listOf(p("m", "质量", "2", 0.1, 10.0, 0.1, "kg"), p("V", "体积", "1", 0.1, 10.0, 0.1, "m³")),
            listOf("明确被测量和仪器量程", "读取数值并记录单位", "按分度值保留合理数字", "需要时换算为统一单位", "用物理关系计算并说明误差来源"),
            listOf("测量结果由数值和单位共同组成。", "仪器精度和实验条件决定结果可以报告到什么程度。"),
            "不能只写数字不写单位，也不能把计算器显示的所有小数都当成有效结果。",
            "可比较不同测量方案的系统误差和随机误差，但不提前引入教材未讲的不确定度统计。",
            setOf(ConceptId.PHYSICAL_QUANTITY, ConceptId.UNIT, ConceptId.DIMENSION, ConceptId.SIGNIFICANT_FIGURES),
            setOf(OperationId.CONVERT_UNIT, OperationId.CHECK_DIMENSION),
            setOf(WidgetType.UNIT_CONVERTER, WidgetType.DIMENSION_CHECKER),
            listOf("ρ=m/V"),
        )
        PhysicsCourseCategory.KINEMATICS -> profile(
            "把参考系、位置变化、时间和图像放在同一运动过程中",
            "v=s/t  ·  a=(v-v₀)/t",
            CourseVisualizationKind.MOTION,
            "运动状态与图像",
            "调节初速度、加速度和时间，观察位置、速度和运动轨迹。",
            listOf(p("v0", "初速度", "0", -10.0, 20.0, 0.5, "m/s"), p("a", "加速度", "2", -5.0, 5.0, 0.5, "m/s²"), p("t", "时间", "3", 0.1, 10.0, 0.1, "s")),
            listOf("选择参考系和正方向", "区分路程、位移、速率和速度", "确认各量对应同一时间段", "用关系式或图像计算", "回到运动情境解释符号和结果"),
            listOf("运动描述依赖参考系。", "平均速度描述一段过程，不能直接代表任意时刻。"),
            "不要把速度为负理解成物体一定在减速；负号首先表示方向。",
            "可观察匀变速模型，但只有教材明确引入后才使用完整运动学公式。",
            setOf(ConceptId.PHYSICAL_QUANTITY, ConceptId.FUNCTION_GRAPH),
            setOf(OperationId.SOLVE_RELATION, OperationId.PLOT_2D),
            setOf(WidgetType.RELATION_CALCULATOR, WidgetType.COORDINATE_GRAPH_2D),
            listOf("v=s/t", "a=(v-v0)/t"),
        )
        PhysicsCourseCategory.FORCE -> profile(
            "从受力对象和相互作用出发建立受力图与运动关系",
            "F合=ma  ·  p=F/S",
            CourseVisualizationKind.FORCE_DIAGRAM,
            "受力与合力",
            "改变质量、力和受力面积，观察力箭头、合力、加速度或压强。",
            listOf(p("m", "质量", "2", 0.1, 10.0, 0.1, "kg"), p("F", "作用力", "6", -20.0, 20.0, 0.5, "N"), p("S", "受力面积", "0.5", 0.05, 5.0, 0.05, "m²")),
            listOf("确定研究对象", "逐个寻找外界对它的相互作用", "按方向画出受力", "求合力并确认模型条件", "联系加速度、平衡或压强"),
            listOf("力是物体间相互作用，受力图只画研究对象受到的力。", "合力为零表示运动状态不改变，不等于物体一定静止。"),
            "作用力与反作用力作用在不同物体上，不能在同一物体的受力图中相互抵消。",
            "可增加摩擦、斜面或流体模型，但每个扩展必须声明方向和理想化条件。",
            setOf(ConceptId.PHYSICAL_MODEL, ConceptId.MODEL_ASSUMPTION),
            setOf(OperationId.VALIDATE_MODEL_CONDITIONS, OperationId.SOLVE_RELATION),
            setOf(WidgetType.VECTOR_GEOMETRY, WidgetType.RELATION_CALCULATOR),
            listOf("F=m*a", "p=F/S"),
        )
        PhysicsCourseCategory.ENERGY -> profile(
            "用功、能量转化和功率描述过程，而不是只背公式",
            "W=Fs  ·  P=W/t",
            CourseVisualizationKind.PROCESS,
            "功、能量与功率流",
            "改变力、位移、时间和质量，观察做功、能量柱和功率变化。",
            listOf(p("F", "力", "10", 0.0, 50.0, 1.0, "N"), p("s", "位移", "2", 0.0, 10.0, 0.5, "m"), p("t", "时间", "4", 0.1, 20.0, 0.5, "s")),
            listOf("明确系统和过程", "判断力是否对位移做功", "选择能量形式和参考面", "写出转化或守恒关系", "用功率说明过程快慢"),
            listOf("能量是状态量或系统属性，功是能量转移的一种方式。", "机械能守恒需要满足教材指定的力和系统条件。"),
            "有力不一定做功；没有机械功也不代表没有能量变化。",
            "可比较功率相同但过程不同的情形，或进一步引入效率和耗散。",
            setOf(ConceptId.PHYSICAL_MODEL, ConceptId.PHYSICAL_QUANTITY),
            setOf(OperationId.SOLVE_RELATION, OperationId.CHECK_DIMENSION),
            setOf(WidgetType.RELATION_CALCULATOR, WidgetType.DIMENSION_CHECKER),
            listOf("W=F*s", "P=W/t", "Ek=m*v^2/2"),
        )
        PhysicsCourseCategory.MOMENTUM -> profile("从相互作用时间与运动状态变化理解动量", "p=mv", CourseVisualizationKind.MOTION, "碰撞前后动量", "调节质量和速度，观察动量方向与系统总量。", listOf(p("m", "质量", "1", 0.1, 5.0, 0.1, "kg"), p("v", "速度", "3", -10.0, 10.0, 0.5, "m/s")), listOf("选择系统", "规定正方向", "写出各物体动量", "检查外力冲量条件", "比较作用前后总动量"), listOf("动量是矢量，系统动量是各物体动量的矢量和。", "动量守恒针对系统且需要外力冲量可忽略。"), "不要把每个物体的动量分别守恒，也不要忽略速度方向。", "可观察弹性与非弹性碰撞的能量差异，但不改变动量守恒的系统条件。", setOf(ConceptId.VECTOR, ConceptId.PHYSICAL_MODEL), setOf(OperationId.SOLVE_RELATION), setOf(WidgetType.VECTOR_GEOMETRY), listOf("p=m*v"))
        PhysicsCourseCategory.GRAVITY -> profile("从重力到天体运动，始终说明适用尺度和理想模型", "Ep=mgh", CourseVisualizationKind.MOTION, "重力场与轨道", "调节高度、速度或质量，观察重力势能与轨迹趋势。", listOf(p("m", "质量", "1", 0.1, 10.0, 0.1, "kg"), p("h", "高度", "5", -10.0, 20.0, 0.5, "m"), p("g", "重力加速度", "9.8", 1.0, 15.0, 0.1, "m/s²")), listOf("明确研究尺度", "选择近地面或万有引力模型", "规定参考面或坐标", "写出关系", "检查结果与模型范围"), listOf("近地面重力场常近似均匀，天体尺度需要使用更一般模型。", "势能零点可以选择，但势能变化具有物理意义。"), "不要在所有高度都把 g 当作完全不变，也不要把质量和重量混为一谈。", "可进一步观察轨道速度和周期，但不在初始重力章节提前使用。", setOf(ConceptId.PHYSICAL_MODEL), setOf(OperationId.SOLVE_RELATION), setOf(WidgetType.COORDINATE_3D), listOf("Ep=m*g*h"))
        PhysicsCourseCategory.THERMAL -> profile("用微观运动和能量转移联系温度、热量与物态", "Q=cmΔT", CourseVisualizationKind.PARTICLE_MODEL, "粒子运动与温度", "调节温度、质量和比热容，观察粒子运动示意和吸放热。", listOf(p("m", "质量", "1", 0.1, 5.0, 0.1, "kg"), p("c", "比热容", "4200", 100.0, 5000.0, 100.0, "J/(kg·°C)"), p("dT", "温度变化", "10", -50.0, 100.0, 1.0, "°C")), listOf("区分温度、内能和热量", "明确系统与过程", "判断有无相变", "选择比热或相变模型", "检查热量正负和能量来源"), listOf("温度反映热运动程度，热量描述传热过程。", "同温度物体内能不一定相等。"), "物体含有的是内能，不是‘含有热量’。", "可比较传导、对流和辐射，但公式验证只用于教材明确的定量模型。", setOf(ConceptId.PHYSICAL_MODEL, ConceptId.PHYSICAL_QUANTITY), setOf(OperationId.SOLVE_RELATION), setOf(WidgetType.RELATION_CALCULATOR), listOf("Q=c*m*dT"))
        PhysicsCourseCategory.WAVE,
        PhysicsCourseCategory.SOUND,
        -> profile("从振动源、介质、周期和传播区分波的各个量", "v=fλ  ·  f=1/T", CourseVisualizationKind.WAVE, "波形与传播", "调节振幅、频率、波长和相位，观察介质质点振动与波形传播。", listOf(p("amplitude", "振幅", "1", 0.1, 3.0, 0.1), p("f", "频率", "2", 0.1, 10.0, 0.1, "Hz"), p("lambda", "波长", "3", 0.1, 10.0, 0.1, "m")), listOf("确定振动源和介质", "区分质点振动与波的传播", "标出周期、频率和波长", "使用 v=fλ", "解释能量传播而非物质整体迁移"), listOf("机械波需要介质，介质质点在平衡位置附近振动。", "声音的音调、响度和音色对应不同物理特征。"), "不要把波形向前移动理解成每个介质质点都随波远距离移动。", "可观察叠加、驻波和多普勒效应，但只在对应章节启用。", setOf(ConceptId.FUNCTION_GRAPH, ConceptId.PHYSICAL_MODEL), setOf(OperationId.PLOT_2D, OperationId.SOLVE_RELATION), setOf(WidgetType.COORDINATE_GRAPH_2D), listOf("v=f*lambda", "f=1/T"))
        PhysicsCourseCategory.LIGHT -> profile("用光线模型描述传播、反射、折射和成像条件", "1/f=1/u+1/v", CourseVisualizationKind.PROCESS, "光线与成像", "调节物距、焦距或介质，观察主光线和像的位置、大小、正倒。", listOf(p("u", "物距", "30", 1.0, 100.0, 1.0, "cm"), p("v", "像距", "15", -100.0, 100.0, 1.0, "cm")), listOf("确定光传播边界和元件", "按教材规则画主光线", "寻找反射或折射后的交点", "判断实像/虚像和正倒", "用定量关系核对"), listOf("光线是表示传播方向的模型，不是可见的实体轨迹。", "成像符号约定必须与教材保持一致。"), "不能只凭一条光线确定像，也不能混用不同教材的正负号约定。", "可进一步观察全反射、光纤或干涉，但几何光学章节先使用教材光线模型。", setOf(ConceptId.LINE, ConceptId.PHYSICAL_MODEL), setOf(OperationId.INTERSECT_GEOMETRY, OperationId.SOLVE_RELATION), setOf(WidgetType.VECTOR_GEOMETRY), listOf("1/f=1/u+1/v"))
        PhysicsCourseCategory.ELECTRICITY -> profile("把电路拓扑、测量接法、欧姆关系和电功率统一验证", "U=IR  ·  P=UI", CourseVisualizationKind.CIRCUIT, "直流电路与电功率", "调节电源、电阻、开关和接法，观察节点电压、电流、功率和拓扑警告。", listOf(p("U", "电源电压", "6", 0.1, 24.0, 0.1, "V"), p("R", "电阻", "3", 0.1, 100.0, 0.1, "Ω")), listOf("识别节点和支路", "检查开路、短路与电表接法", "明确串联或并联", "在条件满足时使用欧姆定律", "计算电流、电压、功率和电能"), listOf("电流表近似串联、低电阻；电压表近似并联、高电阻。", "亮度应由灯泡功率和额定条件解释，而不是只看电流大小。"), "不能在没有检查电路拓扑时直接套 U=IR，也不能把电源直接短接。", "可进一步使用节点电压法分析复杂线性直流电路；电容、电感和二极管已预留但不伪装为完整动态仿真。", setOf(ConceptId.CIRCUIT_TOPOLOGY, ConceptId.VOLTAGE, ConceptId.ELECTRIC_CURRENT, ConceptId.RESISTANCE, ConceptId.ELECTRIC_POWER), setOf(OperationId.CHECK_CIRCUIT_TOPOLOGY, OperationId.SOLVE_DC_CIRCUIT, OperationId.COMPUTE_ELECTRICAL_POWER), setOf(WidgetType.CIRCUIT_EDITOR, WidgetType.CIRCUIT_SOLVER, WidgetType.ELECTRICAL_POWER), listOf("U=I*R", "P=U*I", "Q=I^2*R*t"))
        PhysicsCourseCategory.MAGNETISM -> profile("从磁场方向、受力方向和电磁转换建立空间关系", "方向规则 + 能量转换", CourseVisualizationKind.VECTOR, "磁场与受力方向", "调节电流、磁场和运动方向，观察场线与受力方向。", listOf(p("current", "电流方向", "1", -1.0, 1.0, 2.0), p("field", "磁场强弱", "1", 0.0, 3.0, 0.1)), listOf("明确磁体、电流或运动电荷", "画出磁场方向", "按教材规则判断受力或感应方向", "检查能量转换", "用实验现象验证"), listOf("磁场用方向和强弱描述，场线是模型。", "电动机和发电机体现电能与机械能的转换。"), "磁场线不是实际存在的线，也不能仅靠二维图忽略空间方向。", "洛伦兹力和电磁感应定量关系只在对应高中章节启用。", setOf(ConceptId.VECTOR, ConceptId.PHYSICAL_MODEL), setOf(OperationId.COMPUTE_VECTOR), setOf(WidgetType.VECTOR_GEOMETRY), emptyList())
        PhysicsCourseCategory.EXPERIMENT -> profile("按目的、器材、步骤、变量控制、数据和结论组织实验", "测量 → 数据 → 图像 → 结论", CourseVisualizationKind.PROCESS, "实验过程与数据记录", "改变自变量或测量误差，观察表格、图像和结论可靠性。", listOf(p("trial", "实验次数", "5", 1.0, 20.0, 1.0)), listOf("明确实验目的", "识别自变量、因变量和控制变量", "检查器材量程与接法", "按顺序记录数据", "处理数据并区分现象、证据和结论"), listOf("实验结论必须由数据支持。", "理想模型和真实测量之间的差异需要解释。"), "不能先写结论再挑数据，也不能把偶然一次结果当成稳定规律。", "可增加误差分析和拟合，但不超出教材对数据处理的要求。", setOf(ConceptId.PHYSICAL_MODEL, ConceptId.MEASUREMENT_UNCERTAINTY), setOf(OperationId.VALIDATE_MODEL_CONDITIONS), setOf(WidgetType.DIMENSION_CHECKER), listOf("ρ=m/V", "U=I*R"))
        PhysicsCourseCategory.GENERAL -> profile("围绕物理对象、状态、相互作用、过程和模型条件学习", "物理量 + 条件 + 关系", CourseVisualizationKind.PROCESS, "物理过程验证", "调节教材涉及的量，观察状态、图像和结论如何变化。", listOf(p("x", "示例量", "1", -10.0, 10.0, 1.0)), listOf("定位教材主题和页码", "识别研究对象和物理量", "写明参考系或系统", "声明理想化条件", "选择当前章节允许的关系并验证"), listOf("物理公式是模型关系，不是脱离条件的代数口诀。", "数值、单位、方向和适用条件共同构成物理答案。"), "不要因为字母相同就套用公式；先确认每个符号在本课中的定义。", "可从实验、图像或极限情形继续研究，但扩展不作为本课必会前提。", setOf(ConceptId.PHYSICAL_QUANTITY, ConceptId.PHYSICAL_MODEL), setOf(OperationId.VALIDATE_MODEL_CONDITIONS, OperationId.SOLVE_RELATION), setOf(WidgetType.RELATION_CALCULATOR), listOf("v=s/t"))
    }

    private fun profile(
        subtitle: String,
        formula: String,
        visualization: CourseVisualizationKind,
        visualTitle: String,
        visualDescription: String,
        parameters: List<CourseParameterSpec>,
        steps: List<String>,
        background: List<String>,
        misconception: String,
        extension: String,
        concepts: Set<ConceptId>,
        operations: Set<OperationId>,
        widgets: Set<WidgetType>,
        examples: List<String>,
    ) = PhysicsProfile(subtitle, formula, visualization, visualTitle, visualDescription, parameters, steps, background, misconception, extension, concepts, operations, widgets, examples)

    private fun p(id: String, label: String, default: String, min: Double, max: Double, step: Double, unit: String = "") = CourseParameterSpec(
        id = id,
        label = label,
        kind = if (step >= 1.0 && min % 1.0 == 0.0 && max % 1.0 == 0.0 && default.toDoubleOrNull()?.rem(1.0) == 0.0) CourseParameterKind.INTEGER else CourseParameterKind.NUMBER,
        defaultValue = default,
        unit = unit,
        minimum = min,
        maximum = max,
        step = step,
    )

    private data class PhysicsProfile(
        val subtitle: String,
        val formula: String,
        val visualization: CourseVisualizationKind,
        val visualTitle: String,
        val visualDescription: String,
        val parameters: List<CourseParameterSpec>,
        val steps: List<String>,
        val background: List<String>,
        val misconception: String,
        val extension: String,
        val concepts: Set<ConceptId>,
        val operations: Set<OperationId>,
        val widgets: Set<WidgetType>,
        val examples: List<String>,
    )
}
