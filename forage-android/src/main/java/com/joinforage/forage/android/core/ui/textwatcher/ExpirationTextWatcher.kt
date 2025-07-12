package com.joinforage.forage.android.core.ui.textwatcher

import android.text.Editable
import android.widget.EditText

internal class ExpirationTextWatcher(
    private val editText: EditText
) : TextWatcherAdapter() {
    override fun afterTextChanged(editable: Editable) {
        val currentValue = editable.toString()
        if (currentValue.length >= 3 && !currentValue.contains('/')) {
            val selectionStart = editText.selectionStart
            val valueWithSlash =
                currentValue.substring(0, 2) + "/" + currentValue.substring(2)
            editText.setText(valueWithSlash)
            editText.setSelection(selectionStart + 1)
        }
    }
}
