package com.majortomman.school.learning.course

import com.majortomman.school.data.Lesson
import com.majortomman.school.learning.capability.ConceptId
import com.majortomman.school.learning.capability.ContentOrigin
import com.majortomman.school.learning.capability.OperationId
import com.majortomman.school.learning.capability.WidgetType

enum class ChemistryCourseCategory {
    MATTER,
    PARTICLE,
    ELEMENT,
    FORMULA,
    EQUATION,
    SOLUTION,
    STOICHIOMETRY,
    ACID_BASE,
    ION_REDOX,
    ORGANIC,
    EXPERIMENT,
    GENERAL,
}

data class ChemistryCourseContent(
    val category: ChemistryCourseCategory,
    val subtitle: String,
    val formula: String,
    val sourceSummary: String,
    val steps: List<String>,
    val background: List<String>,
    val misconception: String,
    val enrichment: LessonEnrichment,
)

object ChemistryCourseContentFactory {
    fun create(lesson: Lesson): ChemistryCourseContent {
        val category = classify(lesson.title)
        val profile = profile(category)
        val pages = lesson.textbookPages
        val pageLabel = if (pages.first == pages.last) "第 ${pages.first} 页" else "第 ${pages.first}—${pages.last} 页"
        return ChemistryCourseContent(
            category = category,
            subtitle = profile.subtitle,
            formula = profile.formula,
            sourceSummary = "本课程依据教材目录中的“${lesson.title}”及$pageLabel 组织。仓库没有保存教材正文，因此不编造原文、反应条件或未知生成物；School 解释只使用当前主题可确定的组成、守恒、结构和计算规则。绑定 PDF 后可核对原页。",
            steps = profile.steps,
            background = profile.background,
            misconception = profile.misconception,
            enrichment = LessonEnrichment(
                background = profile.background.mapIndexed { index, text ->
                    CourseNote(ContentOrigin.SCHOOL_EXPLANATION, if (index == 0) "化学背景" else "模型边界", text)
                },
                extensions = listOf(
                    CourseNote(ContentOrigin.OPTIONAL_EXTENSION, "扩展：进一步研究结构和条件", profile.extension),
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
                    kind = if (category == ChemistryCourseCategory.ORGANIC) CourseVerificationKind.ORGANIC_STRUCTURE else if (category == ChemistryCourseCategory.FORMULA || category == ChemistryCourseCategory.ELEMENT) CourseVerificationKind.CHEMICAL_FORMULA else CourseVerificationKind.CHEMICAL_EQUATION,
                    title = "化学结构与守恒验证",
                    prompt = "输入化学式、给出反应物和生成物的方程式，或输入教材范围内的有机结构简式。",
                    inputHint = profile.formula,
                    examples = profile.examples,
                    requiredConcepts = profile.concepts,
                    requiredOperations = profile.operations,
                ),
            ),
        )
    }

    fun classify(title: String): ChemistryCourseCategory {
        val t = title.replace(" ", "").replace("　", "")
        return when {
            t.contains("实验") || t.contains("探究") || t.contains("制取") || t.contains("检验") -> ChemistryCourseCategory.EXPERIMENT
            t.contains("有机") || t.contains("烃") || t.contains("醇") || t.contains("醛") || t.contains("羧酸") || t.contains("酯") || t.contains("糖类") || t.contains("蛋白质") || t.contains("高分子") -> ChemistryCourseCategory.ORGANIC
            t.contains("氧化还原") || t.contains("离子反应") || t.contains("电解") || t.contains("原电池") || t.contains("化合价") -> ChemistryCourseCategory.ION_REDOX
            t.contains("酸") || t.contains("碱") || t.contains("盐") || t.contains("pH") || t.contains("中和") -> ChemistryCourseCategory.ACID_BASE
            t.contains("物质的量") || t.contains("摩尔") || t.contains("化学计量") || t.contains("产率") || t.contains("质量分数") -> ChemistryCourseCategory.STOICHIOMETRY
            t.contains("溶液") || t.contains("浓度") || t.contains("溶解") || t.contains("结晶") -> ChemistryCourseCategory.SOLUTION
            t.contains("方程式") || t.contains("反应") || t.contains("燃烧") || t.contains("分解") || t.contains("化合") -> ChemistryCourseCategory.EQUATION
            t.contains("化学式") || t.contains("式量") || t.contains("相对分子质量") -> ChemistryCourseCategory.FORMULA
            t.contains("元素") || t.contains("周期") || t.contains("原子结构") || t.contains("核外电子") -> ChemistryCourseCategory.ELEMENT
            t.contains("分子") || t.contains("原子") || t.contains("离子") || t.contains("微粒") -> ChemistryCourseCategory.PARTICLE
            t.contains("物质") || t.contains("性质") || t.contains("变化") || t.contains("分类") -> ChemistryCourseCategory.MATTER
            else -> ChemistryCourseCategory.GENERAL
        }
    }

    private fun profile(category: ChemistryCourseCategory): ChemistryProfile = when (category) {
        ChemistryCourseCategory.MATTER -> base(
            "从宏观现象、物质性质和微观解释建立证据链",
            "物质 → 性质 → 变化 → 证据",
            CourseVisualizationKind.PROCESS,
            "宏观现象与微观解释",
            "调节温度、状态或条件，区分物理变化、化学变化和证据。",
            listOf(p("temperature", "温度", "25", -20.0, 200.0, 5.0, "°C")),
            listOf("观察现象并记录", "区分物质与能量变化", "判断是否生成新物质", "用微粒模型解释", "说明结论证据和适用条件"),
            listOf("化学结论来自可重复现象和组成变化。", "宏观现象、微观粒子和符号表达是同一过程的不同表示。"),
            "有颜色、发光或放热不一定单独证明发生化学反应，必须结合是否生成新物质。",
            "可比较反应速率和条件，但不在基础变化章节提前引入完整动力学。",
            setOf(ConceptId.PHYSICAL_MODEL), setOf(OperationId.VALIDATE_MODEL_CONDITIONS), setOf(WidgetType.CHEMICAL_FORMULA_INSPECTOR), listOf("H2O"),
        )
        ChemistryCourseCategory.PARTICLE -> base(
            "用原子、分子和离子解释物质组成与变化",
            "粒子种类 + 数量 + 排列",
            CourseVisualizationKind.PARTICLE_MODEL,
            "粒子组成与重新组合",
            "调节粒子数量和状态，观察组成、间隔和反应前后重新组合。",
            listOf(p("count", "粒子数量", "8", 1.0, 32.0, 1.0)),
            listOf("确定研究的粒子类型", "表示粒子组成", "区分粒子运动与化学键变化", "比较反应前后原子种类和数目", "用符号表达"),
            listOf("化学反应改变原子的组合方式，不创造或消灭元素原子。", "分子、原子和离子是不同层次的粒子概念。"),
            "不能把物质宏观颗粒直接当作分子，也不能把离子的电荷写成原子个数。",
            "可继续研究电子转移和成键，但只在相应教材章节启用。",
            setOf(ConceptId.ELEMENT, ConceptId.ATOM_COUNT, ConceptId.ION), setOf(OperationId.COUNT_ATOMS), setOf(WidgetType.CHEMICAL_FORMULA_INSPECTOR), listOf("H2O", "NH4^+"),
        )
        ChemistryCourseCategory.ELEMENT -> base(
            "从元素符号、原子序数和周期位置理解元素身份",
            "元素符号 ↔ 原子序数 ↔ 周期位置",
            CourseVisualizationKind.DATA_TABLE,
            "周期表位置与性质线索",
            "选择元素，观察符号、原子序数、相对原子质量和常见氧化态。",
            listOf(p("atomicNumber", "原子序数", "8", 1.0, 118.0, 1.0)),
            listOf("确认元素身份", "读取元素符号和原子序数", "联系核外电子或周期位置", "比较同族或同周期趋势", "回到具体物质组成"),
            listOf("元素是具有相同核电荷数的一类原子的总称。", "周期律是性质随原子结构周期变化的规律。"),
            "元素符号表示元素身份，不等同于一个具体分子或某个宏观物质。",
            "可查看完整周期趋势，但不把高中电子排布提前作为初中必会内容。",
            setOf(ConceptId.ELEMENT, ConceptId.PERIODIC_TABLE), setOf(OperationId.PARSE_CHEMICAL_FORMULA), setOf(WidgetType.CHEMICAL_FORMULA_INSPECTOR), listOf("O", "Fe", "Cl"),
        )
        ChemistryCourseCategory.FORMULA -> base(
            "把元素、下标、括号、电荷和相对式量作为一个结构解析",
            "Ca(OH)2  ·  Al2(SO4)3",
            CourseVisualizationKind.MOLECULE,
            "化学式结构与原子计数",
            "输入任意受支持化学式，观察括号展开、各元素原子数、电荷和相对式量。",
            listOf(p("coefficient", "式前系数", "1", 1.0, 10.0, 1.0)),
            listOf("识别元素符号", "读取下标和括号倍率", "处理结晶水或电荷", "统计原子组成", "按需要计算相对式量或质量分数"),
            listOf("化学式中的下标属于粒子内部组成，式前系数表示粒子个数。", "离子电荷和元素化合价是不同概念。"),
            "不能把括号外下标只乘最后一个元素，也不能用改下标的方法配平方程式。",
            "可研究更复杂配合物和同位素标记，但当前解析器限制在教材常见写法。",
            setOf(ConceptId.CHEMICAL_FORMULA, ConceptId.FORMULA_GROUP, ConceptId.HYDRATE, ConceptId.ION_CHARGE), setOf(OperationId.PARSE_CHEMICAL_FORMULA, OperationId.COUNT_ATOMS, OperationId.CALCULATE_MOLAR_MASS), setOf(WidgetType.CHEMICAL_FORMULA_INSPECTOR), listOf("Ca(OH)2", "CuSO4·5H2O", "SO4^2-"),
        )
        ChemistryCourseCategory.EQUATION -> base(
            "用化学式表达已知反应，并用原子和电荷守恒配平",
            "反应物 → 生成物",
            CourseVisualizationKind.CHEMICAL_EQUATION,
            "化学方程式与粒子重组",
            "输入完整反应物和生成物，验证或自动生成最简整数系数。",
            listOf(p("scale", "显示倍数", "1", 1.0, 5.0, 1.0)),
            listOf("确认反应物、生成物和条件", "正确书写每种物质化学式", "统计两侧元素和电荷", "只调整式前系数", "化为最简整数比并复核"),
            listOf("配平依据是质量守恒和电荷守恒。", "生成物由反应事实和条件决定，不能仅靠守恒唯一预测。"),
            "系统不会只根据反应物猜产物；必须给出教材规定的生成物和条件。",
            "可进一步约去旁观离子或分析氧化数，但只在对应章节启用。",
            setOf(ConceptId.CHEMICAL_EQUATION, ConceptId.MASS_CONSERVATION, ConceptId.CHARGE_CONSERVATION), setOf(OperationId.PARSE_CHEMICAL_EQUATION, OperationId.VERIFY_CHEMICAL_CONSERVATION, OperationId.BALANCE_EQUATION), setOf(WidgetType.CHEMICAL_EQUATION), listOf("Fe+O2->Fe2O3", "H2+O2->H2O"),
        )
        ChemistryCourseCategory.SOLUTION -> base("从溶质、溶剂、浓度和过程描述溶液", "c=n/V  ·  c1V1=c2V2", CourseVisualizationKind.PARTICLE_MODEL, "溶液组成与稀释", "调节溶质的量、体积和加水量，观察粒子密度和浓度变化。", listOf(p("amount", "溶质的量", "1", 0.1, 10.0, 0.1, "mol"), p("volume", "溶液体积", "1", 0.1, 10.0, 0.1, "L")), listOf("明确溶质和溶剂", "区分溶解过程与化学反应", "统一体积和物质的量单位", "使用浓度或稀释关系", "检查溶液体积和适用假设"), listOf("浓度是单位体积溶液所含溶质的量。", "稀释过程溶质总量不变，但体积和浓度改变。"), "不能把溶剂体积直接当作最终溶液体积，也不能忽略单位。", "可研究溶解度曲线和结晶，但要区分平衡溶解度与任意浓度。", setOf(ConceptId.MOLE, ConceptId.MOLARITY), setOf(OperationId.CALCULATE_STOICHIOMETRY), setOf(WidgetType.CHEMICAL_STOICHIOMETRY), listOf("c=n/V", "c1*V1=c2*V2"))
        ChemistryCourseCategory.STOICHIOMETRY -> base("从配平系数建立物质的量、质量和产率关系", "n=m/M  ·  化学计量比", CourseVisualizationKind.DATA_TABLE, "反应计量与限量试剂", "调节反应物质量或物质的量，观察限量试剂、剩余量和理论产率。", listOf(p("mass", "反应物质量", "10", 0.1, 100.0, 0.1, "g"), p("yield", "实际产率", "80", 0.0, 100.0, 1.0, "%")), listOf("先写出并配平方程式", "把已知质量换算为物质的量", "按系数比比较反应进度", "确定限量试剂", "计算理论值并与实际值比较"), listOf("方程式系数比对应物质的量比，不直接等于质量比。", "理论产率来自理想完全反应，实际产率受损失和副反应影响。"), "不能跳过配平直接按化学式下标比较，也不能让产率超过合理边界而不解释。", "可研究多步反应和纯度，但每一步必须有明确方程式和条件。", setOf(ConceptId.STOICHIOMETRY, ConceptId.LIMITING_REAGENT, ConceptId.THEORETICAL_YIELD), setOf(OperationId.CALCULATE_STOICHIOMETRY, OperationId.FIND_LIMITING_REAGENT, OperationId.CALCULATE_THEORETICAL_YIELD), setOf(WidgetType.CHEMICAL_STOICHIOMETRY), listOf("n=m/M"))
        ChemistryCourseCategory.ACID_BASE -> base("用粒子、电荷和反应事实理解酸碱盐", "H+ + OH- → H2O", CourseVisualizationKind.PARTICLE_MODEL, "酸碱粒子与中和", "调节酸碱量和浓度，观察剩余粒子、电荷与 pH 趋势。", listOf(p("acid", "酸的量", "1", 0.0, 5.0, 0.1), p("base", "碱的量", "1", 0.0, 5.0, 0.1)), listOf("确认酸碱定义层次", "写出主要粒子", "保持电荷和原子守恒", "判断是否恰好中和", "用实验现象或浓度解释"), listOf("酸碱性质来自特定粒子和反应。", "pH 是对溶液酸碱程度的数量描述，不等于酸的总量。"), "不能仅凭是否含 H 或 OH 判断所有物质酸碱性，也不能在未说明浓度时直接比较总量。", "可进一步研究缓冲和滴定曲线，但不提前作为基础中和章节必会。", setOf(ConceptId.ION, ConceptId.ION_CHARGE, ConceptId.IONIC_EQUATION), setOf(OperationId.REDUCE_IONIC_EQUATION, OperationId.VERIFY_CHEMICAL_CONSERVATION), setOf(WidgetType.CHEMICAL_EQUATION), listOf("H^+ + OH^- -> H2O"))
        ChemistryCourseCategory.ION_REDOX -> base("同时检查元素、总电荷和电子转移", "原子守恒 + 电荷守恒 + 电子守恒", CourseVisualizationKind.CHEMICAL_EQUATION, "离子与氧化还原", "调节氧化数或电子数，观察反应前后电荷、元素和电子转移。", listOf(p("electrons", "电子数", "2", 1.0, 12.0, 1.0)), listOf("识别离子和氧化数", "判断氧化与还原物种", "写出半反应或离子式", "配平原子和电荷", "合并并约去电子或旁观离子"), listOf("氧化还原反应必须同时满足元素和电荷守恒。", "电子只在半反应中显式出现，合并后应约去。"), "不能只配平原子而忽略电荷，也不能把化合价变化等同于离子实际电荷。", "可扩展到酸性或碱性介质半反应，但条件必须来自教材。", setOf(ConceptId.REDOX_REACTION, ConceptId.OXIDATION_STATE, ConceptId.CHARGE_CONSERVATION), setOf(OperationId.BALANCE_EQUATION, OperationId.REDUCE_IONIC_EQUATION), setOf(WidgetType.CHEMICAL_EQUATION), listOf("Fe^2+ -> Fe^3+"))
        ChemistryCourseCategory.ORGANIC -> base("用原子—化学键图理解碳骨架、官能团和反应变化", "结构简式 → 分子图 → 官能团",
            CourseVisualizationKind.MOLECULE, "有机分子结构", "输入结构简式，观察原子、键级、隐式氢、分子式、碳链和官能团。", listOf(p("layout", "布局迭代", "60", 10.0, 120.0, 10.0)), listOf("解析碳骨架和支链", "标出单双三键或芳香键", "补足合理隐式氢", "识别官能团", "比较分子式、连接关系和教材限定反应"), listOf("有机物性质与碳骨架、官能团和空间结构有关。", "同分异构要求分子式相同但原子连接关系不同。"), "系统不会根据任意反应物预测所有有机产物；反应必须由教材给出类别、条件和产物。", "可扩展立体化学和机理，但当前只做可解释的分子图与教材限定图变换。", setOf(ConceptId.MOLECULE_GRAPH, ConceptId.BOND_ORDER, ConceptId.FUNCTIONAL_GROUP, ConceptId.STRUCTURAL_ISOMERISM), setOf(OperationId.PARSE_ORGANIC_STRUCTURE, OperationId.DETECT_FUNCTIONAL_GROUPS, OperationId.COMPARE_STRUCTURAL_ISOMERS), setOf(WidgetType.MOLECULE_VIEWER, WidgetType.FUNCTIONAL_GROUP_INSPECTOR, WidgetType.ISOMER_COMPARATOR), listOf("CCO", "COC", "CC(=O)O"))
        ChemistryCourseCategory.EXPERIMENT -> base("按目的、器材、操作、现象、安全和结论组织实验", "操作 → 现象 → 证据 → 结论", CourseVisualizationKind.PROCESS, "化学实验过程", "调节反应物用量或条件，观察现象、安全边界和记录表。", listOf(p("amount", "试剂相对用量", "1", 0.1, 5.0, 0.1)), listOf("明确实验目的", "检查器材与安全要求", "按顺序加入或加热", "区分现象与解释", "处理废物并用证据得出结论"), listOf("化学实验必须把安全操作和证据记录放在结论之前。", "现象是观察到的事实，化学解释是基于模型的推断。"), "不能闻未知气体或随意混合试剂，也不能把推测写成观察现象。", "可研究反应速率或定量滴定，但需增加对应器材、误差和安全模型。", setOf(ConceptId.CHEMICAL_EQUATION, ConceptId.PHYSICAL_MODEL), setOf(OperationId.VALIDATE_MODEL_CONDITIONS), setOf(WidgetType.CHEMICAL_EQUATION), listOf("实验步骤顺序验证"))
        ChemistryCourseCategory.GENERAL -> base("从物质、组成、性质、变化和证据进入本课", "组成 + 条件 + 守恒", CourseVisualizationKind.PROCESS, "化学概念关系", "调整教材涉及的量，观察宏观现象、微观结构和符号表达如何联系。", listOf(p("amount", "示例数量", "2", 1.0, 10.0, 1.0)), listOf("定位教材页码和主题", "识别物质或粒子", "说明条件和证据", "选择化学式或方程式表示", "检查原子、电荷和实验事实"), listOf("化学学习需要在宏观、微观和符号三个层次来回对应。", "守恒可以检查表达是否可能，但不能单独预测具体反应产物。"), "不要把符号计算脱离实际物质、反应条件和实验事实。", "可以用周期表、分子图或计量关系进一步解释，但不作为未讲章节的前提。", setOf(ConceptId.CHEMICAL_FORMULA, ConceptId.MASS_CONSERVATION), setOf(OperationId.PARSE_CHEMICAL_FORMULA, OperationId.VERIFY_CHEMICAL_CONSERVATION), setOf(WidgetType.CHEMICAL_FORMULA_INSPECTOR), listOf("H2O"))
    }

    private fun base(
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
    ) = ChemistryProfile(subtitle, formula, visualization, visualTitle, visualDescription, parameters, steps, background, misconception, extension, concepts, operations, widgets, examples)

    private fun p(id: String, label: String, default: String, min: Double, max: Double, step: Double, unit: String = "") = CourseParameterSpec(
        id = id,
        label = label,
        kind = if (step >= 1.0 && default.toDoubleOrNull()?.rem(1.0) == 0.0) CourseParameterKind.INTEGER else CourseParameterKind.NUMBER,
        defaultValue = default,
        unit = unit,
        minimum = min,
        maximum = max,
        step = step,
    )

    private data class ChemistryProfile(
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
