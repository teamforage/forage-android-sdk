package com.joinforage.forage.android.core.ui.element

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.joinforage.forage.android.R
import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.VaultType
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.vault.AbstractVaultSubmitter
import com.joinforage.forage.android.core.ui.VaultWrapper
import com.joinforage.forage.android.core.ui.element.state.pin.PinEditTextState

/**
 * A [ForageElement] that securely collects a card PIN. You can use an instance of a [ForagePinElement]
 * to call the methods that:
 * * [Check a card's balance][com.joinforage.forage.android.pos.services.ForageTerminalSDK.checkBalance]
 * * [Collect a card PIN to defer payment capture to the server][com.joinforage.forage.android.pos.services.ForageTerminalSDK.deferPaymentCapture]
 * * [Capture a payment immediately][com.joinforage.forage.android.pos.services.ForageTerminalSDK.capturePayment]
 * * [Refund a Payment immediately][com.joinforage.forage.android.pos.services.ForageTerminalSDK.refundPayment]
 * * [Collect a card PIN to defer payment refund to the server][com.joinforage.forage.android.pos.services.ForageTerminalSDK.deferPaymentRefund]
 */
abstract class ForagePinElement @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.foragePanEditTextStyle
) : ForageVaultElement<PinEditTextState>(context, attrs, defStyleAttr), EditTextElement {
    protected val _linearLayout: LinearLayout
    internal abstract val vault: VaultWrapper

    init {
        context.obtainStyledAttributes(attrs, R.styleable.ForagePINEditText, defStyleAttr, 0)
            .apply {
                try {
                    setWillNotDraw(false)
                    orientation = VERTICAL
                    gravity = Gravity.CENTER

                    val elementWidth: Int = getDimensionPixelSize(R.styleable.ForagePINEditText_elementWidth, ViewGroup.LayoutParams.MATCH_PARENT)
                    val elementHeight: Int = getDimensionPixelSize(R.styleable.ForagePINEditText_elementHeight, ViewGroup.LayoutParams.WRAP_CONTENT)

                    _linearLayout = LinearLayout(context)
                    _linearLayout.layoutParams = ViewGroup.LayoutParams(elementWidth, elementHeight)
                    _linearLayout.orientation = VERTICAL
                    _linearLayout.gravity = Gravity.CENTER
                } finally {
                    recycle()
                }
            }
    }

    override fun clearText() {
        vault.clearText()
    }

    override fun showKeyboard() {
        vault.showKeyboard()
    }

    override fun getVaultSubmitter(
        envConfig: EnvConfig,
        logger: Log
    ): AbstractVaultSubmitter {
        return vault.getVaultSubmitter(envConfig, logger)
    }

    // While the events that ForageElements expose mirrors the
    // blur, focus, change etc events of an Android view,
    // they represent different abstractions. Our users need to
    // interact with the ForageElement abstraction and not the
    // implementation details of which Android view we use.
    // Therefore we expose novel set listener methods instead of
    // overriding the convention setOn*Listener
    override fun setOnFocusEventListener(l: SimpleElementListener) {
        vault.onFocusEventListener = l
    }
    override fun setOnBlurEventListener(l: SimpleElementListener) {
        vault.onBlurEventListener = l
    }
    override fun setOnChangeEventListener(l: StatefulElementListener<PinEditTextState>) {
        vault.onChangeEventListener = l
    }

    override fun getElementState(): PinEditTextState = vault.pinEditTextState

    internal fun getVaultType(): VaultType {
        return vault.vaultType
    }
    internal fun getTextElement(): View {
        return vault.getTextElement()
    }

    override fun setTextColor(textColor: Int) {
        vault.setTextColor(textColor)
    }
    override fun setTextSize(textSize: Float) {
        vault.setTextSize(textSize)
    }

    override fun setBoxStrokeColor(boxStrokeColor: Int) {
        // no-ops for now
    }
    override fun setBoxStrokeWidth(boxStrokeWidth: Int) {
        // no-ops for now
    }
    override fun setBoxStrokeWidthFocused(boxStrokeWidth: Int) {
        // no-ops for now
    }

    @Deprecated(
        message = "setHint (for *PIN* elements) is deprecated.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("")
    )
    /**
     * @deprecated setHint (for **PIN** elements) is deprecated.
     */
    override fun setHint(hint: String) {
        // no-op, deprecated!
    }

    @Deprecated(
        message = "setHintTextColor (for *PIN* elements) is deprecated.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("")
    )
    /**
     * @deprecated setHintTextColor (for **PIN** elements) is deprecated.
     */
    override fun setHintTextColor(hintTextColor: Int) {
        // no-op, deprecated!
    }
}
