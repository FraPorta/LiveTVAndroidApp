package com.example.livetv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.livetv.data.model.TeamEntry

/**
 * Horizontal chip row displayed **above** the match grid when the search bar
 * is active and [suggestions] is non-empty.
 *
 * Each chip shows the team logo (loaded from [TeamLogo]) and the canonical
 * team name. Tapping a chip calls [onSuggestionTap] with the selected entry;
 * the caller is responsible for populating [searchQuery] with the canonical name.
 *
 * The [LazyRow] is used so the list of chips is scrollable when there are more
 * than can fit on screen, while never causing a double-nested scroll issue with
 * the [LazyVerticalGrid] below it (they scroll on different axes).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSuggestions(
    suggestions: List<TeamEntry>,
    onSuggestionTap: (TeamEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (suggestions.isEmpty()) return

    LazyRow(
        modifier         = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding   = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(suggestions, key = { it.name }) { entry ->
            SuggestionChip(
                onClick = { onSuggestionTap(entry) },
                label   = {
                    Text(
                        text  = entry.name,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                },
                icon = {
                    TeamLogo(
                        teamName = entry.name,
                        size     = 18.dp,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
    }
}
