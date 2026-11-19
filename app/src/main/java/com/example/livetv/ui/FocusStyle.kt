package com.example.livetv.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.livetv.ui.theme.FocusColor

/**
 * The one D-pad focus style used everywhere: the focused item grows slightly, gets a
 * white outline and is drawn above its neighbours, so it is easy to follow from a sofa.
 */
@Composable
fun Modifier.tvFocus(
    interactionSource: MutableInteractionSource,
    shape: Shape,
    scaleTo: Float = 1.04f,
): Modifier {
    val focused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (focused) scaleTo else 1f,
        animationSpec = tween(150),
        label         = "focusScale",
    )
    return this
        .zIndex(if (focused) 1f else 0f)
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .then(if (focused) Modifier.border(2.dp, FocusColor, shape) else Modifier)
}
