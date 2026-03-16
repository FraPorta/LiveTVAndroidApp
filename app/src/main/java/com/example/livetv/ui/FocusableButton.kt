package com.example.livetv.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * TV-focusable button built on [Surface].
 *
 * Provides D-pad focus ring (animated border + elevation) and scale animation
 * that the standard Material3 [Button] composable does not offer on TV.
 * Shape is driven by [MaterialTheme.shapes.small] (12 dp rounded corners).
 */
@Composable
fun FocusableButton(
    onClick: () -> Unit,
    backgroundColor: Color,
    contentColor: Color,
    focusColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue   = if (isFocused) 1.05f else 1f,
        animationSpec = tween(200),
        label         = "focusScale",
    )
    val borderAlpha by animateFloatAsState(
        targetValue   = if (isFocused) 1f else 0f,
        animationSpec = tween(200),
        label         = "borderAlpha",
    )

    Surface(
        onClick           = onClick,
        modifier          = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        shape             = MaterialTheme.shapes.small,   // ← theme token (12 dp)
        color             = if (isFocused) backgroundColor.copy(alpha = 0.9f) else backgroundColor,
        border            = if (isFocused) BorderStroke(2.dp, focusColor.copy(alpha = borderAlpha))
                            else null,
        shadowElevation   = if (isFocused) 6.dp else 0.dp,
        interactionSource = interactionSource,
    ) {
        Box(
            modifier         = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}
