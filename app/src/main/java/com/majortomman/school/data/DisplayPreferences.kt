package com.majortomman.school.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** UI display preferences shared by the whole application. */
data class DisplaySettings(
    val textScale: Float = DEFAULT_TEXT_SCALE,
    val backgroundMode: BackgroundMode = BackgroundMode.PRESET,
    val backgroundPreset: BackgroundPreset = BackgroundPreset.BLACK,
    val customImagePath: String? = null,
) {
    companion object {
        const val DEFAULT_TEXT_SCALE = 1f
        const val MIN_TEXT_SCALE = 0.90f
        const val MAX_TEXT_SCALE = 1.50f

        fun normalize(value: Float): Float = value.coerceIn(MIN_TEXT_SCALE, MAX_TEXT_SCALE)
    }
}

enum class BackgroundMode {
    PRESET,
    CUSTOM,
}

enum class BackgroundPreset(
    val id: String,
    val label: String,
    val argb: Long,
) {
    BLACK("black", "纯黑", 0xFF000000),
    GRAPHITE("graphite", "石墨黑", 0xFF111317),
    NAVY("navy", "深蓝黑", 0xFF07111F),
    WARM_BLACK("warm_black", "暖黑", 0xFF17110F),
    FOREST("forest", "墨绿黑", 0xFF071511),
    ;

    companion object {
        fun fromId(id: String?): BackgroundPreset = entries.firstOrNull { it.id == id } ?: BLACK
    }
}

sealed interface BackgroundImportResult {
    data class Success(val path: String) : BackgroundImportResult
    data class Failure(val message: String) : BackgroundImportResult
}

internal object BackgroundImagePolicy {
    private val supportedMimeTypes = setOf("image/jpeg", "image/png", "image/webp")

    fun validate(
        width: Int,
        height: Int,
        fileSize: Long,
        declaredMimeType: String?,
        decodedMimeType: String?,
    ): String? {
        if (width <= 0 || height <= 0) return "无法读取图片尺寸，文件可能已损坏"
        val shortSide = minOf(width, height)
        val longSide = maxOf(width, height)
        if (shortSide < MIN_SHORT_SIDE || longSide < MIN_LONG_SIDE) {
            return "图片分辨率过低，至少需要短边 ${MIN_SHORT_SIDE}px、长边 ${MIN_LONG_SIDE}px"
        }
        if (width.toLong() * height.toLong() > MAX_PIXEL_COUNT) {
            return "图片像素总量过高，请使用不超过 3200 万像素的图片"
        }
        if (fileSize > MAX_FILE_BYTES) return "图片文件过大，请选择不超过 20 MB 的图片"

        val actual = decodedMimeType?.lowercase()
        if (actual !in supportedMimeTypes) return "仅支持 JPEG、PNG 和 WebP 图片"
        val declared = declaredMimeType?.lowercase()
        if (declared != null && declared != "image/*" && declared !in supportedMimeTypes) {
            return "所选文件声明的格式不受支持"
        }
        if (declared != null && declared != "image/*" && declared != actual) {
            return "图片扩展格式与实际文件内容不一致"
        }
        return null
    }

    const val MIN_SHORT_SIDE = 720
    const val MIN_LONG_SIDE = 1280
    const val MAX_FILE_BYTES = 20L * 1024L * 1024L
    const val MAX_PIXEL_COUNT = 32_000_000L
    const val MAX_STORED_LONG_SIDE = 2560
}

object DisplayPreferences {
    private const val PREFERENCES_NAME = "school_display"
    private const val KEY_TEXT_SCALE = "text_scale"
    private const val KEY_BACKGROUND_MODE = "background_mode"
    private const val KEY_BACKGROUND_PRESET = "background_preset"
    private const val KEY_CUSTOM_IMAGE_PATH = "custom_image_path"
    private const val BACKGROUND_DIRECTORY = "display-backgrounds"

    private val mutableState = MutableStateFlow(DisplaySettings())
    val state: StateFlow<DisplaySettings> = mutableState.asStateFlow()

    @Volatile
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            val storedPath = preferences.getString(KEY_CUSTOM_IMAGE_PATH, null)
                ?.takeIf { File(it).isFile }
            val storedMode = preferences.getString(KEY_BACKGROUND_MODE, null)
                ?.let { runCatching { BackgroundMode.valueOf(it) }.getOrNull() }
                ?: BackgroundMode.PRESET
            mutableState.value = DisplaySettings(
                textScale = DisplaySettings.normalize(
                    preferences.getFloat(KEY_TEXT_SCALE, DisplaySettings.DEFAULT_TEXT_SCALE),
                ),
                backgroundMode = if (storedMode == BackgroundMode.CUSTOM && storedPath == null) {
                    BackgroundMode.PRESET
                } else {
                    storedMode
                },
                backgroundPreset = BackgroundPreset.fromId(
                    preferences.getString(KEY_BACKGROUND_PRESET, BackgroundPreset.BLACK.id),
                ),
                customImagePath = storedPath,
            )
            initialized = true
        }
    }

    fun setTextScale(context: Context, scale: Float) {
        initialize(context)
        val normalized = DisplaySettings.normalize(scale)
        preferences(context).edit().putFloat(KEY_TEXT_SCALE, normalized).apply()
        mutableState.value = mutableState.value.copy(textScale = normalized)
    }

    fun setBackgroundPreset(context: Context, preset: BackgroundPreset) {
        initialize(context)
        preferences(context).edit()
            .putString(KEY_BACKGROUND_MODE, BackgroundMode.PRESET.name)
            .putString(KEY_BACKGROUND_PRESET, preset.id)
            .apply()
        mutableState.value = mutableState.value.copy(
            backgroundMode = BackgroundMode.PRESET,
            backgroundPreset = preset,
        )
    }

    fun useExistingCustomBackground(context: Context): Boolean {
        initialize(context)
        val path = mutableState.value.customImagePath?.takeIf { File(it).isFile } ?: return false
        preferences(context).edit()
            .putString(KEY_BACKGROUND_MODE, BackgroundMode.CUSTOM.name)
            .putString(KEY_CUSTOM_IMAGE_PATH, path)
            .apply()
        mutableState.value = mutableState.value.copy(backgroundMode = BackgroundMode.CUSTOM)
        return true
    }

    suspend fun importCustomBackground(context: Context, uri: Uri): BackgroundImportResult =
        withContext(Dispatchers.IO) {
            initialize(context)
            val appContext = context.applicationContext
            val resolver = appContext.contentResolver
            val declaredMime = resolver.getType(uri)
            val fileSize = runCatching {
                resolver.openAssetFileDescriptor(uri, "r")?.use { descriptor -> descriptor.length }
            }.getOrNull()?.takeIf { it >= 0L } ?: -1L

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val boundsRead = runCatching {
                resolver.openInputStream(uri)?.use { input -> BitmapFactory.decodeStream(input, null, bounds) }
            }.isSuccess
            if (!boundsRead) return@withContext BackgroundImportResult.Failure("无法读取图片，请检查文件访问权限")

            val validationError = BackgroundImagePolicy.validate(
                width = bounds.outWidth,
                height = bounds.outHeight,
                fileSize = fileSize,
                declaredMimeType = declaredMime,
                decodedMimeType = bounds.outMimeType,
            )
            if (validationError != null) return@withContext BackgroundImportResult.Failure(validationError)

            val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = runCatching {
                resolver.openInputStream(uri)?.use { input -> BitmapFactory.decodeStream(input, null, decodeOptions) }
            }.getOrNull() ?: return@withContext BackgroundImportResult.Failure("图片解码失败，文件可能已损坏")

            val scaled = scaleForStorage(decoded)
            if (scaled !== decoded) decoded.recycle()
            val directory = File(appContext.filesDir, BACKGROUND_DIRECTORY).apply { mkdirs() }
            val hasAlpha = scaled.hasAlpha()
            val extension = if (hasAlpha) "png" else "jpg"
            val finalFile = File(directory, "background-${System.currentTimeMillis()}.$extension")
            val temporary = File(directory, ".${finalFile.name}.tmp")
            val written = runCatching {
                temporary.outputStream().buffered().use { output ->
                    val format = if (hasAlpha) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                    require(scaled.compress(format, 91, output)) { "图片压缩失败" }
                }
                require(temporary.length() > 0L) { "背景文件为空" }
                require(temporary.renameTo(finalFile)) { "无法保存背景文件" }
            }
            scaled.recycle()
            if (written.isFailure) {
                temporary.delete()
                finalFile.delete()
                return@withContext BackgroundImportResult.Failure(
                    written.exceptionOrNull()?.message ?: "背景图片保存失败",
                )
            }

            val previousPath = mutableState.value.customImagePath
            preferences(appContext).edit()
                .putString(KEY_BACKGROUND_MODE, BackgroundMode.CUSTOM.name)
                .putString(KEY_CUSTOM_IMAGE_PATH, finalFile.absolutePath)
                .apply()
            mutableState.value = mutableState.value.copy(
                backgroundMode = BackgroundMode.CUSTOM,
                customImagePath = finalFile.absolutePath,
            )
            previousPath
                ?.takeIf { it != finalFile.absolutePath }
                ?.let(::File)
                ?.delete()
            BackgroundImportResult.Success(finalFile.absolutePath)
        }

    private fun preferences(context: Context) = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (maxOf(width / sample, height / sample) > BackgroundImagePolicy.MAX_STORED_LONG_SIDE * 2) {
            sample *= 2
        }
        return sample
    }

    private fun scaleForStorage(bitmap: Bitmap): Bitmap {
        val longSide = maxOf(bitmap.width, bitmap.height)
        if (longSide <= BackgroundImagePolicy.MAX_STORED_LONG_SIDE) return bitmap
        val ratio = BackgroundImagePolicy.MAX_STORED_LONG_SIDE.toFloat() / longSide.toFloat()
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
    }
}
