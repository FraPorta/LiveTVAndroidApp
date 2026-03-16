package com.example.livetv.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.livetv.data.model.Match
import com.example.livetv.ui.theme.BadgeTextStyle

@Composable
fun MatchItem(
    match: Match,
    viewModel: MatchViewModel,
    isExpanded: Boolean = false,
    onExpand: () -> Unit = {},
    onCollapse: () -> Unit = {},
    collapsedFocusRequester: FocusRequester? = null,
    forceUniformHeight: Boolean = false
) {
    val context = LocalContext.current

    fun isAcestreamLink(url: String) =
        url.startsWith("acestream://") || url.contains("/ace/getstream?id=")

    val aceStreamLinks = match.streamLinks.filter { isAcestreamLink(it) }
    val webStreamLinks = match.streamLinks.filter { !isAcestreamLink(it) }
    val hasBothTypes = aceStreamLinks.isNotEmpty() && webStreamLinks.isNotEmpty()

    var showAceStreamDialog by remember { mutableStateOf(false) }
    var showWebStreamDialog by remember { mutableStateOf(false) }

    // Focus requester for the first interactive element when the card expands
    val firstButtonFocusRequester = remember { FocusRequester() }
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            try { firstButtonFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    // Build the time + league summary string (used in both layouts)
    val timeAndLeague = buildString {
        if (match.time.isNotBlank()) append("⏰ ${match.time}")
        val leagueInfo = when {
            match.league.isNotBlank() && match.competition.isNotBlank() ->
                if (match.league.equals(match.competition, ignoreCase = true)) match.league
                else "${match.league} - ${match.competition}"
            match.league.isNotBlank() -> match.league
            match.competition.isNotBlank() -> match.competition
            else -> ""
        }
        if (leagueInfo.isNotBlank()) {
            if (isNotEmpty()) append(" • ")
            append("🏆 $leagueInfo")
        }
    }

    if (!isExpanded) {
        // ── Collapsed card ────────────────────────────────────────────────
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused by interactionSource.collectIsFocusedAsState()

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isFocused) 8.dp else 2.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            border = if (isFocused)
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            else null
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left accent bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (collapsedFocusRequester != null) Modifier.focusRequester(collapsedFocusRequester) else Modifier)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onExpand
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = match.teams.ifBlank { "Teams TBD" },
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2
                        )
                        if (timeAndLeague.isNotBlank()) {
                            Text(
                                text = timeAndLeague,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Right-side status badge
                    when {
                        match.areLinksLoading -> SpinningIcon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Loading streams",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        match.streamLinks.isNotEmpty() -> Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.extraSmall
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${match.streamLinks.size}",
                                style = BadgeTextStyle,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
        }   // end Card
    } else {
        // ── Expanded card (full stream detail view) ───────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(if (hasBothTypes) 12.dp else 16.dp)
            ) {
                // Header: teams + interactive refresh button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = match.teams.ifBlank { "Teams TBD" },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        minLines = 2
                    )
                    FocusableButton(
                        onClick = { viewModel.refreshMatchLinks(match) },
                        backgroundColor = if (match.areLinksLoading)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (match.areLinksLoading)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        focusColor = MaterialTheme.colorScheme.primary,
                        // Auto-focus refresh button when no streams are available yet
                        modifier = if (match.streamLinks.isEmpty() && !match.areLinksLoading)
                            Modifier.size(36.dp).focusRequester(firstButtonFocusRequester)
                        else
                            Modifier.size(36.dp)
                    ) {
                        if (match.areLinksLoading) {
                            SpinningIcon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Loading",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Links",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (timeAndLeague.isNotBlank()) {
                    Text(
                        text = timeAndLeague,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = if (hasBothTypes) 4.dp else 6.dp),
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(
                    modifier  = Modifier.padding(bottom = 8.dp),
                    thickness = 1.dp,
                    color     = MaterialTheme.colorScheme.outlineVariant,
                )

                // Streams section
                when {
                    match.areLinksLoading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SpinningIcon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Loading",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Loading...", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    match.streamLinks.isNotEmpty() -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (aceStreamLinks.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = if (hasBothTypes) 2.dp else 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = MaterialTheme.shapes.extraSmall
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "🔗 ${aceStreamLinks.size} Acestream",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    aceStreamLinks.take(3).forEachIndexed { index, link ->
                                        FocusableButton(
                                            onClick = { openUrlWithChooser(context, link) },
                                            backgroundColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                            focusColor = MaterialTheme.colorScheme.tertiary,
                                            modifier = if (index == 0)
                                                Modifier.focusRequester(firstButtonFocusRequester)
                                            else
                                                Modifier
                                        ) {
                                            Text(
                                                text = "ACE ${index + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                    if (aceStreamLinks.size > 3) {
                                        FocusableButton(
                                            onClick = { showAceStreamDialog = true },
                                            backgroundColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            contentColor = MaterialTheme.colorScheme.primary,
                                            focusColor = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = "+${aceStreamLinks.size - 3}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            if (webStreamLinks.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = if (hasBothTypes) 2.dp else 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = MaterialTheme.shapes.extraSmall
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "🌐 ${webStreamLinks.size} Web Streams",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    webStreamLinks.take(3).forEachIndexed { index, link ->
                                        val streamLabel = when {
                                            link.contains(".m3u8") -> "M3U8 ${index + 1}"
                                            link.startsWith("rtmp") -> "RTMP ${index + 1}"
                                            link.contains("youtube.com") || link.contains("youtu.be") -> "YT ${index + 1}"
                                            link.contains("twitch.tv") -> "Twitch ${index + 1}"
                                            link.contains("webplayer") -> "Web ${index + 1}"
                                            else -> "HTTP ${index + 1}"
                                        }
                                        FocusableButton(
                                            onClick = { openUrlWithChooser(context, link) },
                                            backgroundColor = MaterialTheme.colorScheme.tertiary,
                                            contentColor = MaterialTheme.colorScheme.onTertiary,
                                            focusColor = MaterialTheme.colorScheme.primary,
                                            // Auto-focus first web button when there are no ace streams
                                            modifier = if (aceStreamLinks.isEmpty() && index == 0)
                                                Modifier.focusRequester(firstButtonFocusRequester)
                                            else
                                                Modifier
                                        ) {
                                            Text(
                                                text = streamLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onTertiary
                                            )
                                        }
                                    }
                                    if (webStreamLinks.size > 3) {
                                        FocusableButton(
                                            onClick = { showWebStreamDialog = true },
                                            backgroundColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            contentColor = MaterialTheme.colorScheme.tertiary,
                                            focusColor = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = "+${webStreamLinks.size - 3}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        Text(
                            "No streams found",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } // Close main Column
        } // Close expanded Card
    } // End if/else

    // Acestream Dialog
    if (showAceStreamDialog) {
        AlertDialog(
            onDismissRequest = { showAceStreamDialog = false },
            title = {
                Text("🔗 All Acestream Links (${aceStreamLinks.size})")
            },
            text = {
                LazyColumn {
                    itemsIndexed(aceStreamLinks) { index, link ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    openUrlWithChooser(context, link)
                                    showAceStreamDialog = false
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ACE ${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text(
                                text = link.take(50) + if (link.length > 50) "..." else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAceStreamDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Web Stream Dialog
    if (showWebStreamDialog) {
        AlertDialog(
            onDismissRequest = { showWebStreamDialog = false },
            title = {
                Text("🌐 All Web Stream Links (${webStreamLinks.size})")
            },
            text = {
                LazyColumn {
                    itemsIndexed(webStreamLinks) { index, link ->
                        val streamLabel = when {
                            link.contains(".m3u8") -> "M3U8 ${index + 1}"
                            link.startsWith("rtmp") -> "RTMP ${index + 1}"
                            link.contains("youtube.com") || link.contains("youtu.be") -> "YT ${index + 1}"
                            link.contains("twitch.tv") -> "Twitch ${index + 1}"
                            link.contains("webplayer") -> "Web ${index + 1}"
                            else -> "HTTP ${index + 1}"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    openUrlWithChooser(context, link)
                                    showWebStreamDialog = false
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = streamLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                            Text(
                                text = link.take(50) + if (link.length > 50) "..." else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWebStreamDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * Helper function to open a URL with an intent chooser
 */
fun openUrlWithChooser(context: Context, url: String) {
    try {
        // Check if this is an acestream HTTP proxy URL
        if (url.contains("/ace/getstream?id=")) {
            openWithVlc(context, url)
            return
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            // Add flags to ensure compatibility
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // For acestream links, we might want to set specific MIME type
            if (url.startsWith("acestream://")) {
                setDataAndType(Uri.parse(url), "application/x-acestream")
            }
        }

        // Create chooser to let user pick which app to use
        val chooserIntent = Intent.createChooser(intent, "Open stream with...").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        // If something goes wrong, try with a simple intent
        try {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        } catch (e2: Exception) {
            // If that also fails, we could show a toast or log the error
            Log.e("HomeScreen", "Failed to open URL: $url", e2)
        }
    }
}

fun openWithVlc(context: Context, url: String) {
    try {
        // First try to open directly with VLC
        val vlcIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(url), "video/*")
            setPackage("org.videolan.vlc") // VLC package name
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(vlcIntent)
        Log.d("HomeScreen", "Opened acestream HTTP URL with VLC: $url")
    } catch (e: Exception) {
        // VLC not available or other error, fall back to chooser
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(url), "video/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooserIntent = Intent.createChooser(intent, "Open acestream with media player...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooserIntent)
            Log.d("HomeScreen", "Opened acestream HTTP URL with chooser: $url")
        } catch (e2: Exception) {
            Log.e("HomeScreen", "Failed to open acestream URL: $url", e2)
        }
    }
}
