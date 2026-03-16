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
 * Displays the logo for [teamName].
 *
 * When a scraped [logoUrl] is provided it is loaded directly via Coil (remote or
 * asset URI). If the URL fails, or when [logoUrl] is null, the composable falls
 * back to the bundled offline asset database via [TeamMatcher].
 * Renders nothing when neither source has a logo, so callers never see a broken
 * image placeholder in the normal case.
 *
 * @param teamName  Raw scraped team name (e.g. "Manchester United", "PSG").
 * @param logoUrl   Optional scraped logo URL (absolute http/https). When supplied
 *                  this takes priority over the offline asset DB.
 * @param size      Rendered size in dp (default 32 dp for match cards).
 * @param leagueHint League key used to improve offline DB lookup accuracy.
 * @param modifier  Additional Compose modifiers.
 */
@Composable
fun TeamLogo(
    teamName: String,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    leagueHint: String = "",
    logoUrl: String? = null,
) {
    val entry = remember(teamName, leagueHint) { TeamMatcher.lookupTeam(teamName, leagueHint) }

    // If we have neither a scraped URL nor an offline entry there is nothing to show.
    if (logoUrl == null && entry == null) return

    val context = LocalContext.current

    when {
        logoUrl != null -> {
            // Remote scraped URL — Coil loads it directly; shows nothing if the request fails.
            val primaryModel = remember(logoUrl) {
                ImageRequest.Builder(context)
                    .data(logoUrl)
                    .memoryCacheKey(logoUrl)
                    .build()
            }
            AsyncImage(
                model              = primaryModel,
                contentDescription = "${entry?.name ?: teamName} logo",
                modifier           = modifier.size(size),
                contentScale       = ContentScale.Fit,
            )
        }
        entry != null -> {
            val model = remember(entry.logoAssetPath) {
                ImageRequest.Builder(context)
                    .data(Uri.parse("file:///android_asset/${entry.logoAssetPath}"))
                    .memoryCacheKey(entry.logoAssetPath)
                    .build()
            }
            AsyncImage(
                model              = model,
                contentDescription = "${entry.name} logo",
                modifier           = modifier.size(size),
                contentScale       = ContentScale.Fit,
            )
        }
    }
}
