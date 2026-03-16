package com.example.livetv.data.network

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.SslErrorHandler
import android.net.http.SslError
import android.os.Build
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import com.example.livetv.data.model.Match
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.jsoup.Jsoup
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import okhttp3.OkHttpClient
import okhttp3.Request
import com.example.livetv.data.preferences.UrlPreferences
import com.example.livetv.data.model.ScrapingSection
import com.example.livetv.BuildConfig

class Scraper(private val context: Context) {

    private val urlPreferences = UrlPreferences(context)

    // FIX #15: A single OkHttpClient is reused for all scraping calls so the connection pool,
    // thread pool, and SSL session cache are shared. The hostname verifier reads the configured
    // base URL at verification time, so it stays correct when the user changes the URL at runtime.
    private val scrapingClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .sslSocketFactory(createInsecureSslSocketFactory(), createTrustAllManager())
            .hostnameVerifier { hostname, _ ->
                val allowedHost = try {
                    java.net.URI(urlPreferences.getBaseUrl()).host ?: ""
                } catch (_: Exception) { "" }
                val allowed = allowedHost.isNotEmpty() &&
                    (hostname == allowedHost || hostname.endsWith(".$allowedHost"))
                if (!allowed) Log.w("Scraper", "SSL hostname rejected: $hostname (expected $allowedHost)")
                allowed
            }
            .build()
    }

    // FIX #16: Cache the last successfully fetched HTML so that scrapeAllMatches() (called
    // immediately after scrapeMatchList() during initial load) can reuse the same HTML without
    // making a second HTTP request to an identical URL.
    private data class HtmlCacheEntry(val url: String, val html: String, val fetchedAt: Long)
    @Volatile private var htmlCache: HtmlCacheEntry? = null
    private val HTML_CACHE_TTL_MS = 60_000L // 60 seconds

    companion object {
        // Pre-compiled regexes — avoids recompiling the same patterns on every fetchStreamLinks call.
        private val JS_URL_REGEX = """https?://[^\s"'<>]+(?:\.m3u8|stream|live|watch|player)""".toRegex(RegexOption.IGNORE_CASE)
        private val ACESTREAM_REGEX = "acestream://[a-zA-Z0-9]+".toRegex()
        private val M3U8_REGEX = """https?://[^\s"'<>]+\.m3u8""".toRegex(RegexOption.IGNORE_CASE)
        private val RTMP_REGEX = """rtmps?://[^\s"'<>]+""".toRegex(RegexOption.IGNORE_CASE)
        private val WEBPLAYER_REGEX = """(?:https?:)?//[^\s"'<>]+webplayer[^\s"'<>]*""".toRegex(RegexOption.IGNORE_CASE)

        // FIX #27: Single source of truth for football-related keywords.
        // Previously hard-coded twice (in scrapeMatchList and scrapeAllMatches), now referenced
        // from both call sites so a future keyword addition only needs one change.
        val FOOTBALL_KEYWORDS: Set<String> = setOf(
            "football", "soccer", "premier", "liga", "bundesliga", "serie a",
            "ligue", "champions league", "europa league", "uefa", "fifa", "world cup"
        )

        /** Competitions that span multiple countries — country should be "Europe" or "World". */
        private val CROSS_BORDER_LEAGUES: Set<String> = setOf(
            "champions league", "europa league", "conference league",
            "uefa super cup", "world cup", "european championship", "euros",
            "nations league", "olympic", "copa america", "africa cup", "afcon",
            "copa del rey", "fa cup", "dfb pokal", "coupe de france"
        )
        private val WORLD_COMPETITIONS: Set<String> = setOf(
            "world cup", "copa america", "africa cup", "afcon", "olympic"
        )
    }

    /**
     * Scrapes the main page to get a list of upcoming matches, but doesn't
     * fetch the stream links yet. This is designed to be fast.
     * @param section The section to scrape from
     * @param limit Maximum number of matches to return (0 = no limit)
     * @param offset Starting position for pagination (0-based)
     */
    suspend fun scrapeMatchList(
        section: ScrapingSection = ScrapingSection.ALL,
        limit: Int = 0,
        offset: Int = 0
    ): List<Match> = withContext(Dispatchers.IO) {
        val url = urlPreferences.getBaseUrl()
        val baseOrigin = baseOriginOf(url)
        Log.d("Scraper", "Fetching initial match list from: $url (section: ${section.displayName})")
        try {
            // FIX #20: Delegate to shared helper — no more duplicated parsing logic.
            val doc = Jsoup.parse(fetchHtmlWithOkHttp(url))
            val result = parseMatchRows(sectionDocFor(doc, section), section, baseOrigin, limit, offset)
            Log.d("Scraper", "Successfully parsed ${result.size} matches from main page.")
            result
        } catch (e: Exception) {
            Log.e("Scraper", "Error scraping match list", e)
            emptyList()
        }
    }

    /**
     * Scrapes ALL available matches from the selected section without fetching stream links.
     * This is designed for background scraping to enable search functionality.
     * @param section The section to scrape from
     */
    suspend fun scrapeAllMatches(section: ScrapingSection = ScrapingSection.ALL): List<Match> = withContext(Dispatchers.IO) {
        val url = urlPreferences.getBaseUrl()
        val baseOrigin = baseOriginOf(url)
        Log.d("Scraper", "Background scraping ALL matches from: $url (section: ${section.displayName})")
        try {
            // FIX #20: Delegate to shared helper — no more duplicated parsing logic.
            val doc = Jsoup.parse(fetchHtmlWithOkHttp(url))
            val result = parseMatchRows(sectionDocFor(doc, section), section, baseOrigin, 0, 0)
            Log.d("Scraper", "Background scraping completed: ${result.size} matches found")
            result
        } catch (e: Exception) {
            Log.e("Scraper", "Error in background scraping", e)
            emptyList()
        }
    }

    /**
     * FIX #20: Selects the relevant sub-document for a given [ScrapingSection].
     * Previously this identical `when` block was inlined — and duplicated — in both
     * [scrapeMatchList] and [scrapeAllMatches].
     */
    private fun sectionDocFor(
        doc: org.jsoup.nodes.Document,
        section: ScrapingSection
    ): org.jsoup.nodes.Element = when (section) {
        ScrapingSection.ALL -> doc
        ScrapingSection.TOP_EVENTS_LIVE -> {
            val upcoming = doc.select("#upcoming").first()
            if (upcoming != null) {
                Log.d("Scraper", "Found 'upcoming' section with ${upcoming.select("a").size} links")
                upcoming
            } else {
                Log.d("Scraper", "No 'upcoming' section found, falling back to full document")
                doc
            }
        }
        ScrapingSection.FOOTBALL -> {
            val copy = doc.clone()
            copy.select("#upcoming").remove()
            Log.d("Scraper", "Using document minus #upcoming for ${section.displayName} section")
            copy
        }
    }

    /**
     * FIX #20: Single implementation of match-row parsing shared by [scrapeMatchList]
     * (paginated, limit > 0) and [scrapeAllMatches] (limit = 0, no pagination).
     *
     * Handles: link-selector cascade, per-row field extraction, time fallback,
     * team/competition swap heuristic, parent-traversal fallback, dedup,
     * FOOTBALL section filtering, and optional pagination.
     */
    private fun parseMatchRows(
        sectionDoc: org.jsoup.nodes.Element,
        section: ScrapingSection,
        baseOrigin: String,
        limit: Int = 0,
        offset: Int = 0
    ): List<Match> {
        // ── Link discovery ────────────────────────────────────────────────────
        var detailLinks = sectionDoc.select("a[href*='/enx/event/']")
        Log.d("Scraper", "Found ${detailLinks.size} links with '/enx/event/' pattern in ${section.displayName} section")
        if (detailLinks.isEmpty()) {
            detailLinks = sectionDoc.select("a[href*='/event/']")
            Log.d("Scraper", "Found ${detailLinks.size} links with '/event/' pattern")
        }
        if (detailLinks.isEmpty()) {
            detailLinks = sectionDoc.select("a[href*='event']")
            Log.d("Scraper", "Found ${detailLinks.size} links containing 'event'")
        }
        if (BuildConfig.DEBUG) {
            val allLinks = sectionDoc.select("a[href]")
            Log.d("Scraper", "Total links found in section: ${allLinks.size}")
            if (allLinks.isNotEmpty() && detailLinks.isEmpty()) {
                Log.d("Scraper", "Sample of first 10 links found:")
                allLinks.take(10).forEach { Log.d("Scraper", "Link: ${it.attr("href")} - Text: ${it.text().take(50)}") }
            }
            if (detailLinks.isEmpty()) {
                Log.d("Scraper", "Found ${sectionDoc.select("table").size} tables in section")
                Log.d("Scraper", "Section text (first 500 chars): ${sectionDoc.text().take(500)}")
            }
        }

        // ── Per-link processing ───────────────────────────────────────────────
        val matches = mutableListOf<Match>()
        for (link in detailLinks) {
            val href = link.attr("href")
            if (href.isBlank()) continue

            val detailPageUrl = when {
                href.startsWith("http") -> href
                href.startsWith("/")    -> "$baseOrigin$href"
                else                    -> "$baseOrigin/$href"
            }

            val row: org.jsoup.nodes.Element = link.closest("tr") ?: link.parent() ?: link

            var time = row.select("td.time, .time, [class*='time'], td:first-child").text().trim()
            var teams = row.select("td.evdesc, .evdesc, .event-title, .event-desc, [class*='event'], [class*='team'], td:nth-child(3)").text().trim()
            var competition = row.select("td.league > a, .league, .competition, [class*='league'], td:nth-child(2)").text().trim()

            if (teams.isBlank() || teams.length < 5) {
                teams = row.select("a").first()?.text()?.trim() ?: ""
            }

            // Heuristic: swap teams/competition if their content looks reversed.
            if (teams.isNotBlank() && competition.isNotBlank()) {
                val teamsLooksLikeLeague = teams.length < 10 ||
                    teams.contains(Regex("""\([^)]+\)""")) ||
                    teams.contains(Regex("""\d{1,2}\s+\w+\s+at""")) ||
                    teams.lowercase().contains(Regex("""\b(ncaa|nba|nfl|mlb|nhl|premier|liga|serie|bundesliga|league|cup|championship|division|conference|botola|pro|first|elite)\b"""))
                val competitionLooksLikeTeams = competition.length > 15 ||
                    competition.contains(Regex("""[–—-]|\bvs?\.?\b|\d+:\d+""")) ||
                    competition.split(Regex("""[–—-]|\bvs?\.?\b""")).size == 2
                if (teamsLooksLikeLeague && competitionLooksLikeTeams) {
                    val tmp = teams; teams = competition; competition = tmp
                    if (BuildConfig.DEBUG) Log.d("Scraper", "Swapped teams/competition — Teams: '$teams', Competition: '$competition'")
                }
            }

            // Time fallback: extract from combined text
            if (time.isBlank()) {
                val ct = "$teams $competition"
                time = Regex("""\b(\d{1,2}:\d{2})\b""").find(ct)?.value
                    ?: Regex("""\d{1,2}\s+\w+\s+at\s+(\d{1,2}:\d{2})""").find(ct)?.groupValues?.getOrNull(1)
                    ?: ""
            }

            // Team-name fallbacks
            if (teams.isBlank() || teams.length < 5) teams = link.text().trim()
            if (teams.isBlank() || teams.length < 5) {
                var parent = link.parent(); var attempts = 0
                while (parent != null && attempts < 3 && (teams.isBlank() || teams.length < 5)) {
                    val t = parent.ownText().trim()
                    if (t.isNotBlank() && t.length > 5) { teams = t; break }
                    parent = parent.parent(); attempts++
                }
            }

            teams = cleanTeamNames(teams, time, competition)
            val (sport, league, country) = extractSportAndLeague(competition, teams, row, detailPageUrl)

            if (BuildConfig.DEBUG) {
                Log.d("Scraper", "Raw: Time='$time' Teams='$teams' Competition='$competition'")
                Log.d("Scraper", "Final: Teams='$teams' Sport='$sport' League='$league' Country='$country'")
            }

            if (teams.isNotBlank() && teams.length > 3) {
                matches.add(Match(time, teams, competition, sport, league, detailPageUrl, country = country))
            } else if (BuildConfig.DEBUG) {
                Log.w("Scraper", "Skipped — teams too short/blank: '$teams'")
            }
        }

        // ── Dedup ─────────────────────────────────────────────────────────────
        val uniqueMatches = matches.distinctBy { it.detailPageUrl }
        if (matches.size != uniqueMatches.size) {
            Log.d("Scraper", "Removed ${matches.size - uniqueMatches.size} duplicate matches.")
        }

        // ── Section filtering ─────────────────────────────────────────────────
        val filteredMatches = when (section) {
            ScrapingSection.FOOTBALL -> {
                val footballMatches = uniqueMatches.filter { match ->
                    val ct = "${match.teams} ${match.competition} ${match.league} ${match.sport}".lowercase()
                    FOOTBALL_KEYWORDS.any { ct.contains(it) } || match.sport.lowercase() == "football"
                }
                Log.d("Scraper", "Football filtering: ${uniqueMatches.size} -> ${footballMatches.size} matches")
                footballMatches
            }
            else -> uniqueMatches
        }

        // ── Pagination (limit = 0 means no limit) ─────────────────────────────
        return if (limit > 0) {
            val start = offset.coerceAtLeast(0)
            val end = (start + limit).coerceAtMost(filteredMatches.size)
            Log.d("Scraper", "Pagination — offset=$offset limit=$limit returned=${end - start} of ${filteredMatches.size}")
            if (start < filteredMatches.size) filteredMatches.subList(start, end) else emptyList()
        } else {
            filteredMatches
        }
    }

    /**
     * Extracts sport and league information from available data.
     * Uses competition text, team names, and URL patterns to determine sport and league.
     */
    private fun extractSportAndLeague(competition: String, teams: String, row: org.jsoup.nodes.Element?, detailPageUrl: String): Triple<String, String, String> {
        var sport = "Football" // Default to football since it's the most common
        var league = "" // Will be determined based on specific league detection
        var country = "" // Will be resolved via TeamMatcher.lookupLeague
        
        val combinedText = "$competition $teams $detailPageUrl".lowercase()
        
        // Sport detection based on keywords
        when {
            combinedText.contains("football") || combinedText.contains("soccer") || 
            combinedText.contains("premier league") || combinedText.contains("la liga") ||
            combinedText.contains("serie a") || combinedText.contains("bundesliga") ||
            combinedText.contains("champions league") || combinedText.contains("uefa") ||
            combinedText.contains("fifa") || combinedText.contains("world cup") ||
            combinedText.contains("ligue 1") || combinedText.contains("eredivisie") -> {
                sport = "Football"
            }
            combinedText.contains("basketball") || combinedText.contains("nba") || 
            combinedText.contains("euroleague") || combinedText.contains("fiba") -> {
                sport = "Basketball"
            }
            combinedText.contains("tennis") || combinedText.contains("atp") || 
            combinedText.contains("wta") || combinedText.contains("wimbledon") ||
            combinedText.contains("us open") || combinedText.contains("french open") -> {
                sport = "Tennis"
            }
            combinedText.contains("hockey") || combinedText.contains("nhl") || 
            combinedText.contains("iihf") -> {
                sport = "Ice Hockey"
            }
            combinedText.contains("baseball") || combinedText.contains("mlb") -> {
                sport = "Baseball"
            }
            combinedText.contains("rugby") -> {
                sport = "Rugby"
            }
            combinedText.contains("cricket") -> {
                sport = "Cricket"
            }
            combinedText.contains("boxing") || combinedText.contains("mma") || 
            combinedText.contains("ufc") -> {
                sport = "Combat Sports"
            }
            combinedText.contains("formula") || combinedText.contains(Regex("""\bf1\b""", RegexOption.IGNORE_CASE)) ||
            combinedText.contains("motogp") || combinedText.contains("racing") -> {
                sport = "Motor Sports"
            }
            combinedText.contains("volleyball") -> {
                sport = "Volleyball"
            }
        }
        
        // League extraction based on common patterns
        when {
            // Football leagues
            combinedText.contains("premier league") -> league = "Premier League"
            combinedText.contains("la liga") -> league = "La Liga"
            combinedText.contains("serie a") -> league = "Serie A"
            combinedText.contains("bundesliga") -> league = "Bundesliga"
            combinedText.contains("ligue 1") -> league = "Ligue 1"
            combinedText.contains("champions league") -> league = "Champions League"
            combinedText.contains("europa league") -> league = "Europa League"
            combinedText.contains("world cup") -> league = "World Cup"
            combinedText.contains("euros") || combinedText.contains("euro 20") -> league = "European Championship"
            combinedText.contains("eredivisie") -> league = "Eredivisie"
            combinedText.contains("mls") -> league = "MLS"
            
            // Basketball leagues
            combinedText.contains("nba") -> league = "NBA"
            combinedText.contains("euroleague") -> league = "EuroLeague"
            combinedText.contains("ncaa") -> league = "NCAA"
            
            // Tennis tournaments
            combinedText.contains("wimbledon") -> league = "Wimbledon"
            combinedText.contains("us open") -> league = "US Open"
            combinedText.contains("french open") -> league = "French Open"
            combinedText.contains("australian open") -> league = "Australian Open"
            combinedText.contains("atp") -> league = "ATP Tour"
            combinedText.contains("wta") -> league = "WTA Tour"
            
            // Other sports
            combinedText.contains("nhl") -> league = "NHL"
            combinedText.contains("mlb") -> league = "MLB"
            combinedText.contains("nfl") -> league = "NFL"
            combinedText.contains("ufc") -> league = "UFC"
            combinedText.contains("formula 1") || combinedText.contains("f1") -> league = "Formula 1"
            
            // Only set league if we specifically identified one, otherwise leave it blank
            // This prevents duplication with competition field
        }

        // ── Country resolution — team-DB-first approach ──────────────────────────────
        //
        // Priority order:
        // 1. Known cross-border competition → force Europe / World (no team lookup needed)
        // 2. Resolve from both/one team in the DB (ground-truth: each team knows its own league)
        // 3. Fallback: lookupLeague on the short league name (text-based, only as last resort)
        // 4. Fallback: lookupLeague on the raw competition field

        val leagueLower = league.lowercase()
        val competitionLower = competition.lowercase()

        when {
            WORLD_COMPETITIONS.any { leagueLower.contains(it) || competitionLower.contains(it) } -> {
                country = "World"
            }
            CROSS_BORDER_LEAGUES.any { leagueLower.contains(it) || competitionLower.contains(it) } -> {
                country = "Europe"
            }
            else -> {
                // Try team-DB lookup first
                val teamResolution = com.example.livetv.data.local.TeamMatcher.resolveLeagueFromTeams(teams)
                if (teamResolution != null) {
                    country = teamResolution.country
                    // Fill in league from DB resolution whenever it is blank,
                    // regardless of bothMatched — avoids blank league on partial matches.
                    if (league.isBlank()) {
                        val q = teamResolution.qualifiedKey
                        league = if (q.contains(" - ")) q.substringAfter(" - ") else q
                    }
                } else {
                    // Neither team is in the DB — do NOT assign a country.
                    // Keyword-matched league name is kept for display, but showing a flag
                    // would be misleading (we can't confirm which country's competition this is).
                }
            }
        }

        return Triple(sport, league, country)
    }

    /**
     * Cleans team names by removing time and league information that might have been mixed in
     * Also extracts proper time information from mixed content
     */
    private fun cleanTeamNames(teams: String, time: String, competition: String): String {
        var cleaned = teams
        
        // Remove date patterns (e.g., "14 September at", "15 September at")
        cleaned = cleaned.replace(Regex("""\d{1,2}\s+\w+\s+at\s*"""), "").trim()
        
        // Remove time patterns (HH:MM format)
        cleaned = cleaned.replace(Regex("""\d{1,2}:\d{2}"""), "").trim()
        
        // Remove competition/league text if it appears in teams (after parentheses)
        cleaned = cleaned.replace(Regex("""\([^)]*\)"""), "").trim()
        
        // Remove league/competition names that might be mixed in
        if (competition.isNotBlank()) {
            cleaned = cleaned.replace(competition, "", ignoreCase = true).trim()
        }
        
        // Remove common time/date patterns
        val patterns = listOf(
            """\d{1,2}\s+\w+\s+\d{4}\s+at\s*""", // "14 September 2025 at"
            """\w+\s+\d{1,2}\s+at\s*""", // "September 14 at"
            """\d{1,2}\s+\w+\s+at\s*""", // "14 September at"
            """at\s+\d{1,2}:\d{2}""", // "at 15:30"
            """\blive\b|\btoday\b|\btomorrow\b|\bnow\b""",
            """\bGMT\b|\bUTC\b|\bCET\b|\bEST\b|\bPST\b""",
            """\s+0:\d+\s*$""" // Remove scores like "0:0" at the end
        )
        
        patterns.forEach { pattern ->
            cleaned = cleaned.replace(Regex(pattern, RegexOption.IGNORE_CASE), "").trim()
        }
        
        // Remove extra whitespace and clean up
        cleaned = cleaned.replace(Regex("""\s+"""), " ").trim()
        
        // Remove leading/trailing punctuation but keep team separators like "–" and "-"
        cleaned = cleaned.replace(Regex("""^[|:,.;\s]+|[|:,.;\s]+$"""), "").trim()
        
        // Ensure we have actual team names (should contain team separator like – or vs)
        return if (cleaned.isNotBlank() && cleaned.length > 3 && 
                   (cleaned.contains("–") || cleaned.contains("-") || cleaned.contains("vs") || cleaned.contains("v "))) {
            cleaned
        } else {
            teams // Return original if cleaning removed too much
        }
    }

    /**
     * Scrapes a single match detail page to find all available stream links.
     * Detects multiple stream types: Acestream, M3U8, RTMP, YouTube, Twitch, and other HTTP streams.
     */
    suspend fun fetchStreamLinks(detailPageUrl: String): List<String> = withContext(Dispatchers.IO) {
        Log.d("Scraper", "Fetching stream links from: $detailPageUrl")
        
        try {
            val html = fetchHtmlWithOkHttp(detailPageUrl)
            val doc = Jsoup.parse(html)
            val links = mutableSetOf<String>()

            // 1. Acestream links (P2P streaming)
            val acestreamLinks = doc.select("a[href*='acestream://']").map { it.attr("href") }
            links.addAll(acestreamLinks)
            Log.d("Scraper", "Found ${acestreamLinks.size} Acestream links")

            // 2. M3U8 HLS streams (HTTP Live Streaming)
            val m3u8Links = doc.select("a[href*='.m3u8']").map { it.attr("href") }
            links.addAll(m3u8Links)
            
            // 3. RTMP streams (Real-Time Messaging Protocol)
            val rtmpLinks = doc.select("a[href^='rtmp://'], a[href^='rtmps://']").map { it.attr("href") }
            links.addAll(rtmpLinks)

            // 4. YouTube live streams
            val youtubeLinks = doc.select("a[href*='youtube.com/watch'], a[href*='youtu.be/']").map { it.attr("href") }
            links.addAll(youtubeLinks)

            // 5. Twitch streams
            val twitchLinks = doc.select("a[href*='twitch.tv/']").map { it.attr("href") }
            links.addAll(twitchLinks)

            // 6. Webplayer links (protocol-relative URLs starting with //)
            val webplayerLinks = doc.select("a[href*='webplayer']")
                .map { it.attr("href") }
                .map { url ->
                    // Convert protocol-relative URLs to HTTPS
                    if (url.startsWith("//")) {
                        "https:$url"
                    } else {
                        url
                    }
                }
            links.addAll(webplayerLinks)
            if (BuildConfig.DEBUG) Log.d("Scraper", "Found ${webplayerLinks.size} webplayer links: ${webplayerLinks.joinToString()}")

            // 7. Links in JavaScript or embedded content
            val scriptTags = doc.select("script")
            scriptTags.forEach { script ->
                val scriptContent = script.html()
                // FIX #17: Use pre-compiled companion-object regex instead of recompiling each iteration
                val jsUrls = JS_URL_REGEX.findAll(scriptContent).map { it.value }.toList()
                links.addAll(jsUrls)
            }

            // 8. Iframe sources (embedded players)
            val iframeLinks = doc.select("iframe[src]").map { it.attr("src") }
                .filter { url ->
                    url.isNotBlank() && (
                        url.contains("stream", ignoreCase = true) ||
                        url.contains("live", ignoreCase = true) ||
                        url.contains("player", ignoreCase = true) ||
                        url.contains("embed", ignoreCase = true)
                    )
                }
            links.addAll(iframeLinks)

            // 9. Fallback regex search in the HTML text for various stream protocols
            if (links.isEmpty()) {
                val bodyText = doc.body().text()
                links.addAll(ACESTREAM_REGEX.findAll(bodyText).map { it.value })
                links.addAll(M3U8_REGEX.findAll(bodyText).map { it.value })
                links.addAll(RTMP_REGEX.findAll(bodyText).map { it.value })
            }

            // 10. Also search in the raw HTML for hidden links (uses pre-compiled companion regexes)
            listOf(ACESTREAM_REGEX, M3U8_REGEX, RTMP_REGEX, WEBPLAYER_REGEX).forEach { regex ->
                links.addAll(regex.findAll(html).map { m ->
                    if (m.value.startsWith("//")) "https:${m.value}" else m.value
                })
            }

            val allLinks = links.toList().distinct()
            
            // Transform acestream links to HTTP proxy format
            val transformedLinks = allLinks.map { link ->
                if (link.startsWith("acestream://")) {
                    val acestreamId = link.removePrefix("acestream://")
                    val acestreamIp = getAcestreamIp()
                    val httpProxyUrl = "http://$acestreamIp:6878/ace/getstream?id=$acestreamId"
                    Log.d("Scraper", "Transformed acestream link: $link -> $httpProxyUrl")
                    httpProxyUrl
                } else {
                    link
                }
            }
            
            // FIX #18: Guard O(n) per-URL diagnostic logging behind DEBUG so it doesn't
            // emit one log line per URL in release builds.
            if (BuildConfig.DEBUG) {
                Log.d("Scraper", "Found ${transformedLinks.size} URLs before filtering:")
                transformedLinks.forEach { url ->
                    Log.d("Scraper", "URL: $url")
                }
            }

            // Filter out invalid or incomplete URLs
            val finalLinks = transformedLinks.filter { link ->
                val isValid = isValidStreamUrl(link)
                if (!isValid && BuildConfig.DEBUG) {
                    Log.d("Scraper", "FILTERED OUT invalid URL: $link")
                }
                isValid
            }
            
            Log.d("Scraper", "Final valid URLs: ${finalLinks.size}")
            Log.d("Scraper", "Found ${transformedLinks.size} total links, ${finalLinks.size} valid stream links for $detailPageUrl")
            // FIX #18: Building the stream-types summary string is O(n) work; keep it debug-only.
            if (BuildConfig.DEBUG && transformedLinks.size != finalLinks.size) {
                val filteredOut = transformedLinks - finalLinks.toSet()
                Log.d("Scraper", "Filtered out ${filteredOut.size} invalid links: ${filteredOut.joinToString()}")
            }
            if (BuildConfig.DEBUG) {
                Log.d("Scraper", "Stream types found: ${finalLinks.joinToString(", ") { 
                    when {
                        it.startsWith("acestream://") -> "Acestream"
                        it.contains("/ace/getstream?id=") -> "Acestream (HTTP Proxy)"
                        it.contains(".m3u8") -> "M3U8/HLS"
                        it.startsWith("rtmp") -> "RTMP"
                        it.contains("youtube.com") || it.contains("youtu.be") -> "YouTube"
                        it.contains("twitch.tv") -> "Twitch"
                        else -> "HTTP/Web"
                    }
                }}")
            }
            
            finalLinks
        } catch (e: Exception) {
            Log.e("Scraper", "Error fetching stream links for $detailPageUrl", e)
            emptyList()
        }
    }

    /**
     * Validates if a stream URL is complete and potentially functional
     */
    private fun isValidStreamUrl(url: String): Boolean {
        if (url.isBlank()) return false
        
        return when {
            // Acestream links must be properly formatted
            url.startsWith("acestream://") -> {
                url.length > "acestream://".length && 
                url.substringAfter("acestream://").isNotBlank()
            }
            
            // HTTP/HTTPS links validation
            url.startsWith("http://") || url.startsWith("https://") -> {
                // Special validation for acestream HTTP proxy URLs
                if (url.contains("/ace/getstream?id=")) {
                    val idParam = url.substringAfter("/ace/getstream?id=")
                    idParam.isNotBlank() && idParam.length >= 32 // acestream IDs are typically 40 chars but allow some flexibility
                } else {
                    validateHttpUrl(url)
                }
            }
            
            // RTMP links
            url.startsWith("rtmp://") || url.startsWith("rtmps://") -> {
                url.length > 7 && url.contains(".")
            }
            
            // Other protocols should have meaningful content after ://
            url.contains("://") -> {
                val afterProtocol = url.substringAfter("://")
                afterProtocol.isNotBlank() && afterProtocol.length > 2
            }
            
            // Reject anything else that doesn't look like a proper URL
            else -> false
        }
    }
    
    private fun validateHttpUrl(url: String): Boolean {
        // Check for incomplete URLs like "https://cdn.live:" or "http://cdn.live"
        val afterProtocol = when {
            url.startsWith("https://") -> url.substring(8)
            url.startsWith("http://") -> url.substring(7)
            else -> return false
        }
        
        // Enhanced validation for proper domains
        if (afterProtocol.isBlank() || afterProtocol.length < 4) return false
        
        // Must not end with colon or have incomplete domain patterns
        if (afterProtocol.endsWith(":") || afterProtocol.endsWith(".")) return false
        
        // Check for cdn.live pattern specifically (common broken pattern)
        if (afterProtocol.matches(Regex("cdn\\.live[:\\.]*"))) return false
        
        // Must have valid domain structure
        val domainPart = afterProtocol.split("/")[0].split("?")[0].split("#")[0]
        val hasValidDomain = domainPart.contains(".") && 
                           !domainPart.startsWith(".") && 
                           domainPart.split(".").size >= 2 &&
                           domainPart.split(".").all { it.isNotEmpty() } &&
                           domainPart.length > 4
        
        return hasValidDomain
    }

    /**
     * Extracts the origin (scheme + host) from a URL so that relative links scraped from a
     * page are resolved against the configured base URL rather than the hardcoded livetv.sx
     * domain. Falls back to the livetv.sx origin if the URL cannot be parsed.
     * FIX #12
     */
    private fun baseOriginOf(url: String): String = try {
        val parsed = java.net.URI(url)
        "${parsed.scheme}://${parsed.host}"
    } catch (_: Exception) { "https://livetv.sx" }

    private suspend fun fetchHtmlWithOkHttp(url: String): String = withContext(Dispatchers.IO) {
        // FIX #16: Return cached HTML if the same URL was fetched within the TTL window.
        // This prevents scrapeAllMatches() (background) from re-downloading a page that
        // scrapeMatchList() (initial load) already fetched moments earlier.
        htmlCache?.let { entry ->
            if (entry.url == url && System.currentTimeMillis() - entry.fetchedAt < HTML_CACHE_TTL_MS) {
                Log.d("Scraper", "HTML cache hit for $url (${entry.html.length} chars)")
                return@withContext entry.html
            }
        }

        // FIX #15: Reuse the single shared scrapingClient instead of building a new one per call.
        val request = okhttp3.Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Accept-Encoding", "identity")
            .header("Connection", "keep-alive")
            .header("Cache-Control", "no-cache")
            .build()

        scrapingClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e("Scraper", "HTTP error: ${response.code} - ${response.message}")
                throw java.io.IOException("HTTP error: ${response.code}")
            }

            val body = response.body
                ?: throw java.io.IOException("Empty response body")

            val content = body.string()
            if (BuildConfig.DEBUG) Log.d("Scraper", "Response length: ${content.length} chars, first 200 chars: ${content.take(200)}")
            // FIX #16: Populate the cache so a subsequent scrapeAllMatches() call for the same
            // URL can skip the network round-trip entirely.
            htmlCache = HtmlCacheEntry(url, content, System.currentTimeMillis())
            content
        }
    }

    private fun createInsecureSslSocketFactory(): javax.net.ssl.SSLSocketFactory {
        val trustAllManager = createTrustAllManager()
        val sslContext = javax.net.ssl.SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustAllManager), java.security.SecureRandom())
        return sslContext.socketFactory
    }

    private fun createTrustAllManager(): javax.net.ssl.X509TrustManager {
        return object : javax.net.ssl.X509TrustManager {
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
        }
    }

    // A private class that will act as the bridge between JavaScript and Kotlin
    private class WebAppInterface(private val onHtmlReady: (String) -> Unit) {
        @JavascriptInterface
        fun processHTML(html: String) {
            onHtmlReady(html)
        }
    }

    private suspend fun fetchHtmlWithWebView(url: String, waitForSelector: String): String? = withTimeoutOrNull(20000) { // 20 second timeout
        // FIX #13/#14: Use withContext(Dispatchers.Main) instead of GlobalScope.launch so the
        // WebView lifetime is tied to the calling coroutine's scope (viewModelScope). The outer
        // try/finally is the single, reliable cleanup point — it fires on normal completion,
        // cancellation, and exceptions alike, replacing the unreliable invokeOnCancellation lambda.
        withContext(Dispatchers.Main) {
            Log.d("ScraperWebView", "Creating WebView for $url")
            val webView = WebView(context)
            try {
                suspendCancellableCoroutine<String?> { continuation ->
                    val webAppInterface = WebAppInterface { html ->
                        if (continuation.isActive) {
                            continuation.resume(html)
                        }
                        // Cleanup handled by the outer try/finally; do not call webView.destroy() here.
                    }

                    val desktopUserAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"
                    webView.settings.userAgentString = desktopUserAgent
                    webView.settings.javaScriptEnabled = true
                    webView.settings.domStorageEnabled = true
                    webView.settings.allowFileAccess = true
                    webView.settings.allowContentAccess = true
                    // FIX #4: allowUniversalAccessFromFileURLs removed — it allowed JS to bypass
                    // the Same-Origin Policy and read arbitrary local files. Default (false) is secure.
                    webView.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    webView.addJavascriptInterface(webAppInterface, "Android")

                    webView.webViewClient = object : WebViewClient() {
                        // FIX #2: Cancel rather than proceed on SSL errors. Silently proceeding
                        // allowed MITM attacks inside the WebView scraping session.
                        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                            Log.e("ScraperWebView", "SSL error (cancelled): primaryError=${error?.primaryError}, url=${error?.url}")
                            handler?.cancel()
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            Log.d("ScraperWebView", "onPageFinished for $url. Injecting polling script.")
                            val script = """
                                (function() {
                                    const selector = '$waitForSelector';
                                    const maxTries = 40; // Increased from 20
                                    let tries = 0;
                                    const interval = setInterval(() => {
                                        const elementFound = document.querySelector(selector);
                                        if (elementFound || tries >= maxTries) {
                                            clearInterval(interval);
                                            if(elementFound) {
                                                Android.processHTML(document.documentElement.outerHTML);
                                            } else {
                                                // If element is not found after all tries, return the whole html
                                                // to allow for fallback parsing.
                                                Android.processHTML(document.documentElement.outerHTML);
                                            }
                                        }
                                        tries++;
                                    }, 500);
                                })();
                            """
                            view.evaluateJavascript(script, null)
                        }

                         @RequiresApi(Build.VERSION_CODES.M)
                         override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            super.onReceivedError(view, request, error)
                            if (continuation.isActive) {
                                continuation.resumeWithException(RuntimeException("WebView error: ${error?.description}"))
                            }
                            // Cleanup handled by the outer try/finally; do not call view?.destroy() here.
                        }
                    }

                    webView.loadUrl(url)
                }
            } finally {
                // Single, reliable cleanup: covers normal completion, cancellation, and exceptions.
                webView.destroy()
            }
        }
    }
    
    /**
     * Gets the current base URL being used for scraping
     */
    fun getBaseUrl(): String {
        return urlPreferences.getBaseUrl()
    }
    
    /**
     * Updates the base URL used for scraping
     */
    fun updateBaseUrl(newUrl: String) {
        urlPreferences.setBaseUrl(newUrl)
    }
    
    /**
     * Resets the base URL to the default
     */
    fun resetBaseUrl() {
        urlPreferences.resetToDefault()
    }
    
    /**
     * Gets the current acestream engine IP address
     */
    fun getAcestreamIp(): String {
        return urlPreferences.getAcestreamIp()
    }
    
    /**
     * Updates the acestream engine IP address
     */
    fun updateAcestreamIp(ip: String) {
        urlPreferences.setAcestreamIp(ip)
    }
    
    /**
     * Resets the acestream IP to the default
     */
    fun resetAcestreamIp() {
        urlPreferences.resetAcestreamIpToDefault()
    }
}
