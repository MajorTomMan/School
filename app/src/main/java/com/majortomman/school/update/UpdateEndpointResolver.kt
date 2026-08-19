package com.majortomman.school.update

import android.util.Log
import com.majortomman.school.BuildConfig
import java.net.URI
import org.json.JSONObject

internal data class UpdateEndpoints(val releaseBaseUrl: String) {
    val manifestUrl: String = "$releaseBaseUrl/update-manifest.json"
    val signatureUrl: String = "$releaseBaseUrl/update-manifest.sig"
    fun apkUrl(fileName: String): String = "$releaseBaseUrl/$fileName"

    companion object {
        fun fromReleaseBaseUrl(value: String): UpdateEndpoints {
            val normalized = value.trim().trimEnd('/')
            require(normalized.isNotEmpty()) { "更新 Release 地址为空。" }
            val uri = URI(normalized)
            require(uri.scheme == "https" && !uri.host.isNullOrBlank()) { "更新 Release 地址必须使用 HTTPS。" }
            require(uri.userInfo == null && uri.rawQuery == null && uri.rawFragment == null) { "更新 Release 地址格式无效。" }
            return UpdateEndpoints(normalized)
        }
    }
}

internal object UpdateEndpointConfigCodec {
    fun decode(json: String): UpdateEndpoints = UpdateEndpoints.fromReleaseBaseUrl(JSONObject(json).getString("releaseBaseUrl"))
}

internal class UpdateEndpointResolver(private val httpClient: UpdateHttpClient) {
    fun resolve(): UpdateEndpoints {
        val fallback = UpdateEndpoints.fromReleaseBaseUrl(BuildConfig.DEFAULT_UPDATE_RELEASE_BASE_URL)
        val discoveryUrl = BuildConfig.UPDATE_DISCOVERY_URL.trim()
        if (discoveryUrl.isBlank()) return fallback
        return runCatching {
            val payload = httpClient.get(discoveryUrl, MAX_DISCOVERY_SIZE, "application/json")
            UpdateEndpointConfigCodec.decode(payload.toString(Charsets.UTF_8))
        }.onFailure { error ->
            Log.w(TAG, "update discovery failed; using default release", error)
        }.getOrDefault(fallback)
    }

    private companion object {
        const val TAG = "SchoolUpdateEndpoint"
        const val MAX_DISCOVERY_SIZE = 32 * 1024
    }
}
