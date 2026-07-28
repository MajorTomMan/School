package com.majortomman.school.learning.cloud

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Process-wide view of the unique WorkManager course download.
 *
 * The WorkManager record, rather than an Activity or Compose scope, is the source of truth. A new
 * Activity therefore reattaches to the existing job after onStop/onPause, configuration changes or
 * process recreation instead of enqueueing a second download.
 */
object CourseDownloadCoordinator {
    internal const val UNIQUE_WORK_NAME = "school-course-download"
    internal const val KEY_OPERATION_ID = "operation_id"

    private val operationCounter = AtomicLong(System.currentTimeMillis())
    private val initialized = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutableState = MutableStateFlow<CourseDownloadUiState>(CourseDownloadUiState.Restoring)

    val state = mutableState.asStateFlow()

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        if (!initialized.compareAndSet(false, true)) return

        scope.launch {
            WorkManager.getInstance(appContext)
                .getWorkInfosForUniqueWorkFlow(UNIQUE_WORK_NAME)
                .catch {
                    if (mutableState.value is CourseDownloadUiState.Restoring) {
                        mutableState.value = CourseDownloadUiState.Idle
                    }
                }
                .collectLatest(::restoreFromWorkManager)
        }
    }

    fun enqueue(context: Context) {
        val appContext = context.applicationContext
        initialize(appContext)
        val current = mutableState.value
        if (current is CourseDownloadUiState.Queued || current is CourseDownloadUiState.Running) return

        val operationId = operationCounter.incrementAndGet()
        mutableState.value = CourseDownloadUiState.Queued(operationId)
        val request = OneTimeWorkRequestBuilder<CourseDownloadWorker>()
            .setInputData(workDataOf(KEY_OPERATION_ID to operationId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun isBusy(): Boolean = when (mutableState.value) {
        CourseDownloadUiState.Restoring,
        is CourseDownloadUiState.Queued,
        is CourseDownloadUiState.Running,
        -> true
        else -> false
    }

    internal fun reportRunning(operationId: Long, progress: CourseSyncProgress) {
        mutableState.value = CourseDownloadUiState.Running(
            operationId = operationId,
            downloadedBytes = progress.downloadedBytes,
            totalBytes = progress.totalBytes,
            currentItem = progress.currentItem,
            stage = progress.stage,
        )
    }

    internal fun reportSuccess(operationId: Long, updatedTextbooks: Int) {
        mutableState.value = CourseDownloadUiState.Success(operationId, updatedTextbooks)
    }

    internal fun reportFailure(operationId: Long, message: String) {
        mutableState.value = CourseDownloadUiState.Failed(operationId, message)
    }

    fun clearTerminalState() {
        if (mutableState.value is CourseDownloadUiState.Success || mutableState.value is CourseDownloadUiState.Failed) {
            mutableState.value = CourseDownloadUiState.Idle
        }
    }

    private fun restoreFromWorkManager(infos: List<WorkInfo>) {
        val active = infos.firstOrNull { it.state == WorkInfo.State.RUNNING }
            ?: infos.firstOrNull { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED }

        if (active == null) {
            when (mutableState.value) {
                CourseDownloadUiState.Restoring,
                is CourseDownloadUiState.Queued,
                is CourseDownloadUiState.Running,
                -> mutableState.value = CourseDownloadUiState.Idle
                else -> Unit
            }
            return
        }

        val operationId = active.operationId()
        operationCounter.accumulateAndGet(operationId, ::maxOf)
        if (active.state != WorkInfo.State.RUNNING) {
            mutableState.value = CourseDownloadUiState.Queued(operationId)
            return
        }

        val restored = CourseDownloadUiState.Running(
            operationId = operationId,
            downloadedBytes = active.progress.getLong(CourseDownloadWorker.KEY_DOWNLOADED_BYTES, 0L),
            totalBytes = active.progress.getLong(CourseDownloadWorker.KEY_TOTAL_BYTES, 0L),
            currentItem = active.progress.getString(CourseDownloadWorker.KEY_CURRENT_ITEM).orEmpty(),
            stage = active.progress.getString(CourseDownloadWorker.KEY_STAGE).orEmpty().ifBlank { "正在恢复后台下载" },
        )
        val current = mutableState.value
        if (
            current is CourseDownloadUiState.Running &&
            current.operationId == restored.operationId &&
            current.downloadedBytes > restored.downloadedBytes
        ) {
            return
        }
        mutableState.value = restored
    }

    private fun WorkInfo.operationId(): Long {
        val stored = inputData.getLong(KEY_OPERATION_ID, 0L)
            .takeIf { it != 0L }
            ?: progress.getLong(KEY_OPERATION_ID, 0L).takeIf { it != 0L }
            ?: outputData.getLong(KEY_OPERATION_ID, 0L).takeIf { it != 0L }
        return stored ?: id.stableLong()
    }

    private fun UUID.stableLong(): Long = (mostSignificantBits xor leastSignificantBits).let {
        if (it == Long.MIN_VALUE) 1L else kotlin.math.abs(it).coerceAtLeast(1L)
    }
}

sealed interface CourseDownloadUiState {
    data object Restoring : CourseDownloadUiState
    data object Idle : CourseDownloadUiState
    data class Queued(val operationId: Long) : CourseDownloadUiState
    data class Running(
        val operationId: Long,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val currentItem: String,
        val stage: String,
    ) : CourseDownloadUiState
    data class Success(val operationId: Long, val updatedTextbooks: Int) : CourseDownloadUiState
    data class Failed(val operationId: Long, val message: String) : CourseDownloadUiState
}
