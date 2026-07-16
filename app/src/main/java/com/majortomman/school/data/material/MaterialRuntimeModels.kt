package com.majortomman.school.data.material

import java.io.File

data class InstalledMaterialPack(
    val manifest: MaterialPackManifest,
    val rootPath: String,
    val installedAt: Long,
    val sizeBytes: Long,
) {
    val pdfFile: File
        get() = File(rootPath, manifest.pdf.path)

    val catalogFile: File
        get() = File(rootPath, manifest.catalogPath)

    fun printedPageToPdfIndex(printedPage: Int): Int =
        (printedPage - 1 + manifest.pdf.pageIndexOffset).coerceAtLeast(0)

    fun pdfIndexToPrintedPage(pdfIndex: Int): Int =
        (pdfIndex - manifest.pdf.pageIndexOffset + 1).coerceAtLeast(1)
}

data class InstalledTextbook(
    val slot: TextbookSlot,
    val pack: InstalledMaterialPack,
    val pageCount: Int,
    val lessons: List<GeneratedLesson>,
) {
    val key: String
        get() = slot.key
}

enum class TextbookProcessingStage(val label: String) {
    PREPARING("准备教材"),
    EXTRACTING("复制 PDF"),
    VALIDATING("校验 PDF"),
    IDENTIFYING("识别教材信息"),
    INDEXING("建立页面索引"),
    GENERATING_COURSES("生成课程"),
    FINALIZING("完成安装"),
    COMPLETED("处理完成"),
}

enum class TextbookProcessingStatus {
    QUEUED,
    RUNNING,
    FAILED,
}

data class TextbookProcessingState(
    val slot: TextbookSlot,
    val status: TextbookProcessingStatus,
    val stage: TextbookProcessingStage,
    val progress: Int,
    val message: String,
)

data class MaterialLibraryState(
    val installedTextbooks: List<InstalledTextbook> = emptyList(),
    val processing: Map<String, TextbookProcessingState> = emptyMap(),
    val message: String? = null,
) {
    fun installed(slot: TextbookSlot): InstalledTextbook? =
        installedTextbooks.firstOrNull { it.slot.key == slot.key }

    fun processing(slot: TextbookSlot): TextbookProcessingState? = processing[slot.key]
}
