package com.joinforage.forage.android.ui

import android.graphics.Typeface

// an interface that represents that abstract state of
// a ForageElement.
data class ElementState(val isFocused: Boolean)

// Minimalist event signatures where the only information
// conveyed is whether or not a specific event has occurred
typealias ForageElementFocusListener = () -> Unit
typealias ForageElementBlurListener = () -> Unit

interface ForageUI {
    var isValid: Boolean
    var isEmpty: Boolean
    var typeface: Typeface?
    fun setTextColor(textColor: Int)
    fun setTextSize(textSize: Float)
    fun setHint(hint: String)
    fun setHintTextColor(hintTextColor: Int)
}
