package com.majortomman.school.learning.cloud

import android.content.Context
import com.majortomman.school.data.material.MaterialLibraryStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * User-facing course storage operations.
 *
 * Course files and textbook PDFs live below files/course-packs. Assessment attempts, mastery
 * snapshots, preferences and other learning records live elsewhere and are deliberately excluded
 * from cache deletion.
 */
object CourseStorageManager {
    private const val ROOT_DIRECTORY = "course-packs"
    private const val PREFERENCES_NAME = "school_course_storage"
    private const val KEY_LAST_CHECKED_AT = "last_checked_at"

    private val clearLock = Any()

    suspend fun inspect(context: Context): CourseStorageSnapshot = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        CourseCacheFiles.snapshot(File(appContext.filesDir, ROOT_DIRECTORY)).copy(
            lastCheckedAt = preferences(appContext).getLong(KEY_LAST_CHECKED_AT, 0L),
        )
    }

    suspend fun checkForUpdates(
        context: Context,
        textbookIds: Set<String> = emptySet(),
    ): CourseStorageUpdateCheck {
        val appContext = context.applicationContext
        val result = CourseSyncManager.checkForUpdates(appContext, textbookIds)
        val checkedAt = System.currentTimeMillis()
        preferences(appContext).edit().putLong(KEY_LAST_CHECKED_AT, checkedAt).apply()
        return CourseStorageUpdateCheck(result = result, checkedAt = checkedAt)
    }

    suspend fun removeTextbook(
        context: Context,
        textbookId: String,
    ): CourseTextbookRemovalResult = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        synchronized(clearLock) {
            if (CourseDownloadCoordinator.isBusy()) {
                return@synchronized CourseTextbookRemovalResult.Busy
            }

            val root = File(appContext.filesDir, ROOT_DIRECTORY)
            val active = File(File(root, "active"), textbookId)
            val previousLibrary = MaterialLibraryStore.read(appContext)
            val installed = previousLibrary.firstOrNull { textbook ->
                runCatching { File(textbook.pack.rootPath).canonicalFile == active.canonicalFile }.getOrDefault(false)
            }
            if (!active.exists() && installed == null) {
                return@synchronized CourseTextbookRemovalResult.NotInstalled
            }

            runCatching {
                val removedBytes = CourseCacheFiles.removeTextbookAtomically(
                    root = root,
                    textbookId = textbookId,
                    removeCatalog = {
                        MaterialLibraryStore.write(
                            appContext,
                            previousLibrary.filterNot { textbook ->
                                runCatching {
                                    File(textbook.pack.rootPath).canonicalFile == active.canonicalFile
                                }.getOrDefault(false)
                            },
                        )
                    },
                    restoreCatalog = { MaterialLibraryStore.write(appContext, previousLibrary) },
                )
                CloudCourseRepository.markContentChanged()
                CourseTextbookRemovalResult.Removed(removedBytes)
            }.getOrElse { error ->
                CourseTextbookRemovalResult.Failed(error.message ?: error::class.java.simpleName)
            }
        }
    }

    suspend fun clearCache(context: Context): CourseCacheClearResult = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        synchronized(clearLock) {
            if (CourseDownloadCoordinator.isBusy()) {
                return@synchronized CourseCacheClearResult.Busy
            }

            val root = File(appContext.filesDir, ROOT_DIRECTORY)
            val previousLibrary = MaterialLibraryStore.read(appContext)
            runCatching {
                val removed = CourseCacheFiles.clearAtomically(
                    root = root,
                    clearCatalog = { MaterialLibraryStore.write(appContext, emptyList()) },
                    restoreCatalog = { MaterialLibraryStore.write(appContext, previousLibrary) },
                )
                CloudCourseRepository.markContentChanged()
                CourseCacheClearResult.Cleared(
                    removedBytes = removed.totalBytes,
                    removedTextbooks = removed.installedTextbooks,
                )
            }.getOrElse { error ->
                CourseCacheClearResult.Failed(error.message ?: error::class.java.simpleName)
            }
        }
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}

data class CourseStorageSnapshot(
    val totalBytes: Long,
    val activeBytes: Long,
    val temporaryBytes: Long,
    val installedTextbooks: Int,
    val textbookBytes: Map<String, Long> = emptyMap(),
    val lastCheckedAt: Long = 0L,
)

data class CourseStorageUpdateCheck(
    val result: CourseUpdateCheckResult,
    val checkedAt: Long,
)

sealed interface CourseTextbookRemovalResult {
    data object Busy : CourseTextbookRemovalResult
    data object NotInstalled : CourseTextbookRemovalResult
    data class Removed(val removedBytes: Long) : CourseTextbookRemovalResult
    data class Failed(val message: String) : CourseTextbookRemovalResult
}

sealed interface CourseCacheClearResult {
    data object Busy : CourseCacheClearResult
    data class Cleared(val removedBytes: Long, val removedTextbooks: Int) : CourseCacheClearResult
    data class Failed(val message: String) : CourseCacheClearResult
}

internal object CourseCacheFiles {
    private const val ACTIVE_DIRECTORY = "active"
    private val TEXTBOOK_ID_PATTERN = Regex("[A-Za-z0-9._-]+")

    fun snapshot(root: File): CourseStorageSnapshot {
        val active = File(root, ACTIVE_DIRECTORY)
        val activeDirectories = active.listFiles()
            .orEmpty()
            .filter { directory -> directory.isDirectory && File(directory, "course.json").isFile }
        val textbookBytes = activeDirectories.associate { directory ->
            directory.name to directorySize(directory)
        }
        val activeBytes = textbookBytes.values.sum()
        val totalBytes = directorySize(root)
        return CourseStorageSnapshot(
            totalBytes = totalBytes,
            activeBytes = activeBytes,
            temporaryBytes = (totalBytes - activeBytes).coerceAtLeast(0L),
            installedTextbooks = textbookBytes.size,
            textbookBytes = textbookBytes,
        )
    }

    fun removeTextbookAtomically(
        root: File,
        textbookId: String,
        removeCatalog: () -> Unit,
        restoreCatalog: () -> Unit,
    ): Long {
        require(TEXTBOOK_ID_PATTERN.matches(textbookId)) { "教材 ID 格式无效：$textbookId" }
        val activeRoot = File(root, ACTIVE_DIRECTORY).apply { mkdirs() }
        val active = File(activeRoot, textbookId)
        val removedBytes = directorySize(active)
        val trash = File(activeRoot, ".$textbookId-deleting-${System.nanoTime()}")
        trash.deleteRecursively()

        val moved = active.exists()
        if (moved) {
            require(active.renameTo(trash)) { "无法锁定待删除的教材缓存" }
        }

        try {
            removeCatalog()
            File(root, "staging/$textbookId").deleteRecursively()
            File(root, "backup/$textbookId").deleteRecursively()
            File(root, "downloads/$textbookId-full.part").delete()
            if (trash.exists()) {
                require(trash.deleteRecursively()) { "无法删除教材缓存文件" }
            }
            return removedBytes
        } catch (error: Throwable) {
            if (moved && trash.exists()) {
                active.deleteRecursively()
                require(trash.renameTo(active)) { "教材删除失败，且无法恢复原缓存" }
            }
            runCatching(restoreCatalog)
            throw error
        }
    }

    fun clearAtomically(
        root: File,
        clearCatalog: () -> Unit,
        restoreCatalog: () -> Unit,
    ): CourseStorageSnapshot {
        val previous = snapshot(root)
        val parent = requireNotNull(root.parentFile) { "课程缓存目录缺少父目录" }
        parent.mkdirs()
        val trash = File(parent, ".${root.name}-deleting-${System.nanoTime()}")
        trash.deleteRecursively()

        val moved = root.exists()
        if (moved) {
            require(root.renameTo(trash)) { "无法锁定待清理的课程缓存" }
        }

        try {
            require(root.mkdirs() || root.isDirectory) { "无法重建课程缓存目录" }
            clearCatalog()
            if (trash.exists()) {
                require(trash.deleteRecursively()) { "无法删除课程缓存文件" }
            }
            return previous
        } catch (error: Throwable) {
            root.deleteRecursively()
            if (moved && trash.exists()) {
                require(trash.renameTo(root)) { "课程缓存清理失败，且无法恢复原缓存" }
            }
            runCatching(restoreCatalog)
            throw error
        }
    }

    private fun directorySize(directory: File): Long =
        if (!directory.exists()) 0L else directory.walkTopDown().filter(File::isFile).sumOf(File::length)
}
