package com.majortomman.school.data.material

internal object PrebuiltSubjectAnalysisFactory {
    fun create(slot: TextbookSlot, lesson: GeneratedLesson): LessonAnalysis {
        val page = lesson.pageStart
        val path = lesson.path.joinToString(" · ")
        val context = path.takeIf(String::isNotBlank)?.let { "$it · " }.orEmpty()
        val profile = when (slot.subjectId) {
            "chinese" -> Profile(
                summary = "围绕《${lesson.title}》梳理内容、结构、语言表达与文化背景。",
                objectives = listOf("概括主要内容或观点", "分析关键语句与表达方式", "结合语境形成自己的解释"),
                misconception = "不要用作者、体裁或名句代替文本分析；结论必须能回到具体语句和结构。",
                steps = listOf("通读并确定体裁", "划分内容层次", "定位关键语句", "分析表达作用", "形成有证据的概括"),
                question = "请用两三句话概括《${lesson.title}》的核心内容，并指出一个最值得分析的表达细节。",
            )
            "english", "japanese" -> Profile(
                summary = "在${lesson.title}的语境中综合练习词汇、语法、理解与表达。",
                objectives = listOf("理解语篇或任务场景", "掌握本节核心词汇和表达", "能在相近情境中完成表达"),
                misconception = "不要孤立背词或套语法形式；先判断交际情境、说话关系和句子功能。",
                steps = listOf("确认情境和任务", "提取关键词句", "归纳语法或表达功能", "替换信息造句", "完成听说读写迁移"),
                question = "请概括${lesson.title}的交际任务，并写出一个可在相近场景使用的核心表达。",
            )
            "physics" -> Profile(
                summary = "从现象、模型、物理量和规律四个层次理解${lesson.title}。",
                objectives = listOf("识别研究对象和物理过程", "说明相关物理量及条件", "使用规律解释或解决典型问题"),
                misconception = "不要只背公式；先确认研究对象、方向、单位、适用条件和理想化假设。",
                steps = listOf("描述现象", "选定研究对象", "建立物理模型", "列出量和关系", "检查单位与结论"),
                question = "学习${lesson.title}时应先确定哪些对象、条件和物理量？",
            )
            "chemistry" -> Profile(
                summary = "从物质类别、微观结构、反应条件和实验现象理解${lesson.title}。",
                objectives = listOf("识别物质及其类别或结构", "解释性质与转化条件", "用实验或方程式验证结论"),
                misconception = "不要把现象、结论和反应条件混在一起；书写方程式时还要检查守恒与状态。",
                steps = listOf("识别研究物质", "观察或预测现象", "从微观解释原因", "表示转化关系", "检查条件与守恒"),
                question = "请说明${lesson.title}中最核心的物质转化、条件或结构—性质关系。",
            )
            else -> Profile(
                summary = "依据教材目录和页码范围学习${lesson.title}。",
                objectives = listOf("理解核心概念", "说明关键依据", "完成一次迁移练习"),
                misconception = "不要脱离教材条件直接记结论。",
                steps = listOf("定位问题", "识别对象", "梳理关系", "形成结论", "用例子检验"),
                question = "请用自己的话说明${lesson.title}的核心结论。",
            )
        }
        return LessonAnalysis(
            lessonSourceId = lesson.sourceId,
            summary = profile.summary,
            objectives = profile.objectives,
            misconception = profile.misconception,
            sourcePages = lesson.pageStart..lesson.pageEnd,
            scene = LessonSceneSpec(
                type = LessonSceneType.PROCESS,
                title = lesson.title,
                prompt = "${context}这一节应按什么顺序理解？",
                conclusion = profile.summary,
                steps = profile.steps,
                sourcePage = page,
            ),
            exercise = GeneratedExercise(
                question = profile.question,
                acceptedAnswers = emptyList(),
                hints = listOf("先回忆课程目标和关键对象。", "需要时查看教材第 ${lesson.pageStart}—${lesson.pageEnd} 页。"),
                explanation = profile.summary,
            ),
            source = LessonAnalysisSource.PACK,
        )
    }

    private data class Profile(
        val summary: String,
        val objectives: List<String>,
        val misconception: String,
        val steps: List<String>,
        val question: String,
    )
}
