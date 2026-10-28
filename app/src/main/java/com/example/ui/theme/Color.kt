package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Dark palette: soft charcoal background with slightly lifted, borderless cards
val NavyAbyss = Color(0xFF111318)
val NavySurface = Color(0xFF1D2026)
val NavyElevated = Color(0xFF2A2E36)
val NavyCardBorder = Color(0xFF2F333B)

// Light palette: pale blue-grey background with white, borderless cards
val LightSkyBackground = Color(0xFFF1F4F9)
val LightCardSurface = Color(0xFFFFFFFF)
val LightElevatedSurface = Color(0xFFEEF2F8)
val LightCardBorder = Color(0xFFE1E5EC)

// Primary accents
val RadarCyan = Color(0xFF1A73E8)
val MonsoonTeal = Color(0xFF00897B)

// Alert & Hydrology Status Colors
val AlertRed = Color(0xFFE41E3F)
val WarningAmber = Color(0xFFF57C00)
val AdvisoryYellow = Color(0xFFD99B00)
val SafeEmerald = Color(0xFF2E7D32)

// Typography & Neutral Tones
val CloudWhite = Color(0xFFE3E5EA)
val MistSilver = Color(0xFFA9AFB9)
val SlateMuted = Color(0xFF5F6673)
val DeepInk = Color(0xFF1B1D22)

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
    get() = if (LocalIsDarkTheme.current) Color(0xFF8AB4F8) else Color(0xFF1A73E8)

val AdaptiveTealAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFF2ABBA7) else Color(0xFF00796B)
