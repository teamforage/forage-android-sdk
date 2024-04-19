package com.joinforage.forage.android.pos

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.EditText
import com.joinforage.forage.android.R
import com.joinforage.forage.android.core.ForagePinElement
import com.joinforage.forage.android.ui.ForageVaultWrapper
import com.joinforage.forage.android.ui.VaultWrapper

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
    private val forageVaultWrapper: ForageVaultWrapper

    init {
        context.obtainStyledAttributes(attrs, R.styleable.ForagePINEditText, defStyleAttr, 0)
            .apply {
                try {

                    // initialize here since these params are available
                    forageVaultWrapper = ForageVaultWrapper(context, attrs, defStyleAttr)
                } finally {
                    recycle()
                }
            }
    }

    override fun determineBackingVault(): VaultWrapper<EditText> = forageVaultWrapper

    override var typeface: Typeface?
        get() = forageVaultWrapper.typeface
        set(value) { forageVaultWrapper.typeface = value }
}
