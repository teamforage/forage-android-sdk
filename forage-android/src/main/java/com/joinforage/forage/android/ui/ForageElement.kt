package com.joinforage.forage.android.ui

import android.graphics.Typeface
import com.joinforage.forage.android.core.element.SimpleElementListener
import com.joinforage.forage.android.core.element.StatefulElementListener
import com.joinforage.forage.android.core.element.state.ElementState

data class ForageContext(
    val merchantId: String,
    val sessionToken: String
    // TODO: thoughts on customerId being part of the
    //  ForageContext instead of a param for tokenizeEBTCard?
    //  Is there a future where we use customerID as part of
    //  fraud detection in checkBalance?
)

// an interface that represents that abstract state of
// a ForageElement.
// NOTE: isValid is true when validationError = null

// Minimalist event signatures where the only information
// conveyed is whether or not a specific event has occurred

interface ForageElement {
    var typeface: Typeface?

    fun setForageContext(forageContext: ForageContext)

    fun clearText()

    fun setTextColor(textColor: Int)
    fun setTextSize(textSize: Float)
    fun setHint(hint: String)
    fun setHintTextColor(hintTextColor: Int)
    fun setBoxStrokeColor(boxStrokeColor: Int)
    fun setBoxStrokeWidth(boxStrokeWidth: Int)
    fun setBoxStrokeWidthFocused(boxStrokeWidth: Int)

    fun getElementState(): ElementState
    fun setOnFocusEventListener(l: SimpleElementListener)
    fun setOnBlurEventListener(l: SimpleElementListener)
    fun setOnChangeEventListener(l: StatefulElementListener)
}
