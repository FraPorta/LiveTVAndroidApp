package com.example.livetv.ui

import android.net.Uri
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.livetv.data.local.TeamMatcher

/**
 * Displays the bundled logo for [teamName] if a matching [TeamEntry] exists in the
 * offline database. Renders nothing when no match is found, so callers never see
 * a broken image placeholder in the normal case.
 *
 * Logo files live in `assets/logos/<League>/<Team>.png` and are loaded by Coil
 * using the `file:///android_asset/…` URI scheme, which is cached in Coil's
 * in-memory LRU MemoryCache to avoid repeated asset decodes on every scroll frame.
 *
 * @param teamName Raw scraped team name (e.g. "Manchester United", "PSG").
 * @param size     Rendered size in dp (default 32 dp for match cards).
 * @param modifier Additional Compose modifiers.
 */
@Composable
fun TeamLogo(
    teamName: String,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    leagueHint: String = "",
) {
    val entry = remember(teamName, leagueHint) { TeamMatcher.lookupTeam(teamName, leagueHint) }
    entry ?: return

    val context = LocalContext.current
    val model = remember(entry.logoAssetPath) {
        ImageRequest.Builder(context)
            .data(Uri.parse("file:///android_asset/${entry.logoAssetPath}"))
            .memoryCacheKey(entry.logoAssetPath)
            .build()
    }

    AsyncImage(
        model            = model,
        contentDescription = "${entry.name} logo",
        modifier         = modifier.size(size),
        contentScale     = ContentScale.Fit,
    )
}
