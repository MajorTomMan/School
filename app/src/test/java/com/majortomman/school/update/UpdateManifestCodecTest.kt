package com.majortomman.school.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateManifestCodecTest {
    private val releaseBaseUrl = "https://github.com/MajorTomMan/school/releases/download/dev-latest"

    @Test
    fun decodesReleaseManifestStructureAndAcceptsRepositoryCaseDifferences() {
        val manifest = UpdateManifestCodec.decode(
            manifestJson("https://github.com/MajorTomMan/School/releases/download/dev-latest/school-debug.apk"),
            releaseBaseUrl,
        )

        assertEquals(5_300_999L, manifest.versionCode)
        assertEquals(listOf("change"), manifest.changes)
        assertEquals(listOf("fix"), manifest.fixes)
    }

    @Test
    fun rejectsNonGithubDownloadHost() {
        val result = runCatching { UpdateManifestCodec.decode(manifestJson("https://example.com/school-debug.apk"), releaseBaseUrl) }
        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsDifferentGithubRelease() {
        val result = runCatching {
            UpdateManifestCodec.decode(
                manifestJson("https://github.com/MajorTomMan/other/releases/download/dev-latest/school-debug.apk"),
                releaseBaseUrl,
            )
        }
        assertTrue(result.isFailure)
    }

    private fun manifestJson(downloadUrl: String): String = """
        {
          "schemaVersion":1,
          "channel":"development",
          "versionCode":5300999,
          "versionName":"0.27.4",
          "minimumSupportedVersionCode":0,
          "mandatory":false,
          "publishedAt":"2026-08-19T12:00:00Z",
          "changes":["change"],
          "fixes":["fix"],
          "apk":{
            "fileName":"school-debug.apk",
            "downloadUrl":"$downloadUrl",
            "size":15212912,
            "sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            "certificateSha256":"7b816cf2873e5d45320015a80dacbf3e9d303f0513e174d8ddf0e69ef1c421b2"
          }
        }
    """.trimIndent()
}
