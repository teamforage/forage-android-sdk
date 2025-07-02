package com.joinforage.forage.android.ecom.ui.element

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.textfield.TextInputEditText
import com.joinforage.forage.android.R
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.ui.element.DynamicEnvElement
import com.joinforage.forage.android.core.ui.textwatcher.ExpirationTextWatcher
import com.joinforage.forage.android.core.ui.textwatcher.FormatPanTextWatcher

class ForagePaymentSheet @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.foragePaymentSheetStyle
) : ConstraintLayout(context, attrs, defStyleAttr), DynamicEnvElement {
    private val rootView: ForagePaymentSheet =
        inflate(context, R.layout.fragment_forage_payment_sheet, this) as ForagePaymentSheet

    private val cardholderNameEditText: TextInputEditText get() = rootView.findViewById(R.id.cardholderNameEditText)
    private val cardNumberEditText: TextInputEditText get() = rootView.findViewById(R.id.cardNumberEditText)
    private val expirationEditText: TextInputEditText get() = rootView.findViewById(R.id.expirationEditText)
    private val securityCodeEditText: TextInputEditText get() = rootView.findViewById(R.id.securityCodeEditText)
    private val zipCodeEditText: TextInputEditText get() = rootView.findViewById(R.id.zipCodeEditText)

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
    }

    val cardholderName get() = cardholderNameEditText.text.toString()

    val cardNumber: String
        get() {
            val rawText = cardNumberEditText.text.toString()

            // Remove formatting
            val digitsOnly = rawText.filter { it.isDigit() }
            return digitsOnly
        }

    val expiration: Pair<Int, Int>
        get() {
            val rawText = expirationEditText.text.toString()
            val monthAndYear = rawText.split('/')
            check(monthAndYear.size == 2) { "Invalid expiration date" }
            val mm = monthAndYear[0].toInt()
            val yy = monthAndYear[1].toInt()
            val yyyy = if (yy < 100) 2000 + yy else yy
            return Pair(mm, yyyy)
        }

    val securityCode get() = securityCodeEditText.text.toString()

    val zipCode get() = zipCodeEditText.text.toString()

    override fun setForageConfig(forageConfig: ForageConfig) { this._forageConfig = forageConfig }

    private var _forageConfig: ForageConfig? = null
    internal fun getForageConfig() = _forageConfig
}
