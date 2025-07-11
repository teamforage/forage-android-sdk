package com.joinforage.forage.android.ecom.ui.element

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.textfield.TextInputEditText
import com.joinforage.forage.android.R
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.ui.element.DynamicEnvElement
import com.joinforage.forage.android.core.ui.element.ElementValidationError
import com.joinforage.forage.android.core.ui.element.ForageElement
import com.joinforage.forage.android.core.ui.element.StatefulElementListener
import com.joinforage.forage.android.core.ui.element.state.ElementState
import com.joinforage.forage.android.core.ui.textwatcher.ExpirationTextWatcher
import com.joinforage.forage.android.core.ui.textwatcher.FormatPanTextWatcher
import com.joinforage.forage.android.core.ui.textwatcher.TextWatcherAdapter
import com.joinforage.forage.android.ecom.services.ForageSDK

/**
 * A [ForageElement] that securely collects a customer's card number. You need a [ForagePaymentSheet]
 * to call the ForageSDK online-only method to [tokenize a credit Card][ForageSDK.tokenizeCreditCard].
 * ```xml
 * <!-- Example forage_payment_sheet_component.xml -->
 * <?xml version="1.0" encoding="utf-8"?>
 * <androidx.constraintlayout.widget.ConstraintLayout
 *     xmlns:android="http://schemas.android.com/apk/res/android"
 *     xmlns:app="http://schemas.android.com/apk/res-auto"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent">
 *
 *     <com.joinforage.forage.android.ecom.ui.element.ForagePaymentSheet
 *             android:id="@+id/foragePaymentSheet"
 *             android:layout_width="0dp"
 *             android:layout_height="wrap_content"
 *             android:layout_margin="16dp"
 *             app:layout_constraintBottom_toBottomOf="parent"
 *             app:layout_constraintEnd_toEndOf="parent"
 *             app:layout_constraintStart_toStartOf="parent"
 *             app:layout_constraintTop_toTopOf="parent"
 *     />
 *
 * </androidx.constraintlayout.widget.ConstraintLayout>
 * ```
 * @see * [Guide to styling Forage Android Elements](https://docs.joinforage.app/docs/forage-android-styling-guide)
 * * [Online-only Android Quickstart](https://docs.joinforage.app/docs/forage-android-quickstart)
 * * [POS Terminal Android Quickstart](https://docs.joinforage.app/docs/forage-terminal-android)
 */
class ForagePaymentSheet @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.foragePaymentSheetStyle
) : ConstraintLayout(context, attrs, defStyleAttr),
    ForageElement<ForagePaymentSheet.ForagePaymentSheetElementState>, DynamicEnvElement {
    private val rootView: ForagePaymentSheet =
        inflate(context, R.layout.fragment_forage_payment_sheet, this) as ForagePaymentSheet

    private val cardholderNameEditText: TextInputEditText get() = rootView.findViewById(R.id.cardholderNameEditText)
    private val cardNumberEditText: TextInputEditText get() = rootView.findViewById(R.id.cardNumberEditText)
    private val expirationEditText: TextInputEditText get() = rootView.findViewById(R.id.expirationEditText)
    private val securityCodeEditText: TextInputEditText get() = rootView.findViewById(R.id.securityCodeEditText)
    private val zipCodeEditText: TextInputEditText get() = rootView.findViewById(R.id.zipCodeEditText)

    private var onChangeEventListener: StatefulElementListener<ForagePaymentSheetElementState>? =
        null

    override fun onFinishInflate() {
        super.onFinishInflate()

        expirationEditText.let { expirationEditText ->
            val expirationTextWatcher = ExpirationTextWatcher(expirationEditText)
            expirationEditText.addTextChangedListener(expirationTextWatcher)
        }

        cardNumberEditText.let { cardNumberEditText ->
            val formatPanTextWatcher = FormatPanTextWatcher(cardNumberEditText)
            cardNumberEditText.addTextChangedListener(formatPanTextWatcher)
        }

        val textWatcher = object : TextWatcherAdapter() {
            override fun afterTextChanged(editable: Editable) {
                onChangeEventListener?.invoke(ForagePaymentSheetElementState())
            }
        }

        cardholderNameEditText.addTextChangedListener(textWatcher)
        securityCodeEditText.addTextChangedListener(textWatcher)
        zipCodeEditText.addTextChangedListener(textWatcher)
    }

    interface ValueAndState<T> {
        val value: T
        val state: ElementState
    }

    /** Returns the cardholder name value and state. */
    val cardholderName = object : ValueAndState<String> {
        override val value
            get() = cardholderNameEditText.text.toString().trim()
        override val state
            get() = object : ElementState {
                override val isEmpty get() = value.isEmpty()
                override val isValid get() = !isEmpty
                override val isComplete get() = !isEmpty
                override val validationError
                    get() =
                        if (isValid) {
                            null
                        } else {
                            ElementValidationError("Cardholder name required")
                        }
            }
    }

    /** Returns the card number value. */
    val cardNumber = object : ValueAndState<String> {
        override val value
            get() = cardNumberEditText.text.toString().filter { it.isDigit() }
        override val state
            get() = object : ElementState {
                override val isEmpty get() = value.isEmpty()
                override val isValid get() = !isEmpty
                override val isComplete get() = !isEmpty
                override val validationError
                    get() =
                        if (isValid) {
                            null
                        } else {
                            ElementValidationError("Card number required")
                        }
            }
    }

    /** Returns the card expiration value as a [Pair] with values MM and YYYY. */
    val expiration = object : ValueAndState<Pair<Int, Int>> {
        override val value: Pair<Int, Int>
            get() {
                val rawText = expirationEditText.text.toString()
                val monthAndYear = rawText.split('/')
                check(monthAndYear.size == 2) { "Invalid expiration date" }
                val mm = monthAndYear[0].toInt()
                val yy = monthAndYear[1].toInt()
                val yyyy = if (yy < 100) 2000 + yy else yy
                return Pair(mm, yyyy)
            }
        override val state
            get() = object : ElementState {
                fun valueNoThrow(): Pair<Int, Int>? {
                    try {
                        return value
                    } catch (e: Exception) {
                        return null
                    }
                }

                override val isEmpty get() = expirationEditText.text.toString().trim().isEmpty()
                override val isValid get() = valueNoThrow() != null
                override val isComplete get() = isValid
                override val validationError
                    get() =
                        if (isValid) {
                            null
                        } else {
                            ElementValidationError("Expiration date required")
                        }
            }
    }

    /** Returns the card security code. */
    val securityCode = object : ValueAndState<String> {
        override val value
            get() = securityCodeEditText.text.toString().trim()
        override val state
            get() = object : ElementState {
                override val isEmpty get() = value.isEmpty()
                override val isValid get() = !isEmpty
                override val isComplete get() = !isEmpty
                override val validationError
                    get() =
                        if (isValid) {
                            null
                        } else {
                            ElementValidationError("CVV required")
                        }
            }
    }

    /** Returns the cardholder ZIP code. */
    val zipCode = object : ValueAndState<String> {
        override val value
            get() = zipCodeEditText.text.toString().trim()
        override val state
            get() = object : ElementState {
                override val isEmpty get() = value.isEmpty()
                override val isValid get() = !isEmpty
                override val isComplete get() = !isEmpty
                override val validationError
                    get() =
                        if (isValid) {
                            null
                        } else {
                            ElementValidationError("ZIP code required")
                        }
            }
    }

    override fun setForageConfig(forageConfig: ForageConfig) {
        this._forageConfig = forageConfig
    }

    private var _forageConfig: ForageConfig? = null
    internal fun getForageConfig() = _forageConfig

    override var typeface: Typeface?
        get() = TODO("Not yet implemented")
        set(value) {
            TODO("Not yet implemented")
        }

    override fun clearText() {
        TODO("Not yet implemented")
    }

    override fun setTextColor(textColor: Int) {
        TODO("Not yet implemented")
    }

    override fun setTextSize(textSize: Float) {
        TODO("Not yet implemented")
    }

    override fun getElementState(): ForagePaymentSheetElementState =
        ForagePaymentSheetElementState()

    override fun setOnChangeEventListener(l: StatefulElementListener<ForagePaymentSheetElementState>) {
        onChangeEventListener = l
    }

    inner class ForagePaymentSheetElementState : ElementState {
        private val ALL_TEXTVIEW_CONTROLS = mapOf(
            R.id.cardholderNameEditText to cardholderName,
            R.id.cardNumberEditText to cardNumber,
            R.id.expirationEditText to expiration,
            R.id.securityCodeEditText to securityCode,
            R.id.zipCodeEditText to zipCode,
        )

        override val isEmpty get() = ALL_TEXTVIEW_CONTROLS.values.all { it.state.isEmpty }
        override val isValid get() = ALL_TEXTVIEW_CONTROLS.values.all { it.state.isValid }
        override val isComplete get() = ALL_TEXTVIEW_CONTROLS.values.all { it.state.isComplete }
        override val validationError: ElementValidationError?
            get() = ALL_TEXTVIEW_CONTROLS.values.map { it.state.validationError }.firstOrNull()
    }
}
