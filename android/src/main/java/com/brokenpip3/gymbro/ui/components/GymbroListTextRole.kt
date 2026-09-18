package com.brokenpip3.gymbro.ui.components

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

internal enum class GymbroListTextRole {
    Title,
    Subtitle,
    Metadata,
    Notes,
    Status,
    Value,
    Stat,
}

internal fun GymbroListTextRole.color(colors: ColorScheme): Color =
    when (this) {
        GymbroListTextRole.Title -> colors.onSurface
        GymbroListTextRole.Subtitle -> colors.onSurfaceVariant
        GymbroListTextRole.Metadata -> colors.outline
        GymbroListTextRole.Notes -> colors.tertiary
        GymbroListTextRole.Status -> colors.primary
        GymbroListTextRole.Value -> colors.secondary
        GymbroListTextRole.Stat -> colors.secondary
    }
