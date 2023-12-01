package com.joinforage.forage.android.core.element.state

import com.joinforage.forage.android.core.element.ElementValidationError
import com.joinforage.forage.android.core.element.IncompleteEbtPanError
import com.joinforage.forage.android.core.element.InvalidEbtPanError
import com.joinforage.forage.android.core.element.TooLongEbtPanError
import com.joinforage.forage.android.model.hasInvalidStateIIN
import com.joinforage.forage.android.model.isCorrectLength
import com.joinforage.forage.android.model.missingStateIIN
import com.joinforage.forage.android.model.queryForStateIIN
import com.joinforage.forage.android.model.tooLongForStateIIN
import com.joinforage.forage.android.model.tooShortForStateIIN

internal const val MIN_CARD_LENGTH = 16
internal const val MAX_CARD_LENGTH = 19

internal interface PanValidator {
    fun checkIfValid(cardNumber: String): Boolean
    fun checkIfComplete(cardNumber: String): Boolean
    fun checkForValidationError(cardNumber: String): ElementValidationError?
}

internal class StrictEbtValidator : PanValidator {
    override fun checkIfValid(cardNumber: String): Boolean {
        // we consider a user's input valid if it is too short to
        // contain a StateINN or if it contains a legitimate StateIIN
        // but is not greater than the require length for that StateIIN
        val definitelyInvalid = hasInvalidStateIIN(cardNumber) || tooLongForStateIIN(cardNumber)
        return missingStateIIN(cardNumber) || !definitelyInvalid
    }

    override fun checkIfComplete(cardNumber: String): Boolean {
        return checkIfValid(cardNumber) && isCorrectLength(cardNumber)
    }

    override fun checkForValidationError(cardNumber: String): ElementValidationError? {
        return if (cardNumber.isEmpty()) {
            null
        } else if (missingStateIIN(cardNumber)) {
            IncompleteEbtPanError
        } else if (hasInvalidStateIIN(cardNumber)) {
            InvalidEbtPanError
        } else if (tooShortForStateIIN(cardNumber)) {
            IncompleteEbtPanError
        } else if (tooLongForStateIIN(cardNumber)) {
            TooLongEbtPanError
        } else {
            null
        }
    }
}

internal open class WhitelistedCards(
    private val prefix: String,
    private val repeatCount: Int = 1
) : PanValidator {
    override fun checkIfValid(cardNumber: String): Boolean {
        // for example 444444 4444 4444 *** **
        // maps to 44444444444444*****, which could be
        // a whitelisted pattern
        return cardNumber.startsWith(prefix.repeat(repeatCount))
    }

    override fun checkIfComplete(cardNumber: String): Boolean {
        return checkIfValid(cardNumber) && cardNumber.length in MIN_CARD_LENGTH..MAX_CARD_LENGTH
    }

    override fun checkForValidationError(cardNumber: String): ElementValidationError? {
        // this is a developer-only validator and should remain
        // quiet by not returning a validationError
        return null
    }
}

internal class PaymentCaptureErrorCard : WhitelistedCards("4", 14)
internal class BalanceCheckErrorCard : WhitelistedCards("5", 14)
internal class NonProdValidEbtCard : WhitelistedCards("9", 4)
internal class EmptyEbtCashBalanceCard : WhitelistedCards("654321")

internal class PanElementStateManager(
    state: PanElementState,
    private val validators: Array<PanValidator>
) : ElementStateManager<PanElementState>(state) {
    private var derivedCardInfo = DerivedCardInfoDto()

    private fun checkIsValid(cardNumber: String): Boolean {
        return validators.any { it.checkIfValid(cardNumber) }
    }
    private fun checkIfComplete(cardNumber: String): Boolean {
        return validators.any { it.checkIfComplete(cardNumber) }
    }
    private fun checkForValidationError(cardNumber: String): ElementValidationError? {
        return validators
            .map { it.checkForValidationError(cardNumber) }
            .firstOrNull { it != null }
    }
    private fun getDerivedCardInfo(cardNumber: String): DerivedCardInfoDto {
        return DerivedCardInfoDto(queryForStateIIN(cardNumber)?.publicEnum)
    }

    override fun getState(): PanElementState {
        return PanElementStateDto(
            isFocused = this.isFocused,
            isBlurred = this.isBlurred,
            isEmpty = this.isEmpty,
            isValid = this.isValid,
            isComplete = this.isComplete,
            validationError = this.validationError,
            derivedCardInfo = this.derivedCardInfo
        )
    }

    fun handleChangeEvent(rawInput: String) {
        // because the input may be formatted, we need to
        // strip everything but digits
        val newCardNumber = rawInput.filter { it.isDigit() }

        // check to see if any of the validators believe the
        // card to be valid
        isEmpty = newCardNumber.isEmpty()
        isValid = checkIsValid(newCardNumber)
        isComplete = checkIfComplete(newCardNumber)
        validationError = checkForValidationError(newCardNumber)

        // update state details based on newCardNumber
        derivedCardInfo = getDerivedCardInfo(newCardNumber)

        // invoke the registered listener with the updated state
        onChangeEventListener?.invoke(getState())
    }

    companion object {
        fun forEmptyInput(): PanElementStateManager {
            return PanElementStateManager(
                INITIAL_PAN_ELEMENT_STATE,
                arrayOf(StrictEbtValidator())
            )
        }

        fun NON_PROD_forEmptyInput(): PanElementStateManager {
            return PanElementStateManager(
                INITIAL_PAN_ELEMENT_STATE,
                arrayOf(
                    StrictEbtValidator(),
                    PaymentCaptureErrorCard(),
                    BalanceCheckErrorCard(),
                    NonProdValidEbtCard(),
                    EmptyEbtCashBalanceCard()
                )
            )
        }
    }
}
