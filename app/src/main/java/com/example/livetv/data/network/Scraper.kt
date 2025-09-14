package com.example.livetv.data.network

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.SslErrorHandler
import android.net.http.SslError
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.livetv.data.model.Match
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import okhttp3.OkHttpClient
import okhttp3.Request

enum class ScrapingSection(val displayName: String, val selector: String) {
    FOOTBALL("Football", "td"),
    ALL("All Matches", ""),
    TOP_EVENTS_LIVE("Top Events LIVE", "#upcoming")
}

class Scraper(private val context: Context) {

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
        val url = "https://livetv.sx/enx/allupcomingsports/1/"
        Log.d("Scraper", "Fetching initial match list from: $url")
        
        try {
            // Try the mobile version of the site which might be less complex
            val mobileUrl = "https://livetv.sx/enx/allupcomingsports/1/"
            val html = fetchHtmlWithOkHttp(mobileUrl)
            
            val doc = Jsoup.parse(html)
            val matches = mutableListOf<Match>()
            
            Log.d("Scraper", "Scraping section: ${section.displayName}")

            // Filter document by section if specified
            val sectionDoc = when (section) {
                ScrapingSection.ALL -> doc
                ScrapingSection.TOP_EVENTS_LIVE -> {
                    val upcomingSection = doc.select("#upcoming").first()
                    if (upcomingSection != null) {
                        Log.d("Scraper", "Found 'upcoming' section with ${upcomingSection.select("a").size} links")
                        upcomingSection
                    } else {
                        Log.d("Scraper", "No 'upcoming' section found, falling back to full document")
                        doc
                    }
                }
                ScrapingSection.FOOTBALL -> {
                    // For football section, we'll use the full document but filter matches later
                    // This ensures pagination works correctly while still filtering for football
                    Log.d("Scraper", "Using full document for football section - will filter matches by football keywords")
                    doc
                }
            }

            // Try multiple selectors to find match links
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
            
            // Let's also check what links we do have
            val allLinks = sectionDoc.select("a[href]")
            Log.d("Scraper", "Total links found in section: ${allLinks.size}")
            
            if (allLinks.isNotEmpty() && detailLinks.isEmpty()) {
                Log.d("Scraper", "Sample of first 10 links found:")
                allLinks.take(10).forEach { link ->
                    Log.d("Scraper", "Link: ${link.attr("href")} - Text: ${link.text().take(50)}")
                }
            }
            
            if (detailLinks.isEmpty()) {
                // Check for tables or other structures that might contain matches
                val tables = sectionDoc.select("table")
                Log.d("Scraper", "Found ${tables.size} tables in the section")
                
                val divs = sectionDoc.select("div")
                Log.d("Scraper", "Found ${divs.size} div elements in the section")
                
                // Look for any elements that might contain match information
                val matchKeywords = sectionDoc.select(":contains(vs), :contains(VS), :contains(-), :contains(football), :contains(match)")
                Log.d("Scraper", "Found ${matchKeywords.size} elements containing match-related keywords")
                
                Log.d("Scraper", "Section text (first 500 chars): ${sectionDoc.text().take(500)}")
            }

            // Process the links we found
            for (link in detailLinks) {
                val href = link.attr("href")
                if (href.isBlank()) continue
                
                val detailPageUrl = if (href.startsWith("http")) {
                    href
                } else if (href.startsWith("/")) {
                    "https://livetv.sx$href"
                } else {
                    "https://livetv.sx/$href"
                }
                
                Log.d("Scraper", "Processing link: $detailPageUrl")
                
                // Try to find the match information in various ways
                var row = link.closest("tr")
                if (row == null) row = link.parent()
                if (row == null) row = link
                
                var time = ""
                var teams = ""
                var competition = ""
                
                // Enhanced selectors for match information based on livetv.sx structure
                if (row != null) {
                    // Time extraction - try multiple approaches
                    time = row.select("td.time, .time, [class*='time'], td:first-child").text().trim()
                    
                    // Team extraction - try multiple approaches as the main content varies
                    teams = row.select("td.evdesc, .evdesc, .event-title, .event-desc, [class*='event'], [class*='team'], td:nth-child(3)").text().trim()
                    
                    // Competition/League extraction - usually shorter text with league name
                    competition = row.select("td.league > a, .league, .competition, [class*='league'], td:nth-child(2)").text().trim()
                    
                    // Alternative: look for the link text which often contains team names
                    if (teams.isBlank() || teams.length < 5) {
                        teams = row.select("a").first()?.text()?.trim() ?: ""
                    }
                    
                    // If teams still looks like league info and competition looks like team names, swap them
                    if (teams.isNotBlank() && competition.isNotBlank()) {
                        // Check if what we think are "teams" is actually league/date/time info
                        val teamsLooksLikeLeague = teams.length < 10 || 
                                                 teams.contains(Regex("""\([^)]+\)""")) ||  // Contains parentheses with league info
                                                 teams.contains(Regex("""\d{1,2}\s+\w+\s+at""")) ||  // Contains date pattern like "15 September at"
                                                 teams.lowercase().contains(Regex("""\b(ncaa|nba|nfl|mlb|nhl|premier|liga|serie|bundesliga|league|cup|championship|division|conference|botola|pro|first|elite)\b"""))
                        
                        // Check if what we think is "competition" actually contains team names (longer text, contains team separators, actual team names)
                        val competitionLooksLikeTeams = competition.length > 15 ||
                                                       competition.contains(Regex("""[–—-]|\bvs?\.?\b|\d+:\d+""")) ||  // Team separators or scores
                                                       competition.split(Regex("""[–—-]|\bvs?\.?\b""")).size == 2  // Exactly two parts when split by separators
                        
                        if (teamsLooksLikeLeague && competitionLooksLikeTeams) {
                            // Swap them
                            val temp = teams
                            teams = competition
                            competition = temp
                            Log.d("Scraper", "Swapped teams and competition fields - Teams: '$teams', Competition: '$competition'")
                        }
                    }
                    
                    // Extract time from the teams/competition text if time is still empty
                    if (time.isBlank()) {
                        val combinedText = "$teams $competition"
                        val timePattern = Regex("""\b(\d{1,2}:\d{2})\b""")
                        val timeMatch = timePattern.find(combinedText)
                        if (timeMatch != null) {
                            time = timeMatch.value
                        } else {
                            // Try to extract time from date patterns like "14 September at 15:30"
                            val dateTimePattern = Regex("""\d{1,2}\s+\w+\s+at\s+(\d{1,2}:\d{2})""")
                            val dateTimeMatch = dateTimePattern.find(combinedText)
                            if (dateTimeMatch != null) {
                                time = dateTimeMatch.groupValues[1]
                            }
                        }
                    }
                }
                
                // If we couldn't find team info in the row, try the link text itself
                if (teams.isBlank() || teams.length < 5) {
                    teams = link.text().trim()
                }
                
                // If still no teams, try different approaches
                if (teams.isBlank() || teams.length < 5) {
                    // Try to get text from siblings or parent elements
                    var parent = link.parent()
                    var attempts = 0
                    while (parent != null && attempts < 3 && (teams.isBlank() || teams.length < 5)) {
                        val parentText = parent.ownText().trim()
                        if (parentText.isNotBlank() && parentText.length > 5) {
                            teams = parentText
                            break
                        }
                        parent = parent.parent()
                        attempts++
                    }
                }
                
                // Clean up teams text - remove time and league info if they got mixed in
                teams = cleanTeamNames(teams, time, competition)
                
                // Extract sport and league information
                val (sport, league) = extractSportAndLeague(competition, teams, row, detailPageUrl)
                
                Log.d("Scraper", "Raw extraction - Time: '$time', Teams: '$teams', Competition: '$competition'")
                Log.d("Scraper", "Final match - Teams: '$teams', Time: '$time', Competition: '$competition', Sport: '$sport', League: '$league'")
                
                if (teams.isNotBlank() && teams.length > 3) { // Basic validation
                    matches.add(Match(time, teams, competition, sport, league, detailPageUrl))
                } else {
                    Log.w("Scraper", "Skipped match - Teams too short or blank: '$teams' (length: ${teams.length})")
                }
            }
            
            // Remove duplicates based on URL to avoid LazyColumn key conflicts
            val uniqueMatches = matches.distinctBy { it.detailPageUrl }
            
            // Apply section-specific filtering
            val filteredMatches = when (section) {
                ScrapingSection.FOOTBALL -> {
                    // Filter matches to only include football-related ones
                    val footballMatches = uniqueMatches.filter { match ->
                        val combinedText = "${match.teams} ${match.competition} ${match.league} ${match.sport}".lowercase()
                        combinedText.contains("football") || 
                        combinedText.contains("soccer") ||
                        combinedText.contains("premier") || 
                        combinedText.contains("liga") ||
                        combinedText.contains("bundesliga") || 
                        combinedText.contains("serie a") ||
                        combinedText.contains("ligue") ||
                        combinedText.contains("champions league") ||
                        combinedText.contains("europa league") ||
                        combinedText.contains("uefa") ||
                        combinedText.contains("fifa") ||
                        combinedText.contains("world cup") ||
                        match.sport.lowercase() == "football"
                    }
                    Log.d("Scraper", "Football filtering: ${uniqueMatches.size} -> ${footballMatches.size} matches")
                    footballMatches
                }
                else -> uniqueMatches // No additional filtering for other sections
            }
            
            // Apply pagination if requested
            val paginatedMatches = if (limit > 0) {
                val startIndex = offset.coerceAtLeast(0)
                val endIndex = (startIndex + limit).coerceAtMost(filteredMatches.size)
                if (startIndex < filteredMatches.size) {
                    filteredMatches.subList(startIndex, endIndex)
                } else {
                    emptyList()
                }
            } else {
                filteredMatches
            }
            
            Log.d("Scraper", "Successfully parsed ${matches.size} matches from main page.")
            if (matches.size != uniqueMatches.size) {
                Log.d("Scraper", "Removed ${matches.size - uniqueMatches.size} duplicate matches.")
            }
            
            if (limit > 0) {
                Log.d("Scraper", "Applied pagination - Offset: $offset, Limit: $limit, Returned: ${paginatedMatches.size} out of ${filteredMatches.size} total matches")
            }
            
            paginatedMatches
        } catch (e: Exception) {
            Log.e("Scraper", "Error scraping match list", e)
            emptyList()
        }
    }

    /**
     * Extracts sport and league information from available data.
     * Uses competition text, team names, and URL patterns to determine sport and league.
     */
    private fun extractSportAndLeague(competition: String, teams: String, row: org.jsoup.nodes.Element?, detailPageUrl: String): Pair<String, String> {
        var sport = "Football" // Default to football since it's the most common
        var league = "" // Will be determined based on specific league detection
        
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
            combinedText.contains("formula") || combinedText.contains("f1") || 
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
        
        return Pair(sport, league)
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
            """live|today|tomorrow|now""",
            """GMT|UTC|CET|EST|PST""",
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
            Log.d("Scraper", "Found ${webplayerLinks.size} webplayer links: ${webplayerLinks.joinToString()}")

            // 7. Links in JavaScript or embedded content
            val scriptTags = doc.select("script")
            scriptTags.forEach { script ->
                val scriptContent = script.html()
                
                // Extract URLs from JavaScript
                val urlRegex = """https?://[^\s"'<>]+(?:\.m3u8|stream|live|watch|player)""".toRegex(RegexOption.IGNORE_CASE)
                val jsUrls = urlRegex.findAll(scriptContent).map { it.value }.toList()
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
                
                // Acestream regex
                val acestreamRegex = "acestream://[a-zA-Z0-9]+".toRegex()
                val foundAcestream = acestreamRegex.findAll(bodyText).map { it.value }
                links.addAll(foundAcestream)
                
                // M3U8 regex
                val m3u8Regex = """https?://[^\s"'<>]+\.m3u8""".toRegex(RegexOption.IGNORE_CASE)
                val foundM3u8 = m3u8Regex.findAll(bodyText).map { it.value }
                links.addAll(foundM3u8)
                
                // RTMP regex
                val rtmpRegex = """rtmps?://[^\s"'<>]+""".toRegex(RegexOption.IGNORE_CASE)
                val foundRtmp = rtmpRegex.findAll(bodyText).map { it.value }
                links.addAll(foundRtmp)
            }

            // 10. Also search in the raw HTML for hidden links
            val htmlRegexPatterns = listOf(
                "acestream://[a-zA-Z0-9]+".toRegex(),
                """https?://[^\s"'<>]+\.m3u8""".toRegex(RegexOption.IGNORE_CASE),
                """rtmps?://[^\s"'<>]+""".toRegex(RegexOption.IGNORE_CASE),
                """(?:https?:)?//[^\s"'<>]+webplayer[^\s"'<>]*""".toRegex(RegexOption.IGNORE_CASE)
            )
            
            htmlRegexPatterns.forEach { regex ->
                val htmlFound = regex.findAll(html)
                    .map { it.value }
                    .map { url ->
                        // Convert protocol-relative URLs to HTTPS
                        if (url.startsWith("//")) {
                            "https:$url"
                        } else {
                            url
                        }
                    }
                links.addAll(htmlFound)
            }

            val allLinks = links.toList().distinct()
            
            // Debug: Log all found URLs before filtering
            Log.d("Scraper", "Found ${allLinks.size} URLs before filtering:")
            allLinks.forEach { url ->
                Log.d("Scraper", "URL: $url")
            }
            
            // Filter out invalid or incomplete URLs
            val finalLinks = allLinks.filter { link ->
                val isValid = isValidStreamUrl(link)
                if (!isValid) {
                    Log.d("Scraper", "FILTERED OUT invalid URL: $link")
                }
                isValid
            }
            
            Log.d("Scraper", "Final valid URLs: ${finalLinks.size}")
            
            Log.d("Scraper", "Found ${allLinks.size} total links, ${finalLinks.size} valid stream links for $detailPageUrl")
            if (allLinks.size != finalLinks.size) {
                val filteredOut = allLinks - finalLinks.toSet()
                Log.d("Scraper", "Filtered out ${filteredOut.size} invalid links: ${filteredOut.joinToString()}")
            }
            Log.d("Scraper", "Stream types found: ${finalLinks.joinToString(", ") { 
                when {
                    it.startsWith("acestream://") -> "Acestream"
                    it.contains(".m3u8") -> "M3U8/HLS"
                    it.startsWith("rtmp") -> "RTMP"
                    it.contains("youtube.com") || it.contains("youtu.be") -> "YouTube"
                    it.contains("twitch.tv") -> "Twitch"
                    else -> "HTTP/Web"
                }
            }}")
            
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
                validateHttpUrl(url)
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

    private suspend fun fetchHtmlWithOkHttp(url: String): String = withContext(Dispatchers.IO) {
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .sslSocketFactory(createInsecureSslSocketFactory(), createTrustAllManager())
            .hostnameVerifier { _, _ -> true }
            .build()

        val request = okhttp3.Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Accept-Encoding", "identity")
            .header("Connection", "keep-alive")
            .header("Cache-Control", "no-cache")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e("Scraper", "HTTP error: ${response.code} - ${response.message}")
                throw java.io.IOException("HTTP error: ${response.code}")
            }
            
            val body = response.body
            if (body == null) {
                throw java.io.IOException("Empty response body")
            }
            
            // OkHttp should automatically decompress gzipped content, but let's be explicit
            val content = body.string()
            Log.d("Scraper", "Response length: ${content.length} chars, first 200 chars: ${content.take(200)}")
            content
        }
    }

    private fun createInsecureSslSocketFactory(): javax.net.ssl.SSLSocketFactory {
        val trustAllManager = createTrustAllManager()
        val sslContext = javax.net.ssl.SSLContext.getInstance("SSL")
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
        suspendCancellableCoroutine<String?> { continuation ->
            // Must run WebView on the main thread
            // No need for withContext(Dispatchers.Main) if the calling coroutine is already on Main
            // But to be safe, let's ensure it. However, since this is a suspend function,
            // the caller's context matters. Let's assume for now the caller handles the Main thread.
            // The ViewModel will call this from Dispatchers.IO, so we must switch to Main.
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
                Log.d("ScraperWebView", "Creating WebView for $url")
                val webView = WebView(context)

                val webAppInterface = WebAppInterface { html ->
                    if (continuation.isActive) {
                        continuation.resume(html)
                    }
                    // It's crucial to destroy the WebView on the main thread
                    webView.destroy()
                }

                val desktopUserAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"
                webView.settings.userAgentString = desktopUserAgent
                webView.settings.javaScriptEnabled = true
                webView.settings.domStorageEnabled = true
                webView.settings.allowFileAccess = true
                webView.settings.allowContentAccess = true
                webView.settings.allowUniversalAccessFromFileURLs = true
                webView.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                webView.addJavascriptInterface(webAppInterface, "Android")

                webView.webViewClient = object : WebViewClient() {
                    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                        handler?.proceed() // Ignore SSL errors
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

                     override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        super.onReceivedError(view, request, error)
                        if (continuation.isActive) {
                            continuation.resumeWithException(RuntimeException("WebView error: ${error?.description}"))
                        }
                        view?.destroy()
                    }
                }

                continuation.invokeOnCancellation {
                    // Ensure WebView is destroyed on the main thread if the coroutine is cancelled
                     kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
                        webView.destroy()
                    }
                }

                webView.loadUrl(url)
            }
        }
    }
}
