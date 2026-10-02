package com.majortomman.school.learning.cloud

import java.nio.file.Files
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import org.json.JSONObject
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
    @Test
    fun localIntegrityAcceptsMatchingStateAndRejectsCorruption() {
        val parent = Files.createTempDirectory("school-course-integrity")
        try {
            val active = parent.resolve("course-a")
            Files.createDirectories(active)
            active.resolve("course.json").writeBytes("course".toByteArray())
            writeIntegrityState(active, "course.json")

            val state = CourseLocalIntegrityValidator.readValidated(active.toFile())
            assertEquals(setOf("course.json"), state.files.keys)

            active.resolve("course.json").writeBytes("corrupted".toByteArray())
            val failure = runCatching {
                CourseLocalIntegrityValidator.readValidated(active.toFile())
            }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun localIntegrityRejectsUnexpectedActiveFile() {
        val parent = Files.createTempDirectory("school-course-integrity-extra")
        try {
            val active = parent.resolve("course-a")
            Files.createDirectories(active)
            active.resolve("course.json").writeBytes("course".toByteArray())
            writeIntegrityState(active, "course.json")
            active.resolve("undeclared.bin").writeBytes(byteArrayOf(1))

            val failure = runCatching {
                CourseLocalIntegrityValidator.readValidated(active.toFile())
            }.exceptionOrNull()

            assertTrue(failure?.message.orEmpty().contains("未声明"))
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    private fun writeIntegrityState(active: java.nio.file.Path, fileName: String) {
        val file = active.resolve(fileName).toFile()
        val item = JSONObject()
            .put("size", file.length())
            .put("sha256", CoursePackStore.sha256(file))
        val state = JSONObject().put("files", JSONObject().put(fileName, item))
        active.resolve(".course-state.json").writeText(state.toString())
    }

}
