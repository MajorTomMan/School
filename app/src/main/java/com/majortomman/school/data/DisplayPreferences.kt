package com.majortomman.school.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI display preferences shared by the whole application.
 * Text scale affects every Compose sp value through SchoolTheme's LocalDensity provider.
 */
data class DisplaySettings(
    val textScale: Float = DEFAULT_TEXT_SCALE,
) {
    companion object {
        const val DEFAULT_TEXT_SCALE = 1f
        const val MIN_TEXT_SCALE = 0.90f
        const val MAX_TEXT_SCALE = 1.50f

        fun normalize(value: Float): Float = value.coerceIn(MIN_TEXT_SCALE, MAX_TEXT_SCALE)
    }
}

object DisplayPreferences {
    private const val PREFERENCES_NAME = "school_display"
    private const val KEY_TEXT_SCALE = "text_scale"

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
  )
  initialized = true
        }
    }

    fun setTextScale(context: Context, scale: Float) {
        initialize(context)
        val normalized = DisplaySettings.normalize(scale)
        context.applicationContext
  .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
  .edit()
  .putFloat(KEY_TEXT_SCALE, normalized)
  .apply()
        mutableState.value = mutableState.value.copy(textScale = normalized)
    }
}
