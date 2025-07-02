package com.joinforage.forage.android.core.ui.textwatcher

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

internal class ExpirationTextWatcher(
    private val editText: EditText
) : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

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
