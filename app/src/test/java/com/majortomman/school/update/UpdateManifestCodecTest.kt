package com.majortomman.school.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateManifestCodecTest {
    @Test
    fun decodesReleaseManifestStructure() {
        val manifest = UpdateManifestCodec.decode(
            """
            {
              "schemaVersion":1,
              "channel":"development",
              "versionCode":100208,
              "versionName":"0.21.0",
              "minimumSupportedVersionCode":0,
              "mandatory":false,
              "publishedAt":"2026-07-17T12:00:00Z",
              "changes":["change"],
              "fixes":["fix"],
              "apk":{
                "fileName":"school-debug.apk",
                "downloadUrl":"https://github.com/MajorTomMan/school/releases/download/dev-latest/school-debug.apk",
                "size":15212912,
                "sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "certificateSha256":"7b816cf2873e5d45320015a80dacbf3e9d303f0513e174d8ddf0e69ef1c421b2"
              }
            }
            """.trimIndent(),
        )

        assertEquals(100208L, manifest.versionCode)
        assertEquals(listOf("change"), manifest.changes)
        assertEquals(listOf("fix"), manifest.fixes)
    }

    @Test
    fun rejectsNonGithubDownloadHost() {
        val result = runCatching {
            UpdateManifestCodec.decode(
                """
                {
                  "schemaVersion":1,
                  "channel":"development",
                  "versionCode":2,
                  "versionName":"0.0.2",
                  "apk":{
                    "fileName":"school.apk",
                    "downloadUrl":"https://example.com/school.apk",
                    "size":1,
                    "sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                    "certificateSha256":"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
                  }
                }
                """.trimIndent(),
            )
        }

        assertTrue(result.isFailure)
    }
}
