package com.example.livetv.ui

import android.net.Uri
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.livetv.data.local.LastStreamPreferences
import com.example.livetv.data.local.TeamMatcher
import com.example.livetv.data.model.Match
import com.example.livetv.ui.theme.Accent
import com.example.livetv.ui.theme.CardRaised
import com.example.livetv.ui.theme.FavouriteGold
import com.example.livetv.ui.theme.OnAccent
import com.example.livetv.ui.theme.TextSecondary

private data class StreamItem(val link: String, val title: String, val type: String)

private fun typeOf(link: String): String = when {
    isAcestreamLink(link) -> "Ace"
    link.contains(".m3u8") -> "M3U8"
    link.startsWith("rtmp") -> "RTMP"
    link.contains("youtube.com") || link.contains("youtu.be") -> "YouTube"
    link.contains("twitch.tv") -> "Twitch"
    else -> "Web"
}

/** Names each link by its site (web) or "Acestream n", keeping the page order. */
private fun buildStreams(links: List<String>): List<StreamItem> {
    fun hostOf(link: String) =
        runCatching { Uri.parse(link).host }.getOrNull()?.removePrefix("www.")?.takeIf { it.isNotBlank() }
    // Most links share one host, so number the repeats ("livetv.sx 1", "livetv.sx 2").
    val hostCount = links.filter { !isAcestreamLink(it) }.groupingBy { hostOf(it) }.eachCount()
    val seen = HashMap<String?, Int>()
    var ace = 0
    return links.map { link ->
        if (isAcestreamLink(link)) {
            ace++
            StreamItem(link, "Acestream $ace", "Ace")
        } else {
            val host = hostOf(link)
            val n = (seen[host] ?: 0) + 1
            seen[host] = n
            val base = host ?: "Web stream"
            StreamItem(link, if ((hostCount[host] ?: 0) > 1 || host == null) "$base $n" else base, typeOf(link))
        }
    }
}

/**
 * Match details and every stream link, shown in a side panel on TV and a bottom sheet
 * on phones. The stream opened last for this match is listed first.
 */
@Composable
fun StreamPanelContent(
    match: Match,
    viewModel: MatchViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lastPrefs = remember { LastStreamPreferences(context) }
    var lastUsed by remember(match.detailPageUrl) { mutableStateOf(lastPrefs.get(match.detailPageUrl)) }
    val streams = remember(match.streamLinks, lastUsed) {
        val all = buildStreams(match.streamLinks)
        val first = all.firstOrNull { it.link == lastUsed }
        if (first != null) listOf(first) + all.filter { it !== first } else all
    }
    val firstFocus = remember { FocusRequester() }
    val refreshFocus = remember { FocusRequester() }
    LaunchedEffect(match.detailPageUrl, streams.isNotEmpty()) {
        try { (if (streams.isNotEmpty()) firstFocus else refreshFocus).requestFocus() } catch (_: Exception) {}
    }

    val favTeams by viewModel.favouriteTeams
    val favLeagues by viewModel.favouriteLeagues
    val teams = remember(match.teams) { splitTeams(match.teams) }

    Column(modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = remember(match) {
                        val now = System.currentTimeMillis()
                        listOf(MatchClock.dayLabel(match, now), MatchClock.displayTime(match, now))
                            .filter { it.isNotBlank() }.joinToString(" · ")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                if (match.league.isNotBlank()) {
                    val leagueFav = match.qualifiedLeagueKey in favLeagues
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = match.league,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        IconButton(
                            onClick = { viewModel.toggleFavouriteLeague(match.qualifiedLeagueKey) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                if (leagueFav) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = if (leagueFav) "Unfavourite league" else "Favourite league",
                                tint = if (leagueFav) FavouriteGold else TextSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
            BarIconButton(Icons.Default.Refresh, "Refresh streams", { viewModel.refreshMatchLinks(match) },
                modifier = Modifier.focusRequester(refreshFocus))
            Box(Modifier.size(8.dp))
            BarIconButton(Icons.Default.Close, "Close", onClose)
        }

        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val names = teams?.toList() ?: listOf(match.teams.ifBlank { "Teams to be announced" })
            names.forEachIndexed { i, name ->
                val logo = if (i == 0) match.homeLogoUrl else match.awayLogoUrl
                val canonical = remember(name, match.qualifiedLeagueKey) {
                    TeamMatcher.lookupTeam(name, match.qualifiedLeagueKey)?.name ?: name
                }
                val fav = canonical in favTeams
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (teams != null) {
                        Crest(name, match.qualifiedLeagueKey, logo, size = 32.dp)
                        Box(Modifier.size(10.dp))
                    }
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (teams != null) {
                        IconButton(onClick = { viewModel.toggleFavouriteTeam(canonical) }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                if (fav) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = if (fav) "Unfavourite $canonical" else "Favourite $canonical",
                                tint = if (fav) FavouriteGold else TextSecondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }

        when {
            match.areLinksLoading && streams.isEmpty() -> Row(
                Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Accent)
                Text("Finding streams…", color = TextSecondary, modifier = Modifier.padding(start = 12.dp))
            }
            streams.isEmpty() -> Text(
                "No streams yet. Try again closer to kickoff.",
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 16.dp),
            )
            else -> LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(streams, key = { _, s -> s.link }) { index, s ->
                    val wasLast = s.link == lastUsed
                    StreamRow(
                        item = s,
                        highlighted = wasLast,
                        subtitle = if (wasLast) "Last used" else null,
                        modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
                        onClick = {
                            lastPrefs.set(match.detailPageUrl, s.link)
                            lastUsed = s.link
                            openUrlWithChooser(context, s.link)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamRow(
    item: StreamItem,
    highlighted: Boolean,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(10.dp)
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().tvFocus(source, shape, scaleTo = 1.02f),
        shape = shape,
        color = if (highlighted) Accent else CardRaised,
        contentColor = if (highlighted) OnAccent else MaterialTheme.colorScheme.onSurface,
        interactionSource = source,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(item.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelSmall)
            }
            Text(
                item.type,
                style = MaterialTheme.typography.labelSmall,
                color = if (highlighted) OnAccent else TextSecondary,
            )
        }
    }
}
