package com.brokenpip3.gymbro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ForestLightColorScheme =
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

private val ForestDarkColorScheme =
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
    palette: ThemePalette = ThemePalette.Forest,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = colorSchemeFor(palette = palette, darkTheme = darkTheme),
        shapes = GymbroShapes,
        content = content,
    )
}

internal fun colorSchemeFor(
    palette: ThemePalette,
    darkTheme: Boolean,
): ColorScheme =
    when (palette) {
        ThemePalette.Forest -> if (darkTheme) ForestDarkColorScheme else ForestLightColorScheme
        ThemePalette.Ocean -> if (darkTheme) OceanDarkColorScheme else OceanLightColorScheme
        ThemePalette.Sunset -> if (darkTheme) SunsetDarkColorScheme else SunsetLightColorScheme
    }

private val OceanLightColorScheme =
    lightColorScheme(
        primary = Color(0xFF3E5F7E),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD6E4F0),
        onPrimaryContainer = Color(0xFF0F2033),
        secondary = Color(0xFF50646E),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD8E6EC),
        onSecondaryContainer = Color(0xFF0F2129),
        tertiary = Color(0xFF8A4F63),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD9E1),
        onTertiaryContainer = Color(0xFF39112A),
        background = Color(0xFFFAFAFC),
        onBackground = Color(0xFF181C20),
        surface = Color(0xFFFAFAFC),
        onSurface = Color(0xFF181C20),
        surfaceVariant = Color(0xFFDBE1E8),
        onSurfaceVariant = Color(0xFF42474E),
        outline = Color(0xFF73777F),
    )

private val OceanDarkColorScheme =
    darkColorScheme(
        primary = Color(0xFFA9CBEB),
        onPrimary = Color(0xFF0F2A3C),
        primaryContainer = Color(0xFF3E5F7E),
        onPrimaryContainer = Color(0xFFD6E4F0),
        secondary = Color(0xFFB4CBD8),
        onSecondary = Color(0xFF1F333D),
        secondaryContainer = Color(0xFF364E5A),
        onSecondaryContainer = Color(0xFFD8E6EC),
        tertiary = Color(0xFFFFB1C8),
        onTertiary = Color(0xFF541133),
        tertiaryContainer = Color(0xFF6E364B),
        onTertiaryContainer = Color(0xFFFFD9E1),
        background = Color(0xFF101418),
        onBackground = Color(0xFFE1E2E6),
        surface = Color(0xFF101418),
        onSurface = Color(0xFFE1E2E6),
        surfaceVariant = Color(0xFF42474E),
        onSurfaceVariant = Color(0xFFC3C7CF),
        outline = Color(0xFF8D9199),
    )

private val SunsetLightColorScheme =
    lightColorScheme(
        primary = Color(0xFF8B4A24),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDBCA),
        onPrimaryContainer = Color(0xFF391407),
        secondary = Color(0xFF6D563B),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFF5DFC5),
        onSecondaryContainer = Color(0xFF251A04),
        tertiary = Color(0xFF4F6481),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFD6E3F7),
        onTertiaryContainer = Color(0xFF0F1D33),
        background = Color(0xFFFCF8F3),
        onBackground = Color(0xFF1D1A17),
        surface = Color(0xFFFCF8F3),
        onSurface = Color(0xFF1D1A17),
        surfaceVariant = Color(0xFFEDE1D5),
        onSurfaceVariant = Color(0xFF4C453B),
        outline = Color(0xFF7E756A),
    )

private val SunsetDarkColorScheme =
    darkColorScheme(
        primary = Color(0xFFFFB593),
        onPrimary = Color(0xFF54200D),
        primaryContainer = Color(0xFF743A22),
        onPrimaryContainer = Color(0xFFFFDBCA),
        secondary = Color(0xFFE2C097),
        onSecondary = Color(0xFF251A04),
        secondaryContainer = Color(0xFF524327),
        onSecondaryContainer = Color(0xFFF5DFC5),
        tertiary = Color(0xFFB3C8E0),
        onTertiary = Color(0xFF1D3145),
        tertiaryContainer = Color(0xFF354B66),
        onTertiaryContainer = Color(0xFFD6E3F7),
        background = Color(0xFF17130F),
        onBackground = Color(0xFFEFE1D9),
        surface = Color(0xFF17130F),
        onSurface = Color(0xFFEFE1D9),
        surfaceVariant = Color(0xFF4C453B),
        onSurfaceVariant = Color(0xFFD0C5B8),
        outline = Color(0xFF998F82),
    )
