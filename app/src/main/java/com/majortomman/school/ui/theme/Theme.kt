package com.majortomman.school.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF8D302B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF3B0807),
    secondary = Color(0xFF755653),
    background = Color(0xFFFAF8F5),
    surface = Color(0xFFFFFBFF),
    surfaceVariant = Color(0xFFF4DDDA),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4AC),
    onPrimary = Color(0xFF561E1A),
    primaryContainer = Color(0xFF73332E),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFE5BDB8),
    background = Color(0xFF151311),
    surface = Color(0xFF1D1B19),
    surfaceVariant = Color(0xFF534341),
)

@Composable
fun SchoolTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
