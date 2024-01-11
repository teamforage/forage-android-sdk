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
import com.joinforage.forage.android.ui.ForagePINEditText
import java.util.UUID

/**
 * A class implementation of ForageSDKInterface
 */
class ForageSDK : ForageSDKInterface {

    internal fun <T : ElementState> _getForageConfigOrThrow(element: AbstractForageElement<T>): ForageConfig {
        val context = element.getForageConfig()
        return context ?: throw ForageConfigNotSetException(
            """
    The ForageElement you passed did have a ForageConfig. In order to submit
    a request via Forage SDK, your ForageElement MUST have a ForageConfig.
    Make sure to call myForageElement.setForageConfig(forageConfig: ForageConfig) 
    immediately on your ForageElement 
            """.trimIndent()
        )
    }

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
    override suspend fun tokenizeEBTCard(params: TokenizeEBTCardParams): ForageApiResponse<String> {
        val (foragePanEditText, customerId, reusable) = params
        val (merchantId, sessionToken) = _getForageConfigOrThrow(foragePanEditText)

        // TODO: replace Log.getInstance() with Log() in future PR
        val logger = Log.getInstance()
        logger.i(
            "[HTTP] Tokenizing Payment Method",
            attributes = mapOf(
                "merchant_ref" to merchantId,
                "customer_id" to customerId
            )
        )

        val tokenizeCardService = ServiceFactory(sessionToken, merchantId, logger)
            .createTokenizeCardService(
                idempotencyKey = UUID.randomUUID().toString()
            )

        return tokenizeCardService.tokenizeCard(
            cardNumber = foragePanEditText.getPanNumber(),
            customerId = customerId,
            reusable = reusable ?: true
        )
    }

    /**
     * Checks the balance of a given PaymentMethod using a ForagePINEditText
     *
     * @param params The parameters required for balance inquiries, including
     * a reference to a ForagePINEditText and PaymentMethod ref
     *
     * @return A ForageAPIResponse indicating the success or failure of the operation.
     * On success, returns an object with `snap` (SNAP) and `cash` (EBT Cash) fields, whose values
     * indicate the current balance of each respective tender
     *
     * @throws ForageConfigNotSetException If the passed ForagePINEditText instance
     * hasn't had its ForageConfig set via .setForageConfig().
     */
    override suspend fun checkBalance(params: CheckBalanceParams): ForageApiResponse<String> {
        val (foragePinEditText, paymentMethodRef) = params
        val (merchantId, sessionToken) = _getForageConfigOrThrow(foragePinEditText)

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

        val balanceCheckService = ServiceFactory(sessionToken, merchantId, logger)
            .createCheckBalanceRepository(foragePinEditText)
        val response = balanceCheckService.checkBalance(
            paymentMethodRef = paymentMethodRef
        )
        processApiResponseForMetrics(response, measurement)

        return response
    }

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
     * @throws ForageConfigNotSetException If the passed ForagePANEditText instance
     * hasn't had its ForageConfig set via .setForageConfig().
     */
    override suspend fun capturePayment(params: CapturePaymentParams): ForageApiResponse<String> {
        val (foragePinEditText, paymentRef) = params
        val (merchantId, sessionToken) = _getForageConfigOrThrow(foragePinEditText)

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

        val capturePaymentService = ServiceFactory(sessionToken, merchantId, logger)
            .createCapturePaymentRepository(foragePinEditText)
        val response = capturePaymentService.capturePayment(
            paymentRef = paymentRef
        )
        processApiResponseForMetrics(response, measurement)

        return response
    }

    /**
     * Capture a customer's PIN for an EBT payment and defer the capture of the payment to the server
     *
     * @param params The parameters required for pin capture, including
     * reference to a ForagePINEditText and a Payment ref
     *
     * @return A ForageAPIResponse indicating the success or failure of the
     * PIN capture. On success, returns `Nothing`.
     * On failure, provides a detailed error response.
     *
     * @throws ForageConfigNotSetException If the passed ForagePINEditText instance
     * hasn't had its ForageConfig set via .setForageConfig().
     */
    override suspend fun deferPaymentCapture(params: DeferPaymentCaptureParams): ForageApiResponse<String> {
        val (foragePinEditText, paymentRef) = params
        val (merchantId, sessionToken) = _getForageConfigOrThrow(foragePinEditText)

        // TODO: replace Log.getInstance() with Log() in future PR
        val logger = Log.getInstance()
        logger.i(
            "[HTTP] Submitting defer capture request for Payment $paymentRef",
            attributes = mapOf(
                "merchant_ref" to merchantId,
                "payment_ref" to paymentRef
            )
        )

        val deferPaymentCaptureService = ServiceFactory(sessionToken, merchantId, logger)
            .createDeferPaymentCaptureRepository(foragePinEditText)
        return deferPaymentCaptureService.deferPaymentCapture(
            paymentRef = paymentRef
        )
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

        open fun createTokenizeCardService(idempotencyKey: String) = TokenizeCardService(
            config.baseUrl,
            OkHttpClientBuilder.provideOkHttpClient(
                sessionToken = sessionToken,
                merchantId = merchantId,
                idempotencyKey = idempotencyKey,
                traceId = logger.getTraceIdValue()
            ),
            logger = logger
        )

        open fun createCheckBalanceRepository(foragePinEditText: ForagePINEditText): CheckBalanceRepository {
            return CheckBalanceRepository(
                pinCollector = foragePinEditText.getCollector(merchantId),
                encryptionKeyService = encryptionKeyService,
                paymentMethodService = paymentMethodService,
                messageStatusService = messageStatusService,
                logger = logger
            )
        }

        open fun createCapturePaymentRepository(foragePinEditText: ForagePINEditText): CapturePaymentRepository {
            return CapturePaymentRepository(
                pinCollector = foragePinEditText.getCollector(merchantId),
                encryptionKeyService = encryptionKeyService,
                paymentService = paymentService,
                paymentMethodService = paymentMethodService,
                messageStatusService = messageStatusService,
                logger = logger
            )
        }

        open fun createDeferPaymentCaptureRepository(foragePinEditText: ForagePINEditText): DeferPaymentCaptureRepository {
            return DeferPaymentCaptureRepository(
                pinCollector = foragePinEditText.getCollector(merchantId),
                encryptionKeyService = encryptionKeyService,
                paymentService = paymentService,
                paymentMethodService = paymentMethodService
            )
        }

        private fun createEncryptionKeyService() = EncryptionKeyService(config.baseUrl, okHttpClient, logger)
        private fun createPaymentMethodService() = PaymentMethodService(config.baseUrl, okHttpClient, logger)
        private fun createPaymentService() = PaymentService(config.baseUrl, okHttpClient, logger)
        private fun createMessageStatusService() = MessageStatusService(config.baseUrl, okHttpClient, logger)
    }
}
