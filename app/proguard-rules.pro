# FIX #26: ProGuard / R8 rules for LiveTV release builds.
# The default proguard-android-optimize.txt already handles most Android framework classes.
# These rules protect the libraries and patterns that R8 would otherwise strip or rename.

# ── WebView JavaScript bridge ────────────────────────────────────────────────
# Any class that exposes @JavascriptInterface methods must be kept by name so the
# calling JS (Android.processHTML(...)) can still resolve it at runtime.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ── OkHttp ───────────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ── Jsoup ────────────────────────────────────────────────────────────────────
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# ── Kotlinx-Serialization ────────────────────────────────────────────────────
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

# ── Kotlin coroutines ────────────────────────────────────────────────────────
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ── Data / model classes ─────────────────────────────────────────────────────
# Keep all data classes in the model package so their field names survive R8 and
# can still be referenced reflectively by Compose / Serialization.
-keep class com.example.livetv.data.model.** { *; }

# ── FileProvider ─────────────────────────────────────────────────────────────
# Required by UpdateManager to install the downloaded APK.
-keep class androidx.core.content.FileProvider

# ── General ──────────────────────────────────────────────────────────────────
# Remove verbose debug-only logging (pairs with the BuildConfig.DEBUG guards in code).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
