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

        val all = TeamDatabase.getAllTeams()

        fun prefixMatch(a: String, b: String): Boolean {
            if (!a.startsWith(b) && !b.startsWith(a)) return false
            val ratio = minOf(a.length, b.length).toFloat() / maxOf(a.length, b.length, 1)
            return ratio >= 0.75f
        }

        fun entryMatchesPrefix(entry: TeamEntry): Boolean {
            val en = TeamDatabase.normalize(entry.name)
            return prefixMatch(en, n) ||
                    entry.aliases.any { alias -> prefixMatch(TeamDatabase.normalize(alias), n) }
        }

        // 2. Prefix match with 75% length ratio guard.
        //    When a leagueHint is given, first try same-league entries → then any.
        val prefixResult = if (leagueHint.isNotBlank()) {
            all.firstOrNull { it.league == leagueHint && entryMatchesPrefix(it) }
                ?: all.firstOrNull { entryMatchesPrefix(it) }
        } else {
            all.firstOrNull { entryMatchesPrefix(it) }
        }
        prefixResult?.let { return it }

        // 3. Substring fallback: pick the LONGEST matching candidate (more specific = better).
        //    Same-league entries get a large bonus so they win over longer foreign names.
        val LEAGUE_BONUS = 1000
        return all
            .mapNotNull { entry ->
                val en = TeamDatabase.normalize(entry.name)
                val rawLen = when {
                    en.length >= 4 && n.contains(en) -> en.length
                    en.length >= 4 && en.contains(n) -> n.length
                    else -> entry.aliases.mapNotNull { alias ->
                        val an = TeamDatabase.normalize(alias)
                        when {
                            an.length >= 4 && n.contains(an) -> an.length
                            an.length >= 4 && an.contains(n) -> n.length
                            else -> null
                        }
                    }.maxOrNull()
                } ?: return@mapNotNull null
                val score = rawLen + if (leagueHint.isNotBlank() && entry.league == leagueHint) LEAGUE_BONUS else 0
                entry to score
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
     * - If both teams resolve to the **same** league → high-confidence result.
     * - If only one team resolves → best-effort result.
     * - If neither resolves → returns null.
     *
     * No [leagueHint] is used here because we ARE resolving the hint.
     */
    fun resolveLeagueFromTeams(rawTeamsString: String): LeagueResolution? {
        val parts = rawTeamsString
            .split(" vs ", " v ", " \u2013 ", " \u2014 ", " - ", limit = 2)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val entries = parts.mapNotNull { lookupTeam(it) }
        if (entries.isEmpty()) return null

        fun qualifiedToResolution(qualified: String, bothMatched: Boolean): LeagueResolution {
            val country = if (qualified.contains(" - ")) qualified.substringBefore(" - ") else ""
            return LeagueResolution(country, qualified, bothMatched)
        }

        // Both teams resolved
        if (entries.size == 2) {
            return if (entries[0].league == entries[1].league) {
                qualifiedToResolution(entries[0].league, bothMatched = true)
            } else {
                // Disagreement: pick the entry whose name normalized length is closer to the
                // scraped part length (i.e. the better name match).
                val lengths = parts.zip(entries).map { (raw, entry) ->
                    val n = TeamDatabase.normalize(raw)
                    val en = TeamDatabase.normalize(entry.name)
                    minOf(n.length, en.length)
                }
                val winner = if (lengths[0] >= lengths[1]) entries[0] else entries[1]
                qualifiedToResolution(winner.league, bothMatched = false)
            }
        }

        // Only one team resolved
        return qualifiedToResolution(entries[0].league, bothMatched = false)
    }

    /**
     * Returns up to [limit] teams whose name or alias starts with [query].
     * Used to populate the search suggestion chips row.
     */
    fun teamsMatchingQuery(query: String, limit: Int = 12): List<TeamEntry> {
        if (query.isBlank()) return emptyList()
        val nq = TeamDatabase.normalize(query)
        return TeamDatabase.getAllTeams()
            .filter { entry ->
                TeamDatabase.normalize(entry.name).contains(nq) ||
                        entry.aliases.any { TeamDatabase.normalize(it).contains(nq) }
            }
            // Prefer entries where the name *starts* with the query over contains
            .sortedWith(compareByDescending { entry ->
                TeamDatabase.normalize(entry.name).startsWith(nq)
            })
            .take(limit)
    }
}
