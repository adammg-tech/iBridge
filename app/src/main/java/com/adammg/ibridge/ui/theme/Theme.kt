package com.adammg.ibridge.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val BlueDarkColorScheme = darkColorScheme(
    primary = BluePrimary,
    onPrimary = BlueOnPrimary,
    primaryContainer = BluePrimaryContainer,
    onPrimaryContainer = BlueOnPrimaryContainer,
    secondary = BlueSecondary,
    onSecondary = BlueOnSecondary,
    secondaryContainer = BlueSecondaryContainer,
    onSecondaryContainer = BlueOnSecondaryContainer,
    tertiary = BlueTertiary,
    onTertiary = BlueOnTertiary,
    tertiaryContainer = BlueTertiaryContainer,
    onTertiaryContainer = BlueOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

private val BlueLightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = BlueOnPrimary,
    secondary = BlueSecondary,
    onSecondary = BlueOnSecondary,
    tertiary = BlueTertiary,
    onTertiary = BlueOnTertiary
)

@Composable
fun IBridgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> BlueDarkColorScheme
        else -> BlueDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun IBridgeV2Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    IBridgeTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
