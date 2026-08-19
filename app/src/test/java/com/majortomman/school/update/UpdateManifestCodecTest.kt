package com.majortomman.school.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateManifestCodecTest {
    private val updateUrl = "https://github.com/MajorTomMan/School/releases/download/dev-latest"

    @Test
    fun decodesReleaseManifestStructure() {
        val manifest = UpdateManifestCodec.decode(manifestJson(), updateUrl)
        assertEquals(2, manifest.schemaVersion)
        assertEquals(53L, manifest.versionCode)
        assertEquals("school-debug.apk", manifest.apk.fileName)
        assertEquals("$updateUrl/school-debug.apk", manifest.apk.downloadUrl)
        assertEquals(listOf("change"), manifest.changes)
        assertEquals(listOf("fix"), manifest.fixes)
    }

    @Test
    fun rejectsUnsafeApkFileName() {
        val result = runCatching { UpdateManifestCodec.decode(manifestJson("../school-debug.apk"), updateUrl) }
        assertTrue(result.isFailure)
    }

    @Test
    fun decodesRemoteUpdateUrl() {
        assertEquals(updateUrl, UpdateConfigCodec.decode("""{"updateUrl":"$updateUrl"}"""))
    }

    @Test
    fun rejectsNonHttpsUpdateUrl() {
        val result = runCatching { UpdateConfigCodec.decode("""{"updateUrl":"http://example.com/release"}""") }
        assertTrue(result.isFailure)
    }

    private fun manifestJson(fileName: String = "school-debug.apk"): String = """
        {
          "schemaVersion":2,
          "channel":"development",
          "versionCode":53,
          "versionName":"0.27.4",
          "minimumSupportedVersionCode":0,
          "mandatory":false,
          "publishedAt":"2026-08-19T12:00:00Z",
          "changes":["change"],
          "fixes":["fix"],
          "apk":{
            "fileName":"$fileName",
            "size":15212912,
            "sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            "certificateSha256":"7b816cf2873e5d45320015a80dacbf3e9d303f0513e174d8ddf0e69ef1c421b2"
          }
        }
    """.trimIndent()
}
