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
    val usState: USState? = queryForStateIIN(normalizedPanText)?.publicEnum

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
