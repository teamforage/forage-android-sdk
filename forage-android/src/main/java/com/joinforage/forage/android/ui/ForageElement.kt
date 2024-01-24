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
 * The interface that all Forage UI Components adhere to. For example,
 * both the ForagePANEditText and the ForagePINEditText satisfy
 * ForageElement
 */
interface ForageElement<T : ElementState> {
    var typeface: Typeface?

    /**
     * Sets the necessary [ForageConfig] configuration properties for a [ForageElement].
     *
     * [setForageConfig] must be called before any other methods can be executed on the Element.
     *
     * @param forageConfig A [ForageConfig] instance that specifies a `merchantId` and `sessionToken`.
     */
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
