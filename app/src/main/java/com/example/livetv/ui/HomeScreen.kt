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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                // Main content with responsive header and match grids
                val configuration = LocalConfiguration.current
                val isCompactScreen = configuration.screenWidthDp < 600 // Tablet breakpoint
                
                Column(modifier = Modifier.fillMaxSize()) {
                    if (isCompactScreen) {
                        // Smartphone layout - separate rows
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // URL configuration header
                            UrlConfigHeader(
                                currentUrl = viewModel.getBaseUrl(),
                                onUrlUpdate = { newUrl -> viewModel.updateBaseUrl(newUrl) },
                                onResetUrl = { viewModel.resetBaseUrl() },
                                onShowUpdateDialog = { showUpdateDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
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
                                onShowUpdateDialog = { showUpdateDialog = true },
                                modifier = Modifier.weight(2f)
                            )
                            
                            // Section selector (takes up less space)
                            SectionSelector(
                                currentSection = viewModel.selectedSection.value,
                                onSectionChange = { section -> viewModel.changeSection(section) },
                                modifier = Modifier.weight(1f),
                                isCompact = true
                            )
                        }
                    }
                    
                    // Match grid - optimized for TV viewing
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 400.dp), // Adaptive columns based on screen size
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp), // Space between rows
                        horizontalArrangement = Arrangement.spacedBy(8.dp) // Space between columns
                    ) {
                        itemsIndexed(visibleMatches, key = { index, _ -> "match_$index" }) { index, match ->
                            MatchItem(match = match, viewModel = viewModel)
                        }

                        item {
                        // Modern "Load More" button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center
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
    
    // Update Dialog
    if (showUpdateDialog) {
        UpdateDialog(
            updateViewModel = updateViewModel,
            onDismiss = { showUpdateDialog = false }
        )
    }
}

@Composable
fun MatchItem(match: Match, viewModel: MatchViewModel) {
    val context = LocalContext.current
    
    // Categorize links to determine card height
    val aceStreamLinks = match.streamLinks.filter { it.startsWith("acestream://") }
    val webStreamLinks = match.streamLinks.filter { !it.startsWith("acestream://") }
    val hasBothTypes = aceStreamLinks.isNotEmpty() && webStreamLinks.isNotEmpty()
    
    // State for popup dialogs
    var showAceStreamDialog by remember { mutableStateOf(false) }
    var showWebStreamDialog by remember { mutableStateOf(false) }
    
    // Dynamic height based on content - modern approach
    val cardHeight = if (hasBothTypes) 260.dp else 200.dp
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
                
                // Modern refresh button
                Box(
                    modifier = Modifier
                        .background(
                            color = if (match.areLinksLoading) 
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { viewModel.refreshMatchLinks(match) }
                        .padding(8.dp)
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
                        // Modern Acestream Section
                        if (aceStreamLinks.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 4.dp)
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
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { openUrlWithChooser(context, link) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "ACE ${index + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                                if (aceStreamLinks.size > 3) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { showAceStreamDialog = true }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
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
                                modifier = Modifier.padding(bottom = 4.dp)
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
                                    
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.secondary,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { openUrlWithChooser(context, link) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = streamLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondary
                                        )
                                    }
                                }
                                if (webStreamLinks.size > 3) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { showWebStreamDialog = true }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "+${webStreamLinks.size - 3}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
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
        }
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
            Text(
                text = "Section",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            
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
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (isSelected) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSectionChange(section) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
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
    } else {
        // Modern full version for vertical layout
        Column(modifier = modifier.fillMaxWidth()) {
            Text(
                text = "Select Section",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
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
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (isSelected) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSectionChange(section) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
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

/**
 * Helper function to open a URL with an intent chooser
 */
private fun openUrlWithChooser(context: Context, url: String) {
    try {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlConfigHeader(
    currentUrl: String,
    onUrlUpdate: (String) -> Unit,
    onResetUrl: () -> Unit,
    onShowUpdateDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editedUrl by remember { mutableStateOf(currentUrl) }
    
    // Update editedUrl when currentUrl changes
    LaunchedEffect(currentUrl) {
        editedUrl = currentUrl
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Scraping Source",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = currentUrl,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { showEditDialog = true }
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit URL",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onResetUrl() }
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Reset to Default",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onShowUpdateDialog() }
                        .padding(8.dp)
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
    
    // Edit URL Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { 
                showEditDialog = false 
                editedUrl = currentUrl // Reset to current URL if cancelled
            },
            title = { Text("Edit Scraping URL") },
            text = {
                Column {
                    Text(
                        "Enter the base URL for scraping match data:",
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
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editedUrl.isNotBlank() && editedUrl != currentUrl) {
                            onUrlUpdate(editedUrl)
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
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
