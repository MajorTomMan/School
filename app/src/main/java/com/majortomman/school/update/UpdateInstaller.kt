package com.majortomman.school.update

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

enum class InstallLaunchResult {
    STARTED,
    NEEDS_UNKNOWN_SOURCES_PERMISSION,
    FILE_MISSING,
}

object UpdateInstaller {
    fun launch(activity: Activity, apkFile: File): InstallLaunchResult {
        if (!apkFile.isFile) return InstallLaunchResult.FILE_MISSING
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !activity.packageManager.canRequestPackageInstalls()) {
            activity.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${activity.packageName}"),
                ),
            )
            return InstallLaunchResult.NEEDS_UNKNOWN_SOURCES_PERMISSION
        }

        val uri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.update-files",
            apkFile,
        )
        activity.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
        return InstallLaunchResult.STARTED
    }
}
