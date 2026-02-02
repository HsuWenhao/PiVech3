package com.example.pivech3.prefs

import android.content.Context
import androidx.preference.PreferenceManager

object AppPreferences {
    const val KEY_RTSP_URL = "rtsp_url"

    const val DEFAULT_RTSP_URL = "rtsp://192.168.1.10:8554/wmv"

    fun getRtspUrl(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(KEY_RTSP_URL, DEFAULT_RTSP_URL) ?: DEFAULT_RTSP_URL
    }
}
