package com.joinforage.forage.android.ecom.ui.element

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.method.DigitsKeyListener
import android.util.AttributeSet
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.annotation.VisibleForTesting
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.getSystemService
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.joinforage.forage.android.R
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.ui.element.CardExpiredError
import com.joinforage.forage.android.core.ui.element.CardNumberInvalidError
import com.joinforage.forage.android.core.ui.element.CardholderNameInvalidError
import com.joinforage.forage.android.core.ui.element.DynamicEnvElement
import com.joinforage.forage.android.core.ui.element.EditTextElement
import com.joinforage.forage.android.core.ui.element.ElementValidationError
import com.joinforage.forage.android.core.ui.element.ExpirationDateInvalidError
import com.joinforage.forage.android.core.ui.element.ForageElement
import com.joinforage.forage.android.core.ui.element.SecurityCodeInvalidError
import com.joinforage.forage.android.core.ui.element.SimpleElementListener
import com.joinforage.forage.android.core.ui.element.StatefulElementListener
import com.joinforage.forage.android.core.ui.element.ZipCodeInvalidError
import com.joinforage.forage.android.core.ui.element.state.ElementState
import com.joinforage.forage.android.core.ui.element.state.FocusState
import com.joinforage.forage.android.core.ui.textwatcher.ExpirationTextWatcher
import com.joinforage.forage.android.core.ui.textwatcher.FormatPanTextWatcher
import com.joinforage.forage.android.core.ui.textwatcher.TextWatcherAdapter
import com.joinforage.forage.android.ecom.services.ForageSDK
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit
import java.util.Calendar
import java.util.TimeZone

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

    private val allFields: Iterable<ForagePaymentSheetField>

    val cardholderNameField: CardholderNameField
    val cardNumberField: CardNumberField
    val expirationField: ExpirationField
    val securityCodeField: SecurityCodeField
    val zipCodeField: ZipCodeField

    init {
        inflate(context, R.layout.forage_payment_sheet, this)

        val cardholderNameEditText: TextInputEditText = findViewById(R.id.cardholderNameEditText)
        val cardholderNameLayout: TextInputLayout = findViewById(R.id.cardholderNameLayout)
        val cardholderNameBorder = findViewById<LinearLayout?>(R.id.cardholderNameBorder).background as GradientDrawable
        cardholderNameField = CardholderNameField(cardholderNameEditText, cardholderNameLayout, cardholderNameBorder)

        val cardNumberEditText: TextInputEditText = findViewById(R.id.cardNumberEditText)
        val cardNumberLayout: TextInputLayout = findViewById(R.id.cardNumberLayout)
        val cardNumberBorder: GradientDrawable = findViewById<LinearLayout?>(R.id.cardNumberBorder).background as GradientDrawable
        cardNumberField = CardNumberField(cardNumberEditText, cardNumberLayout, cardNumberBorder)

        val expirationEditText: TextInputEditText = findViewById(R.id.expirationEditText)
        val expirationLayout: TextInputLayout = findViewById(R.id.expirationLayout)
        val expirationBorder: GradientDrawable = findViewById<LinearLayout?>(R.id.expirationBorder).background as GradientDrawable
        expirationField = ExpirationField(expirationEditText, expirationLayout, expirationBorder)

        val securityCodeEditText: TextInputEditText = findViewById(R.id.securityCodeEditText)
        val securityCodeLayout: TextInputLayout = findViewById(R.id.securityCodeLayout)
        val securityCodeBorder: GradientDrawable = findViewById<LinearLayout?>(R.id.securityCodeBorder).background as GradientDrawable
        securityCodeField = SecurityCodeField(securityCodeEditText, securityCodeLayout, securityCodeBorder)

        val zipCodeEditText: TextInputEditText = findViewById(R.id.zipCodeEditText)
        val zipCodeLayout: TextInputLayout = findViewById(R.id.zipCodeLayout)
        val zipCodeBorder: GradientDrawable = findViewById<LinearLayout?>(R.id.zipCodeBorder).background as GradientDrawable
        zipCodeField = ZipCodeField(zipCodeEditText, zipCodeLayout, zipCodeBorder)

        allFields = setOf(
            cardholderNameField,
            cardNumberField,
            expirationField,
            securityCodeField,
            zipCodeField
        )
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        cardholderNameField.requestFocus()
        cardholderNameField.showKeyboard()
    }

    override fun setForageConfig(forageConfig: ForageConfig) {
        this._forageConfig = forageConfig
    }

    private var _forageConfig: ForageConfig? = null
    internal fun getForageConfig() = _forageConfig

    override var typeface: Typeface?
        get() = cardNumberField.typeface
        set(value) {
            if (value != null) {
                allFields.forEach { it.typeface = value }
            }
        }

    override fun clearText() {
        allFields.forEach { it.clearText() }
    }

    override fun setTextColor(textColor: Int) {
        allFields.forEach { it.setTextColor(textColor) }
    }

    override fun setTextSize(textSize: Float) {
        allFields.forEach { it.setTextSize(textSize) }
    }

    override fun getElementState() = CombinedElementState()

    override fun setOnChangeEventListener(l: StatefulElementListener<CombinedElementState>) {
        allFields.forEach { it.setOnChangeEventListener { l.invoke(getElementState()) } }
    }

    inner class CombinedElementState : ElementStateAdapter() {
        val cardholderNameState = cardholderNameField.getElementState()
        val cardNumberState = cardNumberField.getElementState()
        val expirationState = expirationField.getElementState()
        val securityCodeState = securityCodeField.getElementState()
        val zipCodeState = zipCodeField.getElementState()

        private val allElementStates = setOf(cardholderNameState, cardNumberState, expirationState, securityCodeState, zipCodeState)

        override val isEmpty get() = allElementStates.all { it.isEmpty }
        override val isValid get() = allElementStates.all { it.isValid }
        override val isComplete get() = allElementStates.all { it.isComplete }
        override val validationError get() = allElementStates.firstNotNullOfOrNull { it.validationError }
    }
}

abstract class ElementStateAdapter : ElementState {
    override fun toString() = "$isEmpty $isValid $isComplete ${validationError?.detail}"
}

abstract class ForagePaymentSheetField(
    private val editText: TextInputEditText,
    private val layout: TextInputLayout,
    private val border: GradientDrawable
) : ForageElement<ElementState>, EditTextElement {
    protected var _onChangeEventListener: StatefulElementListener<ElementState>? = null

    private var onFocusEventListener: SimpleElementListener? = null
    private var onBlurEventListener: SimpleElementListener? = null
    private var focusState = FocusState.forEmptyInput()

    //
    // There's no getStroke() method on GradientDrawable so we have to cache initial values.
    //
    @VisibleForTesting internal var boxStrokeColor = editText.resources.getColor(R.color.editable_frame_color)

    @VisibleForTesting internal var boxStrokeWidth = editText.resources.getDimensionPixelSize(R.dimen.editable_frame_width)

    private fun restartFocusChangeListener() {
        editText.setOnFocusChangeListener { _, hasFocus ->
            focusState = focusState.changeFocus(hasFocus)
            focusState.fireEvent(
                onFocusEventListener = onFocusEventListener,
                onBlurEventListener = onBlurEventListener
            )
        }
    }

    override var typeface: Typeface?
        get() = editText.typeface
        set(value) { editText.typeface = value }

    override fun clearText() {
        editText.setText("")
    }

    override fun setTextColor(textColor: Int) {
        editText.setTextColor(textColor)
    }

    override fun setTextSize(textSize: Float) {
        editText.textSize = textSize
    }

    override fun setOnChangeEventListener(l: StatefulElementListener<ElementState>) {
        _onChangeEventListener = l
    }

    override fun showKeyboard() {
        // See https://developer.android.com/develop/ui/views/touch-and-input/keyboard-input/visibility#ShowReliably
        editText.post {
            val imm = editText.context.getSystemService<InputMethodManager>()
            imm?.showSoftInput(editText, 0)
        }
    }

    override fun setOnFocusEventListener(l: SimpleElementListener) {
        onFocusEventListener = l
        restartFocusChangeListener()
    }

    override fun setOnBlurEventListener(l: SimpleElementListener) {
        onBlurEventListener = l
        restartFocusChangeListener()
    }

    override fun setHint(hint: String) {
        layout.hint = hint
    }

    override fun setHintTextColor(@ColorInt hintTextColor: Int) {
        val colorStateList = ColorStateList.valueOf(hintTextColor)
        layout.hintTextColor = colorStateList
        layout.defaultHintTextColor = colorStateList
    }

    override fun setBoxStrokeColor(@ColorInt boxStrokeColor: Int) {
        this.boxStrokeColor = boxStrokeColor
        border.setStroke(boxStrokeWidth, boxStrokeColor)
    }

    override fun setBoxStrokeWidth(boxStrokeWidth: Int) {
        this.boxStrokeWidth = boxStrokeWidth
        border.setStroke(boxStrokeWidth, boxStrokeColor)
    }

    // Leave unimplemented
    override fun setBoxStrokeWidthFocused(boxStrokeWidthFocused: Int) {}

    fun requestFocus(): Boolean {
        return editText.requestFocus()
    }
}

class CardholderNameElementState(private val value: String) : ElementStateAdapter() {
    override val isEmpty get() = value.isEmpty()
    override val isValid get() = !isEmpty
    override val isComplete get() = isValid
    override val validationError get() = (if (isComplete) null else CardholderNameInvalidError)
}

class CardholderNameField(
    internal val editText: TextInputEditText,
    internal val layout: TextInputLayout,
    internal val border: GradientDrawable
) : ForagePaymentSheetField(
    editText,
    layout,
    border
) {
    var value: String
        get() = editText.text.toString().trim()
        set(value) = editText.setText(value)

    init {
        val textWatcher = object : TextWatcherAdapter() {
            override fun afterTextChanged(editable: Editable) {
                _onChangeEventListener?.invoke(getElementState())
            }
        }

        editText.addTextChangedListener(textWatcher)
    }

    override fun getElementState(): CardholderNameElementState {
        return CardholderNameElementState(value)
    }
}

class CardNumberElementState(private val value: String) : ElementStateAdapter() {
    override val isEmpty get() = value.isEmpty()
    override val isValid get() = CARD_NUMBER_VALID_REGEX.matches(value)
    override val isComplete get() = validationError == null
    override val validationError get() = validate(value)

    private fun validate(value: String): ElementValidationError? {
        return if (value.length == CARD_NUMBER_COMPLETE_LENGTH && LuhnCheckDigit().isValid(value)) {
            null
        } else {
            CardNumberInvalidError
        }
    }

    companion object {
        private val CARD_NUMBER_VALID_REGEX = Regex("^\\d{1,16}$")
        private const val CARD_NUMBER_COMPLETE_LENGTH = 16
    }
}

class CardNumberField(
    internal val editText: TextInputEditText,
    layout: TextInputLayout,
    border: GradientDrawable
) : ForagePaymentSheetField(
    editText,
    layout,
    border
) {
    init {
        val formatPanTextWatcher = FormatPanTextWatcher(editText)
        editText.addTextChangedListener(formatPanTextWatcher)
        formatPanTextWatcher.onFormattedChangeEvent {
            _onChangeEventListener?.invoke(getElementState())
        }

        editText.keyListener = DigitsKeyListener.getInstance("0123456789 ")
    }

    var value: String
        get() = editText.text.toString().replace(" ", "")
        set(value) = editText.setText(value)

    override fun getElementState(): CardNumberElementState {
        return CardNumberElementState(value)
    }
}

class ExpirationElementState(private val value: String) : ElementStateAdapter() {
    override val isEmpty get() = value.isEmpty()
    override val isValid get() = EXPIRATION_VALID_REGEX.matches(value)
    override val isComplete get() = validationError == null
    override val validationError get() = validate(value)

    private fun validate(value: String): ElementValidationError? {
        if (!EXPIRATION_COMPLETE_REGEX.matches(value)) {
            return ExpirationDateInvalidError
        }

        val mmYyyy = ExpirationField.parseExpirationValue(value)
        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        if (!now.before(firstDayOfFollowingMonth(mmYyyy))) {
            return CardExpiredError
        }

        return null
    }

    companion object {
        private val EXPIRATION_VALID_REGEX = Regex("^\\d{1,2}/?\\d{0,2}$")
        private val EXPIRATION_COMPLETE_REGEX = Regex("^\\d{1,2}/\\d{1,2}$")

        @VisibleForTesting
        internal fun firstDayOfFollowingMonth(mmYyyy: Pair<Int, Int>): Calendar {
            //
            // Our SDK level is 21 so we have to do this the hard way.
            //

            val result: Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            result.clear()

            // The month value is zero-based so the "next month" is the exact value passed in.
            // The Calendar class will wrap the month and advance the year automatically if needed.
            result.set(mmYyyy.second, mmYyyy.first, 1, 0, 0, 0)

            return result
        }
    }
}

class ExpirationField(
    internal val editText: TextInputEditText,
    layout: TextInputLayout,
    border: GradientDrawable
) : ForagePaymentSheetField(
    editText,
    layout,
    border
) {
    internal var expirationValueAsString
        get() = editText.text.toString().trim()
        set(value) = editText.setText(value)

    var value: Pair<Int, Int>
        get() = parseExpirationValue(expirationValueAsString)
        set(value) { expirationValueAsString = formatExpirationValue(value) }

    override fun getElementState(): ExpirationElementState {
        return ExpirationElementState(expirationValueAsString)
    }

    init {
        val expirationTextWatcher = ExpirationTextWatcher(editText)
        editText.addTextChangedListener(expirationTextWatcher)
        expirationTextWatcher.onFormattedChangeEvent {
            _onChangeEventListener?.invoke(getElementState())
        }

        editText.keyListener = DigitsKeyListener.getInstance("0123456789/")
    }

    companion object {
        fun parseExpirationValue(expirationValueAsString: String): Pair<Int, Int> {
            val monthAndYear = expirationValueAsString.split('/')
            check(monthAndYear.size == 2) { "Invalid expiration date" }
            val mm = monthAndYear[0].toInt()
            val yy = monthAndYear[1].toInt()
            val yyyy = if (yy < 100) 2000 + yy else yy
            return Pair(mm, yyyy)
        }

        fun formatExpirationValue(value: Pair<Int, Int>): String {
            val (mm, yyyy) = value
            val yy = yyyy % 100
            return "$mm/$yy"
        }
    }
}

class SecurityCodeElementState(private val value: String) : ElementStateAdapter() {
    override val isEmpty get() = value.isEmpty()
    override val isValid get() = SECURITY_CODE_VALID_REGEX.matches(value)
    override val isComplete get() = validationError == null
    override val validationError get() = validate(value)

    private fun validate(value: String): ElementValidationError? {
        return if (SECURITY_CODE_COMPLETE_REGEX.matches(value)) {
            null
        } else {
            SecurityCodeInvalidError
        }
    }

    companion object {
        private val SECURITY_CODE_VALID_REGEX = Regex("^\\d{1,3}$")
        private val SECURITY_CODE_COMPLETE_REGEX = Regex("^\\d{3}$")
    }
}

class SecurityCodeField(
    internal val editText: TextInputEditText,
    layout: TextInputLayout,
    border: GradientDrawable
) : ForagePaymentSheetField(
    editText,
    layout,
    border
) {
    var value: String
        get() = editText.text.toString().trim()
        set(value) = editText.setText(value)

    override fun getElementState(): SecurityCodeElementState {
        return SecurityCodeElementState(value)
    }

    init {
        val textWatcher = object : TextWatcherAdapter() {
            override fun afterTextChanged(editable: Editable) {
                _onChangeEventListener?.invoke(getElementState())
            }
        }

        editText.addTextChangedListener(textWatcher)
    }
}

class ZipCodeElementState(private val value: String) : ElementStateAdapter() {
    override val isEmpty get() = value.isEmpty()
    override val isValid get() = ZIPCODE_VALID_REGEX.matches(value)
    override val isComplete get() = validationError == null
    override val validationError get() = validate(value)

    private fun validate(value: String): ElementValidationError? {
        return if (ZIPCODE_COMPLETE_REGEX.matches(value)) {
            null
        } else {
            ZipCodeInvalidError
        }
    }

    companion object {
        private val ZIPCODE_VALID_REGEX = Regex("^\\d{1,4}|(\\d{5}(-\\d{0,4})?)$")
        private val ZIPCODE_COMPLETE_REGEX = Regex("^\\d{5}(-\\d{4})?$")
    }
}

class ZipCodeField(
    internal val editText: TextInputEditText,
    layout: TextInputLayout,
    border: GradientDrawable
) : ForagePaymentSheetField(
    editText,
    layout,
    border
) {
    var value: String
        get() = editText.text.toString().trim()
        set(value) = editText.setText(value)

    override fun getElementState(): ZipCodeElementState {
        return ZipCodeElementState(value)
    }

    init {
        val textWatcher = object : TextWatcherAdapter() {
            override fun afterTextChanged(editable: Editable) {
                _onChangeEventListener?.invoke(getElementState())
            }
        }

        editText.addTextChangedListener(textWatcher)

        editText.keyListener = DigitsKeyListener.getInstance("0123456789-")
    }
}
