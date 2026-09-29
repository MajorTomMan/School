package com.majortomman.school.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.data.DisplaySettings

private val SchoolBlue = Color(0xFF1677FF)
private val SchoolBlueDark = Color(0xFF3592FF)
private val ConstructRed = Color(0xFFFF4D52)
private val SuccessGreen = Color(0xFF20A464)
private val DayBackground = Color(0xFFF8F8F5)
private val DaySurface = Color(0xFFFFFFFF)
private val DayInk = Color(0xFF111827)
private val DayMuted = Color(0xFF667085)
private val NightBackground = Color(0xFF08131F)
private val NightSurface = Color(0xFF0E1B2A)
private val NightInk = Color(0xFFF3F6FA)
private val NightMuted = Color(0xFF9AA9BA)

private val LightColors = lightColorScheme(
    primary = SchoolBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F1FF),
    onPrimaryContainer = Color(0xFF0B4DA2),
    secondary = ConstructRed,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE9EA),
    onSecondaryContainer = Color(0xFF8E2025),
    tertiary = SuccessGreen,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE5F6ED),
    onTertiaryContainer = Color(0xFF11613C),
    background = DayBackground,
    onBackground = DayInk,
    surface = DaySurface,
    onSurface = DayInk,
    surfaceVariant = Color(0xFFF0F3F6),
    onSurfaceVariant = DayMuted,
    outline = Color(0xFFD8DEE6),
    error = Color(0xFFD94343),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = SchoolBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF123D70),
    onPrimaryContainer = Color(0xFFD8E9FF),
    secondary = ConstructRed,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF552226),
    onSecondaryContainer = Color(0xFFFFDADB),
    tertiary = Color(0xFF4CC38A),
    onTertiary = Color(0xFF052717),
    tertiaryContainer = Color(0xFF143C2D),
    onTertiaryContainer = Color(0xFFC9F6DE),
    background = NightBackground,
    onBackground = NightInk,
    surface = NightSurface,
    onSurface = NightInk,
    surfaceVariant = Color(0xFF142537),
    onSurfaceVariant = NightMuted,
    outline = Color(0xFF2C3D50),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF2B0707),
)

private val SchoolTypography = Typography(
    displayLarge = TextStyle(fontSize = 42.sp, lineHeight = 48.sp, letterSpacing = (-0.7).sp, fontWeight = FontWeight.Bold),
    displayMedium = TextStyle(fontSize = 36.sp, lineHeight = 43.sp, letterSpacing = (-0.5).sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontSize = 34.sp, lineHeight = 41.sp, letterSpacing = (-0.5).sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 35.sp, letterSpacing = (-0.3).sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 23.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 28.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = 0.4.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.6.sp, fontWeight = FontWeight.Medium),
)

private val SchoolShapes = Shapes(
    extraSmall = RoundedCornerShape(3.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(18.dp),
)

@Composable
fun SchoolTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    textScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val scaledDensity = Density(
        density = density.density,
        fontScale = density.fontScale * textScale.coerceIn(DisplaySettings.MIN_TEXT_SCALE, DisplaySettings.MAX_TEXT_SCALE),
    )
    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = SchoolTypography,
            shapes = SchoolShapes,
            content = content,
        )
    }
}
