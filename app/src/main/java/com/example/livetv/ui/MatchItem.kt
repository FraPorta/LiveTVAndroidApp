package com.example.livetv.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.livetv.data.model.Match

@Composable
fun MatchItem(match: Match, viewModel: MatchViewModel, forceUniformHeight: Boolean = false) {
    val context = LocalContext.current

    // Helper function to identify acestream links (both original and HTTP proxy formats)
    fun isAcestreamLink(url: String): Boolean {
        return url.startsWith("acestream://") || url.contains("/ace/getstream?id=")
    }

    // Categorize links to determine card height
    val aceStreamLinks = match.streamLinks.filter { isAcestreamLink(it) }
    val webStreamLinks = match.streamLinks.filter { !isAcestreamLink(it) }
    val hasBothTypes = aceStreamLinks.isNotEmpty() && webStreamLinks.isNotEmpty()

    // State for popup dialogs
    var showAceStreamDialog by remember { mutableStateOf(false) }
    var showWebStreamDialog by remember { mutableStateOf(false) }

    // Use uniform height if any card in the grid has both types, ensuring equal row heights
    val cardHeight = if (forceUniformHeight || hasBothTypes) 300.dp else 210.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight), // Fixed height ensures all cards in row are equal
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (hasBothTypes) 12.dp else 16.dp)
        ) {
            // Header content (teams, time, league info)
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header row with team names and refresh button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Team names - prominent display
                Text(
                    text = match.teams.ifBlank { "Teams TBD" },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    minLines = 2 // Ensure consistent height
                )

                // Modern refresh button with focus support
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
                    modifier = Modifier.size(36.dp) // Fixed size for compact refresh button
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Links",
                        tint = if (match.areLinksLoading)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Match Time and League/Competition info (avoid duplication) - made bigger
            val timeAndLeague = buildString {
                if (match.time.isNotBlank()) {
                    append("⏰ ${match.time}")
                }

                // Choose the better league/competition info and avoid duplication
                val leagueInfo = when {
                    match.league.isNotBlank() && match.competition.isNotBlank() -> {
                        // If both exist, check if they're different
                        if (match.league.equals(match.competition, ignoreCase = true)) {
                            match.league // They're the same, just show one
                        } else {
                            "${match.league} - ${match.competition}" // They're different, show both
                        }
                    }
                    match.league.isNotBlank() -> match.league
                    match.competition.isNotBlank() -> match.competition
                    else -> ""
                }

                if (leagueInfo.isNotBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append("🏆 $leagueInfo")
                }
            }

            if (timeAndLeague.isNotBlank()) {
                Text(
                    text = timeAndLeague,
                    style = MaterialTheme.typography.bodyMedium, // Changed from bodySmall to bodyMedium
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = if (hasBothTypes) 4.dp else 6.dp), // Reduced padding when both types
                    maxLines = 2 // Allow 2 lines for longer text
                )
            }
            } // Close header Column

            // For cards with both types, use weight to push buttons to bottom
            // For single-type cards, use minimal spacing to keep compact
            if (hasBothTypes || forceUniformHeight) {
                Spacer(modifier = Modifier.weight(1f))
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Buttons section
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                when {
                match.areLinksLoading -> {
                    // Compact loader for grid
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                       CircularProgressIndicator(modifier = Modifier.size(14.dp))
                       Spacer(modifier = Modifier.width(4.dp))
                       Text(
                           "Loading...",
                           style = MaterialTheme.typography.labelSmall
                       )
                    }
                }
                match.streamLinks.isNotEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(if (hasBothTypes) 6.dp else 6.dp)
                    ) {
                        // Modern Acestream Section
                        if (aceStreamLinks.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = if (hasBothTypes) 2.dp else 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(8.dp)
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
                                        focusColor = MaterialTheme.colorScheme.tertiary
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

                        // Modern Web Streams Section
                        if (webStreamLinks.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = if (hasBothTypes) 2.dp else 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(8.dp)
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
                                        focusColor = MaterialTheme.colorScheme.primary
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
                    // Compact "no streams" message for grid
                    Text(
                        "No streams found",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            } // Close buttons Column
        } // Close main Column
    }

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
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    openUrlWithChooser(context, link)
                                    showAceStreamDialog = false
                                },
                                modifier = Modifier.height(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(
                                    text = "ACE ${index + 1}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Text(
                                text = link.take(50) + if (link.length > 50) "..." else "",
                                style = MaterialTheme.typography.bodySmall,
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
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    openUrlWithChooser(context, link)
                                    showWebStreamDialog = false
                                },
                                modifier = Modifier.height(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                )
                            ) {
                                Text(
                                    text = streamLabel,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Text(
                                text = link.take(50) + if (link.length > 50) "..." else "",
                                style = MaterialTheme.typography.bodySmall,
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
