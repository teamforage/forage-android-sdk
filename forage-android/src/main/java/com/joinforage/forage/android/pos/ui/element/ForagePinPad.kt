package com.joinforage.forage.android.pos.ui.element

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.annotation.StringRes
import androidx.core.view.children
import com.joinforage.forage.android.R
import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.forageapi.engine.OkHttpEngine
import com.joinforage.forage.android.core.services.vault.ISecurePinCollector
import com.joinforage.forage.android.core.services.vault.RosettaPinSubmitter
import com.joinforage.forage.android.core.ui.element.ForageVaultElement
import com.joinforage.forage.android.core.ui.element.StatefulElementListener
import com.joinforage.forage.android.core.ui.element.state.pin.PinInputState
import com.joinforage.forage.android.databinding.ForageKeypadBinding
import com.joinforage.forage.android.pos.ui.element.state.pin.PinPadState

internal data class PinText(
    val rawText: String
) {
    val isComplete = rawText.length == 4
    val isEmpty = rawText.isEmpty()
    fun clearText(): PinText = PinText("")
    fun addDigit(char: Char): PinText {
        val appendChar = "$rawText$char"
        val keepFirst4 = appendChar.take(4)
        return PinText(keepFirst4)
    }
    fun dropLastOne(): PinText = PinText(rawText.dropLast(1))

    companion object {
        fun forEmptyInput() = PinText("")
    }
}

internal class PinPadStateManager(
    private val pinText: PinText,
    private val onChangeCallback: StatefulElementListener<PinPadState>?,
    private val onDoneCallback: StatefulElementListener<PinPadState>?
) {

    val rawPinText: String = pinText.rawText
    val isComplete: Boolean = pinText.isComplete
    private val pinInputState = PinInputState.from(
        isComplete = pinText.isComplete,
        isEmpty = pinText.isEmpty
    )
    val state = PinPadState.from(pinText, pinInputState)

    private fun onPinTextChange(nextPinText: PinText): PinPadStateManager {
        val nextManager = PinPadStateManager(nextPinText, onChangeCallback, onDoneCallback)
        onChangeCallback?.invoke(nextManager.state)
        return nextManager
    }

    fun addDigit(char: Char) = onPinTextChange(pinText.addDigit(char))
    fun clearText() = onPinTextChange(pinText.clearText())
    fun dropLastOne() = onPinTextChange(pinText.dropLastOne())
    fun onDone() { onDoneCallback?.invoke(state) }

    fun withOnChangeCallback(l: StatefulElementListener<PinPadState>) =
        PinPadStateManager(pinText, l, onDoneCallback)
    fun withOnDoneCallback(l: StatefulElementListener<PinPadState>) =
        PinPadStateManager(pinText, onChangeCallback, l)

    companion object {
        fun forEmptyInput() = PinPadStateManager(
            PinText.forEmptyInput(),
            null,
            null
        )
    }
}

/**
 * A [ForageElement][com.joinforage.forage.android.core.ui.element.ForageElement] that securely
 * collects a card PIN. Use the ForagePinPad if your terminal
 * supports a guest facing display or if you expect to display the PIN pad on an external monitor.
 * You can use a [ForagePinPad] to call
 * the ForageTerminalSDK POS methods that:
 * * [Check a card's balance][com.joinforage.forage.android.pos.services.ForageTerminalSDK.checkBalance]
 * * [Collect a card PIN to defer payment capture to the server][com.joinforage.forage.android.pos.services.ForageTerminalSDK.deferPaymentCapture]
 * * [Capture a payment immediately][com.joinforage.forage.android.pos.services.ForageTerminalSDK.capturePayment]
 * * [Refund a payment immediately][com.joinforage.forage.android.pos.services.ForageTerminalSDK.refundPayment]
 * * [Collect a card PIN to defer payment refund to the server][com.joinforage.forage.android.pos.services.ForageTerminalSDK.deferPaymentRefund]
 * ```xml
 * <!-- Example forage_pin_pad_component.xml -->
 * <androidx.constraintlayout.widget.ConstraintLayout
 *     xmlns:android="http://schemas.android.com/apk/res/android"
 *     xmlns:app="http://schemas.android.com/apk/res-auto"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent">
 *
 *     <com.joinforage.forage.android.pos.ui.element.ForagePinPad
 *         android:id="@+id/my_pin_pad"
 *         android:layout_width="match_parent"
 *         android:layout_height="wrap_content"
 *         app:forage_buttonLayoutMargin="@dimen/keypad_btn_margin"
 *         app:forage_deleteButtonIcon="@android:drawable/ic_delete"
 *      />
 *
 * </androidx.constraintlayout.widget.ConstraintLayout>
 * ```
 * @see * [Guide to styling Forage Android Elements](https://docs.joinforage.app/docs/forage-android-styling-guide)
 * * [POS Terminal Android Quickstart](https://docs.joinforage.app/docs/forage-terminal-android)
 */
class ForagePinPad @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ForageVaultElement<PinPadState>(context, attrs) {

    private val binding = ForageKeypadBinding.inflate(LayoutInflater.from(context), this, true)
    private var manager = PinPadStateManager.forEmptyInput()

    init {
        val styles = Styles(context, attrs)
        KeypadStyler(binding, styles).applyStyling()
        KeypadConfigurator(
            binding,
            object : KeypadConfigurator.EventsManager {
                override fun addDigit(char: Char) { manager = manager.addDigit(char) }
                override fun clearText() { manager = manager.clearText() }
                override fun dropLastOne() { manager = manager.dropLastOne() }
                override fun onDone() { manager.onDone() }
            }
        ).configureKeypad()

        // set Forage logo based on light vs dark mode
        val forageLogoResource = if (styles.useDarkTheme) {
            R.drawable.powered_by_forage_logo_dark
        } else {
            R.drawable.powered_by_forage_logo
        }
        binding.poweredByForageImageView.setImageResource(forageLogoResource)
        orientation = VERTICAL
    }

    override fun getVaultSubmitter(
        envConfig: EnvConfig
    ): RosettaPinSubmitter {
        return RosettaPinSubmitter(
            manager.rawPinText,
            object : ISecurePinCollector {
                override fun clearText() {
                    this@ForagePinPad.clearText()
                }
                override fun isComplete(): Boolean = manager.isComplete
            },
            OkHttpEngine()
        )
    }

    override var typeface: Typeface?
        get() = TODO("Not yet implemented")
        set(value) {}

    @Deprecated(
        message = "This function is deprecated and is not supported in future releases.",
        level = DeprecationLevel.WARNING
    )
    fun setOnDoneListener(l: StatefulElementListener<PinPadState>) {
        manager = manager.withOnDoneCallback(l)
    }

    /**
     * Sets an event listener to be fired when the ForageElement is in focus.
     *
     * @param l The [StatefulElementListener] to be fired on focus events.
     */
    override fun setOnChangeEventListener(l: StatefulElementListener<PinPadState>) {
        manager = manager.withOnChangeCallback(l)
    }

    /**
     * Clears the text input field of the ForageElement.
     */
    override fun clearText() {
        manager = manager.clearText()
    }

    /**
     * Sets the text color for the ForageElement.
     *
     * @param textColor The color value in the form `0xAARRGGBB`.
     */
    override fun setTextColor(textColor: Int) {
        TODO("Not yet implemented")
    }

    /**
     * Sets the text size for the ForageElement.
     *
     * @param textSize The scaled pixel size.
     */
    override fun setTextSize(textSize: Float) {
        TODO("Not yet implemented")
    }

    /**
     * Gets the current [PinPadState].
     * of the ForageElement.
     *
     * @return The [PinPadState].
     */
    override fun getElementState(): PinPadState {
        return manager.state
    }
}

// NOTE: it's named ForagePinPadStyles because it's hard-coded to work
// with R.styleable.ForagePinPad. So even though the class could be
// re-used, it's coupled to <declare-styleable name="ForagePinPad">
private class Styles(context: Context, attrs: AttributeSet?) {
    @DrawableRes
    val deleteButtonIcon: Int

    @DrawableRes
    val doneButtonIcon: Int

    @StringRes
    val doneButtonText: Int

    @Px
    val buttonLayoutMargin: Int

    val useDarkTheme: Boolean

    init {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.ForageKeypad, 0, 0)
        useDarkTheme = attributes.getBoolean(R.styleable.ForageKeypad_forage_useDarkTheme, false)
        deleteButtonIcon = attributes.getResourceId(R.styleable.ForageKeypad_forage_deleteButtonIcon, 0)
        doneButtonIcon = attributes.getResourceId(R.styleable.ForageKeypad_forage_doneButtonIcon, 0)
        doneButtonText = attributes.getResourceId(R.styleable.ForageKeypad_forage_doneButtonText, 0)
        buttonLayoutMargin = attributes.getLayoutDimension(R.styleable.ForageKeypad_forage_buttonLayoutMargin, 8)
        attributes.recycle()
    }
}

/**
 * A class that allows a few succinct styles to get intelligently
 * applied to the 16 button grid. The alternative wuld be really
 * verbose
 */
private class KeypadStyler(
    private val binding: ForageKeypadBinding,
    private val styles: Styles
) {
    fun applyStyling() {
        val rows = with(binding) {
            setOf(forageRow1, forageRow2, forageRow3, forageRow4)
        }

        rows.forEach { row ->
            // apply bottom margin to all cells for consistent spacing
            // between grid rows and the Forage logo
            row.children.forEach { cell ->
                (cell.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = styles.buttonLayoutMargin
            }

            // apply start margin to cells in columns 2 - 4
            // by skipping the cells from the col 1
            val children = row.children.toList().drop(1)
            children.forEach { cell ->
                (cell.layoutParams as ViewGroup.MarginLayoutParams).marginStart = styles.buttonLayoutMargin
            }
        }

        with(binding) {
            forageButtonDelete.setIconResource(styles.deleteButtonIcon)
            forageButtonDone.setIconResource(styles.doneButtonIcon)
            forageButtonDone.setText(styles.doneButtonText)
        }
    }
}

private class KeypadConfigurator(
    private val binding: ForageKeypadBinding,
    private val manager: EventsManager
) {
    interface EventsManager {
        fun addDigit(char: Char)
        fun clearText()
        fun dropLastOne()
        fun onDone()
    }

    fun configureKeypad() {
        with(binding) {
            forageButton0.setOnClickListener { manager.addDigit('0') }
            forageButton1.setOnClickListener { manager.addDigit('1') }
            forageButton2.setOnClickListener { manager.addDigit('2') }
            forageButton3.setOnClickListener { manager.addDigit('3') }
            forageButton4.setOnClickListener { manager.addDigit('4') }
            forageButton5.setOnClickListener { manager.addDigit('5') }
            forageButton6.setOnClickListener { manager.addDigit('6') }
            forageButton7.setOnClickListener { manager.addDigit('7') }
            forageButton8.setOnClickListener { manager.addDigit('8') }
            forageButton9.setOnClickListener { manager.addDigit('9') }

            forageButtonClear.setOnClickListener { manager.clearText() }
            forageButtonDelete.setOnClickListener { manager.dropLastOne() }

            forageButtonDone.setOnClickListener { manager.onDone() }
        }
    }
}
