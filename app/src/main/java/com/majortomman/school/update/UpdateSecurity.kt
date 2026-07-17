package com.majortomman.school.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import com.majortomman.school.BuildConfig
import java.io.File
import java.io.FileInputStream
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

internal object UpdateSecurity {
    fun verifyManifest(rawJson: ByteArray, signatureBytes: ByteArray): Boolean {
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(
            X509EncodedKeySpec(Base64.decode(BuildConfig.UPDATE_PUBLIC_KEY_BASE64, Base64.DEFAULT)),
        )
        return Signature.getInstance("SHA256withRSA").run {
            initVerify(publicKey)
            update(rawJson)
            verify(signatureBytes)
        }
    }

    fun verifyApk(context: Context, file: File, manifest: UpdateManifest) {
        require(file.isFile && file.length() == manifest.apk.size) { "更新文件大小不一致。" }
        require(file.sha256() == manifest.apk.sha256) { "更新文件 SHA-256 校验失败。" }

        @Suppress("DEPRECATION")
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            context.packageManager.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNATURES)
        } ?: error("无法读取更新 APK 信息。")

        require(packageInfo.packageName == context.packageName) { "更新包名与当前应用不一致。" }
        val apkVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        require(apkVersionCode == manifest.versionCode) { "更新包版本号与清单不一致。" }

        @Suppress("DEPRECATION")
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners.orEmpty().map { it.toByteArray() }
        } else {
            packageInfo.signatures.orEmpty().map { it.toByteArray() }
        }
        val expected = manifest.apk.certificateSha256.normalizedSha256()
        require(signatures.any { it.sha256() == expected }) { "更新 APK 签名证书不可信。" }
        require(expected == BuildConfig.DEVELOPMENT_CERT_SHA256.normalizedSha256()) {
            "更新清单声明的签名证书不是当前开发通道证书。"
        }
    }

    private fun File.sha256(): String = FileInputStream(this).use { stream ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) break
            if (count > 0) digest.update(buffer, 0, count)
        }
        digest.digest().toHex()
    }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256").digest(this).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
