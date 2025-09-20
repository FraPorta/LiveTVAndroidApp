package com.example.livetv.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.*
import androidx.compose.ui.text.style.TextOverflow
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusTarget
import androidx.compose.foundation.focusable
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.livetv.data.model.Match
import com.example.livetv.data.network.ScrapingSection
import com.example.livetv.ui.updater.UpdateDialog
import com.example.livetv.ui.updater.UpdateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MatchViewModel = viewModel()) {
    val visibleMatches by viewModel.visibleMatches
    val isLoading by viewModel.isLoadingInitialList
    val errorMessage by viewModel.errorMessage
    val isRefreshing by viewModel.isRefreshing
    
    // Update functionality
    val context = LocalContext.current
    val updateViewModel: UpdateViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                UpdateViewModel(context)
            }
        }
    )
    var showUpdateDialog by remember { mutableStateOf(false) }

    // Always show headers and main layout, only content area changes based on state
    val configuration = LocalConfiguration.current
    val isCompactScreen = configuration.screenWidthDp < 600 // Tablet breakpoint
    val focusRequester = remember { FocusRequester() }
    
    // Pull-to-refresh state
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)
    
    // Request focus for TV key handling
    LaunchedEffect(isCompactScreen) {
        if (!isCompactScreen) {
            focusRequester.requestFocus()
        }
    }
    
    // Handle TV remote key presses
    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp) {
                    when (keyEvent.key) {
                        Key.R -> {
                            // R key to refresh (TV remote)
                            viewModel.refreshCurrentSection()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
                    if (isCompactScreen) {
                        // Smartphone layout - separate rows
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // URL configuration and action buttons row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // URL configuration header
                                UrlConfigHeader(
                                    currentUrl = viewModel.getBaseUrl(),
                                    onUrlUpdate = { newUrl -> viewModel.updateBaseUrl(newUrl) },
                                    onResetUrl = { viewModel.resetBaseUrl() },
                                    currentAcestreamIp = viewModel.getAcestreamIp(),
                                    onAcestreamIpUpdate = { newIp -> viewModel.updateAcestreamIp(newIp) },
                                    onResetAcestreamIp = { viewModel.resetAcestreamIp() },
                                    isCompact = true,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                // Action buttons (search and update) - compact version
                                ActionButtons(
                                    onSearchClick = { viewModel.activateSearch() },
                                    onShowUpdateDialog = { showUpdateDialog = true },
                                    onRefreshClick = { viewModel.refreshCurrentSection() },
                                    isBackgroundScraping = viewModel.isBackgroundScraping.value,
                                    isRefreshing = isRefreshing,
                                    isCompact = true,
                                    showRefreshButton = false // Mobile uses pull-to-refresh
                                )
                            }
                            
                            // Section selector
                            SectionSelector(
                                currentSection = viewModel.selectedSection.value,
                                onSectionChange = { section -> viewModel.changeSection(section) },
                                modifier = Modifier.fillMaxWidth(),
                                isCompact = false
                            )
                        }
                    } else {
                        // Tablet/TV layout - same row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // URL configuration header (takes up more space)
                            UrlConfigHeader(
                                currentUrl = viewModel.getBaseUrl(),
                                onUrlUpdate = { newUrl -> viewModel.updateBaseUrl(newUrl) },
                                onResetUrl = { viewModel.resetBaseUrl() },
                                currentAcestreamIp = viewModel.getAcestreamIp(),
                                onAcestreamIpUpdate = { newIp -> viewModel.updateAcestreamIp(newIp) },
                                onResetAcestreamIp = { viewModel.resetAcestreamIp() },
                                isCompact = false,
                                modifier = Modifier.weight(2f)
                            )
                            
                            // Section selector (takes up less space)
                            SectionSelector(
                                currentSection = viewModel.selectedSection.value,
                                onSectionChange = { section -> viewModel.changeSection(section) },
                                modifier = Modifier.weight(1f),
                                isCompact = true
                            )
                            
                            // Action buttons (search, refresh, and update)
                            ActionButtons(
                                onSearchClick = { viewModel.activateSearch() },
                                onShowUpdateDialog = { showUpdateDialog = true },
                                onRefreshClick = { viewModel.refreshCurrentSection() },
                                isBackgroundScraping = viewModel.isBackgroundScraping.value,
                                isRefreshing = isRefreshing,
                                isCompact = true,
                                showRefreshButton = true // TV/tablet shows refresh button
                            )
                        }
                    }
                    
                    // Search functionality - only show when active
                    if (viewModel.isSearchActive.value) {
                        SearchBar(viewModel = viewModel)
                    }
                    
                    // Content area - changes based on state with pull-to-refresh
                    SwipeRefresh(
                        state = swipeRefreshState,
                        onRefresh = { viewModel.refreshCurrentSection() }
                    ) {
                        when {
                            isLoading -> {
                                // Loading state - only covers content area
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
                                // Error screen - only covers content area
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
                                // Empty state - only covers content area
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
                                // Match grid - optimized for TV viewing
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2), // Fixed 2 columns for consistent row matching
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp), // Space between rows
                                    horizontalArrangement = Arrangement.spacedBy(8.dp) // Space between columns
                                ) {
                                    itemsIndexed(visibleMatches, key = { index, _ -> "match_$index" }) { index, match ->
                                        // Calculate if the adjacent card in the same row has both types
                                        val isEvenColumn = index % 2 == 0
                                        val adjacentIndex = if (isEvenColumn) index + 1 else index - 1
                                        val adjacentMatch = if (adjacentIndex < visibleMatches.size) visibleMatches[adjacentIndex] else null
                                        
                                        // Check if either current or adjacent card has both types
                                        val currentHasBothTypes = run {
                                            val aceStreamLinks = match.streamLinks.filter { url -> 
                                                url.startsWith("acestream://") || url.contains("/ace/getstream?id=") 
                                            }
                                            val webStreamLinks = match.streamLinks.filter { url -> 
                                                !url.startsWith("acestream://") && !url.contains("/ace/getstream?id=") 
                                            }
                                            aceStreamLinks.isNotEmpty() && webStreamLinks.isNotEmpty()
                                        }
                                        
                                        val adjacentHasBothTypes = adjacentMatch?.let { adjMatch ->
                                            val aceStreamLinks = adjMatch.streamLinks.filter { url -> 
                                                url.startsWith("acestream://") || url.contains("/ace/getstream?id=") 
                                            }
                                            val webStreamLinks = adjMatch.streamLinks.filter { url -> 
                                                !url.startsWith("acestream://") && !url.contains("/ace/getstream?id=") 
                                            }
                                            aceStreamLinks.isNotEmpty() && webStreamLinks.isNotEmpty()
                                        } ?: false
                                        
                                        val shouldUseUniformHeight = currentHasBothTypes || adjacentHasBothTypes
                                        
                                        MatchItem(match = match, viewModel = viewModel, forceUniformHeight = shouldUseUniformHeight)
                                    }

                                    // Only show "Load More" button when search is not active
                                    if (!viewModel.isSearchActive.value) {
                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            // Modern "Load More" button - spans all columns and centered
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            color = MaterialTheme.colorScheme.primaryContainer,
                                                            shape = RoundedCornerShape(16.dp)
                                                        )
                                                        .clickable { viewModel.loadMoreMatches() }
                                                        .padding(horizontal = 32.dp, vertical = 12.dp)
                                                ) {
                                                    Text(
                                                        "Load More Matches",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
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
    }
    
    // Update Dialog
    if (showUpdateDialog) {
        UpdateDialog(
            updateViewModel = updateViewModel,
            onDismiss = { showUpdateDialog = false }
        )
    }
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionSelector(
    currentSection: ScrapingSection,
    onSectionChange: (ScrapingSection) -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    if (isCompact) {
        // Modern compact version for horizontal layout
        Column(modifier = modifier) {
            // Modern segmented button style
            Row(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ScrapingSection.values().forEach { section ->
                    val isSelected = currentSection == section
                    Surface(
                        onClick = { onSectionChange(section) },
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = section.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) 
                                    MaterialTheme.colorScheme.onPrimary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Modern full version for vertical layout
        Column(modifier = modifier.fillMaxWidth()) {
            // Modern button group style
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ScrapingSection.values().forEach { section ->
                    val isSelected = currentSection == section
                    Surface(
                        onClick = { onSectionChange(section) },
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            Color.Transparent,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = section.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) 
                                    MaterialTheme.colorScheme.onPrimary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper function to open a URL with an intent chooser
 */
private fun openUrlWithChooser(context: Context, url: String) {
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
            android.util.Log.e("HomeScreen", "Failed to open URL: $url", e2)
        }
    }
}

private fun openWithVlc(context: Context, url: String) {
    try {
        // First try to open directly with VLC
        val vlcIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(url), "video/*")
            setPackage("org.videolan.vlc") // VLC package name
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        context.startActivity(vlcIntent)
        android.util.Log.d("HomeScreen", "Opened acestream HTTP URL with VLC: $url")
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
            android.util.Log.d("HomeScreen", "Opened acestream HTTP URL with chooser: $url")
        } catch (e2: Exception) {
            android.util.Log.e("HomeScreen", "Failed to open acestream URL: $url", e2)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlConfigHeader(
    currentUrl: String,
    onUrlUpdate: (String) -> Unit,
    onResetUrl: () -> Unit,
    currentAcestreamIp: String,
    onAcestreamIpUpdate: (String) -> Unit,
    onResetAcestreamIp: () -> Unit,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editedUrl by remember { mutableStateOf(currentUrl) }
    var editedAcestreamIp by remember { mutableStateOf(currentAcestreamIp) }
    
    // Update edited values when current values change
    LaunchedEffect(currentUrl, currentAcestreamIp) {
        editedUrl = currentUrl
        editedAcestreamIp = currentAcestreamIp
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isCompact) {
                // Mobile layout - more compact vertical arrangement with padding before buttons
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp) // Add padding between text and buttons
                ) {
                    // Compact source display
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = currentUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .weight(1f)
                        )
                    }
                    
                    // Compact acestream IP display
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Router,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Acestream: $currentAcestreamIp",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            } else {
                // Tablet/TV layout - horizontal arrangement with texts close to each other
                Column(modifier = Modifier.weight(1f)) {
                    // Both texts in close horizontal arrangement
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Scraping Source
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Scraping Source:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }
                            Text(
                                text = currentUrl,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 22.dp)
                            )
                        }
                        
                        // Acestream IP
                        Column(modifier = Modifier.weight(0.6f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Router,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Acestream IP:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }
                            Text(
                                text = currentAcestreamIp,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(start = 22.dp)
                            )
                        }
                    }
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FocusableButton(
                    onClick = { showEditDialog = true },
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    focusColor = MaterialTheme.colorScheme.tertiary
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Configuration",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                FocusableButton(
                    onClick = onResetUrl,
                    backgroundColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    focusColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Reset to Default",
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
    
    // Edit URL Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { 
                showEditDialog = false 
                editedUrl = currentUrl // Reset to current URL if cancelled
                editedAcestreamIp = currentAcestreamIp // Reset to current IP if cancelled
            },
            title = { Text("Configuration") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Base URL section
                    Column {
                        Text(
                            "Base URL for scraping match data:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = editedUrl,
                            onValueChange = { editedUrl = it },
                            label = { Text("Base URL") },
                            placeholder = { Text("https://livetv.sx/enx/allupcomingsports/1/") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Acestream IP section
                    Column {
                        Text(
                            "Acestream engine IP address:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = editedAcestreamIp,
                            onValueChange = { editedAcestreamIp = it },
                            label = { Text("Acestream IP") },
                            placeholder = { Text("127.0.0.1") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        var hasChanges = false
                        if (editedUrl.isNotBlank() && editedUrl != currentUrl) {
                            onUrlUpdate(editedUrl)
                            hasChanges = true
                        }
                        if (editedAcestreamIp.isNotBlank() && editedAcestreamIp != currentAcestreamIp) {
                            onAcestreamIpUpdate(editedAcestreamIp)
                            hasChanges = true
                        }
                        showEditDialog = false
                    }
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showEditDialog = false
                        editedUrl = currentUrl
                        editedAcestreamIp = currentAcestreamIp
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ActionButtons(
    onSearchClick: () -> Unit,
    onShowUpdateDialog: () -> Unit,
    onRefreshClick: () -> Unit = {},
    isBackgroundScraping: Boolean = false,
    isRefreshing: Boolean = false,
    isCompact: Boolean = false,
    showRefreshButton: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search button
            FocusableButton(
                onClick = onSearchClick,
                backgroundColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
                focusColor = MaterialTheme.colorScheme.primary
            ) {
                if (isBackgroundScraping) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onTertiary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search matches",
                        tint = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Refresh button (for TV/tablet)
            if (showRefreshButton) {
                FocusableButton(
                    onClick = onRefreshClick,
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    focusColor = MaterialTheme.colorScheme.tertiary
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh matches",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            // Update button
            FocusableButton(
                onClick = onShowUpdateDialog,
                backgroundColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                contentColor = MaterialTheme.colorScheme.onSecondary,
                focusColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Check for Updates",
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun FocusableButton(
    onClick: () -> Unit,
    backgroundColor: Color,
    contentColor: Color,
    focusColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    // Animated focus scale and alpha effects
    val focusScale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = tween(200),
        label = "focusScale"
    )
    
    val focusAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(200),
        label = "focusAlpha"
    )
    
    // Simple focus background color
    val focusedBackgroundColor = if (isFocused) {
        backgroundColor.copy(alpha = 0.9f)
    } else {
        backgroundColor
    }
    
    Surface(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer {
                scaleX = focusScale
                scaleY = focusScale
            },
        shape = RoundedCornerShape(12.dp),
        color = focusedBackgroundColor,
        border = if (isFocused) {
            BorderStroke(width = 2.dp, color = focusColor.copy(alpha = focusAlpha))
        } else null,
        shadowElevation = if (isFocused) 6.dp else 0.dp,
        interactionSource = interactionSource,
        content = {
            Box(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    viewModel: MatchViewModel,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery
    val searchFocusRequester = remember { FocusRequester() }
    
    // Auto-focus when search becomes active
    LaunchedEffect(Unit) {
        searchFocusRequester.requestFocus()
    }
    
    // Active search bar - only shown when search is active
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp)
        )
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Search teams, players, leagues...") },
            modifier = Modifier
                .weight(1f)
                .focusRequester(searchFocusRequester),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent
            )
        )
        
        IconButton(onClick = { viewModel.deactivateSearch() }) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close search",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
