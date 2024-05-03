package com.joinforage.forage.android.core.ui.element

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.joinforage.forage.android.R
import com.joinforage.forage.android.core.ForageConfigNotSetException
import com.joinforage.forage.android.core.services.VaultType
import com.joinforage.forage.android.core.services.vault.AbstractVaultSubmitter
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.ui.element.state.PinElementState
import com.joinforage.forage.android.core.ui.getLogoImageViewLayout
import com.joinforage.forage.android.core.ui.VaultWrapper

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
abstract class ForagePinElement @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.foragePanEditTextStyle
) : AbstractForageElement<PinElementState>(context, attrs, defStyleAttr) {
    private val _linearLayout: LinearLayout

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
    internal val vault: VaultWrapper
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

    internal abstract fun determineBackingVault(forageConfig: ForageConfig, logger: Log) : VaultWrapper

    override fun initWithForageConfig(forageConfig: ForageConfig) {
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

    override fun clearText() {
        vault.clearText()
    }

    internal fun getVaultSubmitter(logger: Log) : AbstractVaultSubmitter =
        vault.getVaultSubmitter(this, logger)

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