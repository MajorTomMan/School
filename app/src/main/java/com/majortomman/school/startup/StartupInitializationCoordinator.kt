package com.majortomman.school.startup

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.majortomman.school.data.curriculum.MasteryTrendRepository
import com.majortomman.school.data.material.MaterialLibraryStore
import com.majortomman.school.learning.cloud.CloudCourseCatalogInstaller
import com.majortomman.school.learning.cloud.CourseSyncManager
import com.majortomman.school.learning.cloud.CourseUpdateCheckResult
import com.majortomman.school.learning.cloud.CourseUpdateOffer
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Keeps course-cache checks and analytics initialization outside the launch-critical path. */
object StartupInitializationCoordinator {
    const val LOG_TAG = "SchoolStartup"

    private const val FIRST_FRAME_GRACE_MILLIS = 750L
    private const val ANALYTICS_GRACE_MILLIS = 750L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val started = AtomicBoolean(false)

    fun start(
        context: Context,
        checkCourseUpdatesOnStartup: Boolean,
        onCourseCatalogChanged: () -> Unit,
        onCourseUpdateAvailable: (CourseUpdateOffer) -> Unit,
    ) {
        if (!started.compareAndSet(false, true)) return
        val appContext = context.applicationContext

        scope.launch {
            delay(FIRST_FRAME_GRACE_MILLIS)

            runCatching {
                val removed = MaterialLibraryStore.purgeLegacyBundledContent(appContext)
                val cachedCatalogs = CloudCourseCatalogInstaller.refreshFromCache(appContext)
                if (removed > 0 || cachedCatalogs > 0) {
                    withContext(Dispatchers.Main.immediate) { onCourseCatalogChanged() }
                }
                Log.i(
                    LOG_TAG,
                    "course cache ready: removedBundled=$removed, cachedCatalogs=$cachedCatalogs",
                )
            }.onFailure { error ->
                Log.e(LOG_TAG, "course cache initialization failed", error)
            }

            if (checkCourseUpdatesOnStartup) {
                checkInstalledCourseUpdates(appContext, onCourseUpdateAvailable)
            } else {
                Log.i(LOG_TAG, "startup course update check skipped")
            }

            delay(ANALYTICS_GRACE_MILLIS)
            val analyticsStartedAt = SystemClock.elapsedRealtime()
            runCatching { MasteryTrendRepository.getInstance(appContext) }
                .onSuccess {
                    Log.i(
                        LOG_TAG,
                        "mastery analytics listener ready in " +
                            "${SystemClock.elapsedRealtime() - analyticsStartedAt} ms",
                    )
                }
                .onFailure { error ->
                    Log.e(LOG_TAG, "mastery analytics initialization failed", error)
                }
        }
    }

    fun requestCourseUpdateCheck(
        context: Context,
        onCourseUpdateAvailable: (CourseUpdateOffer) -> Unit,
    ) {
        scope.launch {
            checkInstalledCourseUpdates(context.applicationContext, onCourseUpdateAvailable)
        }
    }

    private suspend fun checkInstalledCourseUpdates(
        appContext: Context,
        onCourseUpdateAvailable: (CourseUpdateOffer) -> Unit,
    ) {
        val textbookIds = MaterialLibraryStore.read(appContext)
            .map { textbook -> File(textbook.pack.rootPath).name }
            .map(String::trim)
            .filter(String::isNotBlank)
            .toSet()
        if (textbookIds.isEmpty()) {
            Log.i(LOG_TAG, "course update check skipped because no textbook is installed")
            return
        }
        checkCourseUpdates(appContext, textbookIds, onCourseUpdateAvailable)
    }

    private suspend fun checkCourseUpdates(
        appContext: Context,
        textbookIds: Set<String>,
        onCourseUpdateAvailable: (CourseUpdateOffer) -> Unit,
    ) {
        val startedAt = SystemClock.elapsedRealtime()
        when (val result = CourseSyncManager.checkForUpdates(appContext, textbookIds)) {
            CourseUpdateCheckResult.Disabled -> Log.i(LOG_TAG, "cloud course synchronization is not configured")
            CourseUpdateCheckResult.NotPublished -> Log.i(LOG_TAG, "new course packages are not published yet")
            CourseUpdateCheckResult.NoUpdate -> Log.i(
                LOG_TAG,
                "installed course content is current; checked in ${SystemClock.elapsedRealtime() - startedAt} ms",
            )
            is CourseUpdateCheckResult.Available -> {
                Log.i(
                    LOG_TAG,
                    "installed course update available: kind=${result.offer.kind}, bytes=${result.offer.estimatedBytes}",
                )
                withContext(Dispatchers.Main.immediate) {
                    onCourseUpdateAvailable(result.offer)
                }
            }
            is CourseUpdateCheckResult.Failed -> Log.w(
                LOG_TAG,
                "course update check failed; existing cache remains active: ${result.message}",
            )
        }
    }
}
