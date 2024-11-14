package com.joinforage.forage.android.ecom.ui.element

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import com.joinforage.forage.android.R
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.ForageConfigNotSetException
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.ui.VaultWrapper
import com.joinforage.forage.android.core.ui.element.DynamicEnvElement
import com.joinforage.forage.android.core.ui.element.ForageConfigManager
import com.joinforage.forage.android.core.ui.element.ForagePinElement
import com.joinforage.forage.android.core.ui.getLogoImageViewLayout
import com.joinforage.forage.android.ecom.ui.vault.rosetta.RosettaPinElement

/**
 * A [ForageElement][com.joinforage.forage.android.core.ui.element.ForageElement] that securely collects a card PIN. You need a [ForagePINEditText] to call
 * the [ForageSDK][com.joinforage.forage.android.ecom.services.ForageSDK] methods that:
 * * [Check a card's balance][com.joinforage.forage.android.ecom.services.ForageSDK.checkBalance]
 * * [Collect a card PIN to defer payment capture to the server][com.joinforage.forage.android.ecom.services.ForageSDK.deferPaymentCapture]
 * * [Capture a payment immediately][com.joinforage.forage.android.ecom.services.ForageSDK.capturePayment]
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
    private val rosettaPinElement: RosettaPinElement

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
                    rosettaPinElement = RosettaPinElement(context, attrs, defStyleAttr)
                    val textSize = getDimension(R.styleable.ForagePINEditText_textSize, -1f)
                    if (textSize != -1f) {
                        rosettaPinElement.setTextSize(textSize)
                    }
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

        _SET_ONLY_vault = rosettaPinElement

        _linearLayout.addView(vault.getTextElement())
        _linearLayout.addView(getLogoImageViewLayout(context))
        addView(_linearLayout)

        logger.i("[View] ForagePINEditText successfully rendered")
    }

    private val forageConfigManager = ForageConfigManager {
            forageConfig ->
        initWithForageConfig(forageConfig)
    }

    /**
     * Sets the necessary [ForageConfig] configuration properties for a [ForagePINEditText].
     * **[setForageConfig] must be called before any other methods can be executed on the Element.**
     * ```kotlin
     * // Example: Call setForageConfig on a ForagePINEditText Element
     * val foragePinEditText = root?.findViewById<ForagePINEditText>(
     *     R.id.balanceForagePinEditText
     * )
     * foragePinEditText.setForageConfig(
     *     ForageConfig(
     *         merchantId = "<merchant_id>",
     *         sessionToken = "<session_token>"
     *     )
     * )
     * ```
     *
     * @param forageConfig A [ForageConfig] instance that specifies a `merchantId` and `sessionToken`.
     */
    override fun setForageConfig(forageConfig: ForageConfig) {
        forageConfigManager.forageConfig = forageConfig
    }

    internal fun getForageConfig() = forageConfigManager.forageConfig

    override var typeface: Typeface?
        get() = rosettaPinElement.typeface
        set(value) {
            rosettaPinElement.typeface = value
        }
}
