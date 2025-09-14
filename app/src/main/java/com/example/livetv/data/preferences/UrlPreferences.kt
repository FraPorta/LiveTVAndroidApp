package com.example.livetv.data.preferences

import android.content.Context
import android.content.SharedPreferences

class UrlPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("livetv_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_BASE_URL = "base_url"
        const val DEFAULT_BASE_URL = "https://livetv.sx/enx/allupcomingsports/1/"
    }
    
    fun getBaseUrl(): String {
        return prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }
    
    fun setBaseUrl(url: String) {
        prefs.edit().putString(KEY_BASE_URL, url).apply()
    }
    
    fun resetToDefault() {
        prefs.edit().remove(KEY_BASE_URL).apply()
    }
}
