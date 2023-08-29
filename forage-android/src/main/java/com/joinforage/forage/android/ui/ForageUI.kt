package com.joinforage.forage.android.ui

import android.graphics.Typeface
import com.joinforage.forage.android.core.element.SimpleElementListener
import com.joinforage.forage.android.core.element.StatefulElementListener
import com.joinforage.forage.android.core.element.state.ElementState

// an interface that represents that abstract state of
// a ForageElement.
// NOTE: isValid is true when validationError = null

// Minimalist event signatures where the only information
// conveyed is whether or not a specific event has occurred

interface ForageUI {
    var typeface: Typeface?

    fun clearText()

    fun setTextColor(textColor: Int)
    fun setTextSize(textSize: Float)
    fun setHint(hint: String)
    fun setHintTextColor(hintTextColor: Int)

    fun getElementState(): ElementState
    fun setOnFocusEventListener(l: SimpleElementListener)
    fun setOnBlurEventListener(l: SimpleElementListener)
    fun setOnChangeEventListener(l: StatefulElementListener)
}
