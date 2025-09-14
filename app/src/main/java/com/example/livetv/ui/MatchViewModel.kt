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

    // Full list of matches scraped from the main page
    private var allMatches = listOf<Match>()

    // The list of matches currently displayed on the UI
    val visibleMatches = mutableStateOf<List<Match>>(emptyList())

    // Available filter options
    val availableSports = mutableStateOf<List<String>>(emptyList())
    val availableLeagues = mutableStateOf<List<String>>(emptyList())

    // Current filter selections
    val selectedSport = mutableStateOf<String?>(null)
    val selectedLeague = mutableStateOf<String?>(null)
    val selectedSection = mutableStateOf<ScrapingSection>(ScrapingSection.ALL)

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
                Log.d("ViewModel", "Calling repository.getMatchList() with section: ${selectedSection.value.displayName}")
                allMatches = repository.getMatchList(selectedSection.value)
                Log.d("ViewModel", "Repository returned ${allMatches.size} matches from ${selectedSection.value.displayName} section.")
                
                // Extract available sports and leagues for filtering
                updateFilterOptions()
                
                currentVisibleCount = 0
                visibleMatches.value = emptyList()
                loadMoreMatches() // Load the first batch
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
        val filteredMatches = getFilteredMatches()
        Log.d("ViewModel", "loadMoreMatches called. Current visible: $currentVisibleCount / ${filteredMatches.size} (filtered from ${allMatches.size} total)")
        
        val nextLoadCount = if (currentVisibleCount == 0) INITIAL_LOAD_SIZE else LOAD_MORE_SIZE
        val newVisibleCount = (currentVisibleCount + nextLoadCount).coerceAtMost(filteredMatches.size)

        if (newVisibleCount > currentVisibleCount) {
            val nextBatch = filteredMatches.subList(currentVisibleCount, newVisibleCount)
            Log.d("ViewModel", "Loading next batch of ${nextBatch.size} matches.")
            visibleMatches.value = visibleMatches.value + nextBatch
            currentVisibleCount = newVisibleCount

            // Fetch links for the new batch
            fetchLinksForBatch(nextBatch)
        } else {
            Log.d("ViewModel", "No more matches to load.")
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

    private fun updateFilterOptions() {
        val sports = allMatches.map { it.sport }.distinct().sorted()
        val leagues = allMatches.map { it.league }.distinct().sorted()
        
        availableSports.value = listOf("All Sports") + sports
        availableLeagues.value = listOf("All Leagues") + leagues
        
        Log.d("ViewModel", "Available sports: ${sports.joinToString()}")
        Log.d("ViewModel", "Available leagues: ${leagues.joinToString()}")
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
        currentVisibleCount = 0
        visibleMatches.value = emptyList()
        loadMoreMatches()
    }
}
