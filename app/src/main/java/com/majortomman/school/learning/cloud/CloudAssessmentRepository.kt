package com.majortomman.school.learning.cloud

import android.content.Context
import com.majortomman.school.learning.assessment.contract.ASSESSMENTS_FILE_NAME
import com.majortomman.school.learning.assessment.contract.AssessmentDocumentParser
import com.majortomman.school.learning.assessment.contract.AssessmentPackageContract
import com.majortomman.school.learning.assessment.contract.CourseAssessmentQuestionSet
import com.majortomman.school.learning.assessment.contract.KNOWLEDGE_POINTS_FILE_NAME
import com.majortomman.school.learning.assessment.contract.KnowledgePointDefinition
import com.majortomman.school.learning.assessment.domain.KnowledgePointId
import com.majortomman.school.learning.assessment.domain.QuestionSetId
import com.majortomman.school.learning.content.ContentAssetId
import com.majortomman.school.learning.course.CourseChapter
import com.majortomman.school.learning.course.CourseDocument
import com.majortomman.school.learning.course.CoursePage
import com.majortomman.school.learning.course.CourseSection
import java.io.File
import java.security.MessageDigest

data class InstalledLessonAssessments(
    val courseId: String,
    val contentRevision: String,
    val sectionIds: List<String>,
    val questionSets: List<CourseAssessmentQuestionSet>,
    val questionSetsBySectionName: Map<String, List<CourseAssessmentQuestionSet>>,
    val assetFiles: Map<ContentAssetId, File>,
    val knowledgePoints: Map<KnowledgePointId, KnowledgePointDefinition>,
) {
    fun questionSetsFor(sectionLabel: String): List<CourseAssessmentQuestionSet> =
        questionSetsBySectionName[normalizeSectionName(sectionLabel)].orEmpty()
}

object CloudAssessmentRepository {
    private const val ACTIVE_DIRECTORY = "course-packs/active"
    private const val COURSE_FILE_NAME = "course.json"

    fun forLesson(
        context: Context,
        title: String,
        sourcePages: IntRange,
    ): InstalledLessonAssessments? {
        val activeRoot = File(context.applicationContext.filesDir, ACTIVE_DIRECTORY)
        return activeRoot.listFiles()
            .orEmpty()
            .asSequence()
            .filter(File::isDirectory)
            .mapNotNull { root -> runCatching { loadCandidate(root, title, sourcePages) }.getOrNull() }
            .firstOrNull { it.questionSets.isNotEmpty() }
    }

    private fun loadCandidate(
        root: File,
        title: String,
        sourcePages: IntRange,
    ): InstalledLessonAssessments? {
        val courseFile = File(root, COURSE_FILE_NAME)
        val assessmentFile = File(root, ASSESSMENTS_FILE_NAME)
        val knowledgeFile = File(root, KNOWLEDGE_POINTS_FILE_NAME)
        if (!courseFile.isFile || !assessmentFile.isFile || !knowledgeFile.isFile) return null

        val course = CourseDocumentParser.decode(courseFile.readText(Charsets.UTF_8))
        val assessments = AssessmentDocumentParser.decode(assessmentFile.readText(Charsets.UTF_8))
        val knowledge = KnowledgePointDocumentParser.decode(knowledgeFile.readText(Charsets.UTF_8))
        AssessmentPackageContract.validate(course, assessments, knowledge)

        val sections = course.resolveSections(title, sourcePages)
        if (sections.isEmpty()) return null

        val setById = assessments.questionSets.associateBy(CourseAssessmentQuestionSet::id)
        val placementBySection = assessments.placements.associateBy { it.sectionId }
        val sectionQuestionSets = linkedMapOf<String, List<CourseAssessmentQuestionSet>>()
        sections.forEach { section ->
            val placed = placementBySection[section.id]
                ?.questionSetIds
                .orEmpty()
                .map { id: QuestionSetId ->
                    setById[id] ?: error("已验证题组在运行时丢失：$id")
                }
            if (placed.isNotEmpty()) {
                section.names()
                    .map(::normalizeSectionName)
                    .filter(String::isNotBlank)
                    .forEach { normalized -> sectionQuestionSets[normalized] = placed }
            }
        }
        if (sectionQuestionSets.isEmpty()) return null

        val questionSets = sectionQuestionSets.values
            .flatten()
            .distinctBy(CourseAssessmentQuestionSet::id)
        val assetFiles = assessments.assets.associate { asset ->
            val file = safeResolve(root, asset.path)
            require(file.isFile) { "题目资产缺失：${asset.path}" }
            asset.id to file
        }

        return InstalledLessonAssessments(
            courseId = course.textbook.id,
            contentRevision = contentRevision(courseFile, assessmentFile, knowledgeFile),
            sectionIds = sections.map(CourseSection::id),
            questionSets = questionSets,
            questionSetsBySectionName = sectionQuestionSets,
            assetFiles = assetFiles,
            knowledgePoints = knowledge.knowledgePoints.associateBy(KnowledgePointDefinition::id),
        )
    }

    private fun CourseDocument.resolveSections(
        title: String,
        sourcePages: IntRange,
    ): List<CourseSection> {
        val requested = normalizeSectionName(title)
        if (requested.isNotBlank()) {
            chapters.forEach { chapter ->
                if (chapter.names().any { normalizeSectionName(it) == requested }) {
                    return chapter.sections + listOfNotNull(chapter.review)
                }
                chapter.allSections().forEach { section ->
                    if (section.names().any { normalizeSectionName(it) == requested }) return listOf(section)
                    if (section.pages.any { page -> page.names().any { normalizeSectionName(it) == requested } }) {
                        return listOf(section)
                    }
                }
            }
        }

        return chapters
            .flatMap { chapter -> chapter.allSections() }
            .filter { section -> section.pages.any { it.overlaps(sourcePages) } }
            .distinctBy(CourseSection::id)
    }

    private fun CourseChapter.allSections(): List<CourseSection> = sections + listOfNotNull(review)

    private fun CourseChapter.names(): List<String> = buildList {
        add(title)
        if (number.isNotBlank()) add(number + title)
        addAll(aliases)
    }

    private fun CourseSection.names(): List<String> = buildList {
        add(title)
        if (number.isNotBlank()) add(number + title)
        addAll(aliases)
    }

    private fun CoursePage.names(): List<String> = listOf(title) + aliases

    private fun CoursePage.overlaps(range: IntRange): Boolean =
        sourcePage <= range.last && sourcePageEnd >= range.first

    private fun contentRevision(vararg files: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        files.sortedBy { file -> file.name }.forEach { file ->
            digest.update(file.name.toByteArray(Charsets.UTF_8))
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (count > 0) digest.update(buffer, 0, count)
                }
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun safeResolve(root: File, relativePath: String): File {
        val file = File(root, relativePath)
        val canonicalRoot = root.canonicalFile
        val canonicalFile = file.canonicalFile
        require(canonicalFile.path.startsWith(canonicalRoot.path + File.separator)) {
            "题目资产越过课程目录：$relativePath"
        }
        return canonicalFile
    }
}

private fun normalizeSectionName(value: String): String = value
    .replace(" ", "")
    .replace("　", "")
    .replace("（", "(")
    .replace("）", ")")
    .trim()
