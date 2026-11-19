package com.example.livetv.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.livetv.data.model.Match
import com.example.livetv.ui.theme.Accent
import com.example.livetv.ui.theme.AppBackground
import com.example.livetv.ui.theme.CardRaised
import com.example.livetv.ui.theme.LiveRed
import com.example.livetv.ui.theme.PanelColor
import com.example.livetv.ui.theme.TextSecondary
import com.example.livetv.ui.updater.UpdateDialog
import com.example.livetv.ui.updater.UpdateViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MatchViewModel = viewModel()) {
    val visibleMatches by viewModel.visibleMatches
    val isLoading by viewModel.isLoadingInitialList
    val errorMessage by viewModel.errorMessage
    val isRefreshing by viewModel.isRefreshing
    val isLoadingMore by viewModel.isLoadingMore
    val favouritesOnly by viewModel.showFavouritesOnly
    val favouriteMatchUrls by viewModel.favouriteMatchUrls
    val section by viewModel.selectedSection

    val updateViewModel: UpdateViewModel = viewModel()
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isCompactScreen = configuration.screenWidthDp < 600
    val activity = LocalContext.current as? Activity

    val rootFocus = remember { FocusRequester() }
    val returnFocus = remember { FocusRequester() }
    var gridAreaHasFocus by remember { mutableStateOf(false) }

    // The match whose streams are open (null = none)
    var selectedUrl by remember { mutableStateOf<String?>(null) }
    val selectedMatch: Match? = visibleMatches.firstOrNull { it.detailPageUrl == selectedUrl }
    var lastShown by remember { mutableStateOf<Match?>(null) }
    LaunchedEffect(selectedMatch) { if (selectedMatch != null) lastShown = selectedMatch }

    fun closePanel() {
        selectedUrl = null
        try { returnFocus.requestFocus() } catch (_: Exception) {}
    }

    // Re-evaluate live / finished once a minute
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            now = System.currentTimeMillis()
        }
    }
    val feed = remember(visibleMatches, now) { buildFeed(visibleMatches, now) }

    // Spinner angle for the refresh button
    val isBusy = isRefreshing || viewModel.isBackgroundScraping.value
    val spin by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Restart),
        label = "spinAngle",
    )

    val gridState = rememberLazyGridState()
    val nearEnd by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 4
        }
    }
    LaunchedEffect(nearEnd, feed.size) {
        if (nearEnd && !viewModel.isSearchActive.value && !isLoadingMore && !isLoading) {
            viewModel.loadMoreMatches()
        }
    }

    // Back: close the panel first, then (TV) move focus to the header, then ask before exiting
    BackHandler(enabled = selectedUrl != null) { closePanel() }
    BackHandler(enabled = !isCompactScreen && selectedUrl == null && gridAreaHasFocus) {
        gridAreaHasFocus = false
        try { rootFocus.requestFocus() } catch (_: Exception) {}
    }
    BackHandler(enabled = selectedUrl == null && (isCompactScreen || !gridAreaHasFocus)) {
        showExitConfirmDialog = true
    }
    LaunchedEffect(isCompactScreen) {
        if (!isCompactScreen) {
            try { rootFocus.requestFocus() } catch (_: Exception) {}
        }
    }

    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title = { Text("Exit LiveTV?") },
            text = { Text("Close the app?") },
            confirmButton = { TextButton(onClick = { activity?.finish() }) { Text("Exit") } },
            dismissButton = { TextButton(onClick = { showExitConfirmDialog = false }) { Text("Cancel") } },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .focusRequester(rootFocus)
            .onKeyEvent { ev ->
                if (ev.type == KeyEventType.KeyUp && ev.key == Key.R) {
                    viewModel.refreshCurrentSection()
                    true
                } else false
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                compact = isCompactScreen,
                currentSection = section,
                favouritesOnly = favouritesOnly,
                onSectionChange = { s ->
                    viewModel.setFavouritesOnly(false)
                    viewModel.changeSection(s)
                },
                onFavourites = { viewModel.setFavouritesOnly(!favouritesOnly) },
                onSearch = { viewModel.activateSearch() },
                onRefresh = { viewModel.refreshCurrentSection() },
                onSettings = { showSettings = true },
                spin = if (isBusy) spin else 0f,
                showRefresh = !isCompactScreen,
            )
            if (isBusy) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = Accent, trackColor = Color.Transparent)
            }
            FilterBar(viewModel, horizontalPadding = if (isCompactScreen) 16 else 32)

            if (viewModel.isSearchActive.value) SearchBar(viewModel = viewModel)
            val teamSuggestions by viewModel.teamSuggestions
            val leagueSuggestions by viewModel.leagueSuggestions
            if (viewModel.isSearchActive.value && (teamSuggestions.isNotEmpty() || leagueSuggestions.isNotEmpty())) {
                SearchSuggestions(
                    suggestions = teamSuggestions,
                    leagueSuggestions = leagueSuggestions,
                    favouriteTeams = viewModel.favouriteTeams.value,
                    favouriteLeagues = viewModel.favouriteLeagues.value,
                    onSuggestionTap = { entry -> viewModel.updateSearchQuery(entry.name) },
                    onLeagueTap = { league -> viewModel.updateSearchQuery(league) },
                    onToggleTeamFavourite = { name -> viewModel.toggleFavouriteTeam(name) },
                    onToggleLeagueFavourite = { league -> viewModel.toggleFavouriteLeague(league) },
                )
            }

            val minCard = if (isCompactScreen) 320 else 280
            val contentArea: @Composable () -> Unit = {
                when {
                    isLoading -> SkeletonGrid(minCard)
                    errorMessage != null -> MessageState(
                        icon = Icons.Default.CloudOff,
                        title = "Couldn't load matches",
                        body = errorMessage.orEmpty(),
                        actionLabel = "Retry",
                        onAction = { viewModel.loadInitialMatchList() },
                    )
                    feed.isEmpty() -> MessageState(
                        icon = Icons.Default.SportsSoccer,
                        title = if (favouritesOnly) "No favourite matches" else "No matches found",
                        body = if (favouritesOnly) "Star a team or league in a match's details to see its games here."
                               else "Pull down or refresh to check again.",
                        actionLabel = if (favouritesOnly) "Show all matches" else "Refresh",
                        onAction = {
                            if (favouritesOnly) viewModel.setFavouritesOnly(false) else viewModel.loadInitialMatchList()
                        },
                    )
                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minCard.dp),
                        state = gridState,
                        modifier = Modifier
                            .fillMaxSize()
                            .focusProperties { canFocus = selectedUrl == null },
                        contentPadding = PaddingValues(
                            horizontal = if (isCompactScreen) 16.dp else 32.dp,
                            vertical = 8.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(
                            items = feed,
                            key = { item ->
                                when (item) {
                                    is FeedItem.Header -> "header:${item.title}"
                                    is FeedItem.Entry -> item.match.detailPageUrl
                                }
                            },
                            span = { item ->
                                if (item is FeedItem.Header) GridItemSpan(maxLineSpan) else GridItemSpan(1)
                            },
                            contentType = { item -> item::class },
                        ) { item ->
                            when (item) {
                                is FeedItem.Header -> SectionHeader(item)
                                is FeedItem.Entry -> MatchCard(
                                    match = item.match,
                                    phase = item.phase,
                                    isFavourite = item.match.detailPageUrl in favouriteMatchUrls,
                                    onClick = { selectedUrl = item.match.detailPageUrl },
                                    focusRequester = if (item.match.detailPageUrl == selectedUrl) returnFocus else null,
                                )
                            }
                        }
                        if (isLoadingMore) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = Accent)
                                }
                            }
                        }
                    }
                }
            }

            if (isCompactScreen) {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refreshCurrentSection() },
                    modifier = Modifier.weight(1f),
                ) { contentArea() }
            } else {
                Box(Modifier.weight(1f).onFocusChanged { gridAreaHasFocus = it.hasFocus }) { contentArea() }
            }
        }

        // ── Streams: side panel on TV / tablet, bottom sheet on phones ──────────
        if (!isCompactScreen) {
            AnimatedVisibility(visible = selectedUrl != null, enter = fadeIn(), exit = fadeOut()) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
            }
            AnimatedVisibility(
                visible = selectedUrl != null,
                modifier = Modifier.align(Alignment.CenterEnd),
                enter = slideInHorizontally(tween(220)) { it } + fadeIn(tween(220)),
                exit = slideOutHorizontally(tween(180)) { it } + fadeOut(tween(180)),
            ) {
                Surface(
                    modifier = Modifier.width(400.dp).fillMaxHeight(),
                    color = PanelColor,
                    shape = MaterialTheme.shapes.large.copy(topEnd = androidx.compose.foundation.shape.ZeroCornerSize, bottomEnd = androidx.compose.foundation.shape.ZeroCornerSize),
                ) {
                    lastShown?.let { m ->
                        StreamPanelContent(match = selectedMatch ?: m, viewModel = viewModel, onClose = { closePanel() })
                    }
                }
            }
        } else if (selectedMatch != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedUrl = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = PanelColor,
            ) {
                StreamPanelContent(
                    match = selectedMatch,
                    viewModel = viewModel,
                    onClose = { selectedUrl = null },
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            currentUrl = viewModel.getBaseUrl(),
            currentAcestreamIp = viewModel.getAcestreamIp(),
            onUrlUpdate = { viewModel.updateBaseUrl(it) },
            onResetUrl = { viewModel.resetBaseUrl() },
            onAcestreamIpUpdate = { viewModel.updateAcestreamIp(it) },
            onResetAcestreamIp = { viewModel.resetAcestreamIp() },
            onCheckForUpdates = { showUpdateDialog = true },
            onDismiss = { showSettings = false },
        )
    }
    if (showUpdateDialog) {
        UpdateDialog(updateViewModel = updateViewModel, onDismiss = { showUpdateDialog = false })
    }
}

@Composable
private fun SectionHeader(item: FeedItem.Header) {
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.title == "Live now") {
            Box(Modifier.size(8.dp).background(LiveRed, CircleShape))
            Box(Modifier.width(8.dp))
        }
        Text(item.title, style = MaterialTheme.typography.titleMedium)
        Box(
            Modifier.padding(start = 8.dp).background(CardRaised, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text("${item.count}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}
