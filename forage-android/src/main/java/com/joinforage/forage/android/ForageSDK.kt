package com.joinforage.forage.android

import com.joinforage.forage.android.core.telemetry.CustomerPerceivedResponseMonitor
import com.joinforage.forage.android.core.telemetry.EventOutcome
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.core.telemetry.UserAction
import com.joinforage.forage.android.network.EncryptionKeyService
import com.joinforage.forage.android.network.ForageConstants
import com.joinforage.forage.android.network.MessageStatusService
import com.joinforage.forage.android.network.OkHttpClientBuilder
import com.joinforage.forage.android.network.PaymentMethodService
import com.joinforage.forage.android.network.PaymentService
import com.joinforage.forage.android.network.TokenizeCardService
import com.joinforage.forage.android.network.data.CapturePaymentRepository
import com.joinforage.forage.android.network.data.CheckBalanceRepository
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.ui.AbstractForageElement
import com.joinforage.forage.android.ui.ForageConfig
import java.util.UUID

/**
 * A class implementation of ForageSDKInterface
 */
class ForageSDK : ForageSDKInterface {

    private fun _getForageConfigOrThrow(element: AbstractForageElement): ForageConfig {
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

        return TokenizeCardService(
            okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                sessionToken,
                merchantId,
                idempotencyKey = UUID.randomUUID().toString(),
                traceId = logger.getTraceIdValue()
            ),
            httpUrl = ForageConstants.provideHttpUrl(),
            logger = logger
        ).tokenizeCard(
            cardNumber = foragePanEditText.getPanNumber(),
            customerId = customerId,
            reusable = reusable ?: true
        )
    }

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
     * @throws ForageConfigNotSetException If the passed ForagePANEditText instance
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
                httpUrl = ForageConstants.provideHttpUrl(),
                logger = logger
            ),
            paymentMethodService = PaymentMethodService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    sessionToken,
                    merchantId,
                    traceId = logger.getTraceIdValue()
                ),
                httpUrl = ForageConstants.provideHttpUrl(),
                logger = logger
            ),
            messageStatusService = MessageStatusService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    sessionToken,
                    merchantId,
                    traceId = logger.getTraceIdValue()
                ),
                httpUrl = ForageConstants.provideHttpUrl(),
                logger = logger
            ),
            logger = logger,
            responseMonitor = measurement
        ).checkBalance(
            paymentMethodRef = paymentMethodRef
        )
        measurement.end()

        val outcome = if (response is ForageApiResponse.Failure) {
            EventOutcome.FAILURE
        } else {
            EventOutcome.SUCCESS
        }

        measurement.setEventOutcome(outcome).logResult()
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
                httpUrl = ForageConstants.provideHttpUrl(),
                logger = logger
            ),
            paymentService = PaymentService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    sessionToken,
                    merchantId,
                    traceId = logger.getTraceIdValue()
                ),
                httpUrl = ForageConstants.provideHttpUrl(),
                logger = logger
            ),
            paymentMethodService = PaymentMethodService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    sessionToken,
                    merchantId,
                    traceId = logger.getTraceIdValue()
                ),
                httpUrl = ForageConstants.provideHttpUrl(),
                logger = logger
            ),
            messageStatusService = MessageStatusService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    sessionToken,
                    merchantId,
                    traceId = logger.getTraceIdValue()
                ),
                httpUrl = ForageConstants.provideHttpUrl(),
                logger = logger
            ),
            logger = logger,
            responseMonitor = measurement
        ).capturePayment(
            paymentRef = paymentRef
        )
        measurement.end()

        val outcome = if (response is ForageApiResponse.Failure) {
            EventOutcome.FAILURE
        } else {
            EventOutcome.SUCCESS
        }

        measurement.setEventOutcome(outcome).logResult()

        return response
    }
}
