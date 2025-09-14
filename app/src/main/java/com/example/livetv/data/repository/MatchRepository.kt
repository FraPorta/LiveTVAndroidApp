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
}
