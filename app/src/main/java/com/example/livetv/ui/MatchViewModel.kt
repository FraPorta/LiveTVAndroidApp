package com.example.livetv.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.livetv.data.local.FavouritesPreferences
import com.example.livetv.data.local.TeamDatabase
import com.example.livetv.data.local.TeamMatcher
import com.example.livetv.data.model.Match
import com.example.livetv.data.model.TeamEntry
import com.example.livetv.data.repository.MatchRepository
import com.example.livetv.data.model.ScrapingSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val INITIAL_LOAD_SIZE = 16
const val LOAD_MORE_SIZE = 10

class MatchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MatchRepository(application)
    private val favouritesPrefs = FavouritesPreferences(application)

    // Section-based caching for matches and state
    private data class SectionData(
        val allMatches: List<Match> = emptyList(),
        val hasLoadedAllMatches: Boolean = false,
        val totalFetchedMatches: Int = 0,
        val currentVisibleCount: Int = 0,
        val selectedSport: String? = null,
        val selectedLeague: String? = null,
        val availableSports: List<String> = emptyList(),
        val availableLeagues: List<String> = emptyList()
    )
    
    private val sectionCache = mutableMapOf<ScrapingSection, SectionData>()

    // Single source of truth: all mutable section state lives in the cache.
    // Computed properties give callers the same read/write syntax as plain fields.
    private fun currentSectionData(): SectionData =
        sectionCache.getOrPut(selectedSection.value) { SectionData() }

    private var allMatches: List<Match>
        get() = currentSectionData().allMatches
        set(value) { sectionCache[selectedSection.value] = currentSectionData().copy(allMatches = value) }

    private var hasLoadedAllMatches: Boolean
        get() = currentSectionData().hasLoadedAllMatches
        set(value) { sectionCache[selectedSection.value] = currentSectionData().copy(hasLoadedAllMatches = value) }

    private var totalFetchedMatches: Int
        get() = currentSectionData().totalFetchedMatches
        set(value) { sectionCache[selectedSection.value] = currentSectionData().copy(totalFetchedMatches = value) }

    // The list of matches currently displayed on the UI
    val visibleMatches = mutableStateOf<List<Match>>(emptyList())

    // Available filter options
    val availableSports = mutableStateOf<List<String>>(emptyList())
    val availableLeagues = mutableStateOf<List<String>>(emptyList())

    // Current filter selections
    val selectedSport = mutableStateOf<String?>(null)
    val selectedLeague = mutableStateOf<String?>(null)
    val selectedSection = mutableStateOf<ScrapingSection>(ScrapingSection.FOOTBALL)

    // Search functionality
    val searchQuery = mutableStateOf("")
    val isSearchActive = mutableStateOf(false)
    val isBackgroundScraping = mutableStateOf(false)

    // ── Favourites state ──────────────────────────────────────────────────────
    val favouriteTeams   = mutableStateOf<Set<String>>(emptySet())
    val favouriteLeagues = mutableStateOf<Set<String>>(emptySet())

    // ── Team DB search suggestions ────────────────────────────────────────────
    /** Populated while search is active and query is non-blank. */
    val teamSuggestions   = mutableStateOf<List<TeamEntry>>(emptyList())
    /** League suggestions populated while search is active and query is non-blank. */
    val leagueSuggestions = mutableStateOf<List<String>>(emptyList())

    /**
     * Pre-computed set of [Match.detailPageUrl] values for all matches that are currently
     * considered a favourite (team or league match). Recomputed off the main thread whenever
     * [toggleFavouriteTeam] or [toggleFavouriteLeague] is called. UI and [getFilteredMatches]
     * do a cheap O(1) lookup against this set instead of calling [isFavouriteMatch] per item.
     */
    val favouriteMatchUrls = mutableStateOf<Set<String>>(emptySet())

    private var currentVisibleCount: Int
        get() = currentSectionData().currentVisibleCount
        set(value) { sectionCache[selectedSection.value] = currentSectionData().copy(currentVisibleCount = value) }

    val isLoadingInitialList = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val isRefreshing = mutableStateOf(false)
    // FIX #6: Guard against concurrent invocations of loadMoreMatches() that would
    // append duplicate entries to visibleMatches.
    val isLoadingMore = mutableStateOf(false)

    init {
        Log.d("ViewModel", "MatchViewModel initialized.")
        // Load persisted favourites synchronously (fast SharedPreferences reads)
        favouriteTeams.value   = favouritesPrefs.getFavouriteTeams()
        favouriteLeagues.value = favouritesPrefs.getFavouriteLeagues()
        // Initialise the offline team DB off the main thread, then trigger the first
        // network fetch only after the DB is ready.
        viewModelScope.launch {
            withContext(Dispatchers.IO) { TeamDatabase.init(application.assets) }
            loadInitialMatchList()
        }
    }
    
    /**
     * Restores the observable filter state (mutableStateOf) from the cache for [section].
     * Returns true if valid cached data exists.
     */
    private fun restoreSectionData(section: ScrapingSection): Boolean {
        val cached = sectionCache[section]
        if (cached == null || cached.allMatches.isEmpty()) {
            Log.d("ViewModel", "No cached data found for section: ${section.displayName}")
            return false
        }
        selectedSport.value = cached.selectedSport
        selectedLeague.value = cached.selectedLeague
        availableSports.value = cached.availableSports
        availableLeagues.value = cached.availableLeagues
        Log.d("ViewModel", "Restored ${cached.allMatches.size} matches for section: ${section.displayName}")
        return true
    }

    fun loadInitialMatchList() {
        Log.d("ViewModel", "loadInitialMatchList called.")
        viewModelScope.launch {
            isLoadingInitialList.value = true
            errorMessage.value = null
            try {
                // Reset pagination state
                hasLoadedAllMatches = false
                totalFetchedMatches = 0
                allMatches = emptyList()
                
                Log.d("ViewModel", "Fetching initial batch of matches (${INITIAL_LOAD_SIZE}) from section: ${selectedSection.value.displayName}")
                val initialMatches = repository.getMatchList(
                    section = selectedSection.value,
                    limit = INITIAL_LOAD_SIZE,
                    offset = 0
                )
                
                allMatches = initialMatches
                totalFetchedMatches = initialMatches.size
                recomputeFavouriteMatchUrls()
                
                Log.d("ViewModel", "Repository returned ${initialMatches.size} matches from ${selectedSection.value.displayName} section.")
                
                // For now, we'll fetch filter options when needed
                // This avoids loading all matches upfront just for filtering
                updateFilterOptionsFromCurrentMatches()
                
                currentVisibleCount = 0
                visibleMatches.value = emptyList()
                loadMoreMatches() // Load the first batch for display
                
                // Start background scraping for all matches to enable search
                startBackgroundScraping()
            } catch (e: Exception) {
                Log.e("ViewModel", "Error loading initial match list", e)
                errorMessage.value = e.message ?: "Failed to load match list"
            } finally {
                isLoadingInitialList.value = false
                Log.d("ViewModel", "loadInitialMatchList finished.")
            }
        }
    }

    /**
     * Changes the scraping section and restores cached data if available
     */
    fun changeSection(section: ScrapingSection) {
        Log.d("ViewModel", "Changing section from ${selectedSection.value.displayName} to ${section.displayName}")
        
        // Update selected section (setters on allMatches etc. already write to the old
        // section's cache entry, so no explicit save step is needed)
        selectedSection.value = section
        
        // Try to restore cached data for the new section
        if (restoreSectionData(section)) {
            // We have cached data; recompute favourite URLs against this section's
            // allMatches so pinning always reflects the restored data set.
            recomputeFavouriteMatchUrls()
        } else {
            // No cached data, load fresh data
            loadInitialMatchList()
        }
    }

    fun loadMoreMatches() {
        if (isLoadingMore.value) return
        isLoadingMore.value = true
        viewModelScope.launch {
            try {
                loadMoreMatchesInternal()
            } finally {
                isLoadingMore.value = false
            }
        }
    }

    // FIX #6: Core logic extracted so the recursive "fetch next page" call bypasses the
    // public guard (which would be set to true and block it) while still being protected
    // against concurrent external calls via loadMoreMatches().
    private suspend fun loadMoreMatchesInternal() {
        val filteredMatches = getFilteredMatches()
        Log.d("ViewModel", "loadMoreMatches called. Current visible: $currentVisibleCount / ${filteredMatches.size} (filtered from ${allMatches.size} total)")

        val nextLoadCount = if (currentVisibleCount == 0) INITIAL_LOAD_SIZE else LOAD_MORE_SIZE
        val newVisibleCount = (currentVisibleCount + nextLoadCount).coerceAtMost(filteredMatches.size)

        if (newVisibleCount > currentVisibleCount) {
            // We have enough matches locally, use them
            val nextBatch = filteredMatches.subList(currentVisibleCount, newVisibleCount)
            Log.d("ViewModel", "Loading next batch of ${nextBatch.size} matches from local data.")
            visibleMatches.value = visibleMatches.value + nextBatch
            currentVisibleCount = newVisibleCount

            // Fetch links for the new batch
            fetchLinksForBatch(nextBatch)
        } else if (!hasLoadedAllMatches && !hasActiveFilters()) {
            // We don't have enough local matches and no filters are active,
            // so fetch more from the repository
            try {
                Log.d("ViewModel", "Fetching more matches from repository. Current total: $totalFetchedMatches")
                val moreMatches = repository.getMatchList(
                    section = selectedSection.value,
                    limit = LOAD_MORE_SIZE,
                    offset = totalFetchedMatches
                )

                if (moreMatches.isNotEmpty()) {
                    allMatches = allMatches + moreMatches
                    totalFetchedMatches += moreMatches.size
                    recomputeFavouriteMatchUrls()
                    Log.d("ViewModel", "Fetched ${moreMatches.size} more matches. Total now: $totalFetchedMatches")

                    // Update filter options with new matches
                    updateFilterOptionsFromCurrentMatches()

                    // Continue loading — call internal directly to bypass the public guard
                    loadMoreMatchesInternal()
                } else {
                    hasLoadedAllMatches = true
                    Log.d("ViewModel", "No more matches available from repository. Total fetched: $totalFetchedMatches")
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Error fetching more matches", e)
            }
        } else {
            Log.d("ViewModel", "No more matches to load. HasLoadedAll: $hasLoadedAllMatches, HasFilters: ${hasActiveFilters()}")
        }
    }

    private fun fetchLinksForBatchIfNeeded(batch: List<Match>) {
        // Only fetch links for matches that don't already have them
        val matchesNeedingLinks = batch.filter { match ->
            // Check if links are empty AND not currently loading AND not already found in allMatches
            val hasNoLinks = match.streamLinks.isEmpty()
            val notCurrentlyLoading = !match.areLinksLoading
            
            // Also check if this match in allMatches already has links
            val matchInAllMatches = allMatches.find { it.detailPageUrl == match.detailPageUrl }
            val alreadyHasLinksInAllMatches = matchInAllMatches?.streamLinks?.isNotEmpty() == true
            
            hasNoLinks && notCurrentlyLoading && !alreadyHasLinksInAllMatches
        }
        
        if (matchesNeedingLinks.isNotEmpty()) {
            fetchLinksForBatch(matchesNeedingLinks)
        }
        
        Log.d("ViewModel", "Links needed for ${matchesNeedingLinks.size} out of ${batch.size} matches (after checking allMatches cache)")
    }
    
    private fun fetchLinksForBatch(batch: List<Match>) {
        if (batch.isEmpty()) return
        Log.d("ViewModel", "Fetching links for a batch of ${batch.size} matches (concurrent).")
        viewModelScope.launch {
            // 1. Mark entire batch as loading in a single state write
            val batchByUrl = batch.associateBy { it.detailPageUrl }
            applyMatchUpdates(batchByUrl.mapValues { (_, m) -> m.copy(areLinksLoading = true) })

            // 2. Fire all HTTP requests concurrently
            val results = coroutineScope {
                batch.map { match ->
                    async {
                        match.detailPageUrl to runCatching {
                            repository.getStreamLinks(match.detailPageUrl)
                        }
                    }
                }.awaitAll()
            }

            // 3. Apply all results in a single state write
            val updates = results.associate { (url, result) ->
                val original = batchByUrl[url]!!
                url to result.fold(
                    onSuccess = { r -> original.copy(streamLinks = r.links, areLinksLoading = false, homeLogoUrl = r.homeLogoUrl ?: original.homeLogoUrl, awayLogoUrl = r.awayLogoUrl ?: original.awayLogoUrl) },
                    onFailure = { original.copy(areLinksLoading = false) }
                )
            }
            applyMatchUpdates(updates)
            Log.d("ViewModel", "Batch link fetch complete: ${results.size} matches updated.")
        }
    }

    /** Applies a map of url→Match updates to both [visibleMatches] and [allMatches] in one pass each. */
    private fun applyMatchUpdates(updates: Map<String, Match>) {
        visibleMatches.value = visibleMatches.value.map { m -> updates[m.detailPageUrl] ?: m }
        allMatches = allMatches.map { m -> updates[m.detailPageUrl] ?: m }
    }

    private fun updateMatchInList(updatedMatch: Match) {
        applyMatchUpdates(mapOf(updatedMatch.detailPageUrl to updatedMatch))
    }
    
    private fun getFilteredMatches(): List<Match> {
        var filtered = allMatches.filter { match ->
            val sportMatches = selectedSport.value == null || 
                              selectedSport.value == "All Sports" || 
                              match.sport == selectedSport.value
            
            val leagueMatches = selectedLeague.value == null || 
                               selectedLeague.value == "All Leagues" || 
                               match.league == selectedLeague.value ||
                               match.qualifiedLeagueKey == selectedLeague.value
            
            sportMatches && leagueMatches
        }

        // Pin favourite matches to the top of every section (O(1) set lookup, not linear scan)
        val favUrls = favouriteMatchUrls.value
        val (favs, rest) = filtered.partition { it.detailPageUrl in favUrls }
        filtered = favs + rest
        
        // Apply search filter if active
        if (isSearchActive.value && searchQuery.value.isNotBlank()) {
            val searchText = searchQuery.value.trim()
            // If the query resolves to a known team via the same fuzzy lookup used by
            // favourites, use that instead of a plain contains() so aliases are handled.
            val targetEntry = TeamMatcher.lookupTeam(searchText)
            filtered = if (targetEntry != null) {
                filtered.filter { match ->
                    val parts = match.teams.split(" vs ", " v ", " – ", " — ", " - ", limit = 2)
                    parts.any { part ->
                        val resolved = TeamMatcher.lookupTeam(part.trim(), match.qualifiedLeagueKey)
                        resolved != null && resolved.name == targetEntry.name
                    }
                }
            } else {
                // Free-text fallback: use pre-computed searchableText
                val lowerText = searchText.lowercase()
                filtered.filter { match -> match.searchableText.contains(lowerText) }
            }
        }
        
        return filtered
    }

    /**
     * Recomputes [favouriteMatchUrls] synchronously on the main thread and immediately
     * refreshes visible matches, so the gold accent and pinned ordering appear at once
     * when a favourite is toggled. TeamMatcher uses hash-map lookups and the match list
     * is at most a few hundred items, so this completes in microseconds.
     */
    private fun recomputeFavouriteMatchUrls() {
        val currentMatches = allMatches
        val favTeams   = favouriteTeams.value
        val favLeagues = favouriteLeagues.value
        val urls: Set<String> = if (favTeams.isEmpty() && favLeagues.isEmpty()) {
            emptySet()
        } else {
            currentMatches
                .filter { isFavouriteMatch(it) }
                .mapTo(HashSet()) { it.detailPageUrl }
        }
        favouriteMatchUrls.value = urls
        refreshVisibleMatches()
    }

    fun setSportFilter(sport: String?) {
        selectedSport.value = if (sport == "All Sports") null else sport
        sectionCache[selectedSection.value] = currentSectionData().copy(selectedSport = selectedSport.value)
        applyFilters()
    }

    fun setLeagueFilter(league: String?) {
        selectedLeague.value = if (league == "All Leagues") null else league
        sectionCache[selectedSection.value] = currentSectionData().copy(selectedLeague = selectedLeague.value)
        applyFilters()
    }

    private fun applyFilters() {
        Log.d("ViewModel", "Applying filters - Sport: ${selectedSport.value}, League: ${selectedLeague.value}")
        viewModelScope.launch {
            // When filters are applied, we need to ensure we have all matches
            ensureAllMatchesLoaded()
            
            currentVisibleCount = INITIAL_LOAD_SIZE
            refreshVisibleMatches()
        }
    }

    /**
     * Refresh stream links for a specific match
     */
    fun refreshMatchLinks(match: Match) {
        Log.d("ViewModel", "Refreshing links for match: ${match.teams}")
        viewModelScope.launch {
            // Set loading state for this specific match
            updateMatchInList(match.copy(areLinksLoading = true, streamLinks = emptyList()))

            try {
                val result = repository.getStreamLinks(match.detailPageUrl)
                Log.d("ViewModel", "Refreshed ${result.links.size} links for ${match.teams}")
                // Update match with refreshed links and any newly scraped logos
                updateMatchInList(match.copy(
                    streamLinks = result.links,
                    areLinksLoading = false,
                    homeLogoUrl = result.homeLogoUrl ?: match.homeLogoUrl,
                    awayLogoUrl = result.awayLogoUrl ?: match.awayLogoUrl,
                ))
            } catch (e: Exception) {
                Log.e("ViewModel", "Error refreshing links for ${match.teams}", e)
                // Restore the match without loading state but keep existing links
                updateMatchInList(match.copy(areLinksLoading = false))
            }
        }
    }

    /**
     * Check if any filters are currently active
     */
    private fun hasActiveFilters(): Boolean {
        return selectedSport.value != null || selectedLeague.value != null
    }

    /**
     * Update filter options based on currently loaded matches
     * This is more efficient than loading all matches upfront
     */
    private fun updateFilterOptionsFromCurrentMatches() {
        val sports = allMatches.map { it.sport }.distinct().sorted()
        val leagues = allMatches.map { it.league }.distinct().sorted()
        availableSports.value = listOf("All Sports") + sports
        availableLeagues.value = listOf("All Leagues") + leagues
        // Persist so restoreSectionData can reinstate them on section switch
        sectionCache[selectedSection.value] = currentSectionData().copy(
            availableSports = availableSports.value,
            availableLeagues = availableLeagues.value
        )
        Log.d("ViewModel", "Updated filter options from ${allMatches.size} matches - Sports: ${sports.size}, Leagues: ${leagues.size}")
    }

    /**
     * Load all matches if needed for filtering
     * This is only called when filters are applied
     */
    private suspend fun ensureAllMatchesLoaded() {
        if (!hasLoadedAllMatches) {
            try {
                Log.d("ViewModel", "Loading all matches for filtering...")
                val allAvailableMatches = repository.getMatchList(selectedSection.value) // No limit = get all
                allMatches = allAvailableMatches
                hasLoadedAllMatches = true
                totalFetchedMatches = allAvailableMatches.size
                recomputeFavouriteMatchUrls()
                updateFilterOptionsFromCurrentMatches()
                Log.d("ViewModel", "Loaded all ${allAvailableMatches.size} matches for filtering")
            } catch (e: Exception) {
                Log.e("ViewModel", "Error loading all matches for filtering", e)
            }
        }
    }
    
    /**
     * Gets the current base URL being used for scraping
     */
    fun getBaseUrl(): String {
        return repository.getBaseUrl()
    }
    
    /**
     * Updates the base URL and reloads the match list
     */
    fun updateBaseUrl(newUrl: String) {
        repository.updateBaseUrl(newUrl)
        // Reload the match list with the new URL
        loadInitialMatchList()
    }
    
    /**
     * Resets the base URL to default and reloads the match list
     */
    fun resetBaseUrl() {
        repository.resetBaseUrl()
        // Reload the match list with the default URL
        loadInitialMatchList()
    }
    
    /**
     * Gets the current acestream engine IP address
     */
    fun getAcestreamIp(): String {
        return repository.getAcestreamIp()
    }
    
    /**
     * Updates the acestream engine IP address
     */
    fun updateAcestreamIp(newIp: String) {
        repository.updateAcestreamIp(newIp)
    }
    
    /**
     * Resets the acestream IP to default (127.0.0.1)
     */
    fun resetAcestreamIp() {
        repository.resetAcestreamIp()
    }
    
    /**
     * Refreshes all matches and links for the current section
     * This clears the cache for the current section and reloads everything fresh
     */
    fun refreshCurrentSection() {
        Log.d("ViewModel", "Refreshing current section: ${selectedSection.value.displayName}")
        viewModelScope.launch {
            isRefreshing.value = true
            errorMessage.value = null
            
            try {
                // Clear the cache for the current section to force fresh data
                val currentSection = selectedSection.value
                sectionCache.remove(currentSection)
                Log.d("ViewModel", "Cleared cache for section: ${currentSection.displayName}")
                
                // Clear current data
                allMatches = emptyList()
                hasLoadedAllMatches = false
                totalFetchedMatches = 0
                currentVisibleCount = 0
                visibleMatches.value = emptyList()
                
                // Reset filters for this section
                selectedSport.value = null
                selectedLeague.value = null
                availableSports.value = emptyList()
                availableLeagues.value = emptyList()
                
                // Load fresh data
                Log.d("ViewModel", "Loading fresh data for section: ${currentSection.displayName}")
                val freshMatches = repository.getMatchList(
                    section = currentSection,
                    limit = INITIAL_LOAD_SIZE,
                    offset = 0
                )
                
                allMatches = freshMatches
                totalFetchedMatches = freshMatches.size
                
                // Update filter options
                updateFilterOptionsFromCurrentMatches()
                
                // Load the first batch for display
                loadMoreMatches()
                
                // Start background scraping for complete data
                startBackgroundScraping()
                
                Log.d("ViewModel", "Section refresh completed: ${freshMatches.size} matches loaded")
                
            } catch (e: Exception) {
                Log.e("ViewModel", "Error refreshing section", e)
                errorMessage.value = "Failed to refresh: ${e.message}"
            } finally {
                isRefreshing.value = false
            }
        }
    }
    
    /**
     * Starts background scraping to load all available matches for search functionality
     */
    private fun startBackgroundScraping() {
        viewModelScope.launch {
            try {
                isBackgroundScraping.value = true
                Log.d("ViewModel", "Starting background scraping for all matches...")
                
                val allBackgroundMatches = repository.getAllMatches(selectedSection.value)
                Log.d("ViewModel", "Background scraping completed: ${allBackgroundMatches.size} matches")
                
                // Replace our current list with the full list
                allMatches = allBackgroundMatches
                hasLoadedAllMatches = true
                totalFetchedMatches = allBackgroundMatches.size
                
                // Update filter options with the complete list
                updateFilterOptionsFromCurrentMatches()

                // Re-pin favourites now that allMatches contains the full set — without
                // this, matches that appear beyond the initial page are never marked as
                // favourites until the user switches tabs or toggles a favourite manually.
                recomputeFavouriteMatchUrls()
                
                Log.d("ViewModel", "Background scraping updated match list: ${allMatches.size} total matches available for search")
                
            } catch (e: Exception) {
                Log.e("ViewModel", "Error in background scraping", e)
            } finally {
                isBackgroundScraping.value = false
            }
        }
    }
    
    /**
     * Activates search mode
     */
    fun activateSearch() {
        isSearchActive.value = true
        Log.d("ViewModel", "Search activated")
    }
    
    /**
     * Deactivates search mode and clears the search query
     */
    fun deactivateSearch() {
        isSearchActive.value = false
        searchQuery.value = ""
        teamSuggestions.value = emptyList()
        leagueSuggestions.value = emptyList()
        refreshVisibleMatches()
        Log.d("ViewModel", "Search deactivated")
    }
    
    private var searchDebounceJob: Job? = null

    /**
     * Updates the search query and filters matches.
     * Suggestions and refreshVisibleMatches are debounced by 150 ms on a background thread
     * to avoid doing heavy work on every keystroke.
     */
    fun updateSearchQuery(query: String) {
        searchQuery.value = query
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch(Dispatchers.Default) {
            delay(150L)
            val suggestions = if (query.isNotBlank()) TeamMatcher.teamsMatchingQuery(query) else emptyList()
            val leagues = if (query.isNotBlank())
                TeamDatabase.getAllLeagues().filter { it.contains(query, ignoreCase = true) }.take(6)
            else emptyList()
            withContext(Dispatchers.Main) {
                teamSuggestions.value = suggestions
                leagueSuggestions.value = leagues
                refreshVisibleMatches()
            }
        }
        Log.d("ViewModel", "Search query updated: '$query'")
    }
    
    /**
     * Refreshes the visible matches based on current filters and search query
     */
    private fun refreshVisibleMatches() {
        val filteredMatches = getFilteredMatches()
        val searchedMatches = filteredMatches
        // O(1) lookup index — replaces the O(n) allMatches.find{} scan in both branches below
        val latestData = allMatches.associateBy { it.detailPageUrl }

        // Update visible matches - show all matching results when searching
        if (isSearchActive.value && searchQuery.value.isNotBlank()) {
            // When searching, show all matching results immediately
            currentVisibleCount = searchedMatches.size
            val matchesWithLatestData = searchedMatches.map { latestData[it.detailPageUrl] ?: it }
            visibleMatches.value = matchesWithLatestData
            if (matchesWithLatestData.isNotEmpty()) fetchLinksForBatchIfNeeded(matchesWithLatestData)
        } else {
            // When not searching, use pagination logic
            val maxToShow = currentVisibleCount.coerceAtLeast(INITIAL_LOAD_SIZE)
            val toShow = searchedMatches.take(maxToShow)
            currentVisibleCount = toShow.size
            val toShowWithLatestData = toShow.map { latestData[it.detailPageUrl] ?: it }
            visibleMatches.value = toShowWithLatestData
            if (toShowWithLatestData.isNotEmpty()) fetchLinksForBatchIfNeeded(toShowWithLatestData)
        }

        Log.d("ViewModel", "Visible matches refreshed: ${visibleMatches.value.size} (from ${searchedMatches.size} filtered)")
    }

    // ── Favourites API ────────────────────────────────────────────────────────

    /** Toggles a team in/out of favourites and refreshes state. */
    fun toggleFavouriteTeam(teamName: String) {
        if (favouritesPrefs.isTeamFavourite(teamName)) {
            favouritesPrefs.removeTeam(teamName)
        } else {
            favouritesPrefs.addTeam(teamName)
        }
        favouriteTeams.value = favouritesPrefs.getFavouriteTeams()
        recomputeFavouriteMatchUrls()
        Log.d("ViewModel", "Toggled favourite team '$teamName'. Now: ${favouriteTeams.value}")
    }

    /** Toggles a league in/out of favourites and refreshes state. */
    fun toggleFavouriteLeague(leagueName: String) {
        if (favouritesPrefs.isLeagueFavourite(leagueName)) {
            favouritesPrefs.removeLeague(leagueName)
        } else {
            favouritesPrefs.addLeague(leagueName)
        }
        favouriteLeagues.value = favouritesPrefs.getFavouriteLeagues()
        recomputeFavouriteMatchUrls()
        Log.d("ViewModel", "Toggled favourite league '$leagueName'. Now: ${favouriteLeagues.value}")
    }

    /**
     * Returns true when [match] involves at least one favourite team or belongs to
     * a favourite league.
     *
     * Team matching uses [TeamMatcher] to bridge scraped names to canonical DB names.
     */
    fun isFavouriteMatch(match: Match): Boolean {
        val favTeams   = favouriteTeams.value
        val favLeagues = favouriteLeagues.value
        if (favTeams.isEmpty() && favLeagues.isEmpty()) return false

        // Check league first (cheaper).
        // Use the qualified key ("England - Premier League") so different countries'
        // competitions with the same short name don't falsely match.
        if (favLeagues.isNotEmpty()) {
            if (match.qualifiedLeagueKey in favLeagues) return true
            // Also accept the raw short-form key for backward-compat with old stored favourites
            if (match.league.isNotBlank() && match.league in favLeagues) return true
        }

        // Check each team in the "Home vs Away" string.
        // Pass qualifiedLeagueKey as a hint so e.g. "Arsenal Tula" (Russian league)
        // does not resolve to Arsenal FC (English league).
        if (favTeams.isNotEmpty()) {
            val parts = match.teams.split(" vs ", " v ", " – ", " — ", " - ", limit = 2)
            for (part in parts) {
                val entry = com.example.livetv.data.local.TeamMatcher.lookupTeam(
                    part.trim(), match.qualifiedLeagueKey
                )
                if (entry != null && entry.name in favTeams) return true
                if (part.trim() in favTeams) return true
            }
        }
        return false
    }

    fun isTeamFavourite(teamName: String): Boolean = teamName in favouriteTeams.value
    fun isLeagueFavourite(leagueName: String): Boolean = leagueName in favouriteLeagues.value
}
