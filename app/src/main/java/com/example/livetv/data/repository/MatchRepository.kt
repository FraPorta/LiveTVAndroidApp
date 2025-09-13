package com.example.livetv.data.repository

import android.content.Context
import com.example.livetv.data.model.Match
import com.example.livetv.data.network.Scraper

class MatchRepository(context: Context) {
    private val scraper = Scraper(context.applicationContext)

    /**
     * Fetches the initial list of matches without their stream links.
     */
    suspend fun getMatchList(): List<Match> {
        return scraper.scrapeMatchList()
    }

    /**
     * Fetches the stream links for a single match given its detail page URL.
     */
    suspend fun getStreamLinks(detailPageUrl: String): List<String> {
        return scraper.fetchStreamLinks(detailPageUrl)
    }
}
