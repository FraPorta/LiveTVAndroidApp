# Football Teams & Logos Database — Implementation Plan

**Created:** 2026-03-16  
**Last updated:** 2026-03-16

## Decisions

| Concern | Decision |
|---|---|
| Logo scope | All 25 current-season leagues, compressed to 64×64 thumbnails |
| Asset prep | Manual (assets committed directly, no script) |
| Favourites | Teams **and** leagues |
| Search suggestions | Dedicated row **above** match results |
| Image loading | Coil (`io.coil-kt:coil-compose`) |
| Local DB format | `assets/team_db.json` + kotlinx.serialization (already in project) |
| Favourites storage | `SharedPreferences` (following `UrlPreferences.kt` pattern) |
| ORM | None — JSON read-only, loaded once into memory |

---

## Phase 0 — Asset preparation (one-time, manual)

- [ ] Clone `https://github.com/luukhopman/football-logos`, take `logos/` directory (2025/26)
- [ ] Batch-resize all ~450 PNGs to 64×64 (e.g. `mogrify -resize 64x64! **/*.png`)
- [ ] Copy resized tree into `app/src/main/assets/logos/` preserving `<Country - League>/<Team Name>.png` structure
- [ ] Generate `app/src/main/assets/team_db.json` — flat JSON array of `TeamEntry`:
  ```json
  [
    {
      "name": "Liverpool FC",
      "aliases": ["Liverpool"],
      "league": "England - Premier League",
      "logoAssetPath": "logos/England - Premier League/Liverpool FC.png"
    },
    ...
  ]
  ```
  Seed with common alias mappings (e.g. `"Man United" → "Manchester United"`, `"PSG" → "Paris Saint-Germain"`, `"Athletic Club" → "Athletic Bilbao"`, `"Inter" → "Inter Milan"`, `"Spurs" → "Tottenham Hotspur"`)

---

## Phase 1 — Dependencies & Model

- [ ] `app/build.gradle.kts` — add `io.coil-kt:coil-compose:2.7.0`
- [ ] Create `data/model/TeamEntry.kt`:
  ```kotlin
  @Serializable
  data class TeamEntry(
      val name: String,
      val aliases: List<String> = emptyList(),
      val league: String,
      val logoAssetPath: String
  )
  ```

---

## Phase 2 — Team Database & Matching

- [ ] Create `data/local/TeamDatabase.kt` — singleton object, lazily loads `team_db.json` via `AssetManager` + kotlinx.serialization. Builds two in-memory indexes on first access:
  - `byNormalizedName: Map<String, TeamEntry>` (key = lowercase + diacritics stripped)
  - `byLeague: Map<String, List<TeamEntry>>`
- [ ] Create `data/local/TeamMatcher.kt`:
  - `fun lookupTeam(rawName: String): TeamEntry?` — tries exact normalized match → alias match → `startsWith`/`contains` fallback
  - `fun teamsMatchingQuery(query: String): List<TeamEntry>` — prefix search on normalized name + aliases (for search suggestions)
- [ ] Create `data/local/FavouritesPreferences.kt` — wraps `SharedPreferences`, stores:
  - `favouriteTeams: Set<String>` (canonical team names)
  - `favouriteLeagues: Set<String>` (league directory names, e.g. `"England - Premier League"`)
  - `addTeam/removeTeam/isTeamFavourite`, `addLeague/removeLeague/isLeagueFavourite`

---

## Phase 3 — ViewModel

- [ ] Add `ScrapingSection.FAVOURITES` to `data/model/ScrapeSection.kt` (displayName = `"Favourites"`, pass-through selector)
- [ ] In `MatchViewModel.kt`:
  - Construct `TeamDatabase` and `FavouritesPreferences` in `init`
  - Add `favouriteTeams: MutableState<Set<String>>` and `favouriteLeagues: MutableState<Set<String>>`
  - Add `teamSuggestions: MutableState<List<TeamEntry>>` — updated in `setSearchQuery` via `TeamMatcher.teamsMatchingQuery`
  - Add `fun toggleFavouriteTeam(teamName: String)` / `fun toggleFavouriteLeague(leagueName: String)`
  - Add `fun isFavouriteMatch(match: Match): Boolean` — splits `teams` on `" vs "`, looks both up, checks vs favourites sets
  - In `getFilteredMatches()`: if `selectedSection == FAVOURITES`, additionally filter by `isFavouriteMatch`

---

## Phase 4 — UI: Team Logos

- [ ] Create `ui/TeamLogo.kt`:
  ```kotlin
  @Composable
  fun TeamLogo(teamName: String, modifier: Modifier = Modifier) {
      val entry = remember(teamName) { TeamMatcher.lookupTeam(teamName) }
      if (entry != null) {
          AsyncImage(
              model = Uri.parse("file:///android_asset/${entry.logoAssetPath}"),
              contentDescription = teamName,
              modifier = modifier.size(32.dp),
              contentScale = ContentScale.Fit,
              error = painterResource(R.drawable.ic_launcher_foreground)
          )
      }
  }
  ```
- [ ] In `MatchItem` composable (`HomeScreen.kt`):
  - Split `match.teams` on `" vs "` → `homeTeam` / `awayTeam`
  - For football matches, add `TeamLogo(homeTeam)` and `TeamLogo(awayTeam)` flanking team name text

---

## Phase 5 — UI: Favourites Section & Toggling

- [ ] Add `FAVOURITES` to `SectionSelector` segmented button row in `HomeScreen.kt`
- [ ] In expanded `MatchItem`, add ⭐ toggle button per team and per league (visible when a `TeamEntry` is found):
  - Icon fills/unfills based on `viewModel.isFavourite*` state
  - Calls `viewModel.toggleFavouriteTeam` / `viewModel.toggleFavouriteLeague`

---

## Phase 6 — UI: Search Suggestions Row

- [ ] Create `ui/SearchSuggestions.kt`:
  ```kotlin
  @Composable
  fun SearchSuggestions(
      suggestions: List<TeamEntry>,
      onSuggestionTap: (TeamEntry) -> Unit
  ) {
      LazyRow { items(suggestions) { entry ->
          SuggestionChip(
              onClick = { onSuggestionTap(entry) },
              label = { Text(entry.name) },
              icon = { TeamLogo(entry.name, Modifier.size(20.dp)) }
          )
      }}
  }
  ```
- [ ] In `HomeScreen.kt`, show `SearchSuggestions` above `LazyVerticalGrid` when `isSearchActive && searchQuery.isNotBlank() && teamSuggestions.isNotEmpty()`
- [ ] Tapping a chip sets `searchQuery` to canonical team name

---

## Phase 7 — TV / D-pad polish

- [ ] Suggestions `LazyRow` chips: add `FocusRequester`, handle D-pad left/right navigation
- [ ] ⭐ button in match cards: reachable via D-pad after stream links are expanded

---

## Status legend

| Symbol | Meaning |
|---|---|
| `[ ]` | Not started |
| `[~]` | In progress |
| `[x]` | Done |
