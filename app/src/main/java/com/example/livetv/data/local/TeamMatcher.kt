package com.example.livetv.data.local

import com.example.livetv.data.model.TeamEntry

/**
 * Result of a team-based league resolution attempt.
 *
 * @param country       Country component of the qualified key (e.g. "Italy").
 * @param qualifiedKey  Full qualified key (e.g. "Italy - Serie A").
 * @param bothMatched   True when both teams were found in the DB and agreed on the same league.
 */
data class LeagueResolution(
    val country: String,
    val qualifiedKey: String,
    val bothMatched: Boolean,
)
/**
 * Stateless helper that bridges scraped team name strings to [TeamEntry] objects
 * from [TeamDatabase].
 *
 * Three-tier lookup: exact-normalized → alias → substring fallback.
 * All lookup functions accept an optional [leagueHint] (a qualified
 * "Country - League" key) to prefer same-league entries, preventing e.g.
 * "Arsenal Tula" from resolving to Arsenal FC.
 *
 * The `teamsMatchingQuery` function powers the search suggestion row.
 */
object TeamMatcher {

    /**
     * Find the best-matching [TeamEntry] for a raw scraped team name.
     *
     * @param rawName    Scraped team name string.
     * @param leagueHint Qualified league key (e.g. "Italy - Serie A") used to
     *                   prefer same-league entries in tiers 2 and 3. Pass blank
     *                   to disable the hint.
     */
    fun lookupTeam(rawName: String, leagueHint: String = ""): TeamEntry? {
        if (rawName.isBlank()) return null
        val n = TeamDatabase.normalize(rawName)
        if (n.isEmpty()) return null

        // 1. Exact index hit (handles canonical names and all aliases)
        TeamDatabase.lookupByNormalizedName(n)?.let { return it }

        val normalizedEntries = TeamDatabase.getNormalizedEntries()

        fun prefixMatch(a: String, b: String): Boolean {
            if (!a.startsWith(b) && !b.startsWith(a)) return false
            val ratio = minOf(a.length, b.length).toFloat() / maxOf(a.length, b.length, 1)
            return ratio >= 0.75f
        }

        fun entryMatchesPrefix(ne: TeamDatabase.NormalizedEntry): Boolean {
            return prefixMatch(ne.normalizedName, n) ||
                    ne.normalizedAliases.any { an -> prefixMatch(an, n) }
        }

        // 2. Prefix match with 75% length ratio guard.
        //    When a leagueHint is given, first try same-league entries → then any.
        val prefixResult = if (leagueHint.isNotBlank()) {
            normalizedEntries.firstOrNull { it.entry.league == leagueHint && entryMatchesPrefix(it) }
                ?: normalizedEntries.firstOrNull { entryMatchesPrefix(it) }
        } else {
            normalizedEntries.firstOrNull { entryMatchesPrefix(it) }
        }
        prefixResult?.let { return it.entry }

        // 3. Substring fallback: pick the LONGEST matching candidate (more specific = better).
        //    Same-league entries get a large bonus so they win over longer foreign names.
        //    Without a confirming leagueHint, the matched token must cover ≥60% of the
        //    query length to reject e.g. "inter" (5) matching "inter baku" (10) → ratio 0.50.
        val LEAGUE_BONUS = 1000
        val MIN_RATIO = 0.60f
        return normalizedEntries
            .mapNotNull { ne ->
                val en = ne.normalizedName
                val sameLeague = leagueHint.isNotBlank() && ne.entry.league == leagueHint
                val rawLen = when {
                    en.length >= 4 && n.contains(en) -> en.length
                    en.length >= 4 && en.contains(n) -> n.length
                    else -> ne.normalizedAliases.mapNotNull { an ->
                        when {
                            an.length >= 4 && n.contains(an) -> an.length
                            an.length >= 4 && an.contains(n) -> n.length
                            else -> null
                        }
                    }.maxOrNull()
                } ?: return@mapNotNull null
                // Reject low-ratio substring matches unless the leagueHint confirms this entry
                if (!sameLeague && rawLen.toFloat() / maxOf(n.length, 1) < MIN_RATIO) return@mapNotNull null
                val score = rawLen + if (sameLeague) LEAGUE_BONUS else 0
                ne.entry to score
            }
            .maxByOrNull { (_, score) -> score }
            ?.first
    }

    /**
     * Resolves a raw scraped league label to a (country, shortLeagueName) pair.
     *
     * Uses [TeamDatabase.lookupLeagueQualified] to find the best qualified key
     * (e.g. "Italy - Serie A"), then splits it into country and short name.
     *
     * @param rawLeague    Raw league string from scraper (e.g. "Serie A").
     * @param contextTeams Raw "Home vs Away" string used for disambiguation when
     *                     multiple qualified keys share the same short name.
     * @return Pair(country, shortLeagueName), or null if no DB match found.
     */
    fun lookupLeague(rawLeague: String, contextTeams: String = ""): Pair<String, String>? {
        if (rawLeague.isBlank()) return null
        val qualified = TeamDatabase.lookupLeagueQualified(rawLeague, contextTeams) ?: return null
        return if (qualified.contains(" - ")) {
            Pair(qualified.substringBefore(" - "), qualified.substringAfter(" - "))
        } else {
            Pair("", qualified)
        }
    }

    /**
     * Attempts to determine the league for a match by looking up the teams in the DB
     * rather than by keyword-matching competition text.
     *
     * - If **both** teams resolve to the **same** league → high-confidence result.
     * - Any other case (only one resolves, or the two disagree) → returns null so
     *   the caller falls back to competition-text keyword matching rather than
     *   propagating a wrong league association.
     *
     * No [leagueHint] is used here because we ARE resolving the hint.
     */
    fun resolveLeagueFromTeams(rawTeamsString: String): LeagueResolution? {
        val parts = rawTeamsString
            .split(" vs ", " v ", " \u2013 ", " \u2014 ", " - ", limit = 2)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (parts.size < 2) return null

        val entries = parts.mapNotNull { lookupTeam(it) }

        // Only confident when both teams are recognised AND agree on the same league
        if (entries.size == 2 && entries[0].league == entries[1].league) {
            val qualified = entries[0].league
            val country = if (qualified.contains(" - ")) qualified.substringBefore(" - ") else ""
            return LeagueResolution(country, qualified, bothMatched = true)
        }

        return null
    }

    /**
     * Returns up to [limit] teams whose name or alias starts with [query].
     * Used to populate the search suggestion chips row.
     */
    fun teamsMatchingQuery(query: String, limit: Int = 12): List<TeamEntry> {
        if (query.isBlank()) return emptyList()
        val nq = TeamDatabase.normalize(query)
        return TeamDatabase.getNormalizedEntries()
            .filter { ne ->
                ne.normalizedName.contains(nq) ||
                        ne.normalizedAliases.any { it.contains(nq) }
            }
            // Prefer entries where the name *starts* with the query over contains
            .sortedWith(compareByDescending { ne -> ne.normalizedName.startsWith(nq) })
            .map { it.entry }
            .take(limit)
    }
}
