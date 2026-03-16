package com.example.livetv.data.local

import android.content.res.AssetManager
import android.util.Log
import com.example.livetv.data.model.TeamEntry
import kotlinx.serialization.json.Json
import java.text.Normalizer
import java.util.regex.Pattern

/**
 * In-memory singleton that loads team metadata from `assets/team_db.json` once
 * and exposes fast lookup indexes.
 *
 * Call [init] once during Application/ViewModel startup before calling any
 * other method. Subsequent calls to [init] are no-ops.
 */
object TeamDatabase {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    @Volatile private var _allTeams: List<TeamEntry>? = null

    /** Keyed by the normalized form of each entry's name AND all its aliases. */
    @Volatile private var _byNormalizedName: Map<String, TeamEntry> = emptyMap()

    /** All teams grouped by league directory name. */
    @Volatile private var _byLeague: Map<String, List<TeamEntry>> = emptyMap()

    /**
     * Maps normalized short league name (the part after " - ") to the list of all
     * fully-qualified league keys that share that short name.
     * e.g. "serie a" → ["Italy - Serie A", "Argentina - Serie A"]
     */
    @Volatile private var _qualifiedKeysByShortLeague: Map<String, List<String>> = emptyMap()

    // ── Initialization ────────────────────────────────────────────────────────

    fun init(assetManager: AssetManager) {
        if (_allTeams != null) return
        synchronized(this) {
            if (_allTeams != null) return
            try {
                val jsonText = assetManager.open("team_db.json").bufferedReader().use { it.readText() }
                val teams = jsonParser.decodeFromString<List<TeamEntry>>(jsonText)

                val nameIndex: MutableMap<String, TeamEntry> = LinkedHashMap(teams.size * 2)
                teams.forEach { entry ->
                    nameIndex[normalize(entry.name)] = entry
                    entry.aliases.forEach { alias -> nameIndex[normalize(alias)] = entry }
                }

                val leagueByShort: MutableMap<String, MutableList<String>> = LinkedHashMap()
                teams.groupBy { it.league }.keys.forEach { qualifiedKey ->
                    // Qualified key format: "Country - League Name" or just "League Name"
                    val shortPart = if (qualifiedKey.contains(" - "))
                        qualifiedKey.substringAfter(" - ") else qualifiedKey
                    leagueByShort.getOrPut(normalize(shortPart)) { mutableListOf() }.add(qualifiedKey)
                }

                _allTeams           = teams
                _byNormalizedName   = nameIndex
                _byLeague           = teams.groupBy { it.league }
                _qualifiedKeysByShortLeague = leagueByShort

                Log.i("TeamDatabase", "Loaded ${teams.size} team entries from team_db.json")
            } catch (e: Exception) {
                Log.e("TeamDatabase", "Failed to load team_db.json: ${e.message}")
                _allTeams                   = emptyList()
                _byNormalizedName           = emptyMap()
                _byLeague                   = emptyMap()
                _qualifiedKeysByShortLeague = emptyMap()
            }
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    fun getAllTeams(): List<TeamEntry> = _allTeams ?: emptyList()

    fun lookupByNormalizedName(normalized: String): TeamEntry? = _byNormalizedName[normalized]

    fun getByLeague(league: String): List<TeamEntry> = _byLeague[league] ?: emptyList()

    fun getAllLeagues(): List<String> = _byLeague.keys.sorted()

    /**
     * Given a raw scraped league label (e.g. "Premier League", "Serie A"),
     * returns the best-matching fully-qualified key (e.g. "England - Premier League").
     *
     * When [contextTeams] is non-blank the qualified key is cross-checked against
     * the league of any team we can resolve from contextTeams — this disambiguates
     * e.g. "Serie A" → Italy vs Argentina when we already know which teams are playing.
     */
    fun lookupLeagueQualified(rawLeague: String, contextTeams: String = ""): String? {
        if (rawLeague.isBlank()) return null
        val n = normalize(rawLeague)

        // 1. Exact hit on a qualified key (e.g. "England - Premier League")
        if (_byLeague.containsKey(rawLeague)) return rawLeague

        // 2. Short-name index lookup
        val candidates = _qualifiedKeysByShortLeague[n] ?: return null
        if (candidates.size == 1) return candidates.first()

        // 3. Multiple candidates — use team context to disambiguate
        if (contextTeams.isNotBlank()) {
            val parts = contextTeams.split(" vs ", " v ", " – ", " — ", " - ", limit = 2)
            for (part in parts) {
                val entry = _byNormalizedName[normalize(part.trim())] ?: continue
                val match = candidates.firstOrNull { it == entry.league }
                if (match != null) return match
            }
        }
        // Fall back to first candidate (alphabetically first country)
        return candidates.minOrNull()
    }

    // ── Normalisation helper (internal + exposed for matchers) ────────────────

    private val DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")

    /**
     * Returns a stable, comparable key: strips diacritics, lowercases, collapses
     * whitespace, removes non-alphanumeric characters (except spaces).
     *
     * Examples:
     *   "FC Bayern München" → "fc bayern munchen"
     *   "Atlético Madrid"   → "atletico madrid"
     *   "Man. United"       → "man united"
     */
    fun normalize(name: String): String {
        val nfd = Normalizer.normalize(name, Normalizer.Form.NFD)
        val noDiacritics = DIACRITICS.matcher(nfd).replaceAll("")
        // Keep only letters, digits, and spaces; then collapse runs of spaces
        val lettersOnly = noDiacritics.replace(Regex("[^a-zA-Z0-9 ]"), " ")
        return lettersOnly.trim().lowercase().replace(Regex("\\s+"), " ")
    }
}
