package com.joinforage.forage.android.ecom.ui.element

import android.app.Application
import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import com.joinforage.forage.android.R
import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.ForageConfigNotSetException
import com.joinforage.forage.android.core.services.VaultType
import com.joinforage.forage.android.core.services.launchdarkly.LDManager
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.vault.AbstractVaultSubmitter
import com.joinforage.forage.android.core.ui.VaultWrapper
import com.joinforage.forage.android.core.ui.element.DynamicEnvElement
import com.joinforage.forage.android.core.ui.element.ForageConfigManager
import com.joinforage.forage.android.core.ui.element.ForagePinElement
import com.joinforage.forage.android.core.ui.getLogoImageViewLayout
import com.joinforage.forage.android.ecom.ui.vault.bt.BTVaultWrapper
import com.joinforage.forage.android.ecom.ui.vault.forage.ForageVaultWrapper
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
) : ForagePinElement(context, attrs, defStyleAttr), DynamicEnvElement {
    private val btVaultWrapper: BTVaultWrapper
    private val forageVaultWrapper: ForageVaultWrapper

    /**
     * The `vault` property acts as an abstraction for the actual code
     * in ForagePINEditText, allowing it to work with a non-nullable
     * result determined by the choice between BT or VGS. This choice
     * depends on Launch Darkly and requires knowledge of the environment,
     * which is determined by `forageConfig` set on this instance.
     *
     * The underlying value for `vault` is stored in `_SET_ONLY_vault`.
     * This backing property is set only after `ForageConfig` has been
     * initialized for this instance. If `vault` is accessed before
     * `_SET_ONLY_vault` is set, a runtime exception is thrown.
     */
    private var _SET_ONLY_vault: VaultWrapper? = null
    override val vault: VaultWrapper
        get() {
            if (_SET_ONLY_vault == null) {
                throw ForageConfigNotSetException(
                    """You are attempting invoke a method a ForageElement before setting
                    it's ForageConfig. Make sure to call
                    myForageElement.setForageConfig(forageConfig: ForageConfig) 
                    immediately on your ForageElement before you call any other methods.
                    """.trimIndent()
                )
            }
            return _SET_ONLY_vault!!
        }

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
                    forageVaultWrapper = ForageVaultWrapper(context, attrs, defStyleAttr)
                    // ensure all wrappers init with the
                    // same typeface (or the attributes)
                    forageVaultWrapper.typeface = btVaultWrapper.typeface
                } finally {
                    recycle()
                }
            }
    }

    private fun initWithForageConfig(forageConfig: ForageConfig) {
        // Must initialize DD at the beginning of each render function. DD requires the context,
        // so we need to wait until a context is present to run initialization code. However,
        // we have logging all over the SDK that relies on the render happening first.
        val logger = Log.getInstance()
        logger.initializeDD(context, forageConfig)

        // defer to the concrete subclass for the details of obtaining
        // and instantiating the backing vault instance. We just need
        // to guarantee that the vault is instantiated prior to adding
        // it to the parent view
        _SET_ONLY_vault = determineBackingVault(forageConfig, logger)

        _linearLayout.addView(vault.getTextElement())
        _linearLayout.addView(getLogoImageViewLayout(context))
        addView(_linearLayout)

        logger.i("[View] ForagePINEditText successfully rendered")
    }

    private val forageConfigManager = ForageConfigManager {
            forageConfig ->
        initWithForageConfig(forageConfig)
    }
    override fun setForageConfig(forageConfig: ForageConfig) {
        forageConfigManager.forageConfig = forageConfig
    }

    private fun determineBackingVault(forageConfig: ForageConfig, logger: Log): VaultWrapper {
        // initialize Launch Darkly singleton
        val ldMobileKey = EnvConfig.fromForageConfig(forageConfig).ldMobileKey
        val ldConfig = LDConfig.Builder().mobileKey(ldMobileKey).build()
        LDManager.initialize(context.applicationContext as Application, ldConfig)

        // decide on a vault provider and the corresponding vault wrapper
        val vaultType = LDManager.getVaultProvider(logger)
        return if (vaultType == VaultType.FORAGE_VAULT_TYPE) {
            forageVaultWrapper
        } else {
            btVaultWrapper
        }
    }

    internal fun getForageConfig() = forageConfigManager.forageConfig

    override fun getVaultSubmitter(
        envConfig: EnvConfig,
        logger: Log
    ): AbstractVaultSubmitter = vault.getVaultSubmitter(envConfig, logger)

    override var typeface: Typeface?
        get() = if (vault == btVaultWrapper) btVaultWrapper.typeface else forageVaultWrapper.typeface
        set(value) {
            // keep all vault providers in sync regardless of
            // whether they were added to the UI
            btVaultWrapper.typeface = value
            forageVaultWrapper.typeface = value
        }

    override fun showKeyboard() = vault.showKeyboard()
}
