package com.example.livetv.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.livetv.data.model.Match
import com.example.livetv.data.network.ScrapingSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MatchViewModel = viewModel()) {
    val visibleMatches by viewModel.visibleMatches
    val isLoading by viewModel.isLoadingInitialList
    val errorMessage by viewModel.errorMessage

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                // Initial loading screen
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Fetching match list...")
                }
            }
            errorMessage != null -> {
                // Error screen
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "Error: $errorMessage")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadInitialMatchList() }) {
                        Text(text = "Retry")
                    }
                }
            }
            visibleMatches.isEmpty() -> {
                 // Empty state after a successful load
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "No matches found.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadInitialMatchList() }) {
                        Text(text = "Refresh")
                    }
                }
            }
            else -> {
                // Main content with section selector and match grid
                Column(modifier = Modifier.fillMaxSize()) {
                    // Section selection row
                    SectionSelector(
                        currentSection = viewModel.selectedSection.value,
                        onSectionChange = { section -> viewModel.changeSection(section) },
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                    )
                    
                    // Match grid - optimized for TV viewing
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 400.dp), // Adaptive columns based on screen size
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp), // Space between rows
                        horizontalArrangement = Arrangement.spacedBy(8.dp) // Space between columns
                    ) {
                        itemsIndexed(visibleMatches, key = { index, _ -> "match_$index" }) { index, match ->
                            MatchItem(match = match)
                        }

                        item {
                        // "Load More" button - compact for TV
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = { viewModel.loadMoreMatches() },
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    "Load More Matches",
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
fun MatchItem(match: Match) {
    // Categorize links to determine card height
    val aceStreamLinks = match.streamLinks.filter { it.startsWith("acestream://") }
    val webStreamLinks = match.streamLinks.filter { !it.startsWith("acestream://") }
    val hasBothTypes = aceStreamLinks.isNotEmpty() && webStreamLinks.isNotEmpty()
    
    // Dynamic height based on content
    val cardHeight = if (hasBothTypes) 260.dp else 200.dp
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Team names - prominent display
            Text(
                text = match.teams.ifBlank { "Teams TBD" },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 6.dp),
                maxLines = 2,
                minLines = 2 // Ensure consistent height
            )
            
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
                    modifier = Modifier.padding(bottom = 6.dp), // Increased padding
                    maxLines = 2 // Allow 2 lines for longer text
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))

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
                        verticalArrangement = Arrangement.spacedBy(if (hasBothTypes) 4.dp else 6.dp)
                    ) {
                        // Acestream Section
                        if (aceStreamLinks.isNotEmpty()) {
                            Text(
                                text = "� ${aceStreamLinks.size} Acestream",
                                style = if (hasBothTypes) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                aceStreamLinks.take(3).forEachIndexed { index, link ->
                                    Button(
                                        onClick = { 
                                            // TODO: Open acestream link
                                            println("Opening acestream: $link")
                                        },
                                        modifier = Modifier.height(if (hasBothTypes) 28.dp else 32.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Text(
                                            text = "ACE ${index + 1}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                if (aceStreamLinks.size > 3) {
                                    Text(
                                        text = "+${aceStreamLinks.size - 3}",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(4.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Web Streams Section
                        if (webStreamLinks.isNotEmpty()) {
                            Text(
                                text = "🌐 ${webStreamLinks.size} Web Streams",
                                style = if (hasBothTypes) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                                    
                                    Button(
                                        onClick = { 
                                            // TODO: Open web stream link
                                            println("Opening web stream: $link")
                                        },
                                        modifier = Modifier.height(if (hasBothTypes) 28.dp else 32.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary,
                                            contentColor = MaterialTheme.colorScheme.onSecondary
                                        )
                                    ) {
                                        Text(
                                            text = streamLabel,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                if (webStreamLinks.size > 3) {
                                    Text(
                                        text = "+${webStreamLinks.size - 3}",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(4.dp),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionSelector(
    currentSection: ScrapingSection,
    onSectionChange: (ScrapingSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Select Section",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScrapingSection.values().forEach { section ->
                    FilterChip(
                        onClick = { onSectionChange(section) },
                        label = { 
                            Text(
                                text = section.displayName,
                                style = MaterialTheme.typography.labelMedium
                            ) 
                        },
                        selected = currentSection == section
                    )
                }
            }
        }
    }
}
