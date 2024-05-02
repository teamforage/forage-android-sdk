package com.joinforage.forage.android.core.ui.textwatcher

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

internal class PinTextWatcher(
    private val editText: EditText
) : TextWatcher {
    private var onInputChangeEvent: ((Boolean, Boolean) -> Unit)? = null

    fun onInputChangeEvent(callback: (Boolean, Boolean) -> Unit) {
        onInputChangeEvent = callback
    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        // a no-op for now
    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        // a no-op for now
    }

    override fun afterTextChanged(editable: Editable) {
        val isValidAndComplete = editText.length() == 4
        val isEmpty = editText.length() == 0
        onInputChangeEvent?.invoke(isValidAndComplete, isEmpty)
    }
}
