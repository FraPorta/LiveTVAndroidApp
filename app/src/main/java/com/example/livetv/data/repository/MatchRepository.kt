package com.example.livetv.data.repository

import android.content.Context
import com.example.livetv.data.model.Match
import com.example.livetv.data.network.Scraper
import com.example.livetv.data.network.ScrapingSection

class MatchRepository(context: Context) {
    private val scraper = Scraper(context.applicationContext)

    /**
     * Fetches the initial list of matches without their stream links.
     */
    suspend fun getMatchList(
        section: ScrapingSection = ScrapingSection.ALL,
        limit: Int = 0,
        offset: Int = 0
    ): List<Match> {
        return scraper.scrapeMatchList(section, limit, offset)
    }

    /**
     * Fetches the stream links for a single match given its detail page URL.
     */
    suspend fun getStreamLinks(detailPageUrl: String): List<String> {
        return scraper.fetchStreamLinks(detailPageUrl)
    }
    
    /**
     * Fetches ALL available matches from the selected section for background scraping.
     * Does not fetch stream links - only match metadata for search functionality.
     */
    suspend fun getAllMatches(section: ScrapingSection = ScrapingSection.ALL): List<Match> {
        return scraper.scrapeAllMatches(section)
    }
    
    /**
     * Gets the current base URL being used for scraping
     */
    fun getBaseUrl(): String {
        return scraper.getBaseUrl()
    }
    
    /**
     * Updates the base URL used for scraping
     */
    fun updateBaseUrl(newUrl: String) {
        scraper.updateBaseUrl(newUrl)
    }
    
    /**
     * Resets the base URL to the default
     */
    fun resetBaseUrl() {
        scraper.resetBaseUrl()
    }
}
