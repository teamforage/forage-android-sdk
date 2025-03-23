package com.joinforage.forage.android.ecom.services

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.ForageConfigNotSetException
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.payment.PaymentService
import com.joinforage.forage.android.ecom.services.forageapi.engine.EcomOkHttpEngine
import com.joinforage.forage.android.ecom.services.forageapi.paymentmethod.PaymentMethodService
import com.joinforage.forage.android.ecom.services.telemetry.EcomDatadogLoggerFactory
import com.joinforage.forage.android.ecom.services.vault.TokenizeCardService
import com.joinforage.forage.android.ecom.services.vault.submission.EcomBalanceCheckSubmission
import com.joinforage.forage.android.ecom.services.vault.submission.EcomCapturePaymentSubmission
import com.joinforage.forage.android.ecom.services.vault.submission.EcomDeferCapturePaymentSubmission
import com.joinforage.forage.android.ecom.ui.element.ForagePANEditText
import com.joinforage.forage.android.ecom.ui.element.ForagePINEditText

/**
 * The entry point to the Forage SDK.
 *
 * A [ForageSDK] instance interacts with the Forage API to process online-only payments.
 *
 * You need an instance of the ForageSDK to perform operations like:
 *
 * * [Tokenizing card information][tokenizeEBTCard]
 * * [Checking the balance of a card][checkBalance]
 * * [Collecting a card PIN for a payment and deferring
 * the capture of the payment to the server][deferPaymentCapture]
 * * [Capturing a payment immediately][capturePayment]
 *```kotlin
 * // Example: Create a ForageSDK instance
 * val forage = ForageSDK()
 * ```
 * @see * [Online-only Android Quickstart](https://docs.joinforage.app/docs/forage-android-quickstart)
 * * [ForageTerminalSDK](https://docs.joinforage.app/docs/forage-terminal-android) to process POS
 * Terminal transactions
 */
class ForageSDK {
    private val httpEngine = EcomOkHttpEngine()

    /**
     * Retrieves the ForageConfig for a given ForageElement, or throws an exception if the
     * ForageConfig is not set.
     *
     * @param forageConfig A [ForageConfig] instance
     * @return The [ForageConfig] associated with the ForageElement
     * @throws ForageConfigNotSetException If the ForageConfig is not set for the ForageElement
     */
    private fun _getForageConfigOrThrow(forageConfig: ForageConfig?): ForageConfig {
        return forageConfig ?: throw ForageConfigNotSetException(
            """
    The ForageElement you passed did not have a ForageConfig. In order to submit
    a request via Forage SDK, your ForageElement MUST have a ForageConfig.
    Make sure to call myForageElement.setForageConfig(forageConfig: ForageConfig) 
    immediately on your ForageElement 
            """.trimIndent()
        )
    }

    /**
     * Tokenizes an EBT Card via a
     * [ForagePANEditText][com.joinforage.forage.android.ecom.ui.element.ForagePANEditText] Element.
     *
     * * On success, the object includes a `ref` token that represents an instance of a Forage
     * [`PaymentMethod`](https://docs.joinforage.app/reference/payment-methods#paymentmethod-object).
     * You can store the token for future transactions, like to [`checkBalance`](checkBalance) or
     * to [create a `Payment`](https://docs.joinforage.app/reference/create-a-payment) in
     * Forage's database.
     * * On failure, for example in the case of
     * [`unsupported_bin`](https://docs.joinforage.app/reference/errors#unsupported_bin),
     * the response includes a list of
     * [ForageError][com.joinforage.forage.android.core.services.forageapi.network.ForageError] objects that you can
     * unpack to programmatically handle the error and display the appropriate
     * customer-facing message based on the `ForageError.code`.
     * ```kotlin
     * // Example tokenizeEBTCard call in a TokenizeViewModel.kt
     * class TokenizeViewModel : ViewModel() {
     *     val merchantId = "<merchant_id>"
     *     val sessionToken = "<session_token>"
     *
     *     fun tokenizeEBTCard(foragePanEditText: ForagePANEditText) = viewModelScope.launch {
     *         val response = ForageSDK().tokenizeEBTCard(
     *             TokenizeEBTCardParams(
     *                 foragePanEditText = foragePanEditText,
     *                 reusable = true,
     *                 customerId = "<hash_of_customer_id>"
     *             )
     *         )
     *
     *         when (response) {
     *             is ForageApiResponse.Success -> {
     *                 val paymentMethod = response.toPaymentMethod()
     *                 // Unpack paymentMethod.ref, paymentMethod.card, etc.
     *                 val card = paymentMethod.card
     *                 // Unpack card.last4, ...
     *                 if (card is EbtCard) {
     *                     // Unpack card.usState
     *                 }
     *             }
     *             is ForageApiResponse.Failure -> {
     *                 val error = response.error
     *                 // handle error.code here
     *             }
     *         }
     *     }
     * }
     * ```
     * @param params A [TokenizeEBTCardParams] model that passes a [`foragePanEditText`]
     * [com.joinforage.forage.android.ecom.ui.element.ForagePANEditText] instance, a `customerId`, and a `reusable`
     * boolean that Forage uses to tokenize an EBT Card.
     * @throws [ForageConfigNotSetException] If the [ForageConfig] is not set for the provided
     * `foragePanEditText`.
     * @see * [SDK errors](https://docs.joinforage.app/reference/errors#sdk-errors) for more
     * information on error handling.
     * @return A [ForageApiResponse] object. Use [toPaymentMethod()][ForageApiResponse.Success.toPaymentMethod] to
     * convert the `data` string to a [PaymentMethod][com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod].
     */
    suspend fun tokenizeEBTCard(params: TokenizeEBTCardParams): ForageApiResponse<String> {
        val (foragePanEditText, customerId, reusable) = params
        val forageConfig = _getForageConfigOrThrow(foragePanEditText.getForageConfig())
        val logger = EcomDatadogLoggerFactory(
            foragePanEditText.context,
            forageConfig,
            customerId
        ).makeLogger()
        val pmService = PaymentMethodService(
            forageConfig,
            logger.traceId,
            httpEngine
        )
        val tokenizeService = TokenizeCardService(
            logger,
            forageConfig,
            pmService
        )
        return tokenizeService.tokenizeCard(
            cardNumber = foragePanEditText.getPanNumber(),
            customerId = customerId,
            reusable = reusable ?: true
        )
    }

    /**
     * Checks the balance of a previously created
     * [`PaymentMethod`](https://docs.joinforage.app/reference/payment-methods)
     * via a [ForagePINEditText][com.joinforage.forage.android.ecom.ui.element.ForagePINEditText] Element.
     *
     * ⚠️ _FNS prohibits balance inquiries on sites and apps that offer guest checkout. Skip this
     * method if your customers can opt for guest checkout. If guest checkout is not an option, then
     * it's up to you whether or not to add a balance inquiry feature. No FNS regulations apply._
     * * On success, the response object includes `snap` and `cash` fields that indicate
     * the EBT Card's current SNAP and EBT Cash balances.
     * * On failure, for example in the case of
     * [`ebt_error_14`](https://docs.joinforage.app/reference/errors#ebt_error_14),
     * the response includes a list of
     * [ForageError][com.joinforage.forage.android.core.services.forageapi.network.ForageError] objects that you can
     * unpack to programmatically handle the error and display the appropriate
     * customer-facing message based on the `ForageError.code`.
     * ```kotlin
     * // Example checkBalance call in a BalanceCheckViewModel.kt
     * class BalanceCheckViewModel : ViewModel() {
     *     val paymentMethodRef = "020xlaldfh"
     *
     *     fun checkBalance(foragePinEditText: ForagePINEditText) = viewModelScope.launch {
     *         val response = ForageSDK().checkBalance(
     *             CheckBalanceParams(
     *                 foragePinEditText = foragePinEditText,
     *                 paymentMethodRef = paymentMethodRef
     *             )
     *         )
     *
     *         when (response) {
     *             is ForageApiResponse.Success -> {
     *                 val balance = response.toBalance()
     *                 if (balance is EbtBalance) {
     *                     // Unpack balance.snap, ebtBalance.cash
     *                 }
     *             }
     *             is ForageApiResponse.Failure -> {
     *                 val error = response.error
     *                 // handle error.code here
     *             }
     *         }
     *     }
     * }
     * ```
     * @param params A [CheckBalanceParams] model that passes
     * a [`foragePinEditText`][com.joinforage.forage.android.ui.ForagePINEditText] instance and a
     * `paymentMethodRef`, found in the response from a call to [tokenizeEBTCard] or the
     * [Create a `PaymentMethod`](https://docs.joinforage.app/reference/create-payment-method)
     * endpoint, that Forage uses to check the payment method's balance.
     *
     * @throws [ForageConfigNotSetException] If the [ForageConfig] is not set for the provided
     * `foragePinEditText`.
     * @see * [SDK errors](https://docs.joinforage.app/reference/errors#sdk-errors) for more
     * information on error handling.
     * * [Test EBT Cards](https://docs.joinforage.app/docs/test-ebt-cards#balance-inquiry-exceptions)
     * to trigger balance inquiry exceptions during testing.
     * @return A [ForageApiResponse] object. Use [toBalance()][ForageApiResponse.Success.toBalance]
     * to convert the `data` string to a [Balance][com.joinforage.forage.android.core.services.forageapi.paymentmethod.Balance].
     */
    suspend fun checkBalance(params: CheckBalanceParams): ForageApiResponse<String> {
        val (foragePinEditText, paymentMethodRef) = params
        val forageConfig = _getForageConfigOrThrow(foragePinEditText.getForageConfig())
        val logger = EcomDatadogLoggerFactory(
            foragePinEditText.context,
            forageConfig,
            null
        ).makeLogger()
        val pmService = PaymentMethodService(
            forageConfig,
            logger.traceId,
            httpEngine
        )
        return EcomBalanceCheckSubmission(
            paymentMethodRef = paymentMethodRef,
            vaultSubmitter = foragePinEditText.getVaultSubmitter(forageConfig.envConfig, httpEngine),
            paymentMethodService = pmService,
            forageConfig = forageConfig,
            logLogger = logger
        ).rawSubmit()
    }

    /**
     * Immediately captures a payment via a
     * [ForagePINEditText][com.joinforage.forage.android.ui.ForagePINEditText] Element.
     *
     * * On success, the object confirms the transaction. The response includes a Forage
     * [`Payment`](https://docs.joinforage.app/reference/payments) object.
     * * On failure, for example in the case of
     * [`card_not_reusable`](https://docs.joinforage.app/reference/errors#card_not_reusable) or
     * [`ebt_error_51`](https://docs.joinforage.app/reference/errors#ebt_error_51) errors, the
     * response includes a list of
     * [ForageError][com.joinforage.forage.android.core.services.forageapi.network.ForageError] objects that you can
     * unpack to programmatically handle the error and display the appropriate
     * customer-facing message based on the `ForageError.code`.
     * ```kotlin
     * // Example capturePayment call in a PaymentCaptureViewModel.kt
     * class PaymentCaptureViewModel : ViewModel() {
     *     val snapPaymentRef = "s0alzle0fal"
     *     val merchantId = "<merchant_id>"
     *     val sessionToken = "<session_token>"
     *
     *     fun capturePayment(foragePinEditText: ForagePINEditText, paymentRef: String) =
     *         viewModelScope.launch {
     *             val response = ForageSDK().capturePayment(
     *                 CapturePaymentParams(
     *                     foragePinEditText = foragePinEditText,
     *                     paymentRef = snapPaymentRef
     *                 )
     *             )
     *
     *             when (response) {
     *                 is ForageApiResponse.Success -> {
     *                     val payment = response.toPayment()
     *                     // Unpack payment.ref, payment.receipt, etc.
     *                 }
     *                 is ForageApiResponse.Failure -> {
     *                     val error = response.error
     *
     *                     // handle Insufficient Funds error
     *                     if (error.code == "ebt_error_51") {
     *                         val details = error.details as ForageErrorDetails.EbtError51Details
     *                         val (snapBalance, cashBalance) = details
     *
     *                         // display balance to the customer...
     *                     }
     *                 }
     *             }
     *         }
     * }
     *```
     * @param params A [CapturePaymentParams] model that passes a
     * [`foragePinEditText`][com.joinforage.forage.android.core.ui.element.ForagePinElement]
     * instance and a `paymentRef`, returned by the
     * [Create a Payment](https://docs.joinforage.app/reference/create-a-payment) endpoint, that
     * Forage uses to capture a payment.
     *
     * @throws [ForageConfigNotSetException] If the [ForageConfig] is not set for the provided
     * `foragePinEditText`.
     * @see
     * * [SDK errors](https://docs.joinforage.app/reference/errors#sdk-errors) for more information
     * on error handling.
     * * [Test EBT Cards](https://docs.joinforage.app/docs/test-ebt-cards#payment-capture-exceptions)
     * to trigger payment capture exceptions during testing.
     * @return A [ForageApiResponse] object. Use [toPayment()][ForageApiResponse.Success.toPayment] to convert the `data` string to a
     * [Payment][com.joinforage.forage.android.core.services.forageapi.payment.Payment].
     */
    suspend fun capturePayment(params: CapturePaymentParams): ForageApiResponse<String> {
        val (foragePinEditText, paymentRef) = params
        val forageConfig = _getForageConfigOrThrow(foragePinEditText.getForageConfig())
        val logger = EcomDatadogLoggerFactory(
            foragePinEditText.context,
            forageConfig,
            null
        ).makeLogger()
        val pmService = PaymentMethodService(
            forageConfig,
            logger.traceId,
            httpEngine
        )
        val paymentService = PaymentService(
            forageConfig,
            logger.traceId,
            httpEngine
        )
        return EcomCapturePaymentSubmission(
            paymentRef = paymentRef,
            vaultSubmitter = foragePinEditText.getVaultSubmitter(forageConfig.envConfig, httpEngine),
            paymentMethodService = pmService,
            paymentService = paymentService,
            forageConfig = forageConfig,
            logLogger = logger
        ).submit()
    }

    /**
     * Submits a card PIN via a
     * [ForagePINEditText][com.joinforage.forage.android.ui.ForagePINEditText] Element and defers
     * payment capture to the server.
     *
     * * On success, the `data` property of the [ForageApiResponse.Success] object resolves with an empty string.
     * * On failure, for example in the case of [`expired_session_token`](https://docs.joinforage.app/reference/errors#expired_session_token) errors, the
     * response includes a list of
     * [ForageError][com.joinforage.forage.android.core.services.forageapi.network.ForageError] objects that you can
     * unpack to programmatically handle the error and display the appropriate
     * customer-facing message based on the `ForageError.code`.
     * ```kotlin
     * // Example deferPaymentCapture call in a DeferPaymentCaptureViewModel.kt
     * class DeferPaymentCaptureViewModel  : ViewModel() {
     *     val snapPaymentRef = "s0alzle0fal"
     *     val merchantId = "<merchant_id>"
     *     val sessionToken = "<session_token>"
     *
     *     fun deferPaymentCapture(foragePinEditText: ForagePINEditText, paymentRef: String) =
     *         viewModelScope.launch {
     *             val response = ForageSDK().deferPaymentCapture(
     *                 DeferPaymentCaptureParams(
     *                     foragePinEditText = foragePinEditText,
     *                     paymentRef = snapPaymentRef
     *                 )
     *             )
     *
     *             when (response) {
     *                 is ForageApiResponse.Success -> {
     *                     // there will be no financial affects upon success
     *                     // you need to capture from the server to formally
     *                     // capture the payment
     *                 }
     *                 is ForageApiResponse.Failure -> {
     *                     // handle an error response here
     *                 }
     *             }
     *         }
     * }
     * ```
     *
     * @param params A [DeferPaymentCaptureParams] model that passes a
     * [`foragePinEditText`][com.joinforage.forage.android.ecom.ui.element.ForagePINEditText] instance and a
     * `paymentRef`, returned by the
     * [Create a Payment](https://docs.joinforage.app/reference/create-a-payment) endpoint, as the
     * DeferPaymentCaptureParams.
     *
     * @throws [ForageConfigNotSetException] If the [ForageConfig] is not set for the provided
     * `foragePinEditText`.
     * @see * [Defer EBT payment capture to the server](https://docs.joinforage.app/docs/capture-ebt-payments-server-side)
     * for the related step-by-step guide.
     * * [Capture an EBT Payment](https://docs.joinforage.app/reference/capture-a-payment)
     * for the API endpoint to call after [deferPaymentCapture].
     * * [SDK errors](https://docs.joinforage.app/reference/errors#sdk-errors) for more information
     * on error handling.
     * @return A [ForageApiResponse] object.
     */
    suspend fun deferPaymentCapture(params: DeferPaymentCaptureParams): ForageApiResponse<String> {
        val (foragePinEditText, paymentRef) = params
        val forageConfig = _getForageConfigOrThrow(foragePinEditText.getForageConfig())
        val logger = EcomDatadogLoggerFactory(
            foragePinEditText.context,
            forageConfig,
            null
        ).makeLogger()
        val pmService = PaymentMethodService(
            forageConfig,
            logger.traceId,
            httpEngine
        )
        val paymentService = PaymentService(
            forageConfig,
            logger.traceId,
            httpEngine
        )
        return EcomDeferCapturePaymentSubmission(
            paymentRef = paymentRef,
            vaultSubmitter = foragePinEditText.getVaultSubmitter(forageConfig.envConfig, httpEngine),
            paymentMethodService = pmService,
            paymentService = paymentService,
            forageConfig = forageConfig,
            logLogger = logger
        ).submit()
    }
}

/**
 * The Forage SDK public API.
 *
 * Provides a set of methods for interfacing with Forage's EBT infrastructure.
 * Use these methods in conjunction with the UI components ForagePANEditText
 * and ForagePINEditText.
 */

/**
 * A model that represents the parameters that Forage requires to tokenize an EBT Card.
 * [TokenizeEBTCardParams] are passed to the
 * [tokenizeEBTCard][com.joinforage.forage.android.ecom.services.ForageSDK.tokenizeEBTCard] method.
 *
 * @property foragePanEditText A reference to a [ForagePANEditText] instance.
 * [setForageConfig][com.joinforage.forage.android.ecom.ui.element.ForagePANEditText.setForageConfig] must
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
 * [checkBalance][com.joinforage.forage.android.ecom.services.ForageSDK.checkBalance] method.
 *
 * @property foragePinEditText A reference to a [ForagePINEditText] instance.
 * [setForageConfig][com.joinforage.forage.android.ecom.ui.element.ForagePINEditText.setForageConfig] must
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
 * [capturePayment][com.joinforage.forage.android.ecom.services.ForageSDK.capturePayment] method.
 *
 * @property foragePinEditText A reference to a [ForagePINEditText] instance.
 * [setForageConfig][com.joinforage.forage.android.ecom.ui.element.ForagePINEditText.setForageConfig] must
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
 * [deferPaymentCapture][com.joinforage.forage.android.ecom.services.ForageSDK.deferPaymentCapture] method.
 *
 * @see * [Defer EBT payment capture to the server](https://docs.joinforage.app/docs/capture-ebt-payments-server-side)
 * for the related step-by-step guide.
 * * [Capture an EBT Payment](https://docs.joinforage.app/reference/capture-a-payment)
 * for the API endpoint to call after
 * [deferPaymentCapture][com.joinforage.forage.android.ecom.services.ForageSDK.deferPaymentCapture].
 *
 * @property foragePinEditText A reference to a [ForagePINEditText] instance.
 * [setForageConfig][com.joinforage.forage.android.core.ui.element.ForagePanElement.setForageConfig] must
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
