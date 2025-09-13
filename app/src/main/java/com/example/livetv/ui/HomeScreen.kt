package com.example.livetv.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.livetv.data.model.Match

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
                // Main content list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    itemsIndexed(visibleMatches, key = { index, _ -> index }) { index, match ->
                        MatchItem(match = match)
                    }

                    item {
                        // "Load More" button
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(onClick = { viewModel.loadMoreMatches() }) {
                                Text("Load More")
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
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Time: ${match.time}")
            Text(text = "Teams: ${match.teams}")
            Text(text = "Competition: ${match.competition}")
            Spacer(modifier = Modifier.height(12.dp))

            when {
                match.areLinksLoading -> {
                    // Show a small loader while fetching links for this specific match
                    Row(verticalAlignment = Alignment.CenterVertically) {
                       CircularProgressIndicator(modifier = Modifier.size(20.dp))
                       Spacer(modifier = Modifier.width(8.dp))
                       Text("Searching for streams...")
                    }
                }
                match.streamLinks.isNotEmpty() -> {
                    Text(
                        text = "Available Streams (${match.streamLinks.size}):",
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Group streams by type for better organization
                    val streamsByType = match.streamLinks.groupBy { link ->
                        when {
                            link.startsWith("acestream://") -> "Acestream (P2P)"
                            link.contains(".m3u8") -> "M3U8/HLS"
                            link.startsWith("rtmp") -> "RTMP"
                            link.contains("youtube.com") || link.contains("youtu.be") -> "YouTube"
                            link.contains("twitch.tv") -> "Twitch"
                            else -> "Web Stream"
                        }
                    }
                    
                    // Display each stream type as a section
                    streamsByType.forEach { (type, links) ->
                        Text(
                            text = "$type (${links.size}):",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        
                        links.forEachIndexed { index, link ->
                            Button(
                                onClick = { /* TODO: Handle link click - could copy to clipboard or open in external app */ },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = when (type) {
                                        "Acestream (P2P)" -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green
                                        "M3U8/HLS" -> androidx.compose.ui.graphics.Color(0xFF2196F3) // Blue
                                        "RTMP" -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange
                                        "YouTube" -> androidx.compose.ui.graphics.Color(0xFFF44336) // Red
                                        "Twitch" -> androidx.compose.ui.graphics.Color(0xFF9C27B0) // Purple
                                        else -> androidx.compose.material3.MaterialTheme.colorScheme.primary
                                    }
                                )
                            ) {
                                Column {
                                    Text(
                                        text = "$type Stream ${index + 1}",
                                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = link.take(50) + if (link.length > 50) "..." else "",
                                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {
                    // Show if no links were found after searching
                    Text("No stream links found for this match.")
                }
            }
        }
    }
}
