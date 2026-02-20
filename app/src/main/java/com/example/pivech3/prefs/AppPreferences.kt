package com.example.pivech3.prefs

import android.content.Context
import androidx.preference.PreferenceManager

object AppPreferences {
    const val KEY_RTSP_URL = "rtsp_url"
    const val DEFAULT_RTSP_URL = "rtsp://192.168.0.1/live/tcp/ch1"

    const val KEY_RASPBERRY_PI_IP = "raspberry_pi_ip"
    const val KEY_MOTION_CONTROL_PORT = "motion_control_port"
    const val DEFAULT_MOTION_CONTROL_PORT = 8000

    const val KEY_RTSP_CACHE_MS = "rtsp_cache_ms"
    const val DEFAULT_RTSP_CACHE_MS = 60

    const val KEY_RTSP_USE_TCP = "rtsp_use_tcp"
    const val DEFAULT_RTSP_USE_TCP = false

    fun getRtspUrl(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(KEY_RTSP_URL, DEFAULT_RTSP_URL) ?: DEFAULT_RTSP_URL
    }

    fun getRaspberryPiIp(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(KEY_RASPBERRY_PI_IP, "")?.trim().orEmpty()
    }

    fun getMotionControlPort(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val raw = prefs.getString(KEY_MOTION_CONTROL_PORT, DEFAULT_MOTION_CONTROL_PORT.toString())
            ?.trim()
            .orEmpty()
        val port = raw.toIntOrNull() ?: DEFAULT_MOTION_CONTROL_PORT
        return port.coerceIn(1, 65535)
    }

    fun getRtspCacheMs(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val raw = prefs.getString(KEY_RTSP_CACHE_MS, DEFAULT_RTSP_CACHE_MS.toString())
            ?.trim()
            .orEmpty()
        val value = raw.toIntOrNull() ?: DEFAULT_RTSP_CACHE_MS
        return value.coerceIn(20, 1000)
    }

    fun getRtspUseTcp(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(KEY_RTSP_USE_TCP, DEFAULT_RTSP_USE_TCP)
    }

    fun getMotionControlWsUrl(context: Context): String? {
        val ip = getRaspberryPiIp(context)
        if (ip.isBlank()) return null
        val port = getMotionControlPort(context)
        return "ws://$ip:$port"
    }

    fun migrateWebRtcToRtspIfNeeded(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (prefs.contains(KEY_RTSP_URL)) return
        if (prefs.contains("webrtc_url")) {
            val old = prefs.getString("webrtc_url", "")?.trim().orEmpty()
            val value = if (old.startsWith("rtsp://")) old else DEFAULT_RTSP_URL
            prefs.edit().putString(KEY_RTSP_URL, value).apply()
        }
    }
}