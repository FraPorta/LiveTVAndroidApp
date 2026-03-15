package com.example.livetv.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Central Material-3 theme for LiveTV.
 *
 * Behaviour:
 * - Android 12+ (API 31): Material You dynamic colour — adapts to the device
 *   wallpaper palette automatically; orange is the hand-crafted fallback seed.
 * - Android < 12: falls back to the custom orange-seeded [LiveTVDarkColors] /
 *   [LiveTVLightColors] schemes.
 *
 * TV devices default to dark; phones follow the system setting.
 */
@Composable
fun LiveTVTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content:   @Composable () -> Unit,
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> LiveTVDarkColors
        else      -> LiveTVLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = LiveTVTypography,
        shapes      = LiveTVShapes,
        content     = content,
    )
}
