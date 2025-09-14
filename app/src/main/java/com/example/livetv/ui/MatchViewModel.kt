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

const val INITIAL_LOAD_SIZE = 15
const val LOAD_MORE_SIZE = 10

class MatchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MatchRepository(application)

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

    // The number of matches currently shown (after filtering)
    private var currentVisibleCount = 0

    val isLoadingInitialList = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    init {
        Log.d("ViewModel", "MatchViewModel initialized.")
        loadInitialMatchList()
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
     * Changes the scraping section and reloads the match list
     */
    fun changeSection(section: ScrapingSection) {
        Log.d("ViewModel", "Changing section from ${selectedSection.value.displayName} to ${section.displayName}")
        selectedSection.value = section
        loadInitialMatchList()
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
                        
                        // Try loading more matches again with the expanded list
                        loadMoreMatches()
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
        val currentList = visibleMatches.value.toMutableList()
        val index = currentList.indexOfFirst { it.detailPageUrl == updatedMatch.detailPageUrl }
        if (index != -1) {
            currentList[index] = updatedMatch
            visibleMatches.value = currentList
        }
    }



    private fun getFilteredMatches(): List<Match> {
        return allMatches.filter { match ->
            val sportMatches = selectedSport.value == null || 
                              selectedSport.value == "All Sports" || 
                              match.sport == selectedSport.value
            
            val leagueMatches = selectedLeague.value == null || 
                               selectedLeague.value == "All Leagues" || 
                               match.league == selectedLeague.value
            
            sportMatches && leagueMatches
        }
    }

    fun setSportFilter(sport: String?) {
        selectedSport.value = if (sport == "All Sports") null else sport
        applyFilters()
    }

    fun setLeagueFilter(league: String?) {
        selectedLeague.value = if (league == "All Leagues") null else league
        applyFilters()
    }

    private fun applyFilters() {
        Log.d("ViewModel", "Applying filters - Sport: ${selectedSport.value}, League: ${selectedLeague.value}")
        viewModelScope.launch {
            // When filters are applied, we need to ensure we have all matches
            ensureAllMatchesLoaded()
            
            currentVisibleCount = 0
            visibleMatches.value = emptyList()
            loadMoreMatches()
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
}
