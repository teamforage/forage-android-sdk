package com.joinforage.forage.android.ecom.services

import com.joinforage.forage.android.core.CapturePaymentParams
import com.joinforage.forage.android.core.CheckBalanceParams
import com.joinforage.forage.android.core.DeferPaymentCaptureParams
import com.joinforage.forage.android.core.ForageConfigNotSetException
import com.joinforage.forage.android.core.ForageSDKInterface
import com.joinforage.forage.android.core.TokenizeEBTCardParams
import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.ui.element.state.ElementState
import com.joinforage.forage.android.core.services.telemetry.CustomerPerceivedResponseMonitor
import com.joinforage.forage.android.core.services.telemetry.EventOutcome
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.telemetry.UserAction
import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeyService
import com.joinforage.forage.android.core.services.forageapi.polling.MessageStatusService
import com.joinforage.forage.android.core.services.forageapi.network.OkHttpClientBuilder
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethodService
import com.joinforage.forage.android.core.services.forageapi.payment.PaymentService
import com.joinforage.forage.android.core.services.forageapi.polling.PollingService
import com.joinforage.forage.android.core.services.vault.TokenizeCardService
import com.joinforage.forage.android.core.services.vault.CapturePaymentRepository
import com.joinforage.forage.android.core.services.vault.CheckBalanceRepository
import com.joinforage.forage.android.core.services.vault.DeferPaymentCaptureRepository
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.ui.element.AbstractForageElement
import com.joinforage.forage.android.core.ui.element.ForageConfig
import com.joinforage.forage.android.ecom.services.vault.AbstractVaultSubmitter
import com.joinforage.forage.android.ecom.ui.ForagePINEditText

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
 * * [ForageTerminalSDK][com.joinforage.forage.android.pos.ForageTerminalSDK] to process POS
 * Terminal transactions
 */
class ForageSDK : ForageSDKInterface {
    /**
     * Retrieves the ForageConfig for a given ForageElement, or throws an exception if the
     * ForageConfig is not set.
     *
     * @param element A ForageElement instance
     * @return The ForageConfig associated with the ForageElement
     * @throws ForageConfigNotSetException If the ForageConfig is not set for the ForageElement
     */
    internal fun <T : ElementState> _getForageConfigOrThrow(element: AbstractForageElement<T>): ForageConfig {
        val context = element.getForageConfig()
        return context ?: throw ForageConfigNotSetException(
            """
    The ForageElement you passed did not have a ForageConfig. In order to submit
    a request via Forage SDK, your ForageElement MUST have a ForageConfig.
    Make sure to call myForageElement.setForageConfig(forageConfig: ForageConfig) 
    immediately on your ForageElement 
            """.trimIndent()
        )
    }

    /**
     * Tokenizes an EBT Card via a [ForagePANEdit
     * Text][com.joinforage.forage.android.ui.ForagePANEditText] Element.
     *
     * * On success, the object includes a `ref` token that represents an instance of a Forage
     * [`PaymentMethod`](https://docs.joinforage.app/reference/payment-methods#paymentmethod-object).
     * You can store the token for future transactions, like to [`checkBalance`](checkBalance) or
     * to [create a `Payment`](https://docs.joinforage.app/reference/create-a-payment) in
     * Forage's database.
     * * On failure, for example in the case of
     * [`unsupported_bin`](https://docs.joinforage.app/reference/errors#unsupported_bin),
     * the response includes a list of
     * [ForageError][com.joinforage.forage.android.network.model.ForageError] objects that you can
     * unpack to programmatically handle the error and display the appropriate
     * customer-facing message based on the `ForageError.code`.
     * ```kotlin
     * // Example tokenizeEBTCard call in a TokenizeViewModel.kt
     * class TokenizeViewModel : ViewModel() {
     *     val merchantId = "mid/<merchant_id>"
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
     *                 val error = response.errors[0]
     *                 // handle error.code here
     *             }
     *         }
     *     }
     * }
     * ```
     * @param params A [TokenizeEBTCardParams] model that passes a [`foragePanEditText`]
     * [com.joinforage.forage.android.ui.ForagePANEditText] instance, a `customerId`, and a `reusable`
     * boolean that Forage uses to tokenize an EBT Card.
     * @throws [ForageConfigNotSetException] If the [ForageConfig] is not set for the provided
     * `foragePanEditText`.
     * @see * [SDK errors](https://docs.joinforage.app/reference/errors#sdk-errors) for more
     * information on error handling.
     * @return A [ForageApiResponse] object. Use [toPaymentMethod()][ForageApiResponse.Success.toPaymentMethod] to convert the `data` string to a [PaymentMethod][com.joinforage.forage.android.model.PaymentMethod].
     */
    override suspend fun tokenizeEBTCard(params: TokenizeEBTCardParams): ForageApiResponse<String> {
        val (foragePanEditText, customerId, reusable) = params
        val (merchantId, sessionToken) = _getForageConfigOrThrow(foragePanEditText)

        // TODO: replace Log.getInstance() with Log() in future PR
        val logger = Log.getInstance()
            .addAttribute("merchant_ref", merchantId)
            .addAttribute("customer_id", customerId)
        logger.i("[ForageSDK] Tokenizing Payment Method")

        val tokenizeCardService = ServiceFactory(sessionToken, merchantId, logger)
            .createTokenizeCardService()

        return tokenizeCardService.tokenizeCard(
            cardNumber = foragePanEditText.getPanNumber(),
            customerId = customerId,
            reusable = reusable ?: true
        )
    }

    /**
     * Checks the balance of a previously created
     * [`PaymentMethod`](https://docs.joinforage.app/reference/payment-methods)
     * via a [ForagePINEditText][com.joinforage.forage.android.ui.ForagePINEditText] Element.
     *
     * ⚠️ _FNS prohibits balance inquiries on sites and apps that offer guest checkout. Skip this
     * method if your customers can opt for guest checkout. If guest checkout is not an option, then
     * it's up to you whether or not to add a balance inquiry feature. No FNS regulations apply._
     * * On success, the response object includes `snap` and `cash` fields that indicate
     * the EBT Card's current SNAP and EBT Cash balances.
     * * On failure, for example in the case of
     * [`ebt_error_14`](https://docs.joinforage.app/reference/errors#ebt_error_14),
     * the response includes a list of
     * [ForageError][com.joinforage.forage.android.network.model.ForageError] objects that you can
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
     *                 val error = response.errors[0]
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
     * to convert the `data` string to a [Balance][com.joinforage.forage.android.model.Balance].
     */
    override suspend fun checkBalance(params: CheckBalanceParams): ForageApiResponse<String> {
        val (foragePinEditText, paymentMethodRef) = params
        val (merchantId, sessionToken) = _getForageConfigOrThrow(foragePinEditText)

        // TODO: replace Log.getInstance() with Log() in future PR
        val logger = Log.getInstance()
            .addAttribute("merchant_ref", merchantId)
            .addAttribute("payment_method_ref", paymentMethodRef)
        logger.i("[ForageSDK] Called checkBalance for Payment Method $paymentMethodRef")

        // This block is used for Metrics Tracking!
        // ------------------------------------------------------
        val measurement = CustomerPerceivedResponseMonitor.newMeasurement(
            vault = foragePinEditText.getVaultType(),
            vaultAction = UserAction.BALANCE,
            logger
        )
        measurement.start()
        // ------------------------------------------------------

        val balanceCheckService = ServiceFactory(sessionToken, merchantId, logger)
            .createCheckBalanceRepository(foragePinEditText)
        val response = balanceCheckService.checkBalance(
            merchantId = merchantId,
            paymentMethodRef = paymentMethodRef,
            sessionToken = sessionToken
        )
        processApiResponseForMetrics(response, measurement)

        return response
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
     * [ForageError][com.joinforage.forage.android.network.model.ForageError] objects that you can
     * unpack to programmatically handle the error and display the appropriate
     * customer-facing message based on the `ForageError.code`.
     * ```kotlin
     * // Example capturePayment call in a PaymentCaptureViewModel.kt
     * class PaymentCaptureViewModel : ViewModel() {
     *     val snapPaymentRef = "s0alzle0fal"
     *     val merchantId = "mid/<merchant_id>"
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
     *                     val error = response.errors[0]
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
     * [`foragePinEditText`][com.joinforage.forage.android.ui.ForagePINEditText]
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
     * @return A [ForageApiResponse] object. Use [toPayment()][ForageApiResponse.Success.toPayment] to convert the `data` string to a [Payment][com.joinforage.forage.android.model.Payment].
     */
    override suspend fun capturePayment(params: CapturePaymentParams): ForageApiResponse<String> {
        val (foragePinEditText, paymentRef) = params
        val (merchantId, sessionToken) = _getForageConfigOrThrow(foragePinEditText)

        // TODO: replace Log.getInstance() with Log() in future PR
        val logger = Log.getInstance()
            .addAttribute("merchant_ref", merchantId)
            .addAttribute("payment_ref", paymentRef)
        logger.i("[ForageSDK] Called capturePayment for Payment $paymentRef")

        // This block is used for Metrics Tracking!
        // ------------------------------------------------------
        val measurement = CustomerPerceivedResponseMonitor.newMeasurement(
            vault = foragePinEditText.getVaultType(),
            vaultAction = UserAction.CAPTURE,
            logger
        )
        measurement.start()
        // ------------------------------------------------------

        val capturePaymentService = ServiceFactory(sessionToken, merchantId, logger)
            .createCapturePaymentRepository(foragePinEditText)
        val response = capturePaymentService.capturePayment(
            merchantId = merchantId,
            paymentRef = paymentRef,
            sessionToken = sessionToken
        )
        processApiResponseForMetrics(response, measurement)

        return response
    }

    /**
     * Submits a card PIN via a
     * [ForagePINEditText][com.joinforage.forage.android.ui.ForagePINEditText] Element and defers
     * payment capture to the server.
     *
     * * On success, the `data` property of the [ForageApiResponse.Success] object resolves with an empty string.
     * * On failure, for example in the case of [`expired_session_token`](https://docs.joinforage.app/reference/errors#expired_session_token) errors, the
     * response includes a list of
     * [ForageError][com.joinforage.forage.android.network.model.ForageError] objects that you can
     * unpack to programmatically handle the error and display the appropriate
     * customer-facing message based on the `ForageError.code`.
     * ```kotlin
     * // Example deferPaymentCapture call in a DeferPaymentCaptureViewModel.kt
     * class DeferPaymentCaptureViewModel  : ViewModel() {
     *     val snapPaymentRef = "s0alzle0fal"
     *     val merchantId = "mid/<merchant_id>"
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
     * [`foragePinEditText`][com.joinforage.forage.android.ui.ForagePINEditText] instance and a
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
    override suspend fun deferPaymentCapture(params: DeferPaymentCaptureParams): ForageApiResponse<String> {
        val (foragePinEditText, paymentRef) = params
        val (merchantId, sessionToken) = _getForageConfigOrThrow(foragePinEditText)

        // TODO: replace Log.getInstance() with Log() in future PR
        val logger = Log.getInstance()
            .addAttribute("merchant_ref", merchantId)
            .addAttribute("payment_ref", paymentRef)
        logger.i("[ForageSDK] Called deferPaymentCapture for Payment $paymentRef")

        val deferPaymentCaptureService = ServiceFactory(sessionToken, merchantId, logger)
            .createDeferPaymentCaptureRepository(foragePinEditText)
        val response = deferPaymentCaptureService.deferPaymentCapture(
            merchantId = merchantId,
            paymentRef = paymentRef,
            sessionToken = sessionToken
        )

        return when (response) {
            is ForageApiResponse.Success -> {
                logger.i("[ForageSDK] Successfully deferred payment capture for Payment $paymentRef")
                ForageApiResponse.Success("")
            }

            else -> {
                logger.e("[ForageSDK] Failed to defer payment capture for Payment $paymentRef")
                response
            }
        }
    }

    /**
     * Determines the outcome of a Forage API response,
     * to report the measurement to the Telemetry service.
     *
     * This involves stopping the measurement timer,
     * marking the Metrics event as a success or failure,
     * and if the event is a failure, setting the Forage error code.
     */
    internal fun processApiResponseForMetrics(
        apiResponse: ForageApiResponse<String>,
        measurement: CustomerPerceivedResponseMonitor
    ) {
        measurement.end()
        val outcome = if (apiResponse is ForageApiResponse.Failure) {
            if (apiResponse.errors.isNotEmpty()) {
                measurement.setForageErrorCode(apiResponse.errors[0].code)
            }
            EventOutcome.FAILURE
        } else {
            EventOutcome.SUCCESS
        }
        measurement.setEventOutcome(outcome).logResult()
    }

    internal open class ServiceFactory(
        private val sessionToken: String,
        private val merchantId: String,
        private val logger: Log
    ) {
        private val config = EnvConfig.fromSessionToken(sessionToken)
        private val okHttpClient by lazy {
            OkHttpClientBuilder.provideOkHttpClient(
                sessionToken = sessionToken,
                merchantId = merchantId,
                traceId = logger.getTraceIdValue()
            )
        }
        private val encryptionKeyService by lazy { createEncryptionKeyService() }
        private val paymentMethodService by lazy { createPaymentMethodService() }
        private val paymentService by lazy { createPaymentService() }
        private val messageStatusService by lazy { createMessageStatusService() }
        private val pollingService by lazy { createPollingService() }

        open fun createTokenizeCardService() = TokenizeCardService(
            config.apiBaseUrl,
            okHttpClient,
            logger
        )

        open fun createCheckBalanceRepository(foragePinEditText: ForagePINEditText): CheckBalanceRepository {
            return CheckBalanceRepository(
                vaultSubmitter = createVaultSubmitter(foragePinEditText),
                encryptionKeyService = encryptionKeyService,
                paymentMethodService = paymentMethodService,
                pollingService = pollingService,
                logger = logger
            )
        }

        open fun createCapturePaymentRepository(foragePinEditText: ForagePINEditText): CapturePaymentRepository {
            return CapturePaymentRepository(
                vaultSubmitter = createVaultSubmitter(foragePinEditText),
                encryptionKeyService = encryptionKeyService,
                paymentService = paymentService,
                paymentMethodService = paymentMethodService,
                pollingService = pollingService,
                logger = logger
            )
        }

        open fun createDeferPaymentCaptureRepository(foragePinEditText: ForagePINEditText): DeferPaymentCaptureRepository {
            return DeferPaymentCaptureRepository(
                vaultSubmitter = createVaultSubmitter(foragePinEditText),
                encryptionKeyService = encryptionKeyService,
                paymentService = paymentService,
                paymentMethodService = paymentMethodService
            )
        }

        private fun createVaultSubmitter(foragePinEditText: ForagePINEditText) = AbstractVaultSubmitter.create(
            foragePinEditText = foragePinEditText,
            logger = logger
        )
        private fun createEncryptionKeyService() = EncryptionKeyService(config.apiBaseUrl, okHttpClient, logger)
        private fun createPaymentMethodService() = PaymentMethodService(config.apiBaseUrl, okHttpClient, logger)
        private fun createPaymentService() = PaymentService(config.apiBaseUrl, okHttpClient, logger)
        private fun createMessageStatusService() = MessageStatusService(config.apiBaseUrl, okHttpClient, logger)
        private fun createPollingService() = PollingService(
            messageStatusService = messageStatusService,
            logger = logger
        )
    }
}
