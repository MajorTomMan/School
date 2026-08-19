package com.majortomman.school.learning.cloud

import android.content.Context
import com.majortomman.school.learning.course.CourseDocument
import com.majortomman.school.learning.course.CourseLesson
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class InstalledCourse(
    val rootPath: String,
    val document: CourseDocument,
    val contentVersion: Long,
) {
    val id: String get() = document.textbook.id
    val title: String get() = document.textbook.title
    val subject: String get() = document.textbook.subject
    val grade: String get() = document.textbook.grade
    val semester: String get() = document.textbook.semester
    val pdfFile: File get() = File(rootPath, document.textbook.pdf.path)
    val lessons: List<CourseLesson> get() = document.lessons()

    fun printedPageToPdfIndex(printedPage: Int): Int = printedPage - document.textbook.pdf.pageIndexOffset

    fun pdfIndexToPrintedPage(pdfIndex: Int): Int = pdfIndex + document.textbook.pdf.pageIndexOffset

    fun readingRange(lesson: CourseLesson): IntRange? {
        val start = lesson.references.minOfOrNull { it.pageStart } ?: return null
        val end = lesson.references.maxOfOrNull { it.pageEnd } ?: return null
        return start..end
    }

    fun isMath(): Boolean {
        val normalized = subject.trim().lowercase()
        return normalized == "数学" || normalized == "math" || normalized == "mathematics"
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

    fun installedCourseIds(): Set<String> = mutableState.value.courses.map(InstalledCourse::id).toSet()

    fun lessonTitle(lessonId: String): String? = mutableState.value.courses.asSequence()
        .flatMap { it.lessons.asSequence() }
        .firstOrNull { it.id == lessonId }
        ?.title

    private fun loadCourse(root: File): InstalledCourse? {
        val courseFile = File(root, COURSE_FILE_NAME)
        if (!courseFile.isFile) return null
        return runCatching {
            val document = CourseDocumentParser.decode(courseFile.readText(Charsets.UTF_8))
            require(document.textbook.id == root.name) { "课程目录与教材 ID 不一致" }
            val pdf = File(root, document.textbook.pdf.path)
            require(pdf.isFile) { "课程缺少教材 PDF" }
            InstalledCourse(root.absolutePath, document, courseFile.lastModified())
        }.getOrNull()
    }
}
