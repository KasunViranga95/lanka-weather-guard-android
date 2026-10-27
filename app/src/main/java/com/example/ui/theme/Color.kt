package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Facebook-Inspired Dark Mode Palette (Clean, High-Contrast Dark Surfaces)
val NavyAbyss = Color(0xFF18191A)       // Facebook Dark Feed Background
val NavySurface = Color(0xFF242526)     // Facebook Dark Card Surface
val NavyElevated = Color(0xFF3A3B3C)    // Facebook Dark Pill / Inner Card Surface
val NavyCardBorder = Color(0xFF3E4042)  // Subtle Dark Divider / Border

// Facebook-Inspired Light Mode Palette (Clean #F0F2F5 Feed & Crisp White Cards)
val LightSkyBackground = Color(0xFFF0F2F5) // Facebook Light Feed Background
val LightCardSurface = Color(0xFFFFFFFF)   // Crisp White Card Surface
val LightElevatedSurface = Color(0xFFF7F8FA) // Subtle Gray Inner Tile Surface
val LightCardBorder = Color(0xFFDADDE1)    // Clean Facebook Card Border / Divider

// Primary Accents (Facebook Blue & Teal across Light and Dark themes)
val RadarCyan = Color(0xFF1877F2)
val MonsoonTeal = Color(0xFF00897B)

// Alert & Hydrology Status Colors
val AlertRed = Color(0xFFE41E3F)
val WarningAmber = Color(0xFFF57C00)
val AdvisoryYellow = Color(0xFFD99B00)
val SafeEmerald = Color(0xFF2E7D32)

// Typography & Neutral Tones
val CloudWhite = Color(0xFFE4E6EB)
val MistSilver = Color(0xFFB0B3B8)
val SlateMuted = Color(0xFF65676B)
val DeepInk = Color(0xFF050505)

// Theme-aware adaptive color accessors for screens and components
val AdaptiveAppBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) NavyAbyss else LightSkyBackground

val AdaptiveSurfaceColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) NavySurface else LightCardSurface

val AdaptiveElevatedColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) NavyElevated else LightElevatedSurface

val AdaptiveBorderColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) NavyCardBorder else LightCardBorder

val AdaptivePrimaryText: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) CloudWhite else DeepInk

val AdaptiveSecondaryText: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) MistSilver else SlateMuted

val AdaptiveCyanAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFF4599FF) else Color(0xFF1877F2)

val AdaptiveTealAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFF2ABBA7) else Color(0xFF00796B)
