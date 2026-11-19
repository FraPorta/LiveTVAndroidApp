package com.example.livetv.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// ── Brand palette: one fixed dark sports look, identical on every device ─────
val AppBackground = Color(0xFF0E1116)   // page
val CardColor     = Color(0xFF171B22)   // match cards, panels
val CardRaised    = Color(0xFF232833)   // chips, secondary buttons
val PanelColor    = Color(0xFF12161C)   // stream panel / bottom sheet
val Accent        = Color(0xFFFF7A1A)   // the only accent: selected, primary actions
val OnAccent      = Color(0xFF0E1116)
val LiveRed       = Color(0xFFFF3B4E)   // reserved for "live"
val FavouriteGold = Color(0xFFFFC23D)   // reserved for favourites
val TextPrimary   = Color(0xFFE8EAED)
val TextSecondary = Color(0xFF8A919C)
val TextMuted     = Color(0xFF6B7280)
val FocusColor    = Color(0xFFFFFFFF)   // D-pad focus outline, same everywhere

val LiveTVDarkColors = darkColorScheme(
    primary                = Accent,
    onPrimary              = OnAccent,
    primaryContainer       = Color(0xFF3A2410),
    onPrimaryContainer     = Color(0xFFFFD9B8),

    secondary              = TextSecondary,
    onSecondary            = OnAccent,
    secondaryContainer     = CardRaised,
    onSecondaryContainer   = TextPrimary,

    tertiary               = Color(0xFF9FB4D6),
    onTertiary             = OnAccent,
    tertiaryContainer      = CardRaised,
    onTertiaryContainer    = TextPrimary,

    error                  = LiveRed,
    onError                = Color.White,
    errorContainer         = Color(0xFF4A1A20),
    onErrorContainer       = Color(0xFFFFD9DC),

    background             = AppBackground,
    onBackground           = TextPrimary,
    surface                = AppBackground,
    onSurface              = TextPrimary,
    surfaceVariant         = CardRaised,
    onSurfaceVariant       = TextSecondary,

    surfaceContainerLowest = AppBackground,
    surfaceContainerLow    = Color(0xFF12161C),
    surfaceContainer       = CardColor,
    surfaceContainerHigh   = CardColor,
    surfaceContainerHighest = CardRaised,
    surfaceBright          = CardRaised,
    surfaceDim             = AppBackground,

    outline                = TextMuted,
    outlineVariant         = Color(0xFF2A303A),
    scrim                  = Color.Black,

    inverseSurface         = TextPrimary,
    inverseOnSurface       = AppBackground,
    inversePrimary         = Color(0xFF8B3C00),
)
