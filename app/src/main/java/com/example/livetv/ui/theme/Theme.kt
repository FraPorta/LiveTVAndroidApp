package com.example.livetv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Central Material-3 theme for LiveTV.
 *
 * Always dark, with fixed brand colours (no Material You wallpaper colours) so the app
 * looks the same on a TV, a tablet and a phone.
 */
@Composable
fun LiveTVTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LiveTVDarkColors,
        typography  = LiveTVTypography,
        shapes      = LiveTVShapes,
        content     = content,
    )
}
