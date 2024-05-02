package com.joinforage.forage.android.core

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.ecom.ui.ForagePANEditText
import com.joinforage.forage.android.ecom.ui.ForagePINEditText

/**
 * An [Exception](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-exception/) thrown if a
 * reference to a [ForageElement][com.joinforage.forage.android.ui.ForageElement] is passed to a
 * method before [setForageConfig][com.joinforage.forage.android.ui.ForageElement.setForageConfig]
 * is called on the Element.
 * @property message A string that describes the Exception.
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
     * Checks the balance of a given PaymentMethod via ForagePINEditText
     *
     * @param params The parameters required for balance inquiries, including
     * a reference to a ForagePINEditText and PaymentMethod ref
     *
     * @return A ForageAPIResponse indicating the success or failure of the operation.
     * On success, returns an object with `snap` and `cash` fields, whose values
     * indicate the current balance of each tender as of now
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
     * Collects a card PIN for an EBT payment and defers the
     * capture of the payment to the server
     *
     * @param params The parameters required for pin capture, including
     * a reference to a ForagePINEditText and a Payment ref
     *
     * @return A ForageAPIResponse indicating the success or failure of the
     * PIN capture. On success, returns `Nothing`.
     * On failure, provides a detailed error response.
     *
     * @throws ForageConfigNotSetException If the passed ForagePINEditText instance
     * hasn't had its ForageConfig set via .setForageConfig().
     */
    suspend fun deferPaymentCapture(params: DeferPaymentCaptureParams): ForageApiResponse<String>
}

/**
 * A model that represents the parameters that Forage requires to tokenize an EBT Card.
 * [TokenizeEBTCardParams] are passed to the
 * [tokenizeEBTCard][com.joinforage.forage.android.ForageSDK.tokenizeEBTCard] method.
 *
 * @property foragePanEditText A reference to a [ForagePANEditText] instance.
 * [setForageConfig][com.joinforage.forage.android.ui.ForageElement.setForageConfig] must
 * be called on the instance before it can be passed.
 * @property customerId A unique ID for the customer making the payment.
 * If using your internal customer ID, then we recommend that you hash the value
 * before sending it on the payload.
 * @property reusable An optional boolean value that indicates whether the same card can be used
 * to create multiple payments, set to true by default.
 */
data class TokenizeEBTCardParams(
    val foragePanEditText: ForagePANEditText,
    val customerId: String? = null,
    val reusable: Boolean = true
)

/**
 * A model that represents the parameters that Forage requires to check a card's balance.
 * [CheckBalanceParams] are passed to the
 * [checkBalance][com.joinforage.forage.android.ForageSDK.checkBalance] method.
 *
 * @property foragePinEditText A reference to a [ForagePINEditText] instance.
 * [setForageConfig][com.joinforage.forage.android.ui.ForageElement.setForageConfig] must
 * be called on the instance before it can be passed.
 * @property paymentMethodRef A unique string identifier for a previously created
 * [`PaymentMethod`](https://docs.joinforage.app/reference/payment-methods) in Forage's database,
 * found in the response from a call to
 * [tokenizeEBTCard][com.joinforage.forage.android.ForageSDK.tokenizeEBTCard] (online-only),
 * [tokenizeCard][com.joinforage.forage.android.pos.ForageTerminalSDK.tokenizeCard] (POS),
 * or the [Create a `PaymentMethod`](https://docs.joinforage.app/reference/create-payment-method)
 * endpoint.
 */
data class CheckBalanceParams(
    val foragePinEditText: ForagePINEditText,
    val paymentMethodRef: String
)

/**
 * A model that represents the parameters that Forage requires to capture a payment.
 * [CapturePaymentParams] are passed to the
 * [capturePayment][com.joinforage.forage.android.ForageSDK.capturePayment] method.
 *
 * @property foragePinEditText A reference to a [ForagePINEditText] instance.
 * [setForageConfig][com.joinforage.forage.android.ui.ForageElement.setForageConfig] must
 * be called on the instance before it can be passed.
 * @property paymentRef A unique string identifier for a previously created
 * [`Payment`](https://docs.joinforage.app/reference/payments) in Forage's
 * database, returned by the
 * [Create a `Payment`](https://docs.joinforage.app/reference/create-a-payment) endpoint.
 */
data class CapturePaymentParams(
    val foragePinEditText: ForagePINEditText,
    val paymentRef: String
)

/**
 * A model that represents the parameters that Forage requires to collect a card PIN and defer
 * the capture of the payment to the server.
 * [DeferPaymentCaptureParams] are passed to the
 * [deferPaymentCapture][com.joinforage.forage.android.ForageSDK.deferPaymentCapture] method.
 *
 * @see * [Defer EBT payment capture to the server](https://docs.joinforage.app/docs/capture-ebt-payments-server-side)
 * for the related step-by-step guide.
 * * [Capture an EBT Payment](https://docs.joinforage.app/reference/capture-a-payment)
 * for the API endpoint to call after
 * [deferPaymentCapture][com.joinforage.forage.android.ForageSDK.deferPaymentCapture].
 *
 * @property foragePinEditText A reference to a [ForagePINEditText] instance.
 * [setForageConfig][com.joinforage.forage.android.ui.ForageElement.setForageConfig] must
 * be called on the instance before it can be passed.
 * @property paymentRef A unique string identifier for a previously created
 * [`Payment`](https://docs.joinforage.app/reference/payments) in Forage's
 * database, returned by the
 * [Create a `Payment`](https://docs.joinforage.app/reference/create-a-payment) endpoint.
 */
data class DeferPaymentCaptureParams(
    val foragePinEditText: ForagePINEditText,
    val paymentRef: String
)
