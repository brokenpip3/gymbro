package com.brokenpip3.gymbro.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeSelectionTest {
    @Test
    fun forestKeepsTheOriginalColors() {
        val light = colorSchemeFor(ThemePalette.Forest, darkTheme = false)
        val dark = colorSchemeFor(ThemePalette.Forest, darkTheme = true)

        assertEquals(Color(0xFF315C52), light.primary)
        assertEquals(Color(0xFFFAFBF8), light.background)
        assertEquals(Color(0xFF965333), light.tertiary)
        assertEquals(Color(0xFFB8CBC5), dark.primary)
        assertEquals(Color(0xFF111412), dark.background)
    }

    @Test
    fun oceanProvidesDistinctLightAndDarkSchemes() {
        val light = colorSchemeFor(ThemePalette.Ocean, darkTheme = false)
        val dark = colorSchemeFor(ThemePalette.Ocean, darkTheme = true)

        assertEquals(Color(0xFF3E5F7E), light.primary)
        assertEquals(Color(0xFFFAFAFC), light.background)
        assertEquals(Color(0xFF8A4F63), light.tertiary)
        assertEquals(Color(0xFFA9CBEB), dark.primary)
        assertEquals(Color(0xFF101418), dark.background)
    }

    @Test
    fun sunsetProvidesDistinctLightAndDarkSchemes() {
        val light = colorSchemeFor(ThemePalette.Sunset, darkTheme = false)
        val dark = colorSchemeFor(ThemePalette.Sunset, darkTheme = true)

        assertEquals(Color(0xFF8B4A24), light.primary)
        assertEquals(Color(0xFFFCF8F3), light.background)
        assertEquals(Color(0xFF4F6481), light.tertiary)
        assertEquals(Color(0xFFFFB593), dark.primary)
        assertEquals(Color(0xFF17130F), dark.background)
    }
}
