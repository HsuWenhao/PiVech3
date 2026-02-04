package com.example.pivech3.prefs

import android.content.Context
import androidx.preference.PreferenceManager

object AppPreferences {
    const val KEY_WEBRTC_URL = "webrtc_url"
    const val DEFAULT_WEBRTC_URL = "http://192.168.1.3:8889/wmv"

    const val KEY_RASPBERRY_PI_IP = "raspberry_pi_ip"
    const val KEY_MOTION_CONTROL_PORT = "motion_control_port"
    const val DEFAULT_MOTION_CONTROL_PORT = 8000

    fun getWebRtcUrl(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(KEY_WEBRTC_URL, DEFAULT_WEBRTC_URL) ?: DEFAULT_WEBRTC_URL
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

    fun getMotionControlWsUrl(context: Context): String? {
        val ip = getRaspberryPiIp(context)
        if (ip.isBlank()) return null
        val port = getMotionControlPort(context)
        return "ws://$ip:$port"
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