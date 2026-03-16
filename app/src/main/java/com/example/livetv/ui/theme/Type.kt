package com.example.livetv.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Default M3 typography as base — keep Roboto (no extra font dependency)
private val _base = Typography()

val LiveTVTypography = Typography(
    // ── Display / Headline — bolder for TV readability at distance ──────────
    displayLarge   = _base.displayLarge.copy(fontWeight  = FontWeight.Bold),
    displayMedium  = _base.displayMedium.copy(fontWeight = FontWeight.Bold),
    displaySmall   = _base.displaySmall.copy(fontWeight  = FontWeight.SemiBold),
    headlineLarge  = _base.headlineLarge.copy(fontWeight  = FontWeight.Bold),
    headlineMedium = _base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall  = _base.headlineSmall.copy(fontWeight  = FontWeight.SemiBold),

    // ── Title — TV-optimised sizing ─────────────────────────────────────────
    titleLarge  = _base.titleLarge.copy(fontWeight  = FontWeight.Bold),
    titleMedium = _base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    // titleSmall drives the collapsed match-card team name — needs a bit more
    // presence so it reads well at TV distance on a 96dp card.
    titleSmall  = _base.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),

    // ── Body — defaults are fine ─────────────────────────────────────────────
    bodyLarge  = _base.bodyLarge,
    bodyMedium = _base.bodyMedium,
    bodySmall  = _base.bodySmall.copy(letterSpacing = 0.2.sp),

    // ── Label — slightly bolder for chip/button readability ──────────────────
    labelLarge  = _base.labelLarge.copy(fontWeight  = FontWeight.SemiBold),
    labelMedium = _base.labelMedium.copy(fontWeight = FontWeight.SemiBold),
    labelSmall  = _base.labelSmall.copy(fontWeight  = FontWeight.SemiBold),
)

/**
 * Named token for the stream-count badge in the collapsed match card.
 * Replaces the inline `labelSmall.copy(fontWeight = Bold, fontSize = 11.sp)` hack.
 */
val BadgeTextStyle = TextStyle(
    fontWeight    = FontWeight.Bold,
    fontSize      = 11.sp,
    letterSpacing = 0.sp,
)
