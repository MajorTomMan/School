package com.majortomman.school.data.material

import com.majortomman.school.data.Lesson
import com.majortomman.school.data.MasteryStatus
import org.json.JSONArray
import org.json.JSONObject

const val MATERIAL_PACK_SCHEMA_VERSION = 1
const val IMPORT_TUTORIAL_VERSION = 2

enum class EducationStage(
    val id: String,
    val label: String,
    val grades: IntRange,
) {
    PRIMARY("primary", "小学", 1..6),
    JUNIOR_HIGH("junior-high", "初中", 7..9),
    SENIOR_HIGH("senior-high", "高中", 10..12),
    UNIVERSITY("university", "大学", 13..16);

    companion object {
        fun fromId(id: String?): EducationStage? = entries.firstOrNull { it.id == id }

        fun fromGrade(grade: Int): EducationStage = entries.firstOrNull { grade in it.grades }
            ?: when {
                grade <= 6 -> PRIMARY
                grade <= 9 -> JUNIOR_HIGH
                grade <= 12 -> SENIOR_HIGH
                else -> UNIVERSITY
            }
    }
}

enum class TextbookVolume(
    val id: Int,
    val label: String,
) {
    FIRST(1, "上册"),
    SECOND(2, "下册"),
    COMPULSORY_1(101, "必修第一册"),
    COMPULSORY_2(102, "必修第二册"),
    COMPULSORY_3(103, "必修第三册"),
    COMPULSORY_UPPER(111, "必修上册"),
    COMPULSORY_LOWER(112, "必修下册"),
    SELECTIVE_1(201, "选择性必修第一册"),
    SELECTIVE_2(202, "选择性必修第二册"),
    SELECTIVE_3(203, "选择性必修第三册"),
    SELECTIVE_4(204, "选择性必修第四册"),
    SELECTIVE_UPPER(211, "选择性必修上册"),
    SELECTIVE_MIDDLE(212, "选择性必修中册"),
    SELECTIVE_LOWER(213, "选择性必修下册");

    fun labelFor(stage: EducationStage): String = when {
        this !in LEGACY_VOLUMES -> label
        stage == EducationStage.PRIMARY || stage == EducationStage.JUNIOR_HIGH -> label
        this == FIRST -> "上学期"
        else -> "下学期"
    }

    companion object {
        private val LEGACY_VOLUMES = setOf(FIRST, SECOND)

        fun fromId(id: Int): TextbookVolume = entries.firstOrNull { it.id == id } ?: FIRST

        fun optionsFor(stage: EducationStage, subjectId: String): List<TextbookVolume> = when {
            stage != EducationStage.SENIOR_HIGH -> listOf(FIRST, SECOND)
            subjectId == "chinese" -> listOf(
                COMPULSORY_UPPER,
                COMPULSORY_LOWER,
                SELECTIVE_UPPER,
                SELECTIVE_MIDDLE,
                SELECTIVE_LOWER,
            )
            subjectId == "english" -> listOf(
                COMPULSORY_1,
                COMPULSORY_2,
                COMPULSORY_3,
                SELECTIVE_1,
                SELECTIVE_2,
                SELECTIVE_3,
                SELECTIVE_4,
            )
            subjectId == "japanese" -> listOf(
                COMPULSORY_1,
                COMPULSORY_2,
                COMPULSORY_3,
                SELECTIVE_1,
                SELECTIVE_2,
            )
            subjectId == "physics" -> listOf(
                COMPULSORY_1,
                COMPULSORY_2,
                COMPULSORY_3,
                SELECTIVE_1,
                SELECTIVE_2,
                SELECTIVE_3,
            )
            subjectId == "chemistry" -> listOf(
                COMPULSORY_1,
                COMPULSORY_2,
                SELECTIVE_1,
                SELECTIVE_2,
                SELECTIVE_3,
            )
            else -> listOf(FIRST, SECOND)
        }
    }
}

data class SubjectTemplate(
    val id: String,
    val title: String,
    val stages: Set<EducationStage>,
) {
    fun gradesFor(stage: EducationStage): IntRange = stage.grades
}

object SubjectTemplates {
    val all = listOf(
        SubjectTemplate("chinese", "语文", setOf(EducationStage.PRIMARY, EducationStage.JUNIOR_HIGH, EducationStage.SENIOR_HIGH)),
        SubjectTemplate("math", "数学", EducationStage.entries.toSet()),
        SubjectTemplate("english", "英语", setOf(EducationStage.PRIMARY, EducationStage.JUNIOR_HIGH, EducationStage.SENIOR_HIGH, EducationStage.UNIVERSITY)),
        SubjectTemplate("japanese", "日语", setOf(EducationStage.SENIOR_HIGH, EducationStage.UNIVERSITY)),
        SubjectTemplate("science", "科学", setOf(EducationStage.PRIMARY)),
        SubjectTemplate("physics", "物理", setOf(EducationStage.JUNIOR_HIGH, EducationStage.SENIOR_HIGH, EducationStage.UNIVERSITY)),
        SubjectTemplate("chemistry", "化学", setOf(EducationStage.JUNIOR_HIGH, EducationStage.SENIOR_HIGH, EducationStage.UNIVERSITY)),
        SubjectTemplate("biology", "生物", setOf(EducationStage.JUNIOR_HIGH, EducationStage.SENIOR_HIGH, EducationStage.UNIVERSITY)),
        SubjectTemplate("history", "历史", setOf(EducationStage.JUNIOR_HIGH, EducationStage.SENIOR_HIGH, EducationStage.UNIVERSITY)),
        SubjectTemplate("geography", "地理", setOf(EducationStage.JUNIOR_HIGH, EducationStage.SENIOR_HIGH, EducationStage.UNIVERSITY)),
        SubjectTemplate("politics", "思想政治", setOf(EducationStage.JUNIOR_HIGH, EducationStage.SENIOR_HIGH, EducationStage.UNIVERSITY)),
        SubjectTemplate("computer", "计算机", setOf(EducationStage.SENIOR_HIGH, EducationStage.UNIVERSITY)),
        SubjectTemplate("economics", "经济学", setOf(EducationStage.UNIVERSITY)),
        SubjectTemplate("law", "法学", setOf(EducationStage.UNIVERSITY)),
    )

    fun forStage(stage: EducationStage): List<SubjectTemplate> = all.filter { stage in it.stages }

    fun find(id: String): SubjectTemplate? = all.firstOrNull { it.id == id }

    fun findByTitle(title: String): SubjectTemplate? = all.firstOrNull { it.title == title.trim() }
}

data class TextbookSlot(
    val subjectId: String,
    val subjectTitle: String,
    val grade: Int,
    val volume: TextbookVolume,
    val stage: EducationStage = EducationStage.fromGrade(grade),
) {
    val key: String
        get() = "$subjectId-$grade-${volume.id}"

    val levelLabel: String
        get() = gradeLabel(grade)

    val volumeLabel: String
        get() = volume.labelFor(stage)

    val displayTitle: String
        get() = "$levelLabel$subjectTitle$volumeLabel"

    fun toJson(): JSONObject = JSONObject()
        .put("subjectId", subjectId)
        .put("subjectTitle", subjectTitle)
        .put("grade", grade)
        .put("volume", volume.id)
        .put("stage", stage.id)

    companion object {
        fun fromJson(root: JSONObject): TextbookSlot {
            val grade = root.getInt("grade")
            return TextbookSlot(
                subjectId = root.getString("subjectId"),
                subjectTitle = root.getString("subjectTitle"),
                grade = grade,
                volume = TextbookVolume.fromId(root.getInt("volume")),
                stage = EducationStage.fromId(root.optString("stage")) ?: EducationStage.fromGrade(grade),
            )
        }

        fun fromKey(key: String): TextbookSlot? {
            val parts = key.split('-')
            if (parts.size < 3) return null
            val subjectId = parts.dropLast(2).joinToString("-")
            val grade = parts[parts.lastIndex - 1].toIntOrNull() ?: return null
            val volume = parts.last().toIntOrNull()?.let(TextbookVolume::fromId) ?: return null
            val subject = SubjectTemplates.find(subjectId) ?: return null
            return TextbookSlot(
                subjectId = subject.id,
                subjectTitle = subject.title,
                grade = grade,
                volume = volume,
                stage = EducationStage.fromGrade(grade),
            )
        }
    }
}

fun gradeLabel(grade: Int): String = when (grade) {
    1 -> "一年级"
    2 -> "二年级"
    3 -> "三年级"
    4 -> "四年级"
    5 -> "五年级"
    6 -> "六年级"
    7 -> "七年级"
    8 -> "八年级"
    9 -> "九年级"
    10 -> "高一"
    11 -> "高二"
    12 -> "高三"
    13 -> "大一"
    14 -> "大二"
    15 -> "大三"
    16 -> "大四"
    else -> "第${grade}学年"
}

data class MaterialPdfAsset(
    val path: String,
    val sha256: String,
    val pageIndexOffset: Int,
)

data class MaterialPackManifest(
    val schemaVersion: Int,
    val packId: String,
    val version: String,
    val title: String,
    val subject: String,
    val catalogPath: String,
    val pdf: MaterialPdfAsset,
)

data class CatalogBook(
    val id: String,
    val title: String,
    val subject: String,
    val grade: Int,
    val volume: TextbookVolume,
    val publisher: String,
    val edition: String,
)

data class CatalogPathNode(
    val id: String,
    val title: String,
    val type: String,
    val orderIndex: Int,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("type", type)
        .put("orderIndex", orderIndex)

    companion object {
        fun fromJson(root: JSONObject): CatalogPathNode = CatalogPathNode(
            id = root.optString("id").trim(),
            title = root.optString("title").trim(),
            type = root.optString("type", "UNIT").trim().uppercase(),
            orderIndex = root.optInt("orderIndex", 0),
        )
    }
}

data class CatalogLesson(
    val id: String,
    val title: String,
    val pageStart: Int,
    val pageEnd: Int,
    val role: String = "CORE",
    val path: List<CatalogPathNode> = emptyList(),
    val orderIndex: Int = 0,
)

data class TextbookCatalog(
    val book: CatalogBook,
    val lessons: List<CatalogLesson>,
)

data class GeneratedLesson(
    val id: String,
    val sourceId: String,
    val title: String,
    val subtitle: String,
    val estimatedMinutes: Int,
    val pageStart: Int,
    val pageEnd: Int,
    val objectives: List<String>,
    val explanation: String,
    val commonMistake: String,
    val role: String = "CORE",
    val path: List<CatalogPathNode> = emptyList(),
    val orderIndex: Int = 0,
) {
    fun toLesson(status: MasteryStatus): Lesson = Lesson(
        id = id,
        title = title,
        subtitle = subtitle,
        estimatedMinutes = estimatedMinutes,
        textbookPages = pageStart..pageEnd,
        status = status,
        objectives = objectives,
        explanation = explanation,
        commonMistake = commonMistake,
    )

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("sourceId", sourceId)
        .put("title", title)
        .put("subtitle", subtitle)
        .put("estimatedMinutes", estimatedMinutes)
        .put("pageStart", pageStart)
        .put("pageEnd", pageEnd)
        .put("objectives", JSONArray(objectives))
        .put("explanation", explanation)
        .put("commonMistake", commonMistake)
        .put("role", role)
        .put("orderIndex", orderIndex)
        .put("path", JSONArray().apply { path.forEach { put(it.toJson()) } })

    companion object {
        fun fromJson(root: JSONObject): GeneratedLesson = GeneratedLesson(
            id = root.getString("id"),
            sourceId = root.optString("sourceId", root.getString("id")),
            title = root.getString("title"),
            subtitle = root.getString("subtitle"),
            estimatedMinutes = root.optInt("estimatedMinutes", 18),
            pageStart = root.getInt("pageStart"),
            pageEnd = root.getInt("pageEnd"),
            objectives = root.getJSONArray("objectives").toStringList(),
            explanation = root.getString("explanation"),
            commonMistake = root.getString("commonMistake"),
            role = root.optString("role", "CORE"),
            path = root.optJSONArray("path").toPathNodes(),
            orderIndex = root.optInt("orderIndex", 0),
        )
    }
}
