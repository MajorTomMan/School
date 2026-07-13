package com.majortomman.school.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF9A342D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD5),
    onPrimaryContainer = Color(0xFF3C0906),
    secondary = Color(0xFF725754),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF2A1513),
    tertiary = Color(0xFF526440),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD5E9BE),
    onTertiaryContainer = Color(0xFF111F08),
    background = Color(0xFFF8F5F0),
    onBackground = Color(0xFF211A19),
    surface = Color(0xFFFFFBF8),
    onSurface = Color(0xFF211A19),
    surfaceVariant = Color(0xFFF2DFDC),
    onSurfaceVariant = Color(0xFF534341),
    outline = Color(0xFF857370),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4AC),
    onPrimary = Color(0xFF5C1713),
    primaryContainer = Color(0xFF7B2923),
    onPrimaryContainer = Color(0xFFFFDAD5),
    secondary = Color(0xFFE3BDB8),
    onSecondary = Color(0xFF422825),
    secondaryContainer = Color(0xFF5B3F3C),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFFB9CDA4),
    onTertiary = Color(0xFF263516),
    tertiaryContainer = Color(0xFF3C4C2B),
    onTertiaryContainer = Color(0xFFD5E9BE),
    background = Color(0xFF171312),
    onBackground = Color(0xFFEDE0DD),
    surface = Color(0xFF201A19),
    onSurface = Color(0xFFEDE0DD),
    surfaceVariant = Color(0xFF534341),
    onSurfaceVariant = Color(0xFFD8C2BE),
    outline = Color(0xFFA08C88),
)

private val SchoolTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
        fontWeight = FontWeight.Bold,
    ),
    headlineMedium = TextStyle(
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp,
        fontWeight = FontWeight.Bold,
    ),
    headlineSmall = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleLarge = TextStyle(
        fontSize = 19.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
        fontWeight = FontWeight.Medium,
    ),
)

private val SchoolShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun SchoolTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = SchoolTypography,
        shapes = SchoolShapes,
        content = content,
    )
}
