package com.example.livetv.data.network

/** Reads the time zone a schedule page shows its kickoff times in. */
object SourceTimeZone {
    private val TIME_ZONE_REGEX =
        Regex("""time zone:[^(]{0,40}\(\s*UTC\s*(?:([+-])\s*(\d{1,2})(?::(\d{2}))?)?""", RegexOption.IGNORE_CASE)

    /**
     * Parses text like "Your current time zone: 19:54 (UTC +1)" into minutes from UTC
     * (+60 here). Returns null when the page has no such text.
     */
    fun parseOffsetMinutes(pageText: String): Int? {
        val m = TIME_ZONE_REGEX.find(pageText) ?: return null
        val sign = if (m.groupValues[1] == "-") -1 else 1
        val hours = m.groupValues[2].toIntOrNull() ?: return 0
        val minutes = m.groupValues[3].toIntOrNull() ?: 0
        return sign * (hours * 60 + minutes)
    }
}
