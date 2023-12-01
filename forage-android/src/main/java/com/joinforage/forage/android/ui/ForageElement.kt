package com.joinforage.forage.android.ui

import android.graphics.Typeface
import com.joinforage.forage.android.core.element.SimpleElementListener
import com.joinforage.forage.android.core.element.StatefulElementListener
import com.joinforage.forage.android.core.element.state.ElementState

/**
 * Configuration details required by ForageElement to operate correctly
 *
 * @property merchantId Your merchant FNS number
 * @property sessionToken A temporary token used for authenticate
 */
data class ForageConfig(
    val merchantId: String,
    val sessionToken: String
)

/**
 * The interface that all Forage UI Components adhere to. For example,
 * both the ForagePANEditText and the ForagePINEditText satisfy
 * ForageElement
 */
interface ForageElement<T : ElementState> {
    var typeface: Typeface?

    fun setForageConfig(forageConfig: ForageConfig)

    fun clearText()

    fun setTextColor(textColor: Int)
    fun setTextSize(textSize: Float)
    fun setHint(hint: String)
    fun setHintTextColor(hintTextColor: Int)
    fun setBoxStrokeColor(boxStrokeColor: Int)
    fun setBoxStrokeWidth(boxStrokeWidth: Int)
    fun setBoxStrokeWidthFocused(boxStrokeWidth: Int)

    fun getElementState(): T
    fun setOnFocusEventListener(l: SimpleElementListener)
    fun setOnBlurEventListener(l: SimpleElementListener)
    fun setOnChangeEventListener(l: StatefulElementListener<T>)
}
