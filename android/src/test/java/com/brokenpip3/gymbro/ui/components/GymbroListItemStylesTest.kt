package com.brokenpip3.gymbro.ui.components

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class GymbroListItemStylesTest {
    @Test
    fun rolesUseDistinctThemeTokensForScanningHierarchy() {
        val colors =
            lightColorScheme(
                onSurface = Color.Black,
                primary = Color.Red,
                onSurfaceVariant = Color.Blue,
                secondary = Color.Green,
                tertiary = Color.Yellow,
                outline = Color.Magenta,
            )

        assertEquals(Color.Black, GymbroListTextRole.Title.color(colors))
        assertEquals(Color.Blue, GymbroListTextRole.Subtitle.color(colors))
        assertEquals(Color.Magenta, GymbroListTextRole.Metadata.color(colors))
        assertEquals(Color.Yellow, GymbroListTextRole.Notes.color(colors))
        assertEquals(Color.Green, GymbroListTextRole.Value.color(colors))
        assertEquals(Color.Green, GymbroListTextRole.Stat.color(colors))
    }
}
