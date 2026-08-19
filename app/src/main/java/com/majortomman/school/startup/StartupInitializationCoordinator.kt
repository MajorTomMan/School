package com.majortomman.school.startup

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.majortomman.school.learning.cloud.CourseLibraryRepository
import com.majortomman.school.learning.cloud.CourseSyncManager
import com.majortomman.school.learning.cloud.CourseUpdateCheckResult
import com.majortomman.school.learning.cloud.CourseUpdateOffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Keeps course-cache checks outside the launch-critical path. */
object StartupInitializationCoordinator {
    const val LOG_TAG = "SchoolStartup"
    private const val FIRST_FRAME_GRACE_MILLIS = 750L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val started = AtomicBoolean(false)

    fun start(
        context: Context,
        checkCourseUpdatesOnStartup: Boolean,
        onCourseUpdateAvailable: (CourseUpdateOffer) -> Unit,
    ) {
        if (!started.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        scope.launch {
            delay(FIRST_FRAME_GRACE_MILLIS)
            val count = CourseLibraryRepository.refresh(appContext)
            Log.i(LOG_TAG, "course library ready: installed=$count")
            if (checkCourseUpdatesOnStartup) {
                checkInstalledCourseUpdates(appContext, onCourseUpdateAvailable)
            } else {
                Log.i(LOG_TAG, "startup course update check skipped")
            }
        }
    }

    fun requestCourseUpdateCheck(context: Context, onCourseUpdateAvailable: (CourseUpdateOffer) -> Unit) {
        scope.launch { checkInstalledCourseUpdates(context.applicationContext, onCourseUpdateAvailable) }
    }

    private suspend fun checkInstalledCourseUpdates(appContext: Context, onCourseUpdateAvailable: (CourseUpdateOffer) -> Unit) {
        val textbookIds = CourseLibraryRepository.installedCourseIds()
        if (textbookIds.isEmpty()) {
            Log.i(LOG_TAG, "course update check skipped because no course is installed")
            return
        }
        val startedAt = SystemClock.elapsedRealtime()
        when (val result = CourseSyncManager.checkForUpdates(appContext, textbookIds)) {
            CourseUpdateCheckResult.Disabled -> Log.i(LOG_TAG, "cloud course synchronization is not configured")
            CourseUpdateCheckResult.NotPublished -> Log.i(LOG_TAG, "new course packages are not published yet")
            CourseUpdateCheckResult.NoUpdate -> Log.i(LOG_TAG, "installed course content is current; checked in ${SystemClock.elapsedRealtime() - startedAt} ms")
            is CourseUpdateCheckResult.Available -> {
                Log.i(LOG_TAG, "installed course update available: kind=${result.offer.kind}, bytes=${result.offer.estimatedBytes}")
                withContext(Dispatchers.Main.immediate) { onCourseUpdateAvailable(result.offer) }
            }
            is CourseUpdateCheckResult.Failed -> Log.w(LOG_TAG, "course update check failed; existing cache remains active: ${result.message}")
        }
    }
}
