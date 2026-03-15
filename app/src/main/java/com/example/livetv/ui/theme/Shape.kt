package com.example.livetv.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Consolidated shape tokens for LiveTV.
 *
 *  extraSmall  8 dp  — inline badges, chips
 *  small      12 dp  — buttons, individual segmented-tab items
 *  medium     16 dp  — cards, dialogs, search bar, action-buttons pill
 *  large      20 dp  — segmented-button container pill
 *  extraLarge 28 dp  — future bottom-sheet / panel corners
 */
val LiveTVShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
