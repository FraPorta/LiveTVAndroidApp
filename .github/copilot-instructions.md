# GitHub Copilot Instructions

## Project Overview
Android TV app (Kotlin + Jetpack Compose) for scraping and streaming live sports. Targets Android TV (`LEANBACK_LAUNCHER`) but also works on phones/tablets. Package: `com.example.livetv`, `minSdk 21`, `targetSdk 33`.

## Architecture
Unidirectional data flow, no DI framework:
```
Scraper (Jsoup/OkHttp/WebView) → MatchRepository → MatchViewModel (AndroidViewModel) → HomeScreen (Compose)
UpdateManager (OkHttp → GitHub API) → UpdateViewModel → UpdateDialog (Compose)
```
- **`Scraper.kt`** — all HTML scraping logic; uses OkHttp for page fetches and `WebView` with a JS bridge for JavaScript-rendered stream detail pages. Two-phase scraping: fast list load (`scrapeMatchList`) then lazy per-match stream fetch (`fetchStreamLinks`).
- **`MatchRepository.kt`** — thin wrapper over `Scraper`; no caching here.
- **`MatchViewModel.kt`** — holds `mutableStateOf` observable state (not `StateFlow`); manages per-section cache (`sectionCache: Map<ScrapingSection, SectionData>`), pagination (`INITIAL_LOAD_SIZE = 16`, `LOAD_MORE_SIZE = 10`), and background search scraping.
- **`UrlPreferences.kt`** — `SharedPreferences`-backed store for the configurable scrape base URL and Acestream engine IP.
- **`HomeScreen.kt`** — single large Compose screen (~1400 lines); adapts layout between TV grid and phone list based on `LocalConfiguration`.
- **`UpdateManager.kt`** — polls `https://api.github.com/repos/FraPorta/LiveTVAndroidApp/releases/latest`, downloads APK, and installs via `FileProvider` + `REQUEST_INSTALL_PACKAGES`.

## Key Conventions
- **State** is `mutableStateOf` (Compose `State<T>`), not `StateFlow`/`LiveData`. Observe with `val x by viewModel.x`.
- **No Hilt/Dagger** — `MatchViewModel` is `AndroidViewModel(application)` and constructs its own repo/scraper. `UpdateViewModel` requires a `Context` factory: `viewModelFactory { initializer { UpdateViewModel(context) } }`.
- **Coroutines** — scraping runs on `Dispatchers.IO` via `withContext`. Scraper uses `suspendCancellableCoroutine` to bridge `WebView` callbacks.
- **Sections** — `ScrapingSection` enum drives both scrape filtering and UI tab selection; section state is cached in `MatchViewModel.sectionCache`.
- **Stream protocols** — matches can link to Acestream (`acestream://`), M3U8, RTMP, or plain web URLs. Acestream links are rewritten to `http://<acestream_ip>:6878/ace/getstream?...`.

## Build & CI
```bash
./gradlew assembleDebug        # debug build
./gradlew assembleRelease      # release build (requires keystore)
```
- **Versioning** is manual: bump `versionCode` and `versionName` in [app/build.gradle.kts](app/build.gradle.kts) before release.
- **Signing**: locally, place `keystore.properties` (see `keystore.properties.template`) + keystore file. In CI, secrets `KEYSTORE_B64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` drive the release signing step in [`.github/workflows/build-and-release.yml`](.github/workflows/build-and-release.yml).
- **Release**: push to `master` automatically triggers build + GitHub release creation. No manual `release.sh` run needed for standard releases.

## Default Scrape Source
`https://livetv.sx/enx/allupcomingsports/1/` — configurable at runtime via Settings dialog in `HomeScreen`. Changing this persists to `SharedPreferences` via `UrlPreferences`.

## TV-Specific Considerations
- All interactive elements need D-pad focus handling (`focusRequester`, `onFocusChanged`, `onKeyEvent`).
- `android:banner` in the manifest is required for the Android TV launcher row.
- Layout switches between `LazyVerticalGrid` (TV) and `LazyColumn` (phone) based on screen width via `LocalConfiguration.current.screenWidthDp`.
