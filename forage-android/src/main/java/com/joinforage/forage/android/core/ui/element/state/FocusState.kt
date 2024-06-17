package com.joinforage.forage.android.core.ui.element.state

import com.joinforage.forage.android.core.ui.element.SimpleElementListener

internal data class FocusState(
    val isFocused: Boolean,
    val isBlurred: Boolean
) {
    fun changeFocus(hasFocus: Boolean): FocusState = FocusState(
        isFocused = hasFocus,
        isBlurred = !hasFocus
    )

    fun focus(): FocusState = changeFocus(true)
    fun blur(): FocusState = changeFocus(false)

    // We make this method void to avoid a situation where
    // inside a ForageElement we have
    // focusState = changeFocus(...).invoke(...)
    // in the above situation, the callbacks would be
    // invoked before ForageElement.focusState has been
    // updated which could lead to clients reading stale
    // state. Making the method void means the compiler
    // will complain about such statements
    fun fireEvent(
        onFocusEventListener: SimpleElementListener?,
        onBlurEventListener: SimpleElementListener?
    ) {
        if (isFocused) {
            onFocusEventListener?.invoke()
        } else {
            onBlurEventListener?.invoke()
        }
    }

    companion object {
        fun forEmptyInput() = FocusState(
            isFocused = false,
            isBlurred = true
        )
    }
}
