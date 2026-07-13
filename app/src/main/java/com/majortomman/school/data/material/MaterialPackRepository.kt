package com.majortomman.school.data.material

import android.content.Context
import android.net.Uri
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MaterialPackRepository(
    private val context: Context,
) {
    private val materialRoot = File(context.filesDir, "material-packs")
    private val packsRoot = File(materialRoot, "packs")
    private val installedIndex = File(materialRoot, "installed.json")
    private val mutableState = MutableStateFlow(
        MaterialPackState(installed = loadInstalledPack()),
    )

    val state: StateFlow<MaterialPackState> = mutableState.asStateFlow()

    suspend fun importFromUri(uri: Uri): Result<InstalledMaterialPack> = withContext(Dispatchers.IO) {
        mutableState.value = mutableState.value.copy(importing = true, message = "正在校验教材包…")
        val result = runCatching { importInternal(uri) }
        result.onSuccess { installed ->
            mutableState.value = MaterialPackState(
                installed = installed,
                importing = false,
                message = "已导入 ${installed.manifest.title} ${installed.manifest.version}",
            )
        }.onFailure { error ->
            mutableState.value = mutableState.value.copy(
                importing = false,
                message = "导入失败：${error.message ?: error::class.java.simpleName}",
            )
        }
        result
    }

    suspend fun removeInstalledPack() = withContext(Dispatchers.IO) {
        mutableState.value.installed?.let { installed ->
            File(installed.rootPath).deleteRecursively()
        }
        installedIndex.delete()
        mutableState.value = MaterialPackState(message = "教材包已移除")
    }

    private fun importInternal(uri: Uri): InstalledMaterialPack {
        materialRoot.mkdirs()
        packsRoot.mkdirs()
        val staging = File(materialRoot, ".import-${UUID.randomUUID()}")
        require(staging.mkdirs()) { "无法创建教材包临时目录" }

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                extractArchive(input.buffered(), staging)
            } ?: throw IOException("无法读取所选文件")

            val manifestFile = File(staging, "manifest.json")
            require(manifestFile.isFile) { "教材包根目录缺少 manifest.json" }
            val manifest = MaterialPackManifestParser.parse(manifestFile.readText(Charsets.UTF_8))
            val pdfFile = resolveInside(staging, manifest.pdf.path)
            val catalogFile = resolveInside(staging, manifest.catalogPath)
            require(pdfFile.isFile) { "教材包缺少 PDF：${manifest.pdf.path}" }
            require(catalogFile.isFile) { "教材包缺少目录：${manifest.catalogPath}" }

            val actualSha256 = sha256(pdfFile)
            require(actualSha256.equals(manifest.pdf.sha256, ignoreCase = true)) {
                "PDF 校验失败，文件可能损坏或版本不一致"
            }

            val finalDirectory = File(packsRoot, manifest.packId)
            val backup = File(materialRoot, ".backup-${manifest.packId}-${UUID.randomUUID()}")
            if (finalDirectory.exists()) {
                require(finalDirectory.renameTo(backup)) { "无法替换旧教材包" }
            }
            try {
                require(staging.renameTo(finalDirectory)) { "无法保存教材包" }
                backup.deleteRecursively()
            } catch (error: Throwable) {
                if (!finalDirectory.exists() && backup.exists()) backup.renameTo(finalDirectory)
                throw error
            }

            val installed = InstalledMaterialPack(
                manifest = manifest,
                rootPath = finalDirectory.absolutePath,
                installedAt = System.currentTimeMillis(),
                sizeBytes = directorySize(finalDirectory),
            )
            writeInstalledPack(installed)
            return installed
        } finally {
            if (staging.exists()) staging.deleteRecursively()
        }
    }

    private fun extractArchive(input: java.io.InputStream, destination: File) {
        var fileCount = 0
        var totalBytes = 0L
        ZipInputStream(BufferedInputStream(input)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                fileCount += 1
                require(fileCount <= MAX_FILE_COUNT) { "教材包文件数量过多" }
                val safePath = MaterialPackManifestParser.safeRelativePath(entry.name, "ZIP 条目")
                val output = resolveInside(destination, safePath)
                if (entry.isDirectory) {
                    require(output.mkdirs() || output.isDirectory) { "无法创建目录：$safePath" }
                } else {
                    output.parentFile?.let { parent ->
                        require(parent.mkdirs() || parent.isDirectory) { "无法创建目录：${parent.name}" }
                    }
                    BufferedOutputStream(FileOutputStream(output)).use { target ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var entryBytes = 0L
                        while (true) {
                            val read = zip.read(buffer)
                            if (read < 0) break
                            entryBytes += read
                            totalBytes += read
                            require(entryBytes <= MAX_SINGLE_FILE_BYTES) { "教材包内单个文件过大" }
                            require(totalBytes <= MAX_TOTAL_UNCOMPRESSED_BYTES) { "教材包解压后体积过大" }
                            target.write(buffer, 0, read)
                        }
                    }
                }
                zip.closeEntry()
            }
        }
        require(fileCount > 0) { "教材包是空文件" }
    }

    private fun resolveInside(root: File, relativePath: String): File {
        val file = File(root, relativePath)
        val rootPath = root.canonicalFile.path + File.separator
        val filePath = file.canonicalFile.path
        require(filePath.startsWith(rootPath)) { "教材包包含越界路径" }
        return file
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun directorySize(directory: File): Long =
        directory.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    private fun writeInstalledPack(installed: InstalledMaterialPack) {
        materialRoot.mkdirs()
        val root = JSONObject()
            .put("manifest", MaterialPackManifestParser.toJson(installed.manifest))
            .put("rootPath", installed.rootPath)
            .put("installedAt", installed.installedAt)
            .put("sizeBytes", installed.sizeBytes)
        val temporary = File(materialRoot, "installed.json.tmp")
        temporary.writeText(root.toString(2), Charsets.UTF_8)
        if (installedIndex.exists()) installedIndex.delete()
        require(temporary.renameTo(installedIndex)) { "无法保存教材包索引" }
    }

    private fun loadInstalledPack(): InstalledMaterialPack? = runCatching {
        if (!installedIndex.isFile) return@runCatching null
        val root = JSONObject(installedIndex.readText(Charsets.UTF_8))
        val manifest = MaterialPackManifestParser.parse(root.getJSONObject("manifest").toString())
        val installed = InstalledMaterialPack(
            manifest = manifest,
            rootPath = root.getString("rootPath"),
            installedAt = root.getLong("installedAt"),
            sizeBytes = root.optLong("sizeBytes", 0L),
        )
        if (!installed.pdfFile.isFile) null else installed
    }.getOrNull()

    private companion object {
        const val MAX_FILE_COUNT = 10_000
        const val MAX_SINGLE_FILE_BYTES = 1_600L * 1024L * 1024L
        const val MAX_TOTAL_UNCOMPRESSED_BYTES = 2_200L * 1024L * 1024L
    }
}
