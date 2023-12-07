package com.joinforage.forage.android

import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.ui.ForagePANEditText
import com.joinforage.forage.android.ui.ForagePINEditText

/**
 * Exception thrown when attempting to call pass a reference to a
 * ForageElement without first setting its ForageConfig via
 * `.setForageConfig()`.
 */
class ForageConfigNotSetException(override val message: String) : IllegalStateException(message)

/**
 * The Forage SDK public API.
 *
 * Provides a set of methods for interfacing with Forage's EBT infrastructure.
 * Use these methods in conjunction with the UI components ForagePANEditText
 * and ForagePINEditText.
 */
internal interface ForageSDKInterface {

    /**
     * A method to securely tokenize an EBT card via ForagePANEditText
     *
     * @param params The parameters required for tokenization, including
     * reference to a ForagePANEditText view for card input.
     *
     * @return A ForageAPIResponse indicating the success or failure of the operation.
     * On success, returns a [PaymentMethod](https://docs.joinforage.app/reference/create-payment-method)
     * token which can be securely stored and used for subsequent transactions. On failure,
     * returns a detailed error response for proper handling.
     *
     * @throws ForageConfigNotSetException If the passed ForagePANEditText instance
     * hasn't had its ForageConfig set via .setForageConfig().
     */
    suspend fun tokenizeEBTCard(params: TokenizeEBTCardParams): ForageApiResponse<String>

    /**
     * Checks the balance SNAP and EBT Cash balances of an EBT account via
     * ForagePINEditText
     *
     * @param params The parameters required for tokenization, including
     * reference to a ForagePINEditText and PaymentMethod ref
     *
     * @return A ForageAPIResponse indicating the success or failure of the operation.
     * On success, returns an object with `snap` and `cash` fields, whose values
     * indicate the balance of each tender as of now
     *
     * @throws ForageConfigNotSetException If the passed ForagePINEditText instance
     * hasn't had its ForageConfig set via .setForageConfig().
     */
    suspend fun checkBalance(params: CheckBalanceParams): ForageApiResponse<String>

    /**
     * Captures a Forage Payment associated with an EBT card
     *
     * @param params The parameters required for payment capture, including
     * reference to a ForagePINEditText and a Payment ref
     *
     * @return A ForageAPIResponse indicating the success or failure of the
     * payment capture. On success, returns a confirmation of the transaction.
     * On failure, provides a detailed error response.
     *
     * @throws ForageConfigNotSetException If the passed ForagePINEditText instance
     * hasn't had its ForageConfig set via .setForageConfig().
     */
    suspend fun capturePayment(params: CapturePaymentParams): ForageApiResponse<String>

    /**
     * Collects the pin for a Forage Payment associated with an EBT card
     *
     * @param params The parameters required for pin collection, including
     * reference to a ForagePINEditText and a Payment ref
     *
     * @return A ForageAPIResponse indicating the success or failure of the
     * TODO: WHAT DOES IT RETURN ON SUCCESS?
     * pin collection. On success, returns a confirmation of the transaction.
     * On failure, provides a detailed error response.
     *
     * @throws ForageConfigNotSetException If the passed ForagePINEditText instance
     * hasn't had its ForageConfig set via .setForageConfig().
     */
    suspend fun collectPin(params: CollectPinParams): ForageApiResponse<String>
}

/**
 * Data class representing the parameters required for tokenizing an EBT card.
 *
 * @property foragePanEditText A ForagePANEditText  UI component. Importantly,
 * you must have called .setForageConfig() already
 * @property customerId A unique identifier associated with a customer. This is
 * required by FNS for fraud detection purposes
 * @property reusable Indicates whether the tokenized card can be reused for
 * multiple transactions.
 */
data class TokenizeEBTCardParams(
    val foragePanEditText: ForagePANEditText,
    val customerId: String,
    val reusable: Boolean?
)

/**
 * Data class representing the parameters required for checking the balance of an EBT card.
 *
 * @property foragePinEditText A ForagePINEditText UI component. Importantly,
 * you must have called .setForageConfig() already
 * @property paymentMethodRef A tokenized reference to the EBT card
 * (PaymentMethod) whose balance is being checked.
 */
data class CheckBalanceParams(
    val foragePinEditText: ForagePINEditText,
    val paymentMethodRef: String
)

/**
 * Data class representing the parameters required for capturing a payment on an EBT card.
 *
 * @property foragePinEditText A UI ForagePINEditText UI component. Importantly,
 * you must have called .setForageConfig() already
 * @property paymentRef A reference to the intended payment transaction.
 */
data class CapturePaymentParams(
    val foragePinEditText: ForagePINEditText,
    val paymentRef: String
)

/**
 * Data class representing the parameters required for collecting the PIN for a payment on an EBT card.
 *
 * @property foragePinEditText A UI ForagePINEditText UI component. Importantly,
 * you must have called .setForageConfig() already
 * @property paymentRef A reference to the intended payment transaction.
 */
data class CollectPinParams(
    val foragePinEditText: ForagePINEditText,
    val paymentRef: String
)
