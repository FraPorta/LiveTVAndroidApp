package com.example.livetv.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.livetv.data.model.Match
import com.example.livetv.data.model.ScrapingSection
import com.example.livetv.ui.updater.UpdateDialog
import com.example.livetv.ui.updater.UpdateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MatchViewModel = viewModel()) {
    val visibleMatches by viewModel.visibleMatches
    val isLoading by viewModel.isLoadingInitialList
    val errorMessage by viewModel.errorMessage
    val isRefreshing by viewModel.isRefreshing
    
    // FIX #29: UpdateViewModel is now AndroidViewModel; the default viewModel() factory
    // automatically injects Application so no custom factory is needed.
    val updateViewModel: UpdateViewModel = viewModel()
    var showUpdateDialog by remember { mutableStateOf(false) }

    // Always show headers and main layout, only content area changes based on state
    val configuration = LocalConfiguration.current
    val isCompactScreen = configuration.screenWidthDp < 600 // Tablet breakpoint
    val focusRequester = remember { FocusRequester() }
    // Returns focus to the collapsed card after Back is pressed
    val collapsedCardFocusRequester = remember { FocusRequester() }

    // Which match card is currently expanded (null = all collapsed)
    var expandedMatchUrl by remember { mutableStateOf<String?>(null) }

    // Grid scroll state — used to scroll the expanded card into view
    val gridState = rememberLazyGridState()
    LaunchedEffect(expandedMatchUrl) {
        if (expandedMatchUrl != null) {
            val idx = visibleMatches.indexOfFirst { it.detailPageUrl == expandedMatchUrl }
            if (idx >= 0) {
                // Anchor card top to the grid top so the TV focus auto-scroll
                // for the stream buttons cannot push the card header off-screen
                gridState.animateScrollToItem(index = idx, scrollOffset = 0)
            }
        }
    }

    // Intercept the Activity back press when a card is expanded — collapse it instead of closing the app
    BackHandler(enabled = expandedMatchUrl != null) {
        expandedMatchUrl = null
        try { collapsedCardFocusRequester.requestFocus() } catch (_: Exception) {}
    }

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
                    
                    // Content area - pull-to-refresh only on phone; TV uses the Refresh button
                    val contentArea: @Composable () -> Unit = {
                        when {
                            isLoading -> {
                                // Loading state - only covers content area
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    SpinningIcon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Loading",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    )
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
                                // Match grid - responsive layout (1 column mobile, 2 columns TV)
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(if (isCompactScreen) 1 else 2),
                                    state = gridState,
                                    // Disable grid scroll while a card is expanded so TV focus
                                    // movements inside the card don't scroll the grid and hide the header
                                    userScrollEnabled = expandedMatchUrl == null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp), // Space between rows
                                    horizontalArrangement = Arrangement.spacedBy(8.dp) // Space between columns
                                ) {
                                    // FIX #19: Use the stable detailPageUrl as the item key
                                    // so Compose can correctly identity and diff items on recomposition
                                    // (index-based keys cause incorrect animations and unnecessary
                                    // recomposition when the list order changes).
                                    itemsIndexed(visibleMatches, key = { _, match -> match.detailPageUrl }) { _, match ->
                                        MatchItem(
                                            match = match,
                                            viewModel = viewModel,
                                            isExpanded = expandedMatchUrl == match.detailPageUrl,
                                            onExpand = { expandedMatchUrl = match.detailPageUrl },
                                            onCollapse = { expandedMatchUrl = null },
                                            collapsedFocusRequester = if (expandedMatchUrl == match.detailPageUrl)
                                                collapsedCardFocusRequester
                                            else
                                                null
                                        )
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
                    if (isCompactScreen) {
                        SwipeRefresh(
                            state = swipeRefreshState,
                            onRefresh = { viewModel.refreshCurrentSection() },
                            content = contentArea,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f)) {
                            contentArea()
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
    val isSpinning = isBackgroundScraping || isRefreshing
    val infiniteTransition = rememberInfiniteTransition(label = "actionSpin")
    val spinRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing)
        ),
        label = "spinRotation"
    )

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
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Loading",
                        tint = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = spinRotation }
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
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = if (isRefreshing) "Refreshing" else "Refresh matches",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = if (isRefreshing) {
                            Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = spinRotation }
                        } else {
                            Modifier.size(20.dp)
                        }
                    )
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
