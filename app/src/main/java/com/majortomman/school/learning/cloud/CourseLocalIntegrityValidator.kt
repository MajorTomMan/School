package com.majortomman.school.learning.cloud

import java.io.File

internal object CourseLocalIntegrityValidator {
    private const val STATE_FILE_NAME = ".course-state.json"

    fun readValidated(root: File): LocalCourseState {
        require(root.isDirectory) { "课程目录不存在" }
        val stateFile = File(root, STATE_FILE_NAME)
        require(stateFile.isFile) { "课程缺少本地完整性状态" }
        val state = CourseManifestCodec.decodeLocalState(stateFile.readText(Charsets.UTF_8))
        require(state.files.isNotEmpty()) { "课程本地完整性状态不包含文件" }

        val actualFiles = root.walkTopDown()
            .filter(File::isFile)
            .map { it.relativeTo(root).invariantSeparatorsPath }
            .filterNot { it == STATE_FILE_NAME }
            .toSet()
        val expectedFiles = state.files.keys
        require(actualFiles == expectedFiles) {
            val unexpected = (actualFiles - expectedFiles).sorted()
            val missing = (expectedFiles - actualFiles).sorted()
            "课程本地文件集合与完整性状态不一致：未声明=${unexpected.joinToString()}，缺失=${missing.joinToString()}"
        }

        state.files.forEach { (path, expected) ->
            val file = safeResolve(root, path)
            require(file.isFile) { "课程文件缺失：$path" }
            require(file.length() == expected.size) { "课程文件大小校验失败：$path" }
            require(CoursePackStore.sha256(file) == expected.sha256) { "课程文件 SHA-256 校验失败：$path" }
        }
        return state
    }

    private fun safeResolve(root: File, relativePath: String): File {
        val target = File(root, validateRelativePath(relativePath))
        val rootPath = root.canonicalFile.toPath()
        val targetPath = target.canonicalFile.toPath()
        require(targetPath.startsWith(rootPath)) { "课程文件路径越界：$relativePath" }
        return target
    }
}
