package com.joinforage.forage.android.ecom.ui

import android.app.Application
import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import com.joinforage.forage.android.R
import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.VaultType
import com.joinforage.forage.android.core.services.launchdarkly.LDManager
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.ui.VaultWrapper
import com.joinforage.forage.android.core.ui.element.ForageConfig
import com.joinforage.forage.android.core.ui.element.ForagePinElement
import com.joinforage.forage.android.ecom.ui.vault.bt.BTVaultWrapper
import com.joinforage.forage.android.ecom.ui.vault.vgs.VGSVaultWrapper
import com.launchdarkly.sdk.android.LDConfig

/**
 * A [ForageElement] that securely collects a card PIN. You need a [ForagePINEditText] to call
 * the ForageSDK online-only or ForageTerminalSDK POS methods that:
 * * [Check a card's balance][com.joinforage.forage.android.ForageSDK.checkBalance]
 * * [Collect a card PIN to defer payment capture to the server][com.joinforage.forage.android.ForageSDK.deferPaymentCapture]
 * * [Capture a payment immediately][com.joinforage.forage.android.ForageSDK.capturePayment]
 * * [Refund a Payment immediately][com.joinforage.forage.android.pos.ForageTerminalSDK.refundPayment] (**POS-only**)
 * * [Collect a card PIN to defer payment refund to the server][com.joinforage.forage.android.pos.ForageTerminalSDK.deferPaymentRefund]
 * (**POS-only**)
 * ```xml
 * <!-- Example forage_pin_component.xml -->
 * <?xml version="1.0" encoding="utf-8"?>
 * <androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
 *     xmlns:app="http://schemas.android.com/apk/res-auto"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent">
 *
 *     <com.joinforage.forage.android.ui.ForagePINEditText
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
 * * [Online-only Android Quickstart](https://docs.joinforage.app/docs/forage-android-quickstart)
 * * [POS Terminal Android Quickstart](https://docs.joinforage.app/docs/forage-terminal-android)
 */
class ForagePINEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.foragePanEditTextStyle
) : ForagePinElement(context, attrs, defStyleAttr) {
    private val btVaultWrapper: BTVaultWrapper
    private val vgsVaultWrapper: VGSVaultWrapper

    override fun showKeyboard() = vault.showKeyboard()

    init {
        context.obtainStyledAttributes(attrs, R.styleable.ForagePINEditText, defStyleAttr, 0)
            .apply {
                try {

                    // at this point in time, we do not know the environment and
                    // we are operating and thus do not know whether to add
                    // BTVaultWrapper, VGSVaultWrapper, or ForageVaultWrapper to the UI.
                    // But that's OK. We can hedge and instantiate all of them.
                    // Then, within setForageConfig, once we know the environment
                    // and are thus able to initial LaunchDarkly and find out
                    // whether to use BT or VGS. So, below we are hedging.
                    btVaultWrapper = BTVaultWrapper(context, attrs, defStyleAttr)
                    vgsVaultWrapper = VGSVaultWrapper(context, attrs, defStyleAttr)
                    // ensure all wrappers init with the
                    // same typeface (or the attributes)
                    btVaultWrapper.typeface = vgsVaultWrapper.typeface
                } finally {
                    recycle()
                }
            }
    }

    override fun determineBackingVault(forageConfig: ForageConfig, logger: Log): VaultWrapper {
        // initialize Launch Darkly singleton
        val ldMobileKey = EnvConfig.fromForageConfig(forageConfig).ldMobileKey
        val ldConfig = LDConfig.Builder().mobileKey(ldMobileKey).build()
        LDManager.initialize(context.applicationContext as Application, ldConfig)

        // decide on a vault provider and the corresponding vault wrapper
        val vaultType = LDManager.getVaultProvider(logger)
        return if (vaultType == VaultType.BT_VAULT_TYPE) {
            btVaultWrapper
        } else {
            // TODO: Update this to the ForageVaultWrapper once it's back in this codebase
            // https://linear.app/joinforage/issue/FX-1368/re-introduce-the-foragevaultwrapper-into-the-coreui-module-in-the
            vgsVaultWrapper
        }
    }

    override var typeface: Typeface?
        get() = if (vault == btVaultWrapper) btVaultWrapper.typeface else vgsVaultWrapper.typeface
        set(value) {
            // keep all vault providers in sync regardless of
            // whether they were added to the UI
            btVaultWrapper.typeface = value
            vgsVaultWrapper.typeface = value
        }
}
