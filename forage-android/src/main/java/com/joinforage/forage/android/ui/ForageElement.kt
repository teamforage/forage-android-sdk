package com.joinforage.forage.android.ui

import android.graphics.Typeface
import com.joinforage.forage.android.core.element.SimpleElementListener
import com.joinforage.forage.android.core.element.StatefulElementListener
import com.joinforage.forage.android.core.element.state.ElementState

/**
 * The configuration details that Forage needs to create a functional [ForageElement].
 *
 * Pass a [ForageConfig] instance in a call to
 * [setForageConfig][com.joinforage.forage.android.ui.AbstractForageElement.setForageConfig] to
 * configure an Element.
 *
 * @property merchantId Either a unique seven digit numeric string that
 * [FNS](https://docs.joinforage.app/docs/ebt-online-101#food-and-nutrition-service-fns) issues
 * to authorized EBT merchants, or a unique merchant ID that Forage provides during onboarding
 * and can be retrieved from the dashboard.
 * Accepted formats include: `mid/<merchant-id>`, `<fns-number>`.
 *
 * @property sessionToken A short-lived token that authenticates front-end requests to Forage.
 * To create one, send a server-side `POST` request from your backend to the
 * [`/session_token/`](https://docs.joinforage.app/reference/create-session-token) endpoint.
 *
 * @constructor Creates an instance of the [ForageConfig] data class.
 */
data class ForageConfig(
    val merchantId: String,
    val sessionToken: String
)

/**
 * The interface that defines methods for configuring and interacting with a [ForageElement].
 * A ForageElement is a secure, client-side entity that accepts and submits customer input for a
 * transaction.
 * Both [ForagePANEditText] and [ForagePINEditText] adhere to the [ForageElement] interface.
 *
 * @property typeface The [Typeface](https://developer.android.com/reference/android/graphics/Typeface)
 * that is used to render text within the ForageElement.
 */
interface ForageElement<T : ElementState> {
    var typeface: Typeface?

    /**
     * Sets the necessary [ForageConfig] configuration properties for a ForageElement.
     *
     * [setForageConfig] must be called before any other methods can be executed on the Element.
     *
     * @param forageConfig A [ForageConfig] instance that specifies a `merchantId` and `sessionToken`.
     */
    fun setForageConfig(forageConfig: ForageConfig)

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

    /**
     * Gets the current [ElementState] state of the ForageElement.
     *
     * @return The [ElementState].
     */
    fun getElementState(): T

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
     * Sets an event listener to be fired on ForageElement change events.
     *
     * @param l The [StatefulElementListener] to be fired on change events.
     */
    fun setOnChangeEventListener(l: StatefulElementListener<T>)
}
