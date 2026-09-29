package com.majortomman.school.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Display preferences allowed by the fixed School design system. */
data class DisplaySettings(
    val textScale: Float = DEFAULT_TEXT_SCALE,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    companion object {
        const val DEFAULT_TEXT_SCALE = 1f
        const val MIN_TEXT_SCALE = 0.90f
        const val MAX_TEXT_SCALE = 1.50f

        fun normalize(value: Float): Float = value.coerceIn(MIN_TEXT_SCALE, MAX_TEXT_SCALE)
    }
}

enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("日间"),
    DARK("夜间"),
}

object DisplayPreferences {
    private const val PREFERENCES_NAME = "school_display"
    private const val KEY_TEXT_SCALE = "text_scale"
    private const val KEY_THEME_MODE = "theme_mode"

    private val mutableState = MutableStateFlow(DisplaySettings())
    val state: StateFlow<DisplaySettings> = mutableState.asStateFlow()

    @Volatile
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            mutableState.value = DisplaySettings(
                textScale = DisplaySettings.normalize(
                    preferences.getFloat(KEY_TEXT_SCALE, DisplaySettings.DEFAULT_TEXT_SCALE),
                ),
                themeMode = preferences.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
                    ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.SYSTEM,
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

    fun setThemeMode(context: Context, mode: ThemeMode) {
        initialize(context)
        preferences(context).edit().putString(KEY_THEME_MODE, mode.name).apply()
        mutableState.value = mutableState.value.copy(themeMode = mode)
    }

    private fun preferences(context: Context) = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}
