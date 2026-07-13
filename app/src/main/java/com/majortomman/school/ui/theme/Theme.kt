package com.majortomman.school.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TechBlack = Color(0xFF050608)
private val TechWhite = Color(0xFFF5F7FA)
private val TechBlue = Color(0xFF2D7BFF)
private val TechRed = Color(0xFFFF3B30)
private val TechYellow = Color(0xFFFFCC00)

private val TechColors = darkColorScheme(
    primary = TechBlue,
    onPrimary = TechWhite,
    primaryContainer = Color(0xFF0A2552),
    onPrimaryContainer = TechWhite,
    secondary = TechYellow,
    onSecondary = TechBlack,
    secondaryContainer = Color(0xFF3A3000),
    onSecondaryContainer = TechWhite,
    tertiary = TechRed,
    onTertiary = TechWhite,
    tertiaryContainer = Color(0xFF48110E),
    onTertiaryContainer = TechWhite,
    background = TechBlack,
    onBackground = TechWhite,
    surface = Color(0xFF090B0F),
    onSurface = TechWhite,
    surfaceVariant = Color(0xFF11151C),
    onSurfaceVariant = Color(0xFFB8C0CC),
    outline = Color(0xFF56606E),
    error = TechRed,
    onError = TechWhite,
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
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

@Composable
fun SchoolTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = TechColors,
        typography = SchoolTypography,
        shapes = SchoolShapes,
        content = content,
    )
}
