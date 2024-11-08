package com.joinforage.forage.android.pos

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import com.joinforage.forage.android.R
import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.vault.AbstractVaultSubmitter
import com.joinforage.forage.android.core.ui.element.ForagePinElement
import com.joinforage.forage.android.pos.ui.element.ForagePINEditText
import com.joinforage.forage.android.pos.ui.vault.rosetta.RosettaPinElement

/**
 * A [ForagePinElement] that securely collects a card PIN. You can use a [ForagePINEditText]
 * to call the ForageTerminalSDK POS methods that:
 * * [Check a card's balance][com.joinforage.forage.android.pos.services.ForageTerminalSDK.checkBalance]
 * * [Collect a card PIN to defer payment capture to the server][com.joinforage.forage.android.pos.services.ForageTerminalSDK.deferPaymentCapture]
 * * [Capture a payment immediately][com.joinforage.forage.android.pos.services.ForageTerminalSDK.capturePayment]
 * * [Refund a Payment immediately][com.joinforage.forage.android.pos.services.ForageTerminalSDK.refundPayment]
 * * [Collect a card PIN to defer payment refund to the server][com.joinforage.forage.android.pos.services.ForageTerminalSDK.deferPaymentRefund]
 * ```xml
 * <!-- Example forage_pin_component.xml -->
 * <?xml version="1.0" encoding="utf-8"?>
 * <androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
 *     xmlns:app="http://schemas.android.com/apk/res-auto"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent">
 *
 *     <com.joinforage.forage.android.pos.ui.element.ForagePINEditText
 *         android:id="@+id/foragePinEditText"
 *         android:layout_width="0dp"
 *         android:layout_height="wrap_content"
 *         android:layout_margin="16dp"
 *         app:layout_constraintBottom_toBottomOf="parent"
 *         app:layout_constraintEnd_toEndOf="parent"
 *         app:layout_constraintStart_toStartOf="parent"
 *         app:layout_constraintTop_toTopOf="parent"
 *     />
 *
 * </androidx.constraintlayout.widget.ConstraintLayout>
 * ```
 * @see * [Guide to styling Forage Android Elements](https://docs.joinforage.app/docs/forage-android-styling-guide)
 * * [POS Terminal Android Quickstart](https://docs.joinforage.app/docs/forage-terminal-android)
 */
class ForagePINEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.foragePanEditTextStyle
) : ForagePinElement(context, attrs, defStyleAttr) {
    override val vault: RosettaPinElement

    init {
        context.obtainStyledAttributes(attrs, R.styleable.ForagePINEditText, defStyleAttr, 0)
            .apply {
                try {

                    // initialize here since these params are available
                    vault = RosettaPinElement(context, attrs, defStyleAttr)
                } finally {
                    recycle()
                }
            }
    }

    override var typeface: Typeface?
        get() = vault.typeface
        set(value) { vault.typeface = value }

    override fun getVaultSubmitter(
        envConfig: EnvConfig,
        logger: Log
    ): AbstractVaultSubmitter = vault.getVaultSubmitter(envConfig, logger)

    override fun showKeyboard() = vault.showKeyboard()
}
