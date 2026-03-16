package com.example.livetv.data.local

import com.example.livetv.data.model.TeamEntry

/**
 * Stateless helper that bridges scraped team name strings to [TeamEntry] objects
 * from [TeamDatabase].
 *
 * Three-tier lookup: exact-normalized → alias → substring fallback.
 * The `teamsMatchingQuery` function powers the search suggestion row.
 */
object TeamMatcher {

    /**
     * Find the best-matching [TeamEntry] for a raw scraped team name.
     *
     * Tries (in order):
     * 1. Exact match on the normalized index (covers canonical names + aliases)
     * 2. Prefix match where DB name starts with (or equals) the normalized query
     * 3. Substring match: DB canonical name contains the query (or vice-versa)
     *
     * Returns `null` if no sensible match is found.
     */
    fun lookupTeam(rawName: String): TeamEntry? {
        if (rawName.isBlank()) return null
        val n = TeamDatabase.normalize(rawName)
        if (n.isEmpty()) return null

        // 1. Exact index hit (handles canonical names and all aliases)
        TeamDatabase.lookupByNormalizedName(n)?.let { return it }

        val all = TeamDatabase.getAllTeams()
        // 2. Prefix: the DB canonical name starts with the query, or the query
        //    starts with the canonical name (e.g. "Arsenal" finds "Arsenal FC").
        //    Require that the shorter string covers >= 75 % of the longer one to
        //    avoid short names (e.g. "Arsenal") matching long queries
        //    (e.g. "Arsenal Tula") by accident.
        all.firstOrNull { entry ->
            val en = TeamDatabase.normalize(entry.name)
            fun prefixMatch(a: String, b: String): Boolean {
                if (!a.startsWith(b) && !b.startsWith(a)) return false
                val ratio = minOf(a.length, b.length).toFloat() / maxOf(a.length, b.length, 1)
                return ratio >= 0.75f
            }
            prefixMatch(en, n) ||
                    entry.aliases.any { alias -> prefixMatch(TeamDatabase.normalize(alias), n) }
        }?.let { return it }

        // 3. Substring fallback: the query must contain the DB name (or alias) as a
        //    whole word-ish token, and the matched portion must be >= 4 chars.
        //    Pick the LONGEST matching candidate (more specific = better).
        return all
            .mapNotNull { entry ->
                val en = TeamDatabase.normalize(entry.name)
                val matchLen = when {
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
                }
                if (matchLen != null) entry to matchLen else null
            }
            .maxByOrNull { (_, len) -> len }
            ?.first
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
