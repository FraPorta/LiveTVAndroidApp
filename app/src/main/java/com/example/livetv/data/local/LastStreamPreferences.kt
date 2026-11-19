package com.example.livetv.data.local

import android.content.Context

/** Remembers which stream link was opened last for each match, so it can be listed first. */
class LastStreamPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("livetv_last_stream", Context.MODE_PRIVATE)

    fun get(matchUrl: String): String? = prefs.getString(matchUrl, null)

    fun set(matchUrl: String, streamLink: String) {
        // Matches are short-lived, so simply start over if the store ever grows large.
        if (prefs.all.size > 500) prefs.edit().clear().apply()
        prefs.edit().putString(matchUrl, streamLink).apply()
    }
}
