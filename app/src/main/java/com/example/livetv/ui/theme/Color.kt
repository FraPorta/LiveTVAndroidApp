package com.example.livetv.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ── Orange (#F97316) seed — tonal palette key tones ──────────────────────────
val Orange80    = Color(0xFFFFB787)   // primary / inversePrimary
val Orange20    = Color(0xFF5A1A00)   // onPrimary
val Orange30    = Color(0xFF7B2800)   // primaryContainer
val Orange90    = Color(0xFFFFDBCA)   // onPrimaryContainer / primaryContainer light

val WarmBlue80  = Color(0xFFB8CAEA)   // tertiary
val WarmBlue20  = Color(0xFF223344)   // onTertiary
val WarmBlue30  = Color(0xFF3A4D5F)   // tertiaryContainer
val WarmBlue90  = Color(0xFFD5E5FF)   // onTertiaryContainer

val Neutral6    = Color(0xFF17120E)   // surface / background (dark)
val Neutral10   = Color(0xFF1E160F)   // surfaceContainerLow
val Neutral12   = Color(0xFF221A13)   // surfaceContainer
val Neutral17   = Color(0xFF2B2019)   // surfaceContainerHigh
val Neutral22   = Color(0xFF352A22)   // surfaceContainerHighest
val Neutral24   = Color(0xFF3A2E24)   // surfaceBright
val Neutral90   = Color(0xFFF0DDD1)   // onSurface / onBackground (dark)

val NeutralVar30 = Color(0xFF52392D)  // surfaceVariant / outlineVariant
val NeutralVar80 = Color(0xFFD5C1B7)  // onSurfaceVariant

// ── Dark color scheme — used on API < 31 or when dynamic colors unavailable ──
val LiveTVDarkColors = darkColorScheme(
    primary                = Orange80,
    onPrimary              = Orange20,
    primaryContainer       = Orange30,
    onPrimaryContainer     = Orange90,

    secondary              = Color(0xFFE8BEA0),
    onSecondary            = Color(0xFF432E1B),
    secondaryContainer     = Color(0xFF5C3F29),
    onSecondaryContainer   = Orange90,

    tertiary               = WarmBlue80,
    onTertiary             = WarmBlue20,
    tertiaryContainer      = WarmBlue30,
    onTertiaryContainer    = WarmBlue90,

    error                  = Color(0xFFFFB4AB),
    onError                = Color(0xFF690005),
    errorContainer         = Color(0xFF93000A),
    onErrorContainer       = Color(0xFFFFDAD6),

    background             = Neutral6,
    onBackground           = Neutral90,

    surface                = Neutral6,
    onSurface              = Neutral90,
    surfaceVariant         = NeutralVar30,
    onSurfaceVariant       = NeutralVar80,

    surfaceContainerLowest = Color(0xFF110D0A),
    surfaceContainerLow    = Neutral10,
    surfaceContainer       = Neutral12,
    surfaceContainerHigh   = Neutral17,
    surfaceContainerHighest = Neutral22,
    surfaceBright          = Neutral24,
    surfaceDim             = Neutral6,

    outline                = Color(0xFF9C8070),
    outlineVariant         = NeutralVar30,
    scrim                  = Color.Black,

    inverseSurface         = Neutral90,
    inverseOnSurface       = Color(0xFF38261B),
    inversePrimary         = Color(0xFF8B3C00),
)

// ── Light color scheme — used when system is in light mode on API < 31 ───────
val LiveTVLightColors = lightColorScheme(
    primary                = Color(0xFF8B3C00),
    onPrimary              = Color.White,
    primaryContainer       = Orange90,
    onPrimaryContainer     = Color(0xFF2F1200),

    secondary              = Color(0xFF7B5438),
    onSecondary            = Color.White,
    secondaryContainer     = Orange90,
    onSecondaryContainer   = Color(0xFF301A08),

    tertiary               = Color(0xFF516179),
    onTertiary             = Color.White,
    tertiaryContainer      = WarmBlue90,
    onTertiaryContainer    = Color(0xFF0B1D2D),

    error                  = Color(0xFFBA1A1A),
    onError                = Color.White,
    errorContainer         = Color(0xFFFFDAD6),
    onErrorContainer       = Color(0xFF410002),

    background             = Color(0xFFFFF8F5),
    onBackground           = Color(0xFF231109),

    surface                = Color(0xFFFFF8F5),
    onSurface              = Color(0xFF231109),
    surfaceVariant         = Color(0xFFF5DED3),
    onSurfaceVariant       = NeutralVar30,

    surfaceContainerLowest = Color.White,
    surfaceContainerLow    = Color(0xFFFEF3EC),
    surfaceContainer       = Color(0xFFF8EDE6),
    surfaceContainerHigh   = Color(0xFFF2E7E0),
    surfaceContainerHighest = Color(0xFFECDDD8),
    surfaceBright          = Color(0xFFFFF8F5),
    surfaceDim             = Color(0xFFE9D5CA),

    outline                = Color(0xFF86716A),
    outlineVariant         = Color(0xFFD8C2BA),
    scrim                  = Color.Black,

    inverseSurface         = Color(0xFF3A2618),
    inverseOnSurface       = Color(0xFFFFEEE6),
    inversePrimary         = Orange80,
)
