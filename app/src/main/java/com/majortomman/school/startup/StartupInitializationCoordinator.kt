package com.majortomman.school.startup

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.majortomman.school.data.curriculum.MasteryTrendRepository
import com.majortomman.school.data.material.BundledTextbookCatalogPack
import com.majortomman.school.data.material.PrebuiltTextbookBootstrap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Keeps disk-heavy catalogue and analytics initialization out of the launch-critical main-thread path.
 *
 * The launcher Activity first installs its Compose content. After a short grace period this coordinator
 * performs version-gated prebuilt catalogue installation on Dispatchers.IO, then asks the material
 * repository to refresh. A failed or interrupted installation does not advance the marker and is retried
 * on the next process launch.
 */
object StartupInitializationCoordinator {
    const val LOG_TAG = "SchoolStartup"

    internal const val CURRENT_PREBUILT_VERSION =
        "startup-v2|prebuilt-math-unbound-v1|prebuilt-catalog-unbound-v1"

    private const val PREFERENCES_NAME = "school-startup-initialization"
    private const val KEY_PREBUILT_VERSION = "prebuilt-version"
    private const val FIRST_FRAME_GRACE_MILLIS = 750L
    private const val ANALYTICS_GRACE_MILLIS = 750L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val started = AtomicBoolean(false)

    fun start(
        context: Context,
        onPrebuiltReady: () -> Unit,
    ) {
        if (!started.compareAndSet(false, true)) return
        val appContext = context.applicationContext

        scope.launch {
            delay(FIRST_FRAME_GRACE_MILLIS)
            val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            val completedVersion = preferences.getString(KEY_PREBUILT_VERSION, null)
            if (needsPrebuiltBootstrap(completedVersion)) {
                val startedAt = SystemClock.elapsedRealtime()
                runCatching {
                    PrebuiltTextbookBootstrap.installMissing(appContext)
                    BundledTextbookCatalogPack.installMissing(appContext)
                    check(
                        preferences.edit()
                            .putString(KEY_PREBUILT_VERSION, CURRENT_PREBUILT_VERSION)
                            .commit(),
                    ) { "无法保存预制教材初始化版本。" }
                }.onSuccess {
                    Log.i(
                        LOG_TAG,
                        "prebuilt catalog ready in ${SystemClock.elapsedRealtime() - startedAt} ms",
                    )
                    withContext(Dispatchers.Main.immediate) { onPrebuiltReady() }
                }.onFailure { error ->
                    Log.e(LOG_TAG, "prebuilt catalog initialization failed", error)
                }
            } else {
                Log.i(LOG_TAG, "prebuilt catalog fast path: version already current")
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

    internal fun needsPrebuiltBootstrap(completedVersion: String?): Boolean =
        completedVersion != CURRENT_PREBUILT_VERSION
}
