package com.joinforage.forage.android.ui

import android.app.Application
import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import com.basistheory.android.view.TextElement
import com.joinforage.forage.android.ForageConfigNotSetException
import com.joinforage.forage.android.LDManager
import com.joinforage.forage.android.R
import com.joinforage.forage.android.VaultType
import com.joinforage.forage.android.core.EnvConfig
import com.joinforage.forage.android.core.element.SimpleElementListener
import com.joinforage.forage.android.core.element.StatefulElementListener
import com.joinforage.forage.android.core.element.state.PinElementState
import com.joinforage.forage.android.core.telemetry.Log
import com.launchdarkly.sdk.android.LDConfig
import com.verygoodsecurity.vgscollect.widget.VGSEditText

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
) : AbstractForageElement<PinElementState>(context, attrs, defStyleAttr) {
    private val _linearLayout: LinearLayout
    private val btVaultWrapper: BTVaultWrapper
    private val vgsVaultWrapper: VGSVaultWrapper
    private val forageVaultWrapper: ForageVaultWrapper

    override fun showKeyboard() {
        if (vault.getVaultType() == VaultType.VGS_VAULT_TYPE)
            vault.getVGSEditText().showKeyboard()
        if (vault.getVaultType() == VaultType.BT_VAULT_TYPE) {
            TODO("currently blocked by BT's lack of this functionality")
        }
    }

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
    private val vault: VaultWrapper
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
                    setWillNotDraw(false)
                    orientation = VERTICAL
                    gravity = Gravity.CENTER

                    // at this point in time, we do not know the environment and
                    // we are operating and thus do not know whether to add
                    // BTVaultWrapper, VGSVaultWrapper, or ForageVaultWrapper to the UI.
                    // But that's OK. We can hedge and instantiate all of them.
                    // Then, within setForageConfig, once we know the environment
                    // and are thus able to initial LaunchDarkly and find out
                    // whether to use BT or VGS. So, below we are hedging.
                    btVaultWrapper = BTVaultWrapper(context, attrs, defStyleAttr)
                    vgsVaultWrapper = VGSVaultWrapper(context, attrs, defStyleAttr)
                    forageVaultWrapper = ForageVaultWrapper(context, attrs, defStyleAttr)
                    // ensure all wrappers init with the
                    // same typeface (or the attributes)
                    btVaultWrapper.typeface = vgsVaultWrapper.typeface
                    forageVaultWrapper.typeface = vgsVaultWrapper.typeface

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

    override fun initWithForageConfig(forageConfig: ForageConfig, isPos: Boolean) {
        // Must initialize DD at the beginning of each render function. DD requires the context,
        // so we need to wait until a context is present to run initialization code. However,
        // we have logging all over the SDK that relies on the render happening first.
        val logger = Log.getInstance()
        logger.initializeDD(context, forageConfig)

        if (isPos) {
            // only use Forage Vault for POS traffic!
            _SET_ONLY_vault = forageVaultWrapper
        } else {
            // initialize Launch Darkly singleton
            val ldMobileKey = EnvConfig.fromForageConfig(forageConfig).ldMobileKey
            val ldConfig = LDConfig.Builder().mobileKey(ldMobileKey).build()
            LDManager.initialize(context.applicationContext as Application, ldConfig)

            // decide on a vault provider and the corresponding vault wrapper
            val vaultType = LDManager.getVaultProvider(logger)
            _SET_ONLY_vault = if (vaultType == VaultType.BT_VAULT_TYPE) {
                btVaultWrapper
            } else {
                vgsVaultWrapper
            }
        }

        _linearLayout.addView(vault.getUnderlying())
        _linearLayout.addView(getLogoImageViewLayout(context))
        addView(_linearLayout)

        logger.i("[View] ForagePINEditText successfully rendered")
    }

    override fun clearText() {
        vault.clearText()
    }

    // While the events that ForageElements expose mirrors the
    // blur, focus, change etc events of an Android view,
    // they represent different abstractions. Our users need to
    // interact with the ForageElement abstraction and not the
    // implementation details of which Android view we use.
    // Therefore we expose novel set listener methods instead of
    // overriding the convention setOn*Listener
    override fun setOnFocusEventListener(l: SimpleElementListener) {
        vault.setOnFocusEventListener(l)
    }
    override fun setOnBlurEventListener(l: SimpleElementListener) {
        vault.setOnBlurEventListener(l)
    }
    override fun setOnChangeEventListener(l: StatefulElementListener<PinElementState>) {
        vault.setOnChangeEventListener(l)
    }

    override fun getElementState(): PinElementState {
        return vault.manager.getState()
    }

    internal fun getVaultType(): VaultType {
        return vault.getVaultType()
    }

    internal fun getTextInputEditText(): VGSEditText {
        return vault.getVGSEditText()
    }

    internal fun getTextElement(): TextElement {
        return vault.getTextElement()
    }

    internal fun getForageTextElement(): EditText {
        return vault.getForageTextElement()
    }

    override fun setTextColor(textColor: Int) {
        vault.setTextColor(textColor)
    }
    override fun setTextSize(textSize: Float) {
        vault.setTextSize(textSize)
    }

    override var typeface: Typeface?
        get() = if (vault == btVaultWrapper) btVaultWrapper.typeface else vgsVaultWrapper.typeface
        set(value) {
            // keep all vault providers in sync regardless of
            // whether they were added to the UI
            btVaultWrapper.typeface = value
            vgsVaultWrapper.typeface = value
        }
    override fun setHint(hint: String) {
        vault.setHint(hint)
    }
    override fun setHintTextColor(hintTextColor: Int) {
        vault.setHintTextColor(hintTextColor)
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
}
