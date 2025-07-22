package com.joinforage.forage.android.core.ui.textwatcher

import android.text.Editable
import android.text.TextWatcher

/**
 * Base class for scenarios where we want to implement only some methods of [TextWatcher].
 */
internal open class TextWatcherAdapter : TextWatcher {
    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { nothing() }
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) { nothing() }
    override fun afterTextChanged(editable: Editable) { nothing() }

    // We need this method to suppress phantom code coverage failures.
    private fun nothing() {
        @Suppress("UNUSED_VARIABLE")
        val a = 0
    }
}
