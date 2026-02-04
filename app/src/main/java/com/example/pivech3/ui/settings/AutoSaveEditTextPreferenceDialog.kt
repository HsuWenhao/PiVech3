package com.example.pivech3.ui.settings

import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.preference.EditTextPreference
import androidx.preference.EditTextPreferenceDialogFragmentCompat

/**
 * EditTextPreference dialog that also persists the value when the dialog is dismissed
 * (e.g. tapping outside), not only when pressing the positive button.
 */
class AutoSaveEditTextPreferenceDialog : EditTextPreferenceDialogFragmentCompat() {

    private var editTextRef: EditText? = null

    override fun onBindDialogView(view: android.view.View) {
        super.onBindDialogView(view)
        editTextRef = view.findViewById(android.R.id.edit)

        val editText = editTextRef ?: return

        // For URL/IP preferences, we want Enter to act like Done.
        editText.setSingleLine(true)
        editText.imeOptions = EditorInfo.IME_ACTION_DONE

        // Put cursor at the end (default UX for editing existing values)
        val len = editText.text?.length ?: 0
        if (len > 0) {
            editText.setSelection(len)
        }

        editText.setOnEditorActionListener { _, actionId, event ->
            val isEnterKey = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
            val isImeAction = actionId == EditorInfo.IME_ACTION_DONE ||
                actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_NEXT ||
                actionId == EditorInfo.IME_ACTION_SEND

            if (isImeAction || isEnterKey) {
                // Persist now, then close the dialog.
                persistFromEditText()
                dismissAllowingStateLoss()
                true
            } else {
                false
            }
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        // Default behavior: only persist when positiveResult == true.
        // We override and persist on ANY close (including tapping outside).
        persistFromEditText()
    }

    private fun persistFromEditText() {
        val pref = preference as? EditTextPreference ?: return
        val editText = editTextRef ?: return

        val newValue = editText.text?.toString()?.trim().orEmpty()
        if (newValue == pref.text.orEmpty()) return
        if (!pref.callChangeListener(newValue)) return

        pref.text = newValue
    }

    companion object {
        fun newInstance(key: String): AutoSaveEditTextPreferenceDialog {
            val f = AutoSaveEditTextPreferenceDialog()
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            f.arguments = b
            return f
        }
    }
}
