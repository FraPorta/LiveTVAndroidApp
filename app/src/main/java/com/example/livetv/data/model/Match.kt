package com.example.livetv.data.model

data class Match(
    val time: String,
    val date: String = "",  // Date header from the schedule page, e.g. "16 Mar" or "Today"
    val teams: String,
    val competition: String,
    val sport: String,         // The sport type (Football, Basketball, etc.)
    val league: String,        // The specific league short name (e.g. "Premier League")
    val detailPageUrl: String, // URL to the page with the stream links
    val country: String = "",  // Country owning this league (e.g. "England"), used for flag + qualified key
    val streamLinks: List<String> = emptyList(),
    val areLinksLoading: Boolean = false,
    val homeLogoUrl: String? = null,
    val awayLogoUrl: String? = null,
) {
    /** Qualified league key aligned with TeamEntry.league, e.g. "England - Premier League". */
    val qualifiedLeagueKey: String get() =
        if (country.isNotBlank() && league.isNotBlank()) "$country - $league" else league

    /** Pre-computed lowercase text for fast in-memory search — avoids per-call string allocation in getFilteredMatches. */
    val searchableText: String = "$teams $competition $league $qualifiedLeagueKey $country $sport".lowercase()
}
