package com.example.livetv.ui

import com.example.livetv.data.model.Match
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.SimpleTimeZone
import java.util.TimeZone

enum class MatchPhase { LIVE, UPCOMING, FINISHED }

/**
 * Works out whether a match is live from its date header and kickoff time.
 *
 * Assumption: the times on the schedule page are in the device's time zone (they matched
 * the phone clock when checked). A match counts as live from kickoff for a sport-dependent window (see [liveWindowMinutes]).
 */
object MatchClock {
    private const val DEFAULT_LIVE_WINDOW_MIN = 135

    /** How long after kickoff a match of this sport is still shown as live. */
    private fun liveWindowMinutes(sport: String): Int {
        val s = sport.lowercase()
        return when {
            s.contains("tennis") -> 240
            s.contains("boxing") -> 180
            s.contains("hockey") -> 165
            s.contains("basket") || s.contains("racing") || s.contains("volley") -> 150
            else -> DEFAULT_LIVE_WINDOW_MIN
        }
    }
    private val TIME = Regex("""(\d{1,2}):(\d{2})""")
    private val DAY_MONTH = Regex("""(\d{1,2})\s+([A-Za-z]{3})""")
    private val MONTHS = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")

    /** Kickoff in epoch millis, or null when the date or time cannot be read. */
    fun startMillis(match: Match, now: Long): Long? {
        val t = TIME.find(match.time) ?: return null
        // The page's "Today" and kickoff times are in its own time zone, not necessarily the device's.
        val zone = match.sourceUtcOffsetMin
            ?.let { SimpleTimeZone(it * 60_000, "source") } ?: TimeZone.getDefault()
        val cal = Calendar.getInstance(zone).apply { timeInMillis = now }
        val date = match.date.trim()
        when {
            date.isBlank() || date.equals("Today", true) -> Unit
            date.equals("Tomorrow", true) -> cal.add(Calendar.DAY_OF_YEAR, 1)
            date.equals("Yesterday", true) -> cal.add(Calendar.DAY_OF_YEAR, -1)
            else -> {
                val m = DAY_MONTH.find(date) ?: return null
                val month = MONTHS.indexOf(m.groupValues[2].lowercase())
                if (month < 0) return null
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, m.groupValues[1].toInt())
                // A date months away from now belongs to the neighbouring year.
                val diffDays = (cal.timeInMillis - now) / 86_400_000L
                if (diffDays > 200) cal.add(Calendar.YEAR, -1)
                if (diffDays < -200) cal.add(Calendar.YEAR, 1)
            }
        }
        cal.set(Calendar.HOUR_OF_DAY, t.groupValues[1].toInt())
        cal.set(Calendar.MINUTE, t.groupValues[2].toInt())
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun phase(match: Match, now: Long): MatchPhase {
        val start = startMillis(match, now) ?: return MatchPhase.UPCOMING
        val minutes = (now - start) / 60_000L
        return when {
            minutes < 0 -> MatchPhase.UPCOMING
            minutes < liveWindowMinutes(match.sport) -> MatchPhase.LIVE
            else -> MatchPhase.FINISHED
        }
    }

    /** Kickoff time in the device's time zone; the page's own text when it cannot be converted. */
    fun displayTime(match: Match, now: Long): String {
        if (match.sourceUtcOffsetMin == null) return match.time.trim()
        val start = startMillis(match, now) ?: return match.time.trim()
        return SimpleDateFormat("HH:mm", Locale.ENGLISH).format(Date(start))
    }

    /** Day section title in the device's time zone: Today, Tomorrow, or the date. */
    fun dayLabel(match: Match, now: Long): String {
        val start = startMillis(match, now) ?: return match.date.ifBlank { "Other" }
        val a = Calendar.getInstance().apply { timeInMillis = now }
        val b = Calendar.getInstance().apply { timeInMillis = start }
        fun dayOf(c: Calendar) = c.get(Calendar.YEAR) * 366 + c.get(Calendar.DAY_OF_YEAR)
        return when (dayOf(b) - dayOf(a)) {
            0 -> "Today"
            1 -> "Tomorrow"
            -1 -> "Yesterday"
            else -> SimpleDateFormat("d MMM", Locale.ENGLISH).format(Date(start))
        }
    }
}

sealed interface FeedItem {
    data class Header(val title: String, val count: Int) : FeedItem
    data class Entry(val match: Match, val phase: MatchPhase) : FeedItem
}

/** Groups matches into "Live now", one section per day in page order, then "Finished". */
fun buildFeed(matches: List<Match>, now: Long): List<FeedItem> {
    val live = mutableListOf<FeedItem.Entry>()
    val finished = mutableListOf<FeedItem.Entry>()
    val days = LinkedHashMap<String, MutableList<FeedItem.Entry>>()
    matches.forEach { m ->
        val phase = MatchClock.phase(m, now)
        val e = FeedItem.Entry(m, phase)
        when (phase) {
            MatchPhase.LIVE -> live += e
            MatchPhase.FINISHED -> finished += e
            MatchPhase.UPCOMING -> days.getOrPut(MatchClock.dayLabel(m, now)) { mutableListOf() } += e
        }
    }
    return buildList {
        if (live.isNotEmpty()) { add(FeedItem.Header("Live now", live.size)); addAll(live) }
        days.forEach { (label, list) -> add(FeedItem.Header(label, list.size)); addAll(list) }
        if (finished.isNotEmpty()) { add(FeedItem.Header("Finished", finished.size)); addAll(finished) }
    }
}

/** Splits "Home vs Away" into its two names, or null when it is not a two-team title. */
fun splitTeams(teams: String): Pair<String, String>? {
    val parts = teams.split(" vs ", " v ", " – ", " — ", " - ", limit = 2).map { it.trim() }
    return if (parts.size == 2 && parts.all { it.isNotBlank() }) parts[0] to parts[1] else null
}
