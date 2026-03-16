package com.example.livetv.data.model

import kotlinx.serialization.Serializable

/**
 * A single team entry from the bundled offline database (assets/team_db.json).
 *
 * @param name         Canonical team name matching the filename in assets (without .png).
 * @param aliases      Alternate names scraped data may use (e.g. "PSG" for "Paris Saint-Germain").
 * @param league       League directory name from the football-logos repo
 *                     (e.g. "England - Premier League").
 * @param logoAssetPath Relative path inside assets/, e.g. "logos/England - Premier League/Arsenal.png".
 */
@Serializable
data class TeamEntry(
    val name: String,
    val aliases: List<String> = emptyList(),
    val league: String,
    val logoAssetPath: String,
)
