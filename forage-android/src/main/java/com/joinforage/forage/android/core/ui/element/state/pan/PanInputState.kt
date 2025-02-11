package com.joinforage.forage.android.core.ui.element.state.pan

import com.joinforage.forage.android.core.ui.element.ElementValidationError

internal class PanInputState(
    rawPanText: String,
    private val validators: Array<PanValidator>
) {
    // because the input may be formatted, we need to
    // strip everything but digits
    private val normalizedPanText: String = rawPanText.filter { it.isDigit() }

    val isEmpty: Boolean = normalizedPanText.isEmpty()
    val isValid: Boolean = validators.any {
        it.checkIfValid(normalizedPanText)
    }
    val isComplete: Boolean = validators.any {
        it.checkIfComplete(normalizedPanText)
    }
    val validationError: ElementValidationError? = validators
        .map { it.checkForValidationError(normalizedPanText) }
        .firstOrNull { it != null }

    // Some states, like Maine, issue PANs with differing
    // lengths (e.g. 16 and 19). It's Maine in both cases so
    // doing .firstOrNull supports() is sufficient
    val usState: USState? = queryForStateIIN(normalizedPanText).firstOrNull()?.publicEnum

    fun handleChangeEvent(newPanText: String) = PanInputState(
        newPanText,
        validators
    )

    companion object {
        fun forEmptyInput() = PanInputState("", arrayOf(StrictEbtValidator()))

        fun NON_PROD_forEmptyInput() = PanInputState(
            "",
            arrayOf(
                StrictEbtValidator(),
                PaymentCaptureErrorCard(),
                BalanceCheckErrorCard(),
                NonProdValidEbtCard(),
                EmptyEbtCashBalanceCard(),
                PinEncryptionTestCard()
            )
        )
    }
}
