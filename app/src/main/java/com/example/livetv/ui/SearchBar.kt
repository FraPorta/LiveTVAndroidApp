package com.example.livetv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp

/**
 * Overlay search bar shown when the user activates search.
 *
 * Uses a [Surface] with [MaterialTheme.colorScheme.surfaceContainerHigh] and
 * [MaterialTheme.shapes.medium] corners — matching the M3 DockedSearchBar
 * visual spec — rather than a raw background [Row]. Auto-focuses the text
 * field on first composition so TV remote users can start typing immediately.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    viewModel: MatchViewModel,
    modifier: Modifier = Modifier,
) {
    val searchQuery  by viewModel.searchQuery
    val focusRequester = remember { FocusRequester() }
    val focusManager  = LocalFocusManager.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        color         = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape         = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier            = Modifier.fillMaxWidth(),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector        = Icons.Default.Search,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.padding(start = 16.dp),
            )

            TextField(
                value             = searchQuery,
                onValueChange     = { viewModel.updateSearchQuery(it) },
                placeholder       = {
                    Text(
                        "Search teams, leagues…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier          = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onKeyEvent { ev ->
                        // D-pad DOWN: move focus into the suggestions row below
                        if (ev.type == KeyEventType.KeyDown && ev.key == Key.DirectionDown) {
                            focusManager.moveFocus(FocusDirection.Down)
                            true
                        } else false
                    },
                singleLine        = true,
                textStyle         = MaterialTheme.typography.bodyMedium,
                colors            = TextFieldDefaults.colors(
                    // Blend into the Surface so it looks like one unified bar
                    focusedContainerColor   = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor  = Color.Transparent,
                ),
            )

            IconButton(onClick = { viewModel.deactivateSearch() }) {
                Icon(
                    imageVector        = Icons.Default.Close,
                    contentDescription = "Close search",
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
