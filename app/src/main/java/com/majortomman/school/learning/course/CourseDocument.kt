package com.majortomman.school.learning.course

/**
 * Authored learning course contract.
 *
 * The textbook is a reference source, not the course body. Course packages contain original
 * teaching lessons, structured practice and optional interactive scenes. The APK validates the
 * complete document before activation.
 */
data class CourseDocument(
    val textbook: CourseTextbook,
    val knowledgePoints: List<CourseKnowledgePoint>,
    val chapters: List<CourseChapter>,
)

data class CourseTextbook(
    val id: String,
    val title: String,
    val publisher: String,
    val edition: String,
    val grade: String,
    val semester: String,
    val subject: String,
    val pdf: CoursePdf,
)

data class CoursePdf(
    val path: String,
    val pageCount: Int,
    val pageIndexOffset: Int,
)

data class CourseKnowledgePoint(
    val id: String,
    val name: String,
    val description: String,
    val prerequisiteIds: List<String>,
)

data class CourseChapter(
    val id: String,
    val title: String,
    val sections: List<CourseSection>,
)

data class CourseSection(
    val id: String,
    val title: String,
    val lessons: List<CourseLesson>,
)

data class CourseLesson(
    val id: String,
    val title: String,
    val aliases: List<String>,
    val goals: List<String>,
    val knowledgePointIds: List<String>,
    val prerequisiteLessonIds: List<String>,
    val references: List<CourseSourceReference>,
    val steps: List<CourseStep>,
    val practice: List<CoursePractice>,
    val summary: List<String>,
)

data class CourseSourceReference(
    val label: String,
    val pageStart: Int,
    val pageEnd: Int,
)

sealed interface CourseStep

data class CourseExplanation(val title: String?, val text: String) : CourseStep

data class CourseQuestion(val prompt: String, val hint: String?) : CourseStep

data class CourseKeyIdea(val title: String?, val text: String) : CourseStep

data class CourseFormula(val expression: String, val note: String?) : CourseStep

data class CourseExample(
    val title: String,
    val prompt: String,
    val steps: List<String>,
    val answer: String,
) : CourseStep

data class CourseSceneStep(val scene: CourseScene) : CourseStep

data class CourseCheckpoint(
    val prompt: String,
    val expectedAnswer: String,
    val explanation: String,
) : CourseStep

data class CourseSourceLink(val referenceIndex: Int) : CourseStep

data class CourseSummaryStep(val text: String) : CourseStep

data class CoursePractice(
    val id: String,
    val prompt: String,
    val answer: String,
    val analysis: List<String>,
    val knowledgePointIds: List<String>,
    val difficulty: Int,
)

/** Canonical interactive scenes built into the APK. */
enum class CourseSceneTemplate(val id: String) {
    OPPOSITE_QUANTITIES("opposite_quantities"),
    RATIONAL_CLASSIFICATION("rational_classification"),
    INTEGER_TO_FRACTION("integer_to_fraction"),
    NUMBER_LINE("number_line"),
    OPPOSITE_NUMBERS("opposite_numbers"),
    ABSOLUTE_VALUE("absolute_value"),
    NUMBER_COMPARISON("number_comparison"),
    ADDITION_PROCESS("addition_process"),
    SUBTRACTION_TRANSFORM("subtraction_transform"),
    MULTIPLICATION_SIGN("multiplication_sign"),
    DIVISION_TRANSFORM("division_transform"),
    POWER_PROCESS("power_process"),
    ALGEBRA_PROCESS("algebra_process"),
    EQUATION_BALANCE("equation_balance"),
    ROOT_NUMBER_LINE("root_number_line"),
    CARTESIAN_PLANE("cartesian_plane"),
    FUNCTION_GRAPH("function_graph"),
    GEOMETRY("geometry"),
    TRANSFORMATION("transformation"),
    RIGHT_TRIANGLE("right_triangle"),
    DATA_CHART("data_chart"),
    PROBABILITY("probability"),
    PROJECTION("projection"),
    DECLARATIVE_DIAGRAM("diagram");

    companion object {
        fun fromId(id: String): CourseSceneTemplate? = entries.firstOrNull { it.id == id }
    }
}

data class CourseScene(
    val template: CourseSceneTemplate,
    val data: CourseSceneData,
)

class CourseSceneData internal constructor(private val values: Map<String, Any?>) {
    fun has(key: String): Boolean = values.containsKey(key)
    fun string(key: String, default: String = ""): String = values[key] as? String ?: default
    fun boolean(key: String, default: Boolean = false): Boolean = values[key] as? Boolean ?: default
    fun number(key: String, default: Double = 0.0): Double = (values[key] as? Number)?.toDouble() ?: default
    fun integer(key: String, default: Int = 0): Int = (values[key] as? Number)?.toInt() ?: default
    @Suppress("UNCHECKED_CAST") fun objects(key: String): List<Map<String, Any?>> = values[key] as? List<Map<String, Any?>> ?: emptyList()
    @Suppress("UNCHECKED_CAST") fun strings(key: String): List<String> = values[key] as? List<String> ?: emptyList()
    fun raw(key: String): Any? = values[key]
    fun keys(): Set<String> = values.keys
}
