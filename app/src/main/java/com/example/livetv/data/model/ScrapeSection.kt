package com.example.livetv.data.model

// FIX #21: ScrapingSection was previously declared at the top level of Scraper.kt
// (package com.example.livetv.data.network), which is the wrong layer — it is a domain
// model, not a network implementation detail. Moved here so it lives alongside Match.kt
// and is importable without pulling in the network package.
enum class ScrapingSection(val displayName: String, val selector: String) {
    /** Matches where either team or league is marked as favourite. Backed by FOOTBALL data. */
    FAVOURITES("Favourites", ":not(#upcoming)"),
    FOOTBALL("Football", ":not(#upcoming)"),
    TOP_EVENTS_LIVE("Top Live", "#upcoming"),
    ALL("All", "")
}
