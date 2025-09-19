package com.example.livetv.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.livetv.data.model.Match
import com.example.livetv.data.repository.MatchRepository
import com.example.livetv.data.network.ScrapingSection
import kotlinx.coroutines.launch

const val INITIAL_LOAD_SIZE = 16
const val LOAD_MORE_SIZE = 10

class MatchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MatchRepository(application)

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

    // Full list of matches scraped from the main page (loaded on demand)
    private var allMatches = listOf<Match>()
    
    // Track if we have loaded all matches (needed for filtering)
    private var hasLoadedAllMatches = false
    
    // Track how many matches we've fetched so far
    private var totalFetchedMatches = 0

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

    // The number of matches currently shown (after filtering)
    private var currentVisibleCount = 0

    val isLoadingInitialList = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val isRefreshing = mutableStateOf(false)

    init {
        Log.d("ViewModel", "MatchViewModel initialized.")
        loadInitialMatchList()
    }
    
    /**
     * Saves current section data to cache before switching sections
     */
    private fun saveCurrentSectionData() {
        val currentSection = selectedSection.value
        sectionCache[currentSection] = SectionData(
            allMatches = allMatches,
            hasLoadedAllMatches = hasLoadedAllMatches,
            totalFetchedMatches = totalFetchedMatches,
            currentVisibleCount = currentVisibleCount,
            selectedSport = selectedSport.value,
            selectedLeague = selectedLeague.value,
            availableSports = availableSports.value,
            availableLeagues = availableLeagues.value
        )
        Log.d("ViewModel", "Saved ${allMatches.size} matches for section: ${currentSection.displayName}")
    }
    
    /**
     * Restores section data from cache if available
     */
    private fun restoreSectionData(section: ScrapingSection): Boolean {
        val cachedData = sectionCache[section]
        return if (cachedData != null && cachedData.allMatches.isNotEmpty()) {
            allMatches = cachedData.allMatches
            hasLoadedAllMatches = cachedData.hasLoadedAllMatches
            totalFetchedMatches = cachedData.totalFetchedMatches
            currentVisibleCount = cachedData.currentVisibleCount
            selectedSport.value = cachedData.selectedSport
            selectedLeague.value = cachedData.selectedLeague
            availableSports.value = cachedData.availableSports
            availableLeagues.value = cachedData.availableLeagues
            Log.d("ViewModel", "Restored ${allMatches.size} matches for section: ${section.displayName}")
            true
        } else {
            Log.d("ViewModel", "No cached data found for section: ${section.displayName}")
            false
        }
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
        
        // Save current section data to cache
        saveCurrentSectionData()
        
        // Update selected section
        selectedSection.value = section
        
        // Try to restore cached data for the new section
        if (restoreSectionData(section)) {
            // We have cached data, refresh the visible matches with existing data
            refreshVisibleMatches()
        } else {
            // No cached data, load fresh data
            loadInitialMatchList()
        }
    }

    fun loadMoreMatches() {
        viewModelScope.launch {
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
                        Log.d("ViewModel", "Fetched ${moreMatches.size} more matches. Total now: $totalFetchedMatches")
                        
                        // Update filter options with new matches
                        updateFilterOptionsFromCurrentMatches()
                        
                        // Update cache with new data
                        updateCurrentSectionCache()
                        
                        // Try loading more matches again with the expanded list
                        loadMoreMatches()
                    } else {
                        hasLoadedAllMatches = true
                        Log.d("ViewModel", "No more matches available from repository. Total fetched: $totalFetchedMatches")
                        // Update cache to reflect that all matches are loaded
                        updateCurrentSectionCache()
                    }
                } catch (e: Exception) {
                    Log.e("ViewModel", "Error fetching more matches", e)
                }
            } else {
                Log.d("ViewModel", "No more matches to load. HasLoadedAll: $hasLoadedAllMatches, HasFilters: ${hasActiveFilters()}")
            }
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
        Log.d("ViewModel", "Fetching links for a batch of ${batch.size} matches.")
        viewModelScope.launch {
            batch.forEach { match ->
                // Set loading state for this specific match
                updateMatchInList(match.copy(areLinksLoading = true))

                try {
                    Log.d("ViewModel", "Fetching links for: ${match.teams}")
                    val links = repository.getStreamLinks(match.detailPageUrl)
                    Log.d("ViewModel", "Found ${links.size} links for ${match.teams}")
                    // Update match with loaded links
                    updateMatchInList(match.copy(streamLinks = links, areLinksLoading = false))
                } catch (e: Exception) {
                    Log.e("ViewModel", "Error fetching links for ${match.teams}", e)
                    // Handle error for a single match, maybe show a failed state
                    updateMatchInList(match.copy(areLinksLoading = false))
                }
            }
        }
    }

    private fun updateMatchInList(updatedMatch: Match) {
        // Update in visible matches
        val currentList = visibleMatches.value.toMutableList()
        val index = currentList.indexOfFirst { it.detailPageUrl == updatedMatch.detailPageUrl }
        if (index != -1) {
            currentList[index] = updatedMatch
            visibleMatches.value = currentList
        }
        
        // Also update in allMatches to persist the link data
        val allMatchesList = allMatches.toMutableList()
        val allMatchesIndex = allMatchesList.indexOfFirst { it.detailPageUrl == updatedMatch.detailPageUrl }
        if (allMatchesIndex != -1) {
            allMatchesList[allMatchesIndex] = updatedMatch
            allMatches = allMatchesList
            // Update cache immediately when match data changes
            updateCurrentSectionCache()
        }
    }
    
    /**
     * Updates the cache for the current section with latest data
     */
    private fun updateCurrentSectionCache() {
        val currentSection = selectedSection.value
        val currentCache = sectionCache[currentSection] ?: SectionData()
        sectionCache[currentSection] = currentCache.copy(
            allMatches = allMatches,
            hasLoadedAllMatches = hasLoadedAllMatches,
            totalFetchedMatches = totalFetchedMatches,
            currentVisibleCount = currentVisibleCount
        )
    }



    private fun getFilteredMatches(): List<Match> {
        var filtered = allMatches.filter { match ->
            val sportMatches = selectedSport.value == null || 
                              selectedSport.value == "All Sports" || 
                              match.sport == selectedSport.value
            
            val leagueMatches = selectedLeague.value == null || 
                               selectedLeague.value == "All Leagues" || 
                               match.league == selectedLeague.value
            
            sportMatches && leagueMatches
        }
        
        // Apply search filter if active
        if (isSearchActive.value && searchQuery.value.isNotBlank()) {
            val searchText = searchQuery.value.lowercase().trim()
            filtered = filtered.filter { match ->
                val matchText = "${match.teams} ${match.competition} ${match.league} ${match.sport}".lowercase()
                matchText.contains(searchText)
            }
        }
        
        return filtered
    }

    fun setSportFilter(sport: String?) {
        selectedSport.value = if (sport == "All Sports") null else sport
        updateCurrentSectionCache() // Save filter state to cache
        applyFilters()
    }

    fun setLeagueFilter(league: String?) {
        selectedLeague.value = if (league == "All Leagues") null else league
        updateCurrentSectionCache() // Save filter state to cache
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
                val links = repository.getStreamLinks(match.detailPageUrl)
                Log.d("ViewModel", "Refreshed ${links.size} links for ${match.teams}")
                // Update match with refreshed links
                updateMatchInList(match.copy(streamLinks = links, areLinksLoading = false))
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
                
                // Update cache with complete data
                updateCurrentSectionCache()
                
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
        refreshVisibleMatches()
        Log.d("ViewModel", "Search deactivated")
    }
    
    /**
     * Updates the search query and filters matches
     */
    fun updateSearchQuery(query: String) {
        searchQuery.value = query
        refreshVisibleMatches()
        Log.d("ViewModel", "Search query updated: '$query'")
    }
    
    /**
     * Refreshes the visible matches based on current filters and search query
     */
    private fun refreshVisibleMatches() {
        val filteredMatches = getFilteredMatches()
        
        // If search is active and query is not empty, filter by search query
        val searchedMatches = if (isSearchActive.value && searchQuery.value.isNotBlank()) {
            filteredMatches.filter { match ->
                val searchText = searchQuery.value.lowercase().trim()
                val matchText = "${match.teams} ${match.competition} ${match.league} ${match.sport}".lowercase()
                matchText.contains(searchText)
            }
        } else {
            filteredMatches
        }
        
        // Update visible matches - show all matching results when searching
        if (isSearchActive.value && searchQuery.value.isNotBlank()) {
            // When searching, show all matching results immediately
            currentVisibleCount = searchedMatches.size
            // Ensure we use the most up-to-date match data with links
            val matchesWithLatestData = searchedMatches.map { match ->
                allMatches.find { it.detailPageUrl == match.detailPageUrl } ?: match
            }
            visibleMatches.value = matchesWithLatestData
            
            // Fetch links only for matches that don't already have them loaded
            if (matchesWithLatestData.isNotEmpty()) {
                fetchLinksForBatchIfNeeded(matchesWithLatestData)
            }
        } else {
            // When not searching, use pagination logic
            val maxToShow = currentVisibleCount.coerceAtLeast(INITIAL_LOAD_SIZE)
            val toShow = searchedMatches.take(maxToShow)
            currentVisibleCount = toShow.size
            // Ensure we use the most up-to-date match data with links
            val toShowWithLatestData = toShow.map { match ->
                allMatches.find { it.detailPageUrl == match.detailPageUrl } ?: match
            }
            visibleMatches.value = toShowWithLatestData
            
            // Fetch links only for matches that don't already have them loaded
            if (toShowWithLatestData.isNotEmpty()) {
                fetchLinksForBatchIfNeeded(toShowWithLatestData)
            }
        }
        
        Log.d("ViewModel", "Visible matches refreshed: ${visibleMatches.value.size} (from ${searchedMatches.size} filtered)")
    }
    
}
