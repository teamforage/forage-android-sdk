package com.joinforage.forage.android.pos.ui.element

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import com.joinforage.forage.android.R
import com.joinforage.forage.android.core.ui.VaultWrapper
import com.joinforage.forage.android.core.ui.element.ForagePinElement
import com.joinforage.forage.android.core.ui.getLogoImageViewLayout
import com.joinforage.forage.android.pos.ui.ForageVaultWrapper

/**
 * A [ForageElement] that securely collects a card PIN. You need a [ForagePINEditText] to call
 * the ForageSDK online-only or ForageTerminalSDK POS methods that:
 * * [Check a card's balance][com.joinforage.forage.android.ForageSDK.checkBalance]
 * * [Collect a card PIN to defer payment capture to the server][com.joinforage.forage.android.ForageSDK.deferPaymentCapture]
 * * [Capture a payment immediately][com.joinforage.forage.android.ForageSDK.capturePayment]
 * * [Refund a Payment immediately][com.joinforage.forage.android.pos.ForageTerminalSDK.refundPayment] (**POS-only**)
 * * [Collect a card PIN to defer payment refund to the server][com.joinforage.forage.android.pos.ForageTerminalSDK.deferPaymentRefund]
 * (**POS-only**)
 */
class ForagePINEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.foragePanEditTextStyle
) : ForagePinElement(context, attrs, defStyleAttr) {
    override val vault: VaultWrapper

    init {
        context.obtainStyledAttributes(attrs, R.styleable.ForagePINEditText, defStyleAttr, 0)
            .apply {
                try {

                    // initialize here since these params are available
                    vault = ForageVaultWrapper(context, attrs, defStyleAttr)
                } finally {
                    recycle()
                }
            }

        _linearLayout.addView(vault.getTextElement())
        _linearLayout.addView(getLogoImageViewLayout(context))
        addView(_linearLayout)
    }

    override var typeface: Typeface?
        get() = vault.typeface
        set(value) { vault.typeface = value }

    override fun showKeyboard() = vault.showKeyboard()
}
