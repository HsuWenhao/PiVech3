package com.example.pivech3.ui.settings

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.example.pivech3.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // 显示当前值为 summary
        findPreference<EditTextPreference>("raspberry_pi_ip")
            ?.summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()

        findPreference<EditTextPreference>("webrtc_url")
            ?.summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
    }
}