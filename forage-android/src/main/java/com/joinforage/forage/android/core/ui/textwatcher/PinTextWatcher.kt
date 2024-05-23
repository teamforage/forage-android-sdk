package com.joinforage.forage.android.core.ui.textwatcher

import android.text.Editable
import android.text.TextWatcher

internal class PinTextWatcher : TextWatcher {
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
        val isValidAndComplete = editable.length == 4
        val isEmpty = editable.isEmpty()
        onInputChangeEvent?.invoke(isValidAndComplete, isEmpty)
    }
}
