package com.joinforage.forage.android

import com.joinforage.forage.android.core.EnvConfig
import com.joinforage.forage.android.core.element.state.ElementState
import com.joinforage.forage.android.core.telemetry.CustomerPerceivedResponseMonitor
import com.joinforage.forage.android.core.telemetry.EventOutcome
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.core.telemetry.UserAction
import com.joinforage.forage.android.network.EncryptionKeyService
import com.joinforage.forage.android.network.MessageStatusService
import com.joinforage.forage.android.network.OkHttpClientBuilder
import com.joinforage.forage.android.network.PaymentMethodService
import com.joinforage.forage.android.network.PaymentService
import com.joinforage.forage.android.network.TokenizeCardService
import com.joinforage.forage.android.network.data.CapturePaymentRepository
import com.joinforage.forage.android.network.data.CheckBalanceRepository
import com.joinforage.forage.android.network.data.DeferPaymentCaptureRepository
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.ui.AbstractForageElement
import com.joinforage.forage.android.ui.ForageConfig
import java.util.UUID

/**
 * Entry point to the Forage SDK.
 *
 * A [ForageSDK] instance interacts with the Forage API.
 *
 * You need an instance of the ForageSDK to perform operations like:
 *
 * * [capturePayment]
 * * [checkBalance]
 * * [deferPaymentCapture]
 * * [tokenizeEBTCard]
 */
class ForageSDK : ForageSDKInterface {

    /**
     * Retrieves the ForageConfig for a given ForageElement, or throws an exception if the ForageConfig is not set.
     *
     * @param element A ForageElement instance
     * @return The ForageConfig associated with the ForageElement
     * @throws ForageConfigNotSetException If the ForageConfig is not set for the ForageElement
     */
    private fun <T : ElementState> _getForageConfigOrThrow(element: AbstractForageElement<T>): ForageConfig {
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
     * Tokenizes an EBT Card via a [ForagePANEditText][com.joinforage.forage.android.ui.ForagePANEditText] Element.
     *
     * @param TokenizeEBTCardParams A model that passes a [`foragePanEditText`][com.joinforage.forage.android.ui.ForagePANEditText] instance, `customerId`, and `reusable` boolean as the [TokenizeEBTCardParams] that Forage uses to tokenize an EBT Card.
     * @throws [ForageConfigNotSetException] If the [ForageConfig] is not set for the provided `foragePanEditText`.
     * @see * [SDK errors](https://docs.joinforage.app/reference/errors#sdk-errors) for more information on error handling
     * @return A [ForageApiResponse] object. On success, the object includes a `ref` token that represents an instance of a Forage [`PaymentMethod`](https://docs.joinforage.app/reference/payment-methods#paymentmethod-object). You can store the token for future transactions, like to [`checkBalance`](checkBalance) or to [create a `Payment`](https://docs.joinforage.app/reference/create-a-payment) in Forage's database. On failure, for example in the case of [`card_not_reusable`](https://docs.joinforage.app/reference/errors#card_not_reusable) or [`ebt_error_51`](https://docs.joinforage.app/reference/errors#ebt_error_51) errors, the response includes a list of [ForageError][com.joinforage.forage.android.network.model.ForageError] objects that you can unpack to troubleshoot the issue.
     */
    override suspend fun tokenizeEBTCard(params: TokenizeEBTCardParams): ForageApiResponse<String> {
        val (foragePanEditText, customerId, reusable) = params
        val (merchantId, sessionToken) = _getForageConfigOrThrow(foragePanEditText)
        val config = EnvConfig.fromSessionToken(sessionToken)

        // TODO: replace Log.getInstance() with Log() in future PR
        val logger = Log.getInstance()
        logger.i(
            "[HTTP] Tokenizing Payment Method",
            attributes = mapOf(
                "merchant_ref" to merchantId,
                "customer_id" to customerId
            )
        )

        return TokenizeCardService(
            okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                sessionToken,
                merchantId,
                idempotencyKey = UUID.randomUUID().toString(),
                traceId = logger.getTraceIdValue()
            ),
            httpUrl = config.baseUrl,
            logger = logger
        ).tokenizeCard(
            cardNumber = foragePanEditText.getPanNumber(),
            customerId = customerId,
            reusable = reusable ?: true
        )
    }

    /**
     * Checks the balance of a previously created [`PaymentMethod`](https://docs.joinforage.app/reference/payment-methods) via a [ForagePINEditText][com.joinforage.forage.android.ui.ForagePINEditText] Element.
     *
     * @param CheckBalanceParams A model that passes a [`foragePinEditText`][com.joinforage.forage.android.ui.ForagePINEditText] instance and a `paymentMethodRef`, found in the response from a call to [tokenizeEBTCard] or the [Create a `PaymentMethod`](https://docs.joinforage.app/reference/create-payment-method) endpoint, as the [CheckBalanceParams].
     * @throws [ForageConfigNotSetException] If the [ForageConfig] is not set for the provided `foragePinEditText`.
     * @see * [SDK errors](https://docs.joinforage.app/reference/errors#sdk-errors) for more information on error handling.
     * * [Test EBT Cards](https://docs.joinforage.app/docs/test-ebt-cards#balance-inquiry-exceptions) to trigger balance inquiry exceptions during testing.
     * @return A [ForageApiResponse] object. On success, the object includes `snap` and `cash` fields that indicate the EBT Card's current SNAP and EBT Cash balances. On failure, for example in the case of [`card_not_reusable`](https://docs.joinforage.app/reference/errors#card_not_reusable) or [`ebt_error_51`](https://docs.joinforage.app/reference/errors#ebt_error_51) errors, the response includes a list of [ForageError][com.joinforage.forage.android.network.model.ForageError] objects that you can unpack to troubleshoot the issue.
     */
    override suspend fun checkBalance(params: CheckBalanceParams): ForageApiResponse<String> {
        val (foragePinEditText, paymentMethodRef) = params
        val (merchantId, sessionToken) = _getForageConfigOrThrow(foragePinEditText)
        val config = EnvConfig.fromSessionToken(sessionToken)

        // TODO: replace Log.getInstance() with Log() in future PR
        val logger = Log.getInstance()
        logger.i(
            "[HTTP] Submitting balance check for Payment Method $paymentMethodRef",
            attributes = mapOf(
                "merchant_ref" to merchantId,
                "payment_method_ref" to paymentMethodRef
            )
        )

        // This block is used for Metrics Tracking!
        // ------------------------------------------------------
        val measurement = CustomerPerceivedResponseMonitor.newMeasurement(
            vault = foragePinEditText.getCollector(merchantId).getVaultType(),
            vaultAction = UserAction.BALANCE,
            logger
        )
        measurement.start()
        // ------------------------------------------------------

        val response = CheckBalanceRepository(
            pinCollector = foragePinEditText.getCollector(
                merchantId
            ),
            encryptionKeyService = EncryptionKeyService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    sessionToken,
                    merchantId,
                    traceId = logger.getTraceIdValue()
                ),
                httpUrl = config.baseUrl,
                logger = logger
            ),
            paymentMethodService = PaymentMethodService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    sessionToken,
                    merchantId,
                    traceId = logger.getTraceIdValue()
                ),
                httpUrl = config.baseUrl,
                logger = logger
            ),
            messageStatusService = MessageStatusService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    sessionToken,
                    merchantId,
                    traceId = logger.getTraceIdValue()
                ),
                httpUrl = config.baseUrl,
                logger = logger
            ),
            logger = logger
        ).checkBalance(
            paymentMethodRef = paymentMethodRef
        )
        measurement.end()

        val outcome = if (response is ForageApiResponse.Failure) {
            if (response.errors.isNotEmpty()) {
                measurement.setForageErrorCode(response.errors[0].code)
            }
            EventOutcome.FAILURE
        } else {
            EventOutcome.SUCCESS
        }

        measurement.setEventOutcome(outcome).logResult()
        return response
    }

    /**
     * Immediately captures a payment via a [ForagePINEditText][com.joinforage.forage.android.ui.ForagePINEditText] Element.
     *
     * @param CapturePaymentParams A model that passes a [`foragePinEditText`][com.joinforage.forage.android.ui.ForagePINEditText] instance and a `paymentRef`, returned by the [Create a Payment](https://docs.joinforage.app/reference/create-a-payment) endpoint, as the [CapturePaymentParams] that Forage uses to capture a payment.
     * @throws [ForageConfigNotSetException] If the [ForageConfig] is not set for the provided `foragePinEditText`.
     * @see * [SDK errors](https://docs.joinforage.app/reference/errors#sdk-errors) for more information on error handling.
     * * [Test EBT Cards](https://docs.joinforage.app/docs/test-ebt-cards#payment-capture-exceptions) to trigger payment capture exceptions during testing.
     * @return A [ForageApiResponse] object. On success, the object confirms the transaction. The response includes a Forage [`Payment`](https://docs.joinforage.app/reference/payments) object. On failure, for example in the case of [`card_not_reusable`](https://docs.joinforage.app/reference/errors#card_not_reusable) or [`ebt_error_51`](https://docs.joinforage.app/reference/errors#ebt_error_51) errors, the response includes a list of [ForageError][com.joinforage.forage.android.network.model.ForageError] objects that you can unpack to troubleshoot the issue.
     */
    override suspend fun capturePayment(params: CapturePaymentParams): ForageApiResponse<String> {
        val (foragePinEditText, paymentRef) = params
        val (merchantId, sessionToken) = _getForageConfigOrThrow(foragePinEditText)
        val config = EnvConfig.fromSessionToken(sessionToken)

        // TODO: replace Log.getInstance() with Log() in future PR
        val logger = Log.getInstance()
        logger.i(
            "[HTTP] Submitting capture request for Payment $paymentRef",
            attributes = mapOf(
                "merchant_ref" to merchantId,
                "payment_ref" to paymentRef
            )
        )

        // This block is used for Metrics Tracking!
        // ------------------------------------------------------
        val measurement = CustomerPerceivedResponseMonitor.newMeasurement(
            vault = foragePinEditText.getCollector(merchantId).getVaultType(),
            vaultAction = UserAction.CAPTURE,
            logger
        )
        measurement.start()
        // ------------------------------------------------------

        val response = CapturePaymentRepository(
            pinCollector = foragePinEditText.getCollector(
                merchantId
            ),
            encryptionKeyService = EncryptionKeyService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    sessionToken,
                    merchantId,
                    traceId = logger.getTraceIdValue()
                ),
                httpUrl = config.baseUrl,
                logger = logger
            ),
            paymentService = PaymentService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    sessionToken,
                    merchantId,
                    traceId = logger.getTraceIdValue()
                ),
                httpUrl = config.baseUrl,
                logger = logger
            ),
            paymentMethodService = PaymentMethodService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    sessionToken,
                    merchantId,
                    traceId = logger.getTraceIdValue()
                ),
                httpUrl = config.baseUrl,
                logger = logger
            ),
            messageStatusService = MessageStatusService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    sessionToken,
                    merchantId,
                    traceId = logger.getTraceIdValue()
                ),
                httpUrl = config.baseUrl,
                logger = logger
            ),
            logger = logger
        ).capturePayment(
            paymentRef = paymentRef
        )
        measurement.end()

        val outcome = if (response is ForageApiResponse.Failure) {
            if (response.errors.isNotEmpty()) {
                measurement.setForageErrorCode(response.errors[0].code)
            }
            EventOutcome.FAILURE
        } else {
            EventOutcome.SUCCESS
        }

        measurement.setEventOutcome(outcome).logResult()

        return response
    }

    /**
     * Submits a customer's PIN via a [ForagePINEditText][com.joinforage.forage.android.ui.ForagePINEditText] Element and defers payment capture to the server.
     *
     * @param DeferPaymentCaptureParams A model that passes a [`foragePinEditText`][com.joinforage.forage.android.ui.ForagePINEditText] instance and a `paymentRef`, returned by the [Create a Payment](https://docs.joinforage.app/reference/create-a-payment) endpoint, as the [DeferPaymentCaptureParams].
     *
     * @throws [ForageConfigNotSetException] If the [ForageConfig] is not set for the provided `foragePinEditText`.
     * @see * [Defer EBT payment capture to the server](https://docs.joinforage.app/docs/capture-ebt-payments-server-side) for the related step-by-step guide.
     * * [Capture an EBT Payment](https://docs.joinforage.app/reference/capture-a-payment) for the API endpoint to call after [deferPaymentCapture].
     * * [SDK errors](https://docs.joinforage.app/reference/errors#sdk-errors) for more information on error handling.
     * @return A [ForageApiResponse] object. On success, the object returns `Nothing`. On failure, for example in the case of [`card_not_reusable`](https://docs.joinforage.app/reference/errors#card_not_reusable) or [`ebt_error_51`](https://docs.joinforage.app/reference/errors#ebt_error_51) errors, the response includes a list of [ForageError][com.joinforage.forage.android.network.model.ForageError] objects that you can unpack to troubleshoot the issue.
     */
    override suspend fun deferPaymentCapture(params: DeferPaymentCaptureParams): ForageApiResponse<String> {
        val (foragePinEditText, paymentRef) = params
        val (merchantId, sessionToken) = _getForageConfigOrThrow(foragePinEditText)
        val config = EnvConfig.fromSessionToken(sessionToken)

        // TODO: replace Log.getInstance() with Log() in future PR
        val logger = Log.getInstance()
        logger.i(
            "[HTTP] Submitting defer capture request for Payment $paymentRef",
            attributes = mapOf(
                "merchant_ref" to merchantId,
                "payment_ref" to paymentRef
            )
        )

        return DeferPaymentCaptureRepository(
            pinCollector = foragePinEditText.getCollector(
                merchantId
            ),
            encryptionKeyService = EncryptionKeyService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    sessionToken,
                    merchantId,
                    traceId = logger.getTraceIdValue()
                ),
                httpUrl = config.baseUrl,
                logger = logger
            ),
            paymentService = PaymentService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    sessionToken,
                    merchantId,
                    traceId = logger.getTraceIdValue()
                ),
                httpUrl = config.baseUrl,
                logger = logger
            ),
            paymentMethodService = PaymentMethodService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    sessionToken,
                    merchantId,
                    traceId = logger.getTraceIdValue()
                ),
                httpUrl = config.baseUrl,
                logger = logger
            )
        ).deferPaymentCapture(
            paymentRef = paymentRef
        )
    }
}
