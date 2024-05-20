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
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.ui.VaultWrapper
import com.joinforage.forage.android.core.ui.element.state.PinElementState

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
) : LinearLayout(context, attrs, defStyleAttr), ForageElement<PinElementState>, EditTextElement {
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

    internal abstract fun determineBackingVault(forageConfig: ForageConfig, logger: Log): VaultWrapper

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
