package com.majortomman.school.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class UpdateCoordinator(context: Context) {
    private val appContext = context.applicationContext
    private val repository = UpdateRepository(appContext)
    private val preferences = UpdatePreferences(appContext)
    private val workManager = WorkManager.getInstance(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val state = UpdateRuntimeBus.state

    private val mutableSettings = MutableStateFlow(repository.settings())
    val settings = mutableSettings.asStateFlow()

    private val mutableDialogVisible = MutableStateFlow(false)
    val dialogVisible = mutableDialogVisible.asStateFlow()

    init {
        repository.restoreCachedState().also { restored ->
            mutableDialogVisible.value = restored is UpdateState.Available || restored is UpdateState.Ready
        }
        schedulePeriodicCheck()
    }

    fun onAppForeground() {
        val restored = repository.restoreCachedState()
        if (restored is UpdateState.Available || restored is UpdateState.Ready) {
            mutableDialogVisible.value = true
        }
        if (repository.shouldCheckOnForeground()) checkNow(force = false)
    }

    fun checkNow(force: Boolean = true) {
        scope.launch {
            val result = repository.check(force)
            mutableSettings.value = repository.settings()
            mutableDialogVisible.value = when (result) {
                is UpdateState.Available,
                is UpdateState.Ready,
                is UpdateState.Error,
                is UpdateState.UpToDate,
                -> force || result !is UpdateState.UpToDate
                else -> false
            }
        }
    }

    fun download(manifest: UpdateManifest) {
        val networkType = if (preferences.settings().wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        val request = OneTimeWorkRequestBuilder<UpdateDownloadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(networkType).build())
            .build()
        UpdateRuntimeBus.publish(UpdateState.Downloading(manifest, 0, 0L))
        mutableDialogVisible.value = true
        workManager.enqueueUniqueWork(UPDATE_DOWNLOAD_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelDownload() {
        workManager.cancelUniqueWork(UPDATE_DOWNLOAD_WORK_NAME)
        repository.restoreCachedState()
        mutableDialogVisible.value = false
    }

    fun remindLater(manifest: UpdateManifest) {
        repository.snooze(manifest)
        mutableDialogVisible.value = false
    }

    fun ignoreVersion(manifest: UpdateManifest) {
        repository.ignore(manifest)
        mutableDialogVisible.value = false
    }

    fun dismissStatus() {
        mutableDialogVisible.value = false
        if (state.value is UpdateState.UpToDate || state.value is UpdateState.Error) {
            UpdateRuntimeBus.publish(UpdateState.Idle)
        }
    }

    fun showDialog() {
        mutableDialogVisible.value = true
    }

    fun setAutoCheck(enabled: Boolean) {
        repository.setAutoCheck(enabled)
        mutableSettings.value = repository.settings()
        schedulePeriodicCheck()
    }

    fun setWifiOnly(enabled: Boolean) {
        repository.setWifiOnly(enabled)
        mutableSettings.value = repository.settings()
        schedulePeriodicCheck()
    }

    fun isMandatory(manifest: UpdateManifest): Boolean = repository.isMandatory(manifest)

    private fun schedulePeriodicCheck() {
        if (!preferences.settings().autoCheck) {
            workManager.cancelUniqueWork(UPDATE_CHECK_WORK_NAME)
            return
        }
        val networkType = if (preferences.settings().wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(24, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(networkType).build())
            .build()
        workManager.enqueueUniquePeriodicWork(
            UPDATE_CHECK_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
