package com.brokenpip3.gymbro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val GymbroLightColorScheme =
    lightColorScheme(
        primary = Color(0xFF315C52),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD4E7E1),
        onPrimaryContainer = Color(0xFF0B211C),
        secondary = Color(0xFF3E617F),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD8E8F5),
        onSecondaryContainer = Color(0xFF102333),
        tertiary = Color(0xFF965333),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDBCA),
        onTertiaryContainer = Color(0xFF391407),
        background = Color(0xFFFAFBF8),
        onBackground = Color(0xFF191C1A),
        surface = Color(0xFFFAFBF8),
        onSurface = Color(0xFF191C1A),
        surfaceVariant = Color(0xFFDDE5DF),
        onSurfaceVariant = Color(0xFF414944),
        outline = Color(0xFF727A75),
    )

private val GymbroDarkColorScheme =
    darkColorScheme(
        primary = Color(0xFFB8CBC5),
        onPrimary = Color(0xFF203C36),
        primaryContainer = Color(0xFF315C52),
        onPrimaryContainer = Color(0xFFD4E7E1),
        secondary = Color(0xFFA8C9E6),
        onSecondary = Color(0xFF102D43),
        secondaryContainer = Color(0xFF29485F),
        onSecondaryContainer = Color(0xFFD8EAF9),
        tertiary = Color(0xFFFFB596),
        onTertiary = Color(0xFF54200D),
        tertiaryContainer = Color(0xFF743A22),
        onTertiaryContainer = Color(0xFFFFDBCA),
        background = Color(0xFF111412),
        onBackground = Color(0xFFE1E4E0),
        surface = Color(0xFF111412),
        onSurface = Color(0xFFE1E4E0),
        surfaceVariant = Color(0xFF414944),
        onSurfaceVariant = Color(0xFFC1C9C3),
        outline = Color(0xFF8B948E),
    )

private val GymbroShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(20.dp),
    )

@Composable
fun GymbroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) GymbroDarkColorScheme else GymbroLightColorScheme,
        shapes = GymbroShapes,
        content = content,
    )
}
