package com.example.livetv.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * TV-focusable button built on [Surface], using the shared [tvFocus] style
 * (scale + white outline) that the standard Material3 button does not give on TV.
 */
@Composable
fun FocusableButton(
    onClick: () -> Unit,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.small
    Surface(
        onClick           = onClick,
        modifier          = modifier.tvFocus(interactionSource, shape, scaleTo = 1.08f),
        shape             = shape,
        color             = backgroundColor,
        contentColor      = contentColor,
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
