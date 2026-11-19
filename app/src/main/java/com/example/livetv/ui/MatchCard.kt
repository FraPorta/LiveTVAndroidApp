package com.example.livetv.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.livetv.data.local.TeamMatcher
import com.example.livetv.data.model.Match
import com.example.livetv.ui.theme.CardColor
import com.example.livetv.ui.theme.CardRaised
import com.example.livetv.ui.theme.FavouriteGold
import com.example.livetv.ui.theme.LiveRed
import com.example.livetv.ui.theme.ScoreTimeStyle
import com.example.livetv.ui.theme.TextSecondary

/** League name for the card; scraped text like "19 November at 20:45 (France. National)" is cut to the bracket. */
private fun leagueLabel(match: Match): String {
    if (match.league.isNotBlank()) return match.league
    val c = match.competition.trim()
    return Regex("""\((.+)\)\s*$""").find(c)?.groupValues?.get(1) ?: c
}

fun isAcestreamLink(url: String) =
    url.startsWith("acestream://") || url.contains("/ace/getstream?id=")

/** Team crest from the bundled logos or the scraped URL; a coloured initials badge when neither exists. */
@Composable
fun Crest(name: String, leagueHint: String, logoUrl: String?, size: Dp, sport: String = "") {
    val known = remember(name, leagueHint) { TeamMatcher.lookupTeam(name, leagueHint) != null }
    if (known || logoUrl != null) {
        TeamLogo(name, size = size, leagueHint = leagueHint, logoUrl = logoUrl)
    } else if (!isSoccer(sport)) {
        SportBadge(sport, size)
    } else {
        val initials = remember(name) {
            name.split(" ", "-").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
        }
        Box(Modifier.size(size).background(CardRaised, CircleShape), contentAlignment = Alignment.Center) {
            Text(initials, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
    }
}

/** A sport icon in a round badge, for events and teams that have no logo. */
@Composable
fun SportBadge(sport: String, size: Dp) {
    Box(Modifier.size(size).background(CardRaised, CircleShape), contentAlignment = Alignment.Center) {
        Icon(sportIcon(sport), contentDescription = sport, tint = TextSecondary, modifier = Modifier.size(size * 0.55f))
    }
}

/** Red "LIVE" pill with a softly pulsing dot. */
@Composable
fun LivePill(modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "live").animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "liveDot",
    )
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).alpha(pulse).background(LiveRed, CircleShape))
        Text(
            "LIVE",
            style = MaterialTheme.typography.labelSmall,
            color = LiveRed,
            modifier = Modifier.padding(start = 5.dp),
        )
    }
}

@Composable
private fun CountChip(text: String) {
    Box(Modifier.background(CardRaised, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

/**
 * Scoreboard card: league on top, both crests with the kickoff time between them,
 * stream counts at the bottom. Finished matches are dimmed.
 */
@Composable
fun MatchCard(
    match: Match,
    phase: MatchPhase,
    isFavourite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    val source = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.medium
    val teams = remember(match.teams) { splitTeams(match.teams) }
    val kickoff = remember(match) { MatchClock.displayTime(match, System.currentTimeMillis()) }
    val aceCount = match.streamLinks.count { isAcestreamLink(it) }
    val webCount = match.streamLinks.size - aceCount

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .tvFocus(source, shape),
        shape = shape,
        color = CardColor,
        interactionSource = source,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .alpha(if (phase == MatchPhase.FINISHED) 0.55f else 1f),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = leagueLabel(match),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (isFavourite) {
                    Icon(Icons.Default.Star, "Favourite", tint = FavouriteGold, modifier = Modifier.size(16.dp))
                    Box(Modifier.width(8.dp))
                }
                if (phase == MatchPhase.LIVE) LivePill()
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (teams != null) {
                    TeamColumn(teams.first, match.qualifiedLeagueKey, match.homeLogoUrl, match.sport, Modifier.weight(1f))
                    Text(
                        text = kickoff.ifBlank { "TBD" },
                        style = ScoreTimeStyle,
                        color = if (phase == MatchPhase.LIVE) LiveRed else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                    TeamColumn(teams.second, match.qualifiedLeagueKey, match.awayLogoUrl, match.sport, Modifier.weight(1f))
                } else {
                    SportBadge(match.sport, 40.dp)
                    Box(Modifier.width(12.dp))
                    Text(
                        text = match.teams.ifBlank { "Teams to be announced" },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = kickoff,
                        style = ScoreTimeStyle,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                when {
                    match.areLinksLoading ->
                        Text("Finding streams…", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    match.streamLinks.isEmpty() -> Box(Modifier.heightIn(min = 20.dp))
                    else -> {
                        if (aceCount > 0) CountChip("$aceCount Acestream")
                        if (webCount > 0) CountChip("$webCount web")
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamColumn(name: String, leagueHint: String, logoUrl: String?, sport: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Crest(name, leagueHint, logoUrl, size = 40.dp, sport = sport)
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
