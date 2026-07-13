package com.majortomman.school.data.material

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Observer
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class MaterialPackRepository(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)
    private val mutableState = MutableStateFlow(
        MaterialLibraryState(installedTextbooks = MaterialLibraryStore.read(appContext)),
    )
    private val workObserver = Observer<List<WorkInfo>> { workInfos ->
        refresh(workInfos.orEmpty())
    }

    val state: StateFlow<MaterialLibraryState> = mutableState.asStateFlow()

    init {
        workManager.getWorkInfosByTagLiveData(TextbookProcessingContract.TAG)
            .observeForever(workObserver)
    }

    fun enqueueImport(
        slot: TextbookSlot,
        uri: Uri,
    ) {
        runCatching {
            appContext.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        val request = OneTimeWorkRequestBuilder<TextbookProcessingWorker>()
            .setInputData(
                workDataOf(
                    TextbookProcessingContract.KEY_SOURCE_URI to uri.toString(),
                    TextbookProcessingContract.KEY_SUBJECT_ID to slot.subjectId,
                    TextbookProcessingContract.KEY_SUBJECT_TITLE to slot.subjectTitle,
                    TextbookProcessingContract.KEY_GRADE to slot.grade,
                    TextbookProcessingContract.KEY_VOLUME to slot.volume.id,
                    TextbookProcessingContract.KEY_SLOT_KEY to slot.key,
                ),
            )
            .addTag(TextbookProcessingContract.TAG)
            .addTag(TextbookProcessingContract.slotTag(slot))
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                15,
                TimeUnit.SECONDS,
            )
            .build()
        workManager.enqueueUniqueWork(
            TextbookProcessingContract.uniqueWorkName(slot),
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun cancelProcessing(slot: TextbookSlot) {
        workManager.cancelUniqueWork(TextbookProcessingContract.uniqueWorkName(slot))
    }

    suspend fun removeInstalled(slot: TextbookSlot) = withContext(Dispatchers.IO) {
        cancelProcessing(slot)
        val removed = MaterialLibraryStore.remove(appContext, slot.key)
        removed?.pack?.rootPath?.let { java.io.File(it).deleteRecursively() }
        MaterialLibraryStore.processingRoot(appContext, slot).deleteRecursively()
        refreshCurrent()
    }

    fun refreshCurrent() {
        workManager.getWorkInfosByTag(TextbookProcessingContract.TAG).get()
            .let(::refresh)
    }

    private fun refresh(workInfos: List<WorkInfo>) {
        val installed = MaterialLibraryStore.read(appContext)
        val jobs = workInfos
            .mapNotNull(::toProcessingState)
            .groupBy { it.slot.key }
            .mapValues { (_, states) ->
                states.maxByOrNull { statePriority(it.status) } ?: states.first()
            }
        mutableState.value = MaterialLibraryState(
            installedTextbooks = installed,
            processing = jobs,
            message = jobs.values.firstOrNull { it.status == TextbookProcessingStatus.FAILED }?.message,
        )
    }

    private fun toProcessingState(info: WorkInfo): TextbookProcessingState? {
        val slotKey = info.tags
            .firstOrNull { it.startsWith(TextbookProcessingContract.TAG_SLOT_PREFIX) }
            ?.removePrefix(TextbookProcessingContract.TAG_SLOT_PREFIX)
            ?: return null
        val slot = TextbookSlot.fromKey(slotKey) ?: return null
        val data = if (info.state == WorkInfo.State.FAILED) info.outputData else info.progress
        val stage = data.getString(TextbookProcessingContract.KEY_STAGE)
            ?.let { runCatching { TextbookProcessingStage.valueOf(it) }.getOrNull() }
            ?: TextbookProcessingStage.PREPARING
        val progress = data.getInt(TextbookProcessingContract.KEY_PROGRESS, 0).coerceIn(0, 100)
        val message = data.getString(TextbookProcessingContract.KEY_MESSAGE)
            ?: when (info.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> "等待后台处理"
                WorkInfo.State.RUNNING -> stage.label
                WorkInfo.State.FAILED -> "教材处理失败"
                else -> ""
            }
        val status = when (info.state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> TextbookProcessingStatus.QUEUED
            WorkInfo.State.RUNNING -> TextbookProcessingStatus.RUNNING
            WorkInfo.State.FAILED -> TextbookProcessingStatus.FAILED
            else -> return null
        }
        return TextbookProcessingState(
            slot = slot,
            status = status,
            stage = stage,
            progress = progress,
            message = message,
        )
    }

    private fun statePriority(status: TextbookProcessingStatus): Int = when (status) {
        TextbookProcessingStatus.RUNNING -> 3
        TextbookProcessingStatus.QUEUED -> 2
        TextbookProcessingStatus.FAILED -> 1
    }
}
