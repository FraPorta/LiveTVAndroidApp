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
        //    starts with the canonical name (e.g. "Arsenal" finds "Arsenal FC")
        all.firstOrNull { entry ->
            val en = TeamDatabase.normalize(entry.name)
            en.startsWith(n) || n.startsWith(en) ||
                    entry.aliases.any { alias ->
                        val an = TeamDatabase.normalize(alias)
                        an.startsWith(n) || n.startsWith(an)
                    }
        }?.let { return it }

        // 3. Substring fallback (broader, but lower quality — take shortest match)
        return all
            .filter { entry ->
                val en = TeamDatabase.normalize(entry.name)
                en.contains(n) || n.contains(en) ||
                        entry.aliases.any { alias ->
                            val an = TeamDatabase.normalize(alias)
                            an.contains(n) || n.contains(an)
                        }
            }
            .minByOrNull { TeamDatabase.normalize(it.name).length }
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
