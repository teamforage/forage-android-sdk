package com.joinforage.forage.android.core.ui

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.VaultType
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.vault.AbstractVaultSubmitter
import com.joinforage.forage.android.core.ui.element.ForagePinElement
import com.joinforage.forage.android.core.ui.element.SimpleElementListener
import com.joinforage.forage.android.core.ui.element.StatefulElementListener
import com.joinforage.forage.android.core.ui.element.state.PinElementState
import com.joinforage.forage.android.core.ui.element.state.PinElementStateManager

internal abstract class VaultWrapper @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    abstract var typeface: Typeface?

    // mutable references to event listeners. We use mutable
    // references because the implementations of our vaults
    // require that we are only able to ever pass a single
    // monolithic event within init call. This is mutability
    // allows us simulate setting and overwriting a listener
    // with every set call
    internal abstract val manager: PinElementStateManager
    abstract val vaultType: VaultType

    abstract fun clearText()

    abstract fun setTextColor(textColor: Int)
    abstract fun setTextSize(textSize: Float)
    abstract fun setHint(hint: String)
    abstract fun setHintTextColor(hintTextColor: Int)
    abstract fun getTextElement(): View
    abstract fun showKeyboard()

    abstract fun getVaultSubmitter(
        foragePinElement: ForagePinElement,
        envConfig: EnvConfig,
        logger: Log
    ): AbstractVaultSubmitter

    fun getThemeAccentColor(context: Context): Int {
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorAccent, outValue, true)
        return outValue.data
    }

    fun setOnFocusEventListener(l: SimpleElementListener) {
        manager.setOnFocusEventListener(l)
    }

    fun setOnBlurEventListener(l: SimpleElementListener) {
        manager.setOnBlurEventListener(l)
    }

    fun setOnChangeEventListener(l: StatefulElementListener<PinElementState>) {
        manager.setOnChangeEventListener(l)
    }
}
