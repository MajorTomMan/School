package com.majortomman.school.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.majortomman.school.data.BackgroundMode
import com.majortomman.school.data.DisplaySettings
import java.io.File

/** Draws the selected preset or private copied image behind the existing dark UI. */
@Composable
fun AppBackgroundHost(
    settings: DisplaySettings,
    content: @Composable () -> Unit,
) {
    val customImage = remember(settings.backgroundMode, settings.customImagePath) {
        settings.customImagePath
            ?.takeIf { settings.backgroundMode == BackgroundMode.CUSTOM }
            ?.let(::File)
            ?.takeIf(File::isFile)
            ?.let { file -> runCatching { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }.getOrNull() }
    }
    val presetColor = Color(settings.backgroundPreset.argb)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(presetColor),
    ) {
        customImage?.let { image ->
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        if (customImage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.62f)),
            )
        }
        content()
    }
}
