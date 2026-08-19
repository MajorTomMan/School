package com.majortomman.school.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateManifestCodecTest {
    private val releaseBaseUrl = "https://github.com/MajorTomMan/School/releases/download/dev-latest"

    @Test
    fun decodesReleaseManifestStructure() {
        val manifest = UpdateManifestCodec.decode(manifestJson("$releaseBaseUrl/school-debug.apk"), releaseBaseUrl)
        assertEquals(53L, manifest.versionCode)
        assertEquals(listOf("change"), manifest.changes)
        assertEquals(listOf("fix"), manifest.fixes)
    }

    @Test
    fun rejectsDownloadOutsideResolvedRelease() {
        val result = runCatching {
            UpdateManifestCodec.decode(manifestJson("https://github.com/MajorTomMan/Other/releases/download/dev-latest/school-debug.apk"), releaseBaseUrl)
        }
        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsReleasePathCaseMismatch() {
        val result = runCatching {
            UpdateManifestCodec.decode(manifestJson("https://github.com/MajorTomMan/school/releases/download/dev-latest/school-debug.apk"), releaseBaseUrl)
        }
        assertTrue(result.isFailure)
    }

    @Test
    fun decodesDiscoveryReleaseBaseUrl() {
        val endpoints = UpdateEndpointConfigCodec.decode("""{"releaseBaseUrl":"$releaseBaseUrl"}""")
        assertEquals(releaseBaseUrl, endpoints.releaseBaseUrl)
        assertEquals("$releaseBaseUrl/update-manifest.json", endpoints.manifestUrl)
        assertEquals("$releaseBaseUrl/update-manifest.sig", endpoints.signatureUrl)
    }

    @Test
    fun rejectsNonHttpsDiscoveryRelease() {
        val result = runCatching { UpdateEndpointConfigCodec.decode("""{"releaseBaseUrl":"http://example.com/release"}""") }
        assertTrue(result.isFailure)
    }

    private fun manifestJson(downloadUrl: String): String = """
        {
          "schemaVersion":1,
          "channel":"development",
          "versionCode":53,
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
