package com.joinforage.forage.android.ecom.ui.element

import android.content.Context
import android.util.AttributeSet
import com.joinforage.forage.android.R
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.ui.element.DynamicEnvElement
import com.joinforage.forage.android.core.ui.element.ForagePinElement

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
    override fun setForageConfig(forageConfig: ForageConfig) { this._forageConfig = forageConfig }

    private var _forageConfig: ForageConfig? = null
    internal fun getForageConfig() = _forageConfig
}
