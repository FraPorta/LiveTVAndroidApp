package com.example.livetv.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences-backed store for favourite teams and leagues.
 *
 * Mirrors the pattern used in [com.example.livetv.data.preferences.UrlPreferences]:
 * a thin wrapper with named keys and type-safe getters/setters.
 *
 * Stored values:
 * - `favouriteTeams`   → Set<String> of canonical team names from [TeamEntry.name]
 * - `favouriteLeagues` → Set<String> of league directory names from [TeamEntry.league]
 */
class FavouritesPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME         = "livetv_favourites"
        private const val KEY_FAV_TEAMS      = "favourite_teams"
        private const val KEY_FAV_LEAGUES    = "favourite_leagues"
    }

    // ── Teams ─────────────────────────────────────────────────────────────────

    fun getFavouriteTeams(): Set<String> =
        prefs.getStringSet(KEY_FAV_TEAMS, emptySet()) ?: emptySet()

    fun addTeam(teamName: String) = setTeams(getFavouriteTeams() + teamName)

    fun removeTeam(teamName: String) = setTeams(getFavouriteTeams() - teamName)

    fun isTeamFavourite(teamName: String): Boolean = teamName in getFavouriteTeams()

    private fun setTeams(teams: Set<String>) =
        prefs.edit().putStringSet(KEY_FAV_TEAMS, teams).apply()

    // ── Leagues ───────────────────────────────────────────────────────────────

    fun getFavouriteLeagues(): Set<String> =
        prefs.getStringSet(KEY_FAV_LEAGUES, emptySet()) ?: emptySet()

    fun addLeague(leagueName: String) = setLeagues(getFavouriteLeagues() + leagueName)

    fun removeLeague(leagueName: String) = setLeagues(getFavouriteLeagues() - leagueName)

    fun isLeagueFavourite(leagueName: String): Boolean = leagueName in getFavouriteLeagues()

    private fun setLeagues(leagues: Set<String>) =
        prefs.edit().putStringSet(KEY_FAV_LEAGUES, leagues).apply()

    // ── Combined ──────────────────────────────────────────────────────────────

    fun hasAnyFavourites(): Boolean =
        getFavouriteTeams().isNotEmpty() || getFavouriteLeagues().isNotEmpty()
}
