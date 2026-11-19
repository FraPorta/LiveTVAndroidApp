package com.example.livetv.data.local

import com.example.livetv.data.model.TeamEntry
import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sanity checks on the bundled team database (assets/team_db.json). */
class TeamDbTest {
    private val teams: List<TeamEntry> by lazy {
        val file = listOf("src/main/assets/team_db.json", "app/src/main/assets/team_db.json")
            .map(::File).first { it.exists() }
        Json { ignoreUnknownKeys = true }.decodeFromString(file.readText())
    }

    @Test fun everyEntryPointsToAnExistingLogoFile() {
        val assets = listOf("src/main/assets", "app/src/main/assets").map(::File).first { it.exists() }
        val missing = teams.filter { !File(assets, it.logoAssetPath).exists() }.map { it.name }
        assertTrue("Entries without a logo file: $missing", missing.isEmpty())
    }

    @Test fun bareParisMeansParisFcNotPsg() {
        val holders = teams.filter { t -> t.aliases.any { it.equals("Paris", ignoreCase = true) } }.map { it.name }
        assertEquals(listOf("Paris FC"), holders)
    }
}
