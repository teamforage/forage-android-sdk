package com.joinforage.forage.android.core.ui.element

import android.graphics.Typeface
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.ui.element.state.ElementState

internal interface DynamicEnvElement {

    /**
     * Sets the necessary [ForageConfig] configuration properties for a ForageElement.
     * **[setForageConfig] must be called before any other methods can be executed on the Element.**
     * ```kotlin
     * // Example: Call setForageConfig on a ForagePANEditText Element
     * val foragePanEditText = root?.findViewById<ForagePANEditText>(
     *     R.id.tokenizeForagePanEditText
     * )
     * foragePanEditText.setForageConfig(
     *     ForageConfig(
     *         merchantId = "<merchant_id>",
     *         sessionToken = "<session_token>"
     *     )
     * )
     * ```
     *
     * @param forageConfig A [ForageConfig] instance that specifies a `merchantId` and `sessionToken`.
     */
    fun setForageConfig(forageConfig: ForageConfig)
}

internal interface EditTextElement {

    /**
     * Explicitly request that the current input method's soft
     * input be shown to the user, if needed. This only has an
     * effect if the ForageElement is focused, which can be
     * done using `.requestFocus()`
     */
    fun showKeyboard()

    /**
     * Sets an event listener to be fired when the ForageElement is in focus.
     *
     * @param l The [SimpleElementListener] to be fired on focus events.
     */
    fun setOnFocusEventListener(l: SimpleElementListener)

    /**
     * Sets an event listener to be fired when the ForageElement is blurred.
     *
     * @param l The [SimpleElementListener] to be fired on blur events.
     */
    fun setOnBlurEventListener(l: SimpleElementListener)

    /**
     * Sets the text to be displayed when the ForageElement input field is empty.
     *
     * @param hint The text to display.
     */
    fun setHint(hint: String)

    /**
     * Sets the hint text color.
     *
     * @param hintTextColor The color value in the form `0xAARRGGBB`.
     */
    fun setHintTextColor(hintTextColor: Int)

    /**
     * Sets the border color of the input field.
     *
     * @param boxStrokeColor The color value in the form `0xAARRGGBB`.
     */
    fun setBoxStrokeColor(boxStrokeColor: Int)

    /**
     * Sets the border thickness of the input field.
     *
     * @param boxStrokeWidth The scaled pixel size.
     */
    fun setBoxStrokeWidth(boxStrokeWidth: Int)

    /**
     * Sets the border thickness of the input field when the field is in focus state.
     *
     * @param boxStrokeWidth The scaled pixel size.
     */
    fun setBoxStrokeWidthFocused(boxStrokeWidth: Int)
}

/**
 * The interface that defines methods for configuring and interacting with a [ForageElement].
 * A ForageElement is a secure, client-side entity that accepts and submits customer input for a
 * transaction.
 * Both [ForagePanElement][com.joinforage.forage.android.core.ui.element.ForagePanElement] and
 * [ForagePinElement][com.joinforage.forage.android.core.ui.element.ForagePinElement] adhere to the [ForageElement] interface.
 *
 * @property typeface The [Typeface](https://developer.android.com/reference/android/graphics/Typeface)
 * that is used to render text within the ForageElement.
 * @see * [Online-only Android Quickstart](https://docs.joinforage.app/docs/forage-android-quickstart)
 * * [POS Terminal Android Quickstart](https://docs.joinforage.app/docs/forage-terminal-android)
 * * [Guide to styling Forage Android Elements](https://docs.joinforage.app/docs/forage-android-styling-guide)
 */
interface ForageElement<out T : ElementState> {
    var typeface: Typeface?

    /**
     * Clears the text input field of the ForageElement.
     */
    fun clearText()

    /**
     * Sets the text color for the ForageElement.
     *
     * @param textColor The color value in the form `0xAARRGGBB`.
     */
    fun setTextColor(textColor: Int)

    /**
     * Sets the text size for the ForageElement.
     *
     * @param textSize The scaled pixel size.
     */
    fun setTextSize(textSize: Float)

    /**
     * Gets the current [ElementState] state of the ForageElement.
     *
     * @return The [ElementState].
     */
    fun getElementState(): T

    /**
     * Sets an event listener to be fired when the text inside the ForageElement input field changes.
     *
     * @param l The [StatefulElementListener] to be fired on change events.
     */
    fun setOnChangeEventListener(l: StatefulElementListener<T>)
}
