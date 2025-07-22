package com.joinforage.forage.android.ecom.ui.element

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.method.DigitsKeyListener
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
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit

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
    ForageElement<ForagePaymentSheet.CombinedElementState>,
    DynamicEnvElement {
    private val cardholderNameEditText: TextInputEditText
    private val cardNumberEditText: TextInputEditText
    private val expirationEditText: TextInputEditText
    private val securityCodeEditText: TextInputEditText
    private val zipCodeEditText: TextInputEditText
    private val allEditText: Iterable<TextInputEditText>

    private var onChangeEventListener: StatefulElementListener<CombinedElementState>? = null

    init {
        inflate(context, R.layout.forage_payment_sheet, this)

        cardholderNameEditText = findViewById(R.id.cardholderNameEditText)
        cardNumberEditText = findViewById(R.id.cardNumberEditText)
        expirationEditText = findViewById(R.id.expirationEditText)
        securityCodeEditText = findViewById(R.id.securityCodeEditText)
        zipCodeEditText = findViewById(R.id.zipCodeEditText)

        allEditText = setOf(
            cardholderNameEditText,
            cardNumberEditText,
            expirationEditText,
            securityCodeEditText,
            zipCodeEditText
        )

        val expirationTextWatcher = ExpirationTextWatcher(expirationEditText)
        expirationEditText.addTextChangedListener(expirationTextWatcher)
        expirationTextWatcher.onFormattedChangeEvent {
            onChangeEventListener?.invoke(CombinedElementState())
        }

        val formatPanTextWatcher = FormatPanTextWatcher(cardNumberEditText)
        cardNumberEditText.addTextChangedListener(formatPanTextWatcher)
        formatPanTextWatcher.onFormattedChangeEvent {
            onChangeEventListener?.invoke(CombinedElementState())
        }

        val textWatcher = object : TextWatcherAdapter() {
            override fun afterTextChanged(editable: Editable) {
                onChangeEventListener?.invoke(CombinedElementState())
            }
        }

        cardholderNameEditText.addTextChangedListener(textWatcher)
        securityCodeEditText.addTextChangedListener(textWatcher)
        zipCodeEditText.addTextChangedListener(textWatcher)

        cardNumberEditText.keyListener = DigitsKeyListener.getInstance("0123456789 ")
        expirationEditText.keyListener = DigitsKeyListener.getInstance("0123456789/")
        zipCodeEditText.keyListener = DigitsKeyListener.getInstance("0123456789-")
    }

    internal val cardholderNameValue: String get() = cardholderNameEditText.text.toString().trim()
    internal val cardNumberValue: String get() = cardNumberEditText.text.toString().replace(" ", "")
    internal val expirationValueAsString get() = expirationEditText.text.toString().trim()
    internal val expirationValue: Pair<Int, Int> get() = parseExpirationValue(expirationValueAsString)
    internal val securityCodeValue: String get() = securityCodeEditText.text.toString().trim()
    internal val zipCodeValue get() = zipCodeEditText.text.toString().trim()

    override fun setForageConfig(forageConfig: ForageConfig) {
        this._forageConfig = forageConfig
    }

    private var _forageConfig: ForageConfig? = null
    internal fun getForageConfig() = _forageConfig

    override var typeface: Typeface?
        get() = cardNumberEditText.typeface
        set(value) {
            if (value != null) {
                allEditText.forEach { it.typeface = value }
            }
        }

    override fun clearText() {
        allEditText.forEach { it.setText("") }
    }

    override fun setTextColor(textColor: Int) {
        allEditText.forEach { it.setTextColor(textColor) }
    }

    override fun setTextSize(textSize: Float) {
        allEditText.forEach { it.textSize = textSize }
    }

    override fun getElementState() = CombinedElementState()

    override fun setOnChangeEventListener(l: StatefulElementListener<CombinedElementState>) {
        onChangeEventListener = l
    }

    abstract class ElementStateAdapter : ElementState {
        override fun toString() = "$isEmpty $isValid $isComplete ${validationError?.detail}"
    }

    inner class CombinedElementState : ElementStateAdapter() {
        val cardholderNameState = CardholderNameElementState()
        val cardNumberState = CardNumberElementState()
        val expirationState = ExpirationElementState()
        val securityCodeState = SecurityCodeElementState()
        val zipCodeState = ZipCodeElementState()

        private val components = setOf(cardholderNameState, cardNumberState, expirationState, securityCodeState, zipCodeState)

        override val isEmpty get() = components.all { it.isEmpty }
        override val isValid get() = components.all { it.isValid }
        override val isComplete get() = components.all { it.isComplete }
        override val validationError get() = components.firstNotNullOfOrNull { it.validationError }
    }

    inner class CardholderNameElementState : ElementStateAdapter() {
        private val value = cardholderNameValue

        override val isEmpty get() = value.isEmpty()
        override val isValid get() = !isEmpty
        override val isComplete get() = isValid
        override val validationError get() = (if (isComplete) null else CARDHOLDER_NAME_VALIDATION_ERROR)
    }

    inner class CardNumberElementState : ElementStateAdapter() {
        private val value = cardNumberValue

        override val isEmpty get() = value.isEmpty()
        override val isValid get() = CARD_NUMBER_VALID_REGEX.matches(value)
        override val isComplete
            get() = value.length == CARD_NUMBER_COMPLETE_LENGTH && LuhnCheckDigit().isValid(value)
        override val validationError get() = (if (isComplete) null else CARD_NUMBER_VALIDATION_ERROR)
    }

    inner class ExpirationElementState : ElementStateAdapter() {
        private val value = expirationValueAsString

        override val isEmpty get() = value.isEmpty()
        override val isValid get() = EXPIRATION_VALID_REGEX.matches(value)
        override val isComplete get() = EXPIRATION_COMPLETE_REGEX.matches(value)
        override val validationError get() = (if (isComplete) null else EXPIRATION_VALIDATION_ERROR)
    }

    inner class SecurityCodeElementState : ElementStateAdapter() {
        private val value = securityCodeValue

        override val isEmpty get() = value.isEmpty()
        override val isValid get() = SECURITY_CODE_VALID_REGEX.matches(value)
        override val isComplete get() = SECURITY_CODE_COMPLETE_REGEX.matches(value)
        override val validationError get() = (if (isComplete) null else SECURITY_CODE_VALIDATION_ERROR)
    }

    inner class ZipCodeElementState : ElementStateAdapter() {
        private val value = zipCodeValue

        override val isEmpty get() = value.isEmpty()
        override val isValid get() = ZIPCODE_VALID_REGEX.matches(value)
        override val isComplete get() = ZIPCODE_COMPLETE_REGEX.matches(value)
        override val validationError get() = (if (isComplete) null else ZIP_CODE_VALIDATION_ERROR)
    }

    companion object {
        private val CARDHOLDER_NAME_VALIDATION_ERROR = ElementValidationError("Cardholder name required")

        private val CARD_NUMBER_VALID_REGEX = Regex("^\\d{1,16}$")
        private const val CARD_NUMBER_COMPLETE_LENGTH = 16
        private val CARD_NUMBER_VALIDATION_ERROR = ElementValidationError("Card number required")

        private val EXPIRATION_VALID_REGEX = Regex("^\\d{1,2}/?\\d{0,2}$")
        private val EXPIRATION_COMPLETE_REGEX = Regex("^\\d{1,2}/\\d{1,2}$")
        private val EXPIRATION_VALIDATION_ERROR = ElementValidationError("Expiration date required")

        private val SECURITY_CODE_VALID_REGEX = Regex("^\\d{1,3}$")
        private val SECURITY_CODE_COMPLETE_REGEX = Regex("^\\d{3}$")
        private val SECURITY_CODE_VALIDATION_ERROR = ElementValidationError("CVV required")

        private val ZIPCODE_VALID_REGEX = Regex("^\\d{1,4}|(\\d{5}(-\\d{0,4})?)$")
        private val ZIPCODE_COMPLETE_REGEX = Regex("^\\d{5}(-\\d{4})?$")
        private val ZIP_CODE_VALIDATION_ERROR = ElementValidationError("ZIP code required")

        fun parseExpirationValue(expirationValueAsString: String): Pair<Int, Int> {
            val monthAndYear = expirationValueAsString.split('/')
            check(monthAndYear.size == 2) { "Invalid expiration date" }
            val mm = monthAndYear[0].toInt()
            val yy = monthAndYear[1].toInt()
            val yyyy = if (yy < 100) 2000 + yy else yy
            return Pair(mm, yyyy)
        }
    }
}
