package com.majortomman.school.learning.cloud

import java.nio.file.Files
import kotlin.io.path.writeBytes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseCacheFilesTest {
    @Test
    fun snapshotSeparatesActiveAndTemporaryFiles() {
        val parent = Files.createTempDirectory("school-course-cache")
        try {
            val root = parent.resolve("course-packs")
            val active = root.resolve("active/pep-math-7-1")
            Files.createDirectories(active)
            active.resolve("course.json").writeBytes(ByteArray(11))
            active.resolve("textbook.pdf").writeBytes(ByteArray(29))
            val downloads = root.resolve("downloads")
            Files.createDirectories(downloads)
            downloads.resolve("resume.part").writeBytes(ByteArray(17))

            val snapshot = CourseCacheFiles.snapshot(root.toFile())

            assertEquals(1, snapshot.installedTextbooks)
            assertEquals(40L, snapshot.activeBytes)
            assertEquals(17L, snapshot.temporaryBytes)
            assertEquals(57L, snapshot.totalBytes)
            assertEquals(40L, snapshot.textbookBytes["pep-math-7-1"])
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun removeTextbookOnlyRemovesSelectedDirectory() {
        val parent = Files.createTempDirectory("school-course-single-remove")
        try {
            val root = parent.resolve("course-packs")
            val first = root.resolve("active/pep-math-7-1")
            val second = root.resolve("active/pep-math-7-2")
            Files.createDirectories(first)
            Files.createDirectories(second)
            first.resolve("course.json").writeBytes(ByteArray(13))
            second.resolve("course.json").writeBytes(ByteArray(17))
            var catalogRemoved = false

            val removedBytes = CourseCacheFiles.removeTextbookAtomically(
                root = root.toFile(),
                textbookId = "pep-math-7-1",
                removeCatalog = { catalogRemoved = true },
                restoreCatalog = { error("restore must not run") },
            )

            assertTrue(catalogRemoved)
            assertEquals(13L, removedBytes)
            assertFalse(first.toFile().exists())
            assertTrue(second.resolve("course.json").toFile().isFile)
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun removeTextbookRestoresFilesAndCatalogWhenCatalogWriteFails() {
        val parent = Files.createTempDirectory("school-course-single-restore")
        try {
            val root = parent.resolve("course-packs")
            val course = root.resolve("active/pep-math-7-1/course.json")
            Files.createDirectories(course.parent)
            course.writeBytes(ByteArray(9))
            var catalogRestored = false

            val failure = runCatching {
                CourseCacheFiles.removeTextbookAtomically(
                    root = root.toFile(),
                    textbookId = "pep-math-7-1",
                    removeCatalog = { error("catalog write failed") },
                    restoreCatalog = { catalogRestored = true },
                )
            }.exceptionOrNull()

            assertEquals("catalog write failed", failure?.message)
            assertTrue(catalogRestored)
            assertTrue(course.toFile().isFile)
            assertEquals(9L, course.toFile().length())
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun clearMovesCacheAwayBeforeClearingCatalog() {
        val parent = Files.createTempDirectory("school-course-clear")
        try {
            val root = parent.resolve("course-packs")
            val active = root.resolve("active/pep-math-7-1")
            Files.createDirectories(active)
            active.resolve("course.json").writeBytes(ByteArray(7))
            var catalogCleared = false

            val removed = CourseCacheFiles.clearAtomically(
                root = root.toFile(),
                clearCatalog = {
                    assertFalse(active.toFile().exists())
                    catalogCleared = true
                },
                restoreCatalog = { error("restore must not run") },
            )

            assertTrue(catalogCleared)
            assertEquals(7L, removed.totalBytes)
            assertEquals(1, removed.installedTextbooks)
            assertTrue(root.toFile().isDirectory)
            assertTrue(root.toFile().listFiles().orEmpty().isEmpty())
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun clearRestoresCacheAndCatalogWhenCatalogWriteFails() {
        val parent = Files.createTempDirectory("school-course-restore")
        try {
            val root = parent.resolve("course-packs")
            val course = root.resolve("active/pep-math-7-1/course.json")
            Files.createDirectories(course.parent)
            course.writeBytes(ByteArray(5))
            var catalogRestored = false

            val failure = runCatching {
                CourseCacheFiles.clearAtomically(
                    root = root.toFile(),
                    clearCatalog = { error("catalog write failed") },
                    restoreCatalog = { catalogRestored = true },
                )
            }.exceptionOrNull()

            assertEquals("catalog write failed", failure?.message)
            assertTrue(catalogRestored)
            assertTrue(course.toFile().isFile)
            assertEquals(5L, course.toFile().length())
        } finally {
            parent.toFile().deleteRecursively()
        }
    }
}
