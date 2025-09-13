package com.example.livetv.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.livetv.data.model.Match
import com.example.livetv.data.repository.MatchRepository
import kotlinx.coroutines.launch

const val INITIAL_LOAD_SIZE = 15
const val LOAD_MORE_SIZE = 10

class MatchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MatchRepository(application)

    // Full list of matches scraped from the main page
    private var allMatches = listOf<Match>()

    // The list of matches currently displayed on the UI
    val visibleMatches = mutableStateOf<List<Match>>(emptyList())

    // The number of matches currently shown
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
                Log.d("ViewModel", "Calling repository.getMatchList()")
                allMatches = repository.getMatchList()
                Log.d("ViewModel", "Repository returned ${allMatches.size} matches.")
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

    fun loadMoreMatches() {
        Log.d("ViewModel", "loadMoreMatches called. Current visible: $currentVisibleCount / ${allMatches.size}")
        val nextLoadCount = if (currentVisibleCount == 0) INITIAL_LOAD_SIZE else LOAD_MORE_SIZE
        val newVisibleCount = (currentVisibleCount + nextLoadCount).coerceAtMost(allMatches.size)

        if (newVisibleCount > currentVisibleCount) {
            val nextBatch = allMatches.subList(currentVisibleCount, newVisibleCount)
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
}
