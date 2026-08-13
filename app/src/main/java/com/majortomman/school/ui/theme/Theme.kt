package com.majortomman.school.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.data.DisplayPreferences
import com.majortomman.school.data.DisplaySettings
import com.majortomman.school.ui.AppBackgroundHost

private val MinimalBlack = Color(0xFF000000)
private val MinimalWhite = Color(0xFFF5F5F7)
private val MinimalBlue = Color(0xFF0A84FF)
private val MinimalRed = Color(0xFFFF453A)
private val MinimalYellow = Color(0xFFFFD60A)

private val MinimalColors = darkColorScheme(
    primary = MinimalBlue,
    onPrimary = MinimalWhite,
    primaryContainer = Color(0xFF001D3A),
    onPrimaryContainer = MinimalWhite,
    secondary = MinimalYellow,
    onSecondary = MinimalBlack,
    secondaryContainer = Color(0xFF211C00),
    onSecondaryContainer = MinimalWhite,
    tertiary = MinimalRed,
    onTertiary = MinimalWhite,
    tertiaryContainer = Color(0xFF2B0806),
    onTertiaryContainer = MinimalWhite,
    background = Color.Transparent,
    onBackground = MinimalWhite,
    surface = Color.Transparent,
    onSurface = MinimalWhite,
    surfaceVariant = Color(0xCC0B0B0D),
    onSurfaceVariant = Color(0xFFA1A1A6),
    outline = Color(0xFF3A3A3C),
    error = MinimalRed,
    onError = MinimalWhite,
)

/** Canonical typography for every screen. User text scaling is applied once through LocalDensity. */
private val SchoolTypography = Typography(
    displayLarge = TextStyle(fontSize = 42.sp, lineHeight = 48.sp, letterSpacing = (-0.7).sp, fontWeight = FontWeight.Bold),
    displayMedium = TextStyle(fontSize = 36.sp, lineHeight = 43.sp, letterSpacing = (-0.5).sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontSize = 34.sp, lineHeight = 41.sp, letterSpacing = (-0.5).sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 35.sp, letterSpacing = (-0.3).sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 23.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 28.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = 0.1.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.1.sp, fontWeight = FontWeight.Medium),
)

private val MinimalShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(10.dp),
)

@Composable
fun SchoolTheme(
    darkTheme: Boolean = true,
    textScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val displaySettings by DisplayPreferences.state.collectAsState(initial = DisplaySettings())
    val density = LocalDensity.current
    val scaledDensity = Density(
        density = density.density,
        fontScale = density.fontScale * textScale.coerceIn(DisplaySettings.MIN_TEXT_SCALE, DisplaySettings.MAX_TEXT_SCALE),
    )
    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialTheme(
            colorScheme = MinimalColors,
            typography = SchoolTypography,
            shapes = MinimalShapes,
        ) {
            AppBackgroundHost(settings = displaySettings, content = content)
        }
    }
}
