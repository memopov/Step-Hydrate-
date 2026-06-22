package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CosmicDarkColorScheme = darkColorScheme(
    primary = LightTealAccent,
    secondary = LightBlueAccent,
    tertiary = EnergyPink,
    background = SlateDark,
    surface = SlateCard,
    onPrimary = SlateDark,
    onSecondary = SlateDark,
    onTertiary = SmoothWhite,
    onBackground = SmoothWhite,
    onSurface = SmoothWhite,
    outline = SoftGray
)

private val CosmicLightColorScheme = lightColorScheme(
    primary = TealAccent,
    secondary = HydrationBlue,
    tertiary = EnergyPink,
    background = SmoothWhite,
    surface = SmoothWhite,
    onPrimary = SmoothWhite,
    onSecondary = SmoothWhite,
    onTertiary = SmoothWhite,
    onBackground = SlateDark,
    onSurface = SlateDark,
    outline = SoftGray
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark theme by default for Cosmic Slate branding
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CosmicDarkColorScheme else CosmicLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
