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

        findPreference<EditTextPreference>("motion_control_port")
            ?.apply {
                summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
                setOnPreferenceChangeListener { _, newValue ->
                    val raw = (newValue as? String)?.trim().orEmpty()
                    val port = raw.toIntOrNull()
                    // Reject invalid values; keep old.
                    port != null && port in 1..65535
                }
            }

        findPreference<EditTextPreference>("webrtc_url")
            ?.summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
    }

    override fun onDisplayPreferenceDialog(preference: androidx.preference.Preference) {
        // Ensure EditTextPreference persists even when the dialog is dismissed by tapping outside.
        if (preference is EditTextPreference) {
            val fm = parentFragmentManager
            if (fm.findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) return

            val f = AutoSaveEditTextPreferenceDialog.newInstance(preference.key)
            f.setTargetFragment(this, 0)
            f.show(fm, DIALOG_FRAGMENT_TAG)
            return
        }

        super.onDisplayPreferenceDialog(preference)
    }

    private companion object {
        const val DIALOG_FRAGMENT_TAG = "androidx.preference.PreferenceFragment.DIALOG"
    }
}