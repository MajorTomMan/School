package com.majortomman.school.data.material

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFailsWith
import org.junit.Test

class MaterialPackManifestParserTest {
    @Test
    fun parsesValidManifest() {
        val manifest = MaterialPackManifestParser.parse(
            """
            {
              "schemaVersion": 1,
              "packId": "math-grade7-volume1",
              "version": "1.0.0",
              "title": "七年级数学上册",
              "subject": "数学",
              "catalog": "catalog.json",
              "pdf": {
                "path": "books/textbook.pdf",
                "sha256": "${"a".repeat(64)}",
                "pageIndexOffset": 2
              }
            }
            """.trimIndent(),
        )

        assertEquals("math-grade7-volume1", manifest.packId)
        assertEquals("books/textbook.pdf", manifest.pdf.path)
        assertEquals(2, manifest.pdf.pageIndexOffset)
    }

    @Test
    fun rejectsTraversalPath() {
        assertFailsWith<IllegalArgumentException> {
            MaterialPackManifestParser.safeRelativePath("../outside.pdf", "pdf.path")
        }
    }

    @Test
    fun mapsPrintedPageToPdfIndex() {
        val directory = Files.createTempDirectory("material-pack-test").toFile()
        val pack = InstalledMaterialPack(
            manifest = MaterialPackManifest(
                schemaVersion = 1,
                packId = "math-grade7-volume1",
                version = "1.0.0",
                title = "七年级数学上册",
                subject = "数学",
                catalogPath = "catalog.json",
                pdf = MaterialPdfAsset("books/textbook.pdf", "a".repeat(64), 3),
            ),
            rootPath = directory.absolutePath,
            installedAt = 0,
            sizeBytes = 0,
        )

        assertEquals(12, pack.printedPageToPdfIndex(10))
        assertEquals(10, pack.pdfIndexToPrintedPage(12))
        directory.deleteRecursively()
    }
}
