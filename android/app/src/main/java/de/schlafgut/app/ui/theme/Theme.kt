package de.schlafgut.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SchlafGutColorScheme = darkColorScheme(
    primary = Dream500,
    onPrimary = TextPrimary,
    primaryContainer = Dream600,
    onPrimaryContainer = Dream300,
    secondary = MedianWakeColor,
    onSecondary = TextPrimary,
    background = Night900,
    onBackground = TextPrimary,
    surface = Night800,
    onSurface = TextPrimary,
    surfaceVariant = Night700,
    onSurfaceVariant = TextSecondary,
    outline = Night600,
    error = DangerRed,
    onError = TextPrimary
)

@Composable
fun SchlafGutTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SchlafGutColorScheme,
        typography = SchlafGutTypography,
        content = content
    )
}
