
package com.example.finora.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = FinoraGreen,
    onPrimary = FinoraTextOnGreen,
    primaryContainer = FinoraGreenLight,
    onPrimaryContainer = FinoraGreenDark,

    secondary = FinoraGreenDark,
    onSecondary = FinoraTextOnGreen,
    secondaryContainer = FinoraMint,
    onSecondaryContainer = FinoraTextPrimary,

    tertiary = FinoraBlue,
    onTertiary = FinoraTextOnGreen,

    background = FinoraBackground,
    onBackground = FinoraTextPrimary,

    surface = FinoraSurface,
    onSurface = FinoraTextPrimary,

    surfaceVariant = FinoraSurfaceVariant,
    onSurfaceVariant = FinoraTextSecondary,

    error = FinoraExpense,
    onError = FinoraTextOnGreen
)

private val DarkColorScheme = darkColorScheme(
    primary = FinoraMint,
    onPrimary = FinoraDarkBackground,
    primaryContainer = FinoraGreenDark,
    onPrimaryContainer = FinoraMint,

    secondary = FinoraGreenLight,
    onSecondary = FinoraDarkBackground,
    secondaryContainer = FinoraDarkSurfaceVariant,
    onSecondaryContainer = FinoraDarkTextPrimary,

    tertiary = FinoraBlue,
    onTertiary = FinoraTextOnGreen,

    background = FinoraDarkBackground,
    onBackground = FinoraDarkTextPrimary,

    surface = FinoraDarkSurface,
    onSurface = FinoraDarkTextPrimary,

    surfaceVariant = FinoraDarkSurfaceVariant,
    onSurfaceVariant = FinoraDarkTextSecondary,

    error = FinoraExpense,
    onError = FinoraTextOnGreen
)

@Composable
fun FinoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            // Finora uses its own brand colors by default.
            // Set dynamicColor = true only if system colors are desired.
            if (darkTheme) DarkColorScheme else LightColorScheme
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
