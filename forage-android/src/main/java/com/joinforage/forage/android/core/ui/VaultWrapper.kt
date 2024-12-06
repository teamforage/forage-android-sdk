package com.joinforage.forage.android.core.ui

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.VaultType
import com.joinforage.forage.android.core.services.vault.RosettaPinSubmitter
import com.joinforage.forage.android.core.ui.element.SimpleElementListener
import com.joinforage.forage.android.core.ui.element.StatefulElementListener
import com.joinforage.forage.android.core.ui.element.state.FocusState
import com.joinforage.forage.android.core.ui.element.state.pin.PinEditTextState
import com.joinforage.forage.android.core.ui.element.state.pin.PinInputState

internal abstract class VaultWrapper @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    abstract var typeface: Typeface?
    abstract val vaultType: VaultType

    protected var focusState = FocusState.forEmptyInput()
    protected var inputState = PinInputState.forEmptyInput()

    // mutable references to event listeners. We use mutable
    // references because the implementations of our vaults
    // require that we are only able to ever pass a single
    // monolithic event within init call. This is mutability
    // allows us simulate setting and overwriting a listener
    // with every set call
    var onFocusEventListener: SimpleElementListener? = null
    var onBlurEventListener: SimpleElementListener? = null
    var onChangeEventListener: StatefulElementListener<PinEditTextState>? = null

    abstract fun clearText()

    abstract fun setTextColor(textColor: Int)
    abstract fun setTextSize(textSize: Float)
    abstract fun setHint(hint: String)
    abstract fun setHintTextColor(hintTextColor: Int)
    abstract fun getTextElement(): View
    abstract fun showKeyboard()

    abstract fun getVaultSubmitter(envConfig: EnvConfig): RosettaPinSubmitter

    fun getThemeAccentColor(context: Context): Int {
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorAccent, outValue, true)
        return outValue.data
    }

    val pinEditTextState: PinEditTextState
        get() = PinEditTextState.from(focusState, inputState)
}
