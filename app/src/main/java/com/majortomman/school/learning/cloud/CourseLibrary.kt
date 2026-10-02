package com.majortomman.school.learning.cloud

import android.content.Context
import com.majortomman.school.learning.assessment.contract.ASSESSMENTS_FILE_NAME
import com.majortomman.school.learning.assessment.contract.AssessmentDocument
import com.majortomman.school.learning.assessment.contract.AssessmentDocumentParser
import com.majortomman.school.learning.assessment.contract.AssessmentPackageContract
import com.majortomman.school.learning.assessment.contract.KNOWLEDGE_POINTS_FILE_NAME
import com.majortomman.school.learning.assessment.contract.KnowledgePointDocument
import com.majortomman.school.learning.assessment.contract.KnowledgePointDocumentParser
import com.majortomman.school.learning.content.ContentAssetId
import com.majortomman.school.learning.course.CourseDocument
import com.majortomman.school.learning.course.CourseLesson
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class InstalledCourse(
    val rootPath: String,
    val document: CourseDocument,
    val contentVersion: Long,
    val assessments: AssessmentDocument? = null,
    val assessmentKnowledgePoints: KnowledgePointDocument? = null,
) {
    val id: String get() = document.textbook.id
    val title: String get() = document.textbook.title
    val subject: String get() = document.textbook.subject
    val grade: String get() = document.textbook.grade
    val semester: String get() = document.textbook.semester
    val pdfFile: File get() = File(rootPath, document.textbook.pdf.path)
    val lessons: List<CourseLesson> get() = document.chapters.flatMap { chapter -> chapter.sections.flatMap { section -> section.lessons } }

    fun assessmentAssetFiles(): Map<ContentAssetId, File> = assessments?.assets.orEmpty().associate { asset ->
        asset.id to File(rootPath, asset.path)
    }

    fun printedPageToPdfIndex(printedPage: Int): Int = printedPage + document.textbook.pdf.pageIndexOffset - 1

    fun pdfIndexToPrintedPage(pdfIndex: Int): Int = pdfIndex - document.textbook.pdf.pageIndexOffset + 1

    fun readingRange(lesson: CourseLesson): IntRange? {
        val start = lesson.references.minOfOrNull { it.pageStart } ?: return null
        val end = lesson.references.maxOfOrNull { it.pageEnd } ?: return null
        return start..end
    }
}

data class CourseLibraryState(
    val courses: List<InstalledCourse> = emptyList(),
) {
    fun course(id: String?): InstalledCourse? = id?.let { key -> courses.firstOrNull { it.id == key } }
}

object CourseLibraryRepository {
    private const val ACTIVE_DIRECTORY = "course-packs/active"
    private const val COURSE_FILE_NAME = "course.json"

    @Volatile private var appContext: Context? = null
    private val mutableState = MutableStateFlow(CourseLibraryState())
    val state = mutableState.asStateFlow()

    fun initialize(context: Context) {
        appContext = context.applicationContext
        refresh()
    }

    @Synchronized
    fun refresh(context: Context? = null): Int {
        val resolvedContext = context?.applicationContext ?: appContext ?: return 0
        appContext = resolvedContext
        val activeRoot = File(resolvedContext.filesDir, ACTIVE_DIRECTORY)
        val courses = activeRoot.listFiles().orEmpty()
            .filter(File::isDirectory)
            .mapNotNull(::loadCourse)
            .sortedWith(compareBy({ it.subject }, { it.grade }, { it.semester }, { it.title }))
        mutableState.value = CourseLibraryState(courses)
        return courses.size
    }

    fun hasInstalledCourseContent(): Boolean = mutableState.value.courses.isNotEmpty()

    fun lessonTitle(lessonId: String): String? = mutableState.value.courses.asSequence()
        .flatMap { it.lessons.asSequence() }
        .firstOrNull { it.id == lessonId }
        ?.title

    private fun loadCourse(root: File): InstalledCourse? {
        val courseFile = File(root, COURSE_FILE_NAME)
        if (!courseFile.isFile) return null
        return runCatching {
            CourseLocalIntegrityValidator.readValidated(root)
            val document = CourseDocumentParser.decode(courseFile.readText(Charsets.UTF_8))
            require(document.textbook.id == root.name) { "课程目录与教材 ID 不一致" }
            CourseRuntimeCompatibilityValidator.validate(document)
            val pdf = File(root, document.textbook.pdf.path)
            require(pdf.isFile) { "课程缺少教材 PDF" }

            val assessmentsFile = File(root, ASSESSMENTS_FILE_NAME)
            val knowledgePointsFile = File(root, KNOWLEDGE_POINTS_FILE_NAME)
            require(assessmentsFile.isFile == knowledgePointsFile.isFile) {
                "$ASSESSMENTS_FILE_NAME 与 $KNOWLEDGE_POINTS_FILE_NAME 必须同时存在"
            }
            val assessments = assessmentsFile.takeIf(File::isFile)?.let {
                AssessmentDocumentParser.decode(it.readText(Charsets.UTF_8))
            }
            val assessmentKnowledgePoints = knowledgePointsFile.takeIf(File::isFile)?.let {
                KnowledgePointDocumentParser.decode(it.readText(Charsets.UTF_8))
            }
            if (assessments != null && assessmentKnowledgePoints != null) {
                AssessmentPackageContract.validate(document, assessments, assessmentKnowledgePoints)
                LearningContentRuntimeCompatibilityValidator.validate(assessments)
            }
            InstalledCourse(
                rootPath = root.absolutePath,
                document = document,
                contentVersion = courseFile.lastModified(),
                assessments = assessments,
                assessmentKnowledgePoints = assessmentKnowledgePoints,
            )
        }.getOrNull()
    }
}
