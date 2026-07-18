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

data class MaterialLibraryState(
    val installedTextbooks: List<InstalledTextbook> = emptyList(),
    val message: String? = null,
) {
    fun installed(slot: TextbookSlot): InstalledTextbook? =
        installedTextbooks.firstOrNull { it.slot.key == slot.key }
}
