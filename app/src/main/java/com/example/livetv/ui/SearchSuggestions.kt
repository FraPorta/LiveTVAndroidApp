package com.example.livetv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.livetv.data.model.TeamEntry

/**
 * Horizontal scrollable chip row shown while search is active.
 *
 * Team chips: logo + name (tap fills the search box) + star button (toggles favourite).
 * League chips: trophy icon + league name (tap fills search) + star button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSuggestions(
    suggestions: List<TeamEntry>,
    leagueSuggestions: List<String>,
    favouriteTeams: Set<String>,
    favouriteLeagues: Set<String>,
    onSuggestionTap: (TeamEntry) -> Unit,
    onLeagueTap: (String) -> Unit,
    onToggleTeamFavourite: (String) -> Unit,
    onToggleLeagueFavourite: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (suggestions.isEmpty() && leagueSuggestions.isEmpty()) return

    LazyRow(
        modifier              = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding        = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // ── Team chips ──────────────────────────────────────────────────────────────
        items(suggestions, key = { "team:${it.name}" }) { entry ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                SuggestionChip(
                    onClick = { onSuggestionTap(entry) },
                    label   = {
                        Text(
                            text     = entry.name,
                            style    = MaterialTheme.typography.labelMedium,
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
                val isFav = entry.name in favouriteTeams
                IconButton(
                    onClick  = { onToggleTeamFavourite(entry.name) },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector        = if (isFav) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = if (isFav) "Remove from favourites" else "Add to favourites",
                        tint               = if (isFav) Color(0xFFFFB300)
                                             else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier.size(18.dp),
                    )
                }
            }
        }

        // ── League chips ─────────────────────────────────────────────────────────
        items(leagueSuggestions, key = { "league:$it" }) { league ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                SuggestionChip(
                    onClick = { onLeagueTap(league) },
                    label   = {
                        Text(
                            text     = league,
                            style    = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                    },
                    icon = {
                        Icon(
                            imageVector        = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            modifier           = Modifier.size(16.dp),
                        )
                    },
                )
                val isFav = league in favouriteLeagues
                IconButton(
                    onClick  = { onToggleLeagueFavourite(league) },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector        = if (isFav) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = if (isFav) "Remove league from favourites" else "Add league to favourites",
                        tint               = if (isFav) Color(0xFFFFB300)
                                             else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
