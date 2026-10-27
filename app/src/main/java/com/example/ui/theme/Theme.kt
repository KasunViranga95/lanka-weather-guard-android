package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalIsDarkTheme = staticCompositionLocalOf { false }

private val DarkWeatherColorScheme = darkColorScheme(
    primary = Color(0xFF4599FF),
    onPrimary = Color.White,
    primaryContainer = NavyElevated,
    onPrimaryContainer = CloudWhite,
    secondary = Color(0xFF2ABBA7),
    onSecondary = Color(0xFF00281F),
    secondaryContainer = Color(0xFF1F3834),
    onSecondaryContainer = CloudWhite,
    tertiary = WarningAmber,
    onTertiary = Color.White,
    error = AlertRed,
    onError = Color.White,
    background = NavyAbyss,
    onBackground = CloudWhite,
    surface = NavySurface,
    onSurface = CloudWhite,
    surfaceVariant = NavyElevated,
    onSurfaceVariant = MistSilver,
    outline = NavyCardBorder
)

private val LightWeatherColorScheme = lightColorScheme(
    primary = Color(0xFF1877F2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7F3FF),
    onPrimaryContainer = Color(0xFF050505),
    secondary = Color(0xFF00796B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2F1),
    onSecondaryContainer = Color(0xFF00251E),
    tertiary = Color(0xFFE65100),
    onTertiary = Color.White,
    error = AlertRed,
    onError = Color.White,
    background = LightSkyBackground,
    onBackground = DeepInk,
    surface = LightCardSurface,
    onSurface = DeepInk,
    surfaceVariant = LightElevatedSurface,
    onSurfaceVariant = SlateMuted,
    outline = LightCardBorder
)

@Composable
fun LankaWeatherGuardTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkWeatherColorScheme else LightWeatherColorScheme

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
