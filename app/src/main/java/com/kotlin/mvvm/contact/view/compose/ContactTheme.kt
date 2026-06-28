package com.kotlin.mvvm.contact.view.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// "Quiet Native" palette: system-native restraint, one warm orange reserved for tap targets.
// primary = orange fills (avatar, FAB, focus, progress). tertiary = orange for text/icons on
// light surfaces (deeper, passes AA on white). All other surfaces come from neutral tokens.
private val LightColors = lightColorScheme(
    primary = Color(0xFFFF8C00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE2BD),
    onPrimaryContainer = Color(0xFF4D2600),
    tertiary = Color(0xFFE0760A),
    onTertiary = Color.White,
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF1C1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFE3E3E8),
    onSurfaceVariant = Color(0xFF8E8E93),
    outlineVariant = Color(0xFFE5E5EA)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF9F0A),
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFF3A2A12),
    onPrimaryContainer = Color(0xFFFFDDB8),
    tertiary = Color(0xFFFF9F0A),
    onTertiary = Color(0xFF1A1A1A),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF2F2F7),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF8E8E93),
    outlineVariant = Color(0xFF38383A)
)

@Composable
fun ContactUsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
