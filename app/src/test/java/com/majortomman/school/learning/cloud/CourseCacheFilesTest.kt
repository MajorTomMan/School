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
            val active = root.resolve("active/course-a")
            Files.createDirectories(active)
            active.resolve("course.json").writeBytes(ByteArray(11))
            active.resolve("resource.bin").writeBytes(ByteArray(29))
            val downloads = root.resolve("downloads")
            Files.createDirectories(downloads)
            downloads.resolve("resume.part").writeBytes(ByteArray(17))

            val snapshot = CourseCacheFiles.snapshot(root.toFile())

            assertEquals(1, snapshot.installedTextbooks)
            assertEquals(40L, snapshot.activeBytes)
            assertEquals(17L, snapshot.temporaryBytes)
            assertEquals(57L, snapshot.totalBytes)
            assertEquals(40L, snapshot.textbookBytes["course-a"])
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun removeOnlyDeletesSelectedCourseDirectory() {
        val parent = Files.createTempDirectory("school-course-remove")
        try {
            val root = parent.resolve("course-packs")
            val first = root.resolve("active/course-a")
            val second = root.resolve("active/course-b")
            Files.createDirectories(first)
            Files.createDirectories(second)
            first.resolve("course.json").writeBytes(ByteArray(13))
            second.resolve("course.json").writeBytes(ByteArray(17))

            val removedBytes = CourseCacheFiles.removeTextbookAtomically(root.toFile(), "course-a")

            assertEquals(13L, removedBytes)
            assertFalse(first.toFile().exists())
            assertTrue(second.resolve("course.json").toFile().isFile)
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun removeRejectsUnsafeCourseId() {
        val parent = Files.createTempDirectory("school-course-id")
        try {
            val failure = runCatching {
                CourseCacheFiles.removeTextbookAtomically(parent.resolve("course-packs").toFile(), "../escape")
            }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun clearRecreatesAnEmptyCacheDirectory() {
        val parent = Files.createTempDirectory("school-course-clear")
        try {
            val root = parent.resolve("course-packs")
            val active = root.resolve("active/course-a")
            Files.createDirectories(active)
            active.resolve("course.json").writeBytes(ByteArray(7))
            val downloads = root.resolve("downloads")
            Files.createDirectories(downloads)
            downloads.resolve("partial.bin").writeBytes(ByteArray(5))

            val removed = CourseCacheFiles.clearAtomically(root.toFile())

            assertEquals(12L, removed.totalBytes)
            assertEquals(1, removed.installedTextbooks)
            assertTrue(root.toFile().isDirectory)
            assertTrue(root.toFile().listFiles().orEmpty().isEmpty())
        } finally {
            parent.toFile().deleteRecursively()
        }
    }
}
