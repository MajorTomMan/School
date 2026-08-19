package com.majortomman.school.update

import android.content.Context
import com.majortomman.school.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class UpdateRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = UpdatePreferences(appContext)
    private val httpClient = UpdateHttpClient(appContext)

    suspend fun check(force: Boolean = false): UpdateState = withContext(Dispatchers.IO) {
        val current = UpdateRuntimeBus.state.value
        if (current is UpdateState.Downloading) return@withContext current
        UpdateRuntimeBus.publish(UpdateState.Checking)
        runCatching {
            val updateUrl = resolveUpdateUrl()
            val manifestBytes = httpClient.get("$updateUrl/update-manifest.json", MAX_MANIFEST_SIZE)
            val signatureBytes = httpClient.get("$updateUrl/update-manifest.sig", MAX_SIGNATURE_SIZE)
            require(UpdateSecurity.verifyManifest(manifestBytes, signatureBytes)) { "更新清单签名校验失败。" }
            val rawJson = manifestBytes.toString(Charsets.UTF_8)
            val manifest = UpdateManifestCodec.decode(rawJson, updateUrl)
            require(manifest.apk.certificateSha256 == BuildConfig.DEVELOPMENT_CERT_SHA256.normalizedSha256()) { "更新清单声明了错误的 APK 证书。" }

            val now = System.currentTimeMillis()
            preferences.setLastChecked(now)
            preferences.cacheManifest(rawJson, updateUrl)

            val state = when {
                manifest.versionCode <= BuildConfig.VERSION_CODE.toLong() -> UpdateState.UpToDate(now)
                !force && preferences.ignoredVersion() == manifest.versionCode && !isMandatory(manifest) -> UpdateState.Idle
                !force && preferences.snoozeUntil() > now && !isMandatory(manifest) -> UpdateState.Idle
                else -> restoreDownloadedState(manifest) ?: UpdateState.Available(manifest)
            }
            UpdateRuntimeBus.publish(state)
            state
        }.getOrElse { throwable ->
            val state = UpdateState.Error(throwable.message ?: "检查更新失败。")
            UpdateRuntimeBus.publish(state)
            state
        }
    }

    fun restoreCachedState(): UpdateState {
        val manifest = preferences.cachedManifest() ?: return publishAndReturn(UpdateState.Idle)
        if (manifest.versionCode <= BuildConfig.VERSION_CODE.toLong()) return publishAndReturn(UpdateState.Idle)
        val ready = restoreDownloadedState(manifest)
        if (ready != null) return publishAndReturn(ready)
        val now = System.currentTimeMillis()
        val suppressed = !isMandatory(manifest) && (preferences.ignoredVersion() == manifest.versionCode || preferences.snoozeUntil() > now)
        return publishAndReturn(if (suppressed) UpdateState.Idle else UpdateState.Available(manifest))
    }

    fun settings(): UpdateSettings = preferences.settings()

    fun shouldCheckOnForeground(now: Long = System.currentTimeMillis()): Boolean {
        val settings = preferences.settings()
        return settings.autoCheck && now - settings.lastCheckedAt >= FOREGROUND_CHECK_INTERVAL_MS
    }

    fun setAutoCheck(enabled: Boolean) = preferences.setAutoCheck(enabled)
    fun setWifiOnly(enabled: Boolean) = preferences.setWifiOnly(enabled)

    fun snooze(manifest: UpdateManifest) {
        if (!isMandatory(manifest)) preferences.setSnoozeUntil(System.currentTimeMillis() + SNOOZE_MS)
    }

    fun ignore(manifest: UpdateManifest) {
        if (!isMandatory(manifest)) preferences.ignoreVersion(manifest.versionCode)
    }

    fun isMandatory(manifest: UpdateManifest): Boolean = manifest.mandatory || BuildConfig.VERSION_CODE.toLong() < manifest.minimumSupportedVersionCode

    private fun resolveUpdateUrl(): String {
        val configUrl = BuildConfig.UPDATE_CONFIG_URL.trim()
        if (configUrl.isBlank()) return UpdateConfigCodec.normalize(BuildConfig.DEFAULT_UPDATE_URL)
        val config = httpClient.get(configUrl, MAX_CONFIG_SIZE, "application/json").toString(Charsets.UTF_8)
        return UpdateConfigCodec.decode(config)
    }

    private fun publishAndReturn(state: UpdateState): UpdateState {
        UpdateRuntimeBus.publish(state)
        return state
    }

    private fun restoreDownloadedState(manifest: UpdateManifest): UpdateState? {
        val path = preferences.downloadedApk(manifest.versionCode) ?: return null
        val file = java.io.File(path)
        return runCatching {
            UpdateSecurity.verifyApk(appContext, file, manifest)
            UpdateState.Ready(manifest, file)
        }.getOrElse {
            preferences.clearDownloadedApk()
            file.delete()
            null
        }
    }

    private companion object {
        const val MAX_CONFIG_SIZE = 32 * 1024
        const val MAX_MANIFEST_SIZE = 256 * 1024
        const val MAX_SIGNATURE_SIZE = 16 * 1024
        const val FOREGROUND_CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L
        const val SNOOZE_MS = 24 * 60 * 60 * 1000L
    }
}
