package com.tasknotes.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary              = Color(0xFF4F46E5),
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFE8E6FF),
    onPrimaryContainer   = Color(0xFF10006B),
    secondary            = Color(0xFF6B7280),
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFE5E7EB),
    onSecondaryContainer = Color(0xFF374151),
    background           = Color(0xFFF9FAFB),
    onBackground         = Color(0xFF1A1A2E),
    surface              = Color.White,
    onSurface            = Color(0xFF1A1A2E),
    surfaceVariant       = Color(0xFFF3F4F6),
    onSurfaceVariant     = Color(0xFF6B7280),
    outline              = Color(0xFFD1D5DB),
    error                = Color(0xFFDC2626),
    onError              = Color.White,
    errorContainer       = Color(0xFFFFE4E4),
    onErrorContainer     = Color(0xFF7F1D1D),
)

private val DarkColorScheme = darkColorScheme(
    primary              = Color(0xFF818CF8),
    onPrimary            = Color(0xFF1E1B4B),
    primaryContainer     = Color(0xFF312E81),
    onPrimaryContainer   = Color(0xFFE0E7FF),
    secondary            = Color(0xFF9CA3AF),
    onSecondary          = Color(0xFF1F2937),
    secondaryContainer   = Color(0xFF374151),
    onSecondaryContainer = Color(0xFFD1D5DB),
    background           = Color(0xFF111827),
    onBackground         = Color(0xFFF9FAFB),
    surface              = Color(0xFF1F2937),
    onSurface            = Color(0xFFF3F4F6),
    surfaceVariant       = Color(0xFF374151),
    onSurfaceVariant     = Color(0xFF9CA3AF),
    outline              = Color(0xFF4B5563),
    error                = Color(0xFFF87171),
    onError              = Color(0xFF7F1D1D),
    errorContainer       = Color(0xFF991B1B),
    onErrorContainer     = Color(0xFFFEE2E2),
)

@Composable
fun TaskNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content     = content
    )
}
