package com.joinforage.forage.android.pos.ui.element

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.annotation.StringRes
import androidx.core.content.res.getResourceIdOrThrow
import com.joinforage.forage.android.R
import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.vault.AbstractVaultSubmitter
import com.joinforage.forage.android.core.services.vault.SecurePinCollector
import com.joinforage.forage.android.core.ui.element.StatefulElementListener
import com.joinforage.forage.android.pos.services.vault.rosetta.RosettaPinSubmitter
import com.joinforage.forage.android.databinding.ForageKeypadBinding
import com.joinforage.forage.android.pos.services.ForagePosVaultElement


internal class PinTextManager(
    val rawText: String,
    val callback: (isComplete: Boolean, isEmpty: Boolean, pinLength: Int, ) -> Unit
) {
    val isComplete = rawText.length == 4
    val isEmpty = rawText.isEmpty()
    val pinLength = rawText.length

    init {
        // every operation returns a new instance of PinTextManager
        // with the exception of addDigit if there are already 4
        // digits. By calling callback in the constructor, we are
        // de-facto invoking the callback upon every meaningful
        // pin input state change
        callback(this.isComplete, this.isEmpty, this.pinLength)
    }
    fun clearText() : PinTextManager = PinTextManager("", callback)
    fun addDigit(char: Char): PinTextManager {
        if (isComplete) return this
        val appendChar = "$rawText$char"
        return PinTextManager(appendChar, callback)
    }
    fun dropLastOne() : PinTextManager = PinTextManager(rawText.dropLast(1), callback)
}

class ForagePinPad @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ForagePosVaultElement(context, attrs) {

    private val binding = ForageKeypadBinding.inflate(LayoutInflater.from(context), this, true)
    private val manager = PosPinElementStateManager.forEmptyInput()
    private var onDoneListener: StatefulElementListener<PosPinElementState>? = null
    private var pinText = PinTextManager("") { isComplete, isEmpty, pinLength ->
        manager.handleChangeEvent(isComplete, isEmpty, pinLength)
    }

    init {
//        val styles = ForagePinPadStyles(context, attrs)
//        KeypadStyler(binding, styles).applyStyling()
        KeypadConfigurator(binding, object : KeypadConfigurator.EventsManager {
            override fun addDigit(char: Char) {
                pinText = pinText.addDigit(char)
            }
            override fun dropLastOne() {
                pinText = pinText.dropLastOne()
            }
            override fun clearText() {
                pinText = pinText.clearText()
            }
            override fun onDone() {
                onDoneListener?.invoke(manager.getState())
            }
        }).configureKeypad()
    }

    override fun getVaultSubmitter(envConfig: EnvConfig, logger: Log): AbstractVaultSubmitter {
        return RosettaPinSubmitter(
            pinText.rawText,
            object : SecurePinCollector {
                override fun clearText() {
                    this@ForagePinPad.clearText()
                }
                override fun isComplete(): Boolean = manager.isComplete
            },
            envConfig,
            logger
        )
    }

    override var typeface: Typeface?
        get() = TODO("Not yet implemented")
        set(value) {}

    fun setOnDoneListener(l: StatefulElementListener<PosPinElementState>) {
        onDoneListener = l
    }

    override fun clearText() {
        pinText.clearText()
    }

    override fun setTextColor(textColor: Int) {
        TODO("Not yet implemented")
    }

    override fun setTextSize(textSize: Float) {
        TODO("Not yet implemented")
    }

    override fun getElementState(): PosPinElementState {
        return manager.getState()
    }

    override fun setOnChangeEventListener(l: StatefulElementListener<PosPinElementState>) {
        manager.setOnChangeEventListener(l)
    }
}

//// NOTE: it's named ForagePinPadStyles because it's hard-coded to work
//// with R.styleable.ForagePinPad. So even though the class could be
//// re-used, it's coupled to <declare-styleable name="ForagePinPad">
//private class ForagePinPadStyles(context: Context, attrs: AttributeSet?) {
////    @DrawableRes
////    val deleteButtonIcon: Int
////
////    @DrawableRes
////    val doneButtonIcon: Int
//
//    @StringRes
//    val doneButtonText: Int
//
//    @Px
//    val buttonLayoutHeight: Int
//
//    @Px
//    val buttonLayoutMargin: Int
//
//    init {
//        val attributes = context.obtainStyledAttributes(attrs, R.styleable.ForageKeypad, 0, 0)
//        doneButtonText = "Done"
//        buttonLayoutHeight = attributes.getLayoutDimension(R.styleable.ForageKeypad_forage_buttonLayoutHeight, 12)
//        buttonLayoutMargin = attributes.getLayoutDimension(R.styleable.ForageKeypad_forage_buttonLayoutMargin, 12)
////        deleteButtonIcon = attributes.getResourceIdOrThrow(R.styleable.ForageKeypad_forage_deleteButtonIcon)
////        doneButtonIcon = attributes.getResourceIdOrThrow(R.styleable.ForageKeypad_forage_doneButtonIcon)
//        attributes.recycle()
//    }
//}

///**
// * A class that allows a few succint styles to get intelligently
// * applied to the 16 button grid. The alternative wuld be really
// * verbose
// */
//private class KeypadStyler(
//    private val binding: ForageKeypadBinding,
//    private val styles: ForagePinPadStyles
//) {
//    fun applyStyling() {
//        val rowOneButtons =
//            with(binding) {
//                setOf(forageButton1, forageButton2, forageButton3, forageButtonDelete)
//            }
//
//        val rowTwoButtons =
//            with(binding) {
//                setOf(forageButton4, forageButton5, forageButton6)
//            }
//
//        val rowThreeButtons =
//            with(binding) {
//                setOf(forageButton7, forageButton8, forageButton9)
//            }
//
//        val rowFourButtons =
//            with(binding) {
//                setOf(forageButton0, forageButtonClear, forageButtonDone)
//            }
//
//        val columnTwoButtons =
//            with(binding) {
//                setOf(forageButton2, forageButton5, forageButton8, forageButton0)
//            }
//
//        val columnThreeButtons =
//            with(binding) {
//                setOf(forageButton3, forageButton6, forageButton9, forageButtonClear)
//            }
//
//        val columnFourButtons =
//            with(binding) {
//                setOf(forageButtonDelete, forageButtonDone)
//            }
//
//        rowOneButtons.forEach {
//            it.layoutParams.height = styles.buttonLayoutHeight
//        }
//
//        (rowTwoButtons + rowThreeButtons + rowFourButtons).forEach {
//            it.layoutParams.height = styles.buttonLayoutHeight
//            (it.layoutParams as
//                    ViewGroup.MarginLayoutParams).topMargin = styles.buttonLayoutMargin
//        }
//
//        // we could exclude row four from the definitions of three columns referenced below since we
//        // don't use those sets anywhere but here, but this more accurately describes our intentions
//        ((columnTwoButtons + columnThreeButtons + columnFourButtons) - rowFourButtons).forEach {
//            (it.layoutParams as ViewGroup.MarginLayoutParams).marginStart = styles.buttonLayoutMargin
//        }
//
//        with(binding) {
////            forageButtonDelete.setIconResource(styles.deleteButtonIcon)
////            forageButtonDone.setIconResource(styles.doneButtonIcon)
//            forageButtonDone.setText(styles.doneButtonText)
//        }
//    }
//}

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