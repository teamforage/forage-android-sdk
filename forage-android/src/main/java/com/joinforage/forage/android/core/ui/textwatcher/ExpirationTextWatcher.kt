package com.joinforage.forage.android.core.ui.textwatcher

import android.text.Editable
import android.widget.EditText

internal class ExpirationTextWatcher(
    private val editText: EditText
) : TextWatcherAdapter() {
    private var onFormattedChangeEvent: ((String) -> Unit)? = null
    private var changeInProgress = false

    fun onFormattedChangeEvent(callback: (String) -> Unit) {
        onFormattedChangeEvent = callback
    }

    override fun afterTextChanged(editable: Editable) {
        if (changeInProgress) return

        try {
            changeInProgress = true
            var value = editable.toString()
            if (value.length >= 3 && !value.contains('/')) {
                val selectionStart = editText.selectionStart
                value =
                    value.substring(0, 2) + "/" + value.substring(2)
                editText.setText(value)
                editText.setSelection(selectionStart + 1)
            }
            onFormattedChangeEvent?.invoke(value)
        } finally {
            changeInProgress = false
        }
    }
}
