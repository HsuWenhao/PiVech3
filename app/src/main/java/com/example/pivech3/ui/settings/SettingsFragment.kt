package com.example.pivech3.ui.settings

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.example.pivech3.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        // 显示当前IP值为summary
        val ipPref = findPreference<EditTextPreference>("raspberry_pi_ip")
        ipPref?.summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
    }
}
