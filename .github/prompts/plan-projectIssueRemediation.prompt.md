## Plan: Project-Wide Issue Remediation

The project has 44 distinct issues across security, bugs, memory, performance, and maintainability. The plan below addresses them in priority order. Critical security issues should be fixed before any release; the rest can be tackled incrementally.

---

### Critical — Security (fix before any public release)

1. ✅ ~~**Trust-all SSL / MITM** — [Scraper.kt ~L842](app/src/main/java/com/example/livetv/data/network/Scraper.kt#L842): Replaced blanket `hostnameVerifier { _, _ -> true }` with `buildScrapingClient(host)` which scopes the hostname bypass to the specific domain extracted from the request URL. The `UpdateManager` OkHttpClient is unaffected and uses the secure default CA store.~~

2. ✅ ~~**WebView SSL bypass** — [Scraper.kt ~L930](app/src/main/java/com/example/livetv/data/network/Scraper.kt#L930): Replace `handler?.proceed()` in `onReceivedSslError` with `handler?.cancel()`. Log the error instead of silently accepting it.~~

3. ✅ ~~**Signature verification empty-list bypass** — [UpdateManager.kt ~L225](app/src/main/java/com/example/livetv/data/updater/UpdateManager.kt#L225): Change the guard to `if (current.isEmpty() || downloaded.isEmpty()) return false` so an empty signature list is treated as a mismatch, not a pass.~~

4. ✅ ~~**Dangerous WebView flags** — [Scraper.kt ~L924](app/src/main/java/com/example/livetv/data/network/Scraper.kt#L924): Remove `allowUniversalAccessFromFileURLs = true` and set `mixedContentMode = MIXED_CONTENT_NEVER_ALLOW`.~~ `allowUniversalAccessFromFileURLs` removed; `MIXED_CONTENT_ALWAYS_ALLOW` kept intentionally for stream scraping compatibility (revisit later).

5. ✅ ~~**CI secrets written as cleartext** — [build-and-release.yml ~L89](app/.github/workflows/build-and-release.yml#L89): Add `echo "::add-mask::${{ secrets.RELEASE_STORE_PASSWORD }}"` (and equivalent for each secret) before writing them to `gradle.properties`.~~

---

### High — Bugs

6. ✅ ~~**`loadMoreMatches()` race condition** — Extracted core logic into `loadMoreMatchesInternal()`; the public `loadMoreMatches()` now guards with `isLoadingMore` state and sets it in a `try/finally`. The recursive page-fetch call invokes `loadMoreMatchesInternal()` directly to bypass the guard correctly.~~

7. ✅ ~~**Non-thread-safe ViewModel fields** — All mutations occur within `viewModelScope.launch` (Dispatchers.Main), so mutations are sequentially safe. Noted with a comment; full architectural fix is tracked under item 23 (eliminate duplicate SectionData state).~~

8. ✅ ~~**`var` fields on `data class Match`** — Changed `streamLinks` and `areLinksLoading` to `val`. All existing mutation sites already used `.copy()`.~~

9. ✅ ~~**Download stream leak** — Wrapped `FileOutputStream` and `InputStream` inside nested `use {}` blocks in `downloadUpdate()`. Both streams are now guaranteed to close on exception.~~

10. ✅ ~~**Double search filter** — Removed the redundant `.filter { searchQuery... }` block in `refreshVisibleMatches()`; `getFilteredMatches()` already applies the search, sport, and league filters.~~

11. ✅ ~~**`release-on-tag.yml` builds unsigned APK** — Deleted the file entirely via `git rm`. The `build-and-release.yml` workflow already handles signed releases on every push to master; the tag-triggered workflow was redundant and dangerous.~~

12. ✅ ~~**Relative URLs always resolve to `livetv.sx`** — Added `baseOriginOf(url)` private helper to `Scraper`; both `scrapeMatchList` and `scrapeAllMatches` now derive the origin from the configured base URL and use it when resolving relative `/enx/event/...` links.~~

---

### High — Memory Leaks

13. ✅ ~~**`GlobalScope.launch` for WebView** — [Scraper.kt ~L904](app/src/main/java/com/example/livetv/data/network/Scraper.kt#L904): Replaced `GlobalScope.launch(Dispatchers.Main)` with `withContext(Dispatchers.Main)`, so the WebView coroutine inherits the calling scope (which flows from `viewModelScope`) instead of leaking into `GlobalScope`. Removed the unused `import kotlinx.coroutines.launch`.~~

14. ✅ ~~**Unreliable WebView cleanup** — [Scraper.kt ~L966](app/src/main/java/com/example/livetv/data/network/Scraper.kt#L966): Consolidated WebView destruction into a single `try/finally` block wrapping `suspendCancellableCoroutine`. Removed `webView.destroy()` from the `WebAppInterface` callback and `view?.destroy()` from `onReceivedError`; removed the `invokeOnCancellation` lambda that re-launched a `GlobalScope` coroutine. `finally` now handles all cases: normal completion, exceptions, and cancellation.~~

---

### High — Performance

15. ✅ ~~**New `OkHttpClient` per request** — [Scraper.kt ~L836](app/src/main/java/com/example/livetv/data/network/Scraper.kt#L836): Deleted `buildScrapingClient(host)`. Added a `scrapingClient: OkHttpClient` lazy class-level property built once in `init`. Its hostname verifier closes over `urlPreferences` and reads `getBaseUrl()` dynamically at verification time, so it stays correct when the user changes the URL at runtime. All `fetchHtmlWithOkHttp` calls now reuse this single client.~~

16. ✅ ~~**Same URL fetched twice on load** — [MatchViewModel.kt ~L111](app/src/main/java/com/example/livetv/ui/MatchViewModel.kt#L111): Added a short-lived HTML cache (`HtmlCacheEntry`, TTL 60 s) in `Scraper`. `fetchHtmlWithOkHttp` writes to it on every successful fetch and returns the cached copy on cache-hits. Because `startBackgroundScraping()` calls `scrapeAllMatches()` (which calls `fetchHtmlWithOkHttp`) seconds after `loadInitialMatchList()` has already fetched the same URL, the second call resolves from cache with zero network overhead.~~

---

### Medium — Code Quality & Performance

17. **Regex compiled inside loop** — [Scraper.kt ~L654](app/src/main/java/com/example/livetv/data/network/Scraper.kt#L654): Move `urlRegex` to a `companion object val`.

18. **Excessive diagnostic logging in release** — [Scraper.kt ~L88](app/src/main/java/com/example/livetv/data/network/Scraper.kt#L88): Wrap these O(n) `Log.d` blocks in `if (BuildConfig.DEBUG)` guards.

19. **Index-based `key` in `LazyVerticalGrid`** — [HomeScreen.kt ~L259](app/src/main/java/com/example/livetv/ui/HomeScreen.kt#L259): Use a stable identity from `Match` (e.g., its URL or a combination of team names and time) as the key.

20. **`scrapeMatchList` / `scrapeAllMatches` duplication** — [Scraper.kt ~L302](app/src/main/java/com/example/livetv/data/network/Scraper.kt#L302): Extract a shared `parseMatchRows(doc, limit)` function; `scrapeMatchList` passes a page limit, `scrapeAllMatches` passes `Int.MAX_VALUE`.

21. **Empty `ScrapeSection.kt` / misplaced enum** — [ScrapeSection.kt](app/src/main/java/com/example/livetv/data/model/ScrapeSection.kt): Move `ScrapingSection` enum from `Scraper.kt` into this file and delete the dead declaration.

22. **1391-line `HomeScreen.kt`** — [HomeScreen.kt](app/src/main/java/com/example/livetv/ui/HomeScreen.kt): Split into composable files: `MatchItem.kt`, `SectionSelector.kt`, `SearchBar.kt`, `SettingsDialog.kt`, `UpdateDialog.kt`.

23. **Duplicate `SectionData` + top-level vars in ViewModel** — [MatchViewModel.kt ~L20](app/src/main/java/com/example/livetv/ui/MatchViewModel.kt#L20): Eliminate the top-level vars; read/write exclusively through `sectionCache[currentSection]`, removing `saveCurrentSectionData()` / `restoreSectionData()`.

24. **`loadMoreMatches()` bypasses search filter** — [MatchViewModel.kt ~L312](app/src/main/java/com/example/livetv/ui/MatchViewModel.kt#L312): Ensure `loadMoreMatches()` uses the same code path as `refreshVisibleMatches()` so search is consistently applied.

25. **`com.example` package** — [app/build.gradle.kts](app/build.gradle.kts#L12): Rename to a real reverse-domain namespace (e.g., `com.fporta.livetv`). This requires a manifest rename and file structure refactor — worth doing before any Play Store submission.

26. **`isMinifyEnabled = false`** — [app/build.gradle.kts](app/build.gradle.kts#L67): Enable R8 minification/obfuscation in the release build type and add a `proguard-rules.pro` file to keep necessary classes (WebView interfaces, OkHttp, Jsoup).

27. **Hardcoded football keyword list duplicated** — [Scraper.kt ~L243](app/src/main/java/com/example/livetv/data/network/Scraper.kt#L243): Extract to one `companion object val FOOTBALL_KEYWORDS: Set<String>` and reference it from both call sites.

28. **`"f1"` false-positive substring match** — [Scraper.kt ~L471](app/src/main/java/com/example/livetv/data/network/Scraper.kt#L471): Use a word-boundary regex `\\bf1\\b` (case-insensitive) or explicit string list matching instead of `contains("f1")`.

29. **Activity `Context` in `UpdateViewModel`** — [UpdateViewModel.kt ~L16](app/src/main/java/com/example/livetv/ui/updater/UpdateViewModel.kt#L16): Change to `AndroidViewModel(application)` and use `application.applicationContext` everywhere inside.

30. **Deprecated `accompanist-swiperefresh`** — [app/build.gradle.kts](app/build.gradle.kts#L112): Replace with Material3's `PullToRefreshBox` (available from `compose-bom 2024.x` onward, which requires the dependency update in step 32).

---

### Medium — Config & Manifest

31. **`targetSdk 33`** — [app/build.gradle.kts](app/build.gradle.kts#L15): Bump to `targetSdk 34` (minimum for Play Store). Review any API 34 behavior changes (intent flags, exact alarms, photo picker).

32. **All dependencies 2–3 years outdated** — [app/build.gradle.kts](app/build.gradle.kts#L91): Update `compose-bom` to `2024.09.00` or later, `core-ktx` to `1.13.x`, `lifecycle` to `2.8.x`, `okhttp` to `4.12.x`, and graduate TV libraries from alpha to stable.

33. **`network_security_config.xml` too narrow** — [network_security_config.xml](app/src/main/res/xml/network_security_config.xml): Add `127.0.0.1` (Acestream proxy) and any other known HTTP stream origins to the cleartext-permitted domains list.

34. **Unused manifest permissions** — [AndroidManifest.xml](app/src/main/AndroidManifest.xml#L12): Remove `READ_MEDIA_VIDEO` and `READ_MEDIA_AUDIO` if the app never accesses local media files.

35. **`android:largeHeap="true"`** — [AndroidManifest.xml](app/src/main/AndroidManifest.xml#L33): Profile actual memory usage (especially during WebView scraping); fix any leaks found, then evaluate whether the flag is still needed.

---

### Low

36. **`.values()` → `.entries`** — Two call sites in [HomeScreen.kt ~L717](app/src/main/java/com/example/livetv/ui/HomeScreen.kt#L717) and [~L806](app/src/main/java/com/example/livetv/ui/HomeScreen.kt#L806).
37. **`data object` for sealed states** — [UpdateViewModel.kt ~L87](app/src/main/java/com/example/livetv/ui/updater/UpdateViewModel.kt#L87).
38. **Consistent log tags** — Define a `TAG` constant per file, named after the class.
39. **Remove debug APK from releases** — [build-and-release.yml ~L195](app/.github/workflows/build-and-release.yml#L195): Remove the `assembleDebug` step or exclude its output from the release asset upload.
40. **Package-level constants** — [MatchViewModel.kt ~L13](app/src/main/java/com/example/livetv/ui/MatchViewModel.kt#L13): Move to `companion object` inside the class.

---

### Missing Test Coverage

41. **Unit tests**: Add tests for `Scraper.cleanTeamNames()`, `extractSportAndLeague()`, `isValidStreamUrl()`, `UpdateManager.isNewerVersion()`, and `signaturesMatch()`.
42. **ViewModel tests**: Test section switching, search filter, load-more pagination, and cache restore logic.

---

**Verification**

- `./gradlew assembleRelease` — should succeed with minification enabled and proper keystore.
- Manual D-pad navigation smoke test on an emulator with Android TV profile.
- HTTP proxy (e.g. mitmproxy) to confirm SSL is no longer bypassed.
- Logcat filter on `"livetv"` tag to confirm no PII/secrets in release logs.

**Decisions**

- Relative URL fix (issue 12) is purely internal — no user-visible behaviour change.
- Package rename (issue 25) is a breaking change for existing installs; plan a migration notice if users are already using the app.
- `release-on-tag.yml` appears redundant with `build-and-release.yml`; recommend deleting it unless it serves a distinct purpose.
