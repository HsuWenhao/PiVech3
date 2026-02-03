package com.example.pivech3.prefs

import android.content.Context
import androidx.preference.PreferenceManager

object AppPreferences {
    const val KEY_WEBRTC_URL = "webrtc_url"
    const val DEFAULT_WEBRTC_URL = "http://192.168.1.3:8889/wmv"

    fun getWebRtcUrl(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(KEY_WEBRTC_URL, DEFAULT_WEBRTC_URL) ?: DEFAULT_WEBRTC_URL
    }

    fun migrateRtspToWebRtcIfNeeded(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (prefs.contains(KEY_WEBRTC_URL)) return
        if (prefs.contains("rtsp_url")) {
            val old = prefs.getString("rtsp_url", "")?.trim().orEmpty()
            val value = if (old.startsWith("http")) old else DEFAULT_WEBRTC_URL
            prefs.edit().putString(KEY_WEBRTC_URL, value).apply()
        }
    }
}