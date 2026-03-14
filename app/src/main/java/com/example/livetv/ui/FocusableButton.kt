package com.example.livetv.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun FocusableButton(
    onClick: () -> Unit,
    backgroundColor: Color,
    contentColor: Color,
    focusColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Animated focus scale and alpha effects
    val focusScale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = tween(200),
        label = "focusScale"
    )

    val focusAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(200),
        label = "focusAlpha"
    )

    // Simple focus background color
    val focusedBackgroundColor = if (isFocused) {
        backgroundColor.copy(alpha = 0.9f)
    } else {
        backgroundColor
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer {
                scaleX = focusScale
                scaleY = focusScale
            },
        shape = RoundedCornerShape(12.dp),
        color = focusedBackgroundColor,
        border = if (isFocused) {
            BorderStroke(width = 2.dp, color = focusColor.copy(alpha = focusAlpha))
        } else null,
        shadowElevation = if (isFocused) 6.dp else 0.dp,
        interactionSource = interactionSource,
        content = {
            Box(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    )
}
