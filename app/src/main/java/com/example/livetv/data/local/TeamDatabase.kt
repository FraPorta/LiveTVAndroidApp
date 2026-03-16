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

                _allTeams       = teams
                _byNormalizedName = nameIndex
                _byLeague       = teams.groupBy { it.league }

                Log.i("TeamDatabase", "Loaded ${teams.size} team entries from team_db.json")
            } catch (e: Exception) {
                Log.e("TeamDatabase", "Failed to load team_db.json: ${e.message}")
                _allTeams       = emptyList()
                _byNormalizedName = emptyMap()
                _byLeague       = emptyMap()
            }
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    fun getAllTeams(): List<TeamEntry> = _allTeams ?: emptyList()

    fun lookupByNormalizedName(normalized: String): TeamEntry? = _byNormalizedName[normalized]

    fun getByLeague(league: String): List<TeamEntry> = _byLeague[league] ?: emptyList()

    fun getAllLeagues(): List<String> = _byLeague.keys.sorted()

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
