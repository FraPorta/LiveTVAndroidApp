package com.example.livetv.data.model

data class Match(
    val time: String,
    val teams: String,
    val competition: String,
    val sport: String, // The sport type (Football, Basketball, etc.)
    val league: String, // The specific league (Premier League, La Liga, etc.)
    val detailPageUrl: String, // URL to the page with the stream links
    var streamLinks: List<String> = emptyList(), // The actual stream links, fetched later
    var areLinksLoading: Boolean = false // To show a loading indicator per match
)
