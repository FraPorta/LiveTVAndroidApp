package com.example.livetv.ui

/**
 * Maps country names (as they appear in TeamEntry.league keys, e.g. "England")
 * to their corresponding flag emoji.
 *
 * Countries are taken from all unique prefixes in assets/team_db.json.
 */
object CountryFlags {

    private val flags: Map<String, String> = mapOf(
        "Austria"        to "🇦🇹",
        "Belgium"        to "🇧🇪",
        "Bulgaria"       to "🇧🇬",
        "Croatia"        to "🇭🇷",
        "Czech Republic" to "🇨🇿",
        "Denmark"        to "🇩🇰",
        "England"        to "🏴󠁧󠁢󠁥󠁮󠁧󠁿",
        "France"         to "🇫🇷",
        "Germany"        to "🇩🇪",
        "Greece"         to "🇬🇷",
        "Israel"         to "🇮🇱",
        "Italy"          to "🇮🇹",
        "Netherlands"    to "🇳🇱",
        "Norway"         to "🇳🇴",
        "Poland"         to "🇵🇱",
        "Portugal"       to "🇵🇹",
        "Romania"        to "🇷🇴",
        "Russia"         to "🇷🇺",
        "Scotland"       to "🏴󠁧󠁢󠁳󠁣󠁴󠁿",
        "Serbia"         to "🇷🇸",
        "Spain"          to "🇪🇸",
        "Sweden"         to "🇸🇪",
        "Switzerland"    to "🇨🇭",
        "Türkiye"        to "🇹🇷",
        "Ukraine"        to "🇺🇦",
        // Non-country / international
        "Europe"         to "🇪🇺",
        "World"          to "🌍",
        "USA"            to "🇺🇸",
        "Argentina"      to "🇦🇷",
        "Brazil"         to "🇧🇷",
        "Mexico"         to "🇲🇽",
        "Japan"          to "🇯🇵",
        "South Korea"    to "🇰🇷",
        "China"          to "🇨🇳",
        "Australia"      to "🇦🇺",
    )

    /**
     * Returns the flag emoji for [country] (case-insensitive), or an empty string
     * if no mapping is found.
     */
    fun forCountry(country: String): String =
        flags[country]
            ?: flags.entries.firstOrNull {
                it.key.equals(country, ignoreCase = true)
            }?.value
            ?: ""
}
