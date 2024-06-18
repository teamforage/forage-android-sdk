package com.joinforage.forage.android.core.ui.element.state

/**
 * An interface that represents the `EditText` state of the [ForageElement][com.joinforage.forage.android.core.ui.element.ForageElement]
 * as the customer interacts with it.
 * @property isFocused Whether the Element is in focus.
 * @property isBlurred Whether the Element is blurred.
 */
interface EditTextState : ElementState {
    val isFocused: Boolean
    val isBlurred: Boolean
}
