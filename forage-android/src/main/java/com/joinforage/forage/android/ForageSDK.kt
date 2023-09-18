package com.joinforage.forage.android

import android.content.Context
import com.joinforage.forage.android.core.telemetry.ActionType
import com.joinforage.forage.android.core.telemetry.CustomerPerceivedResponseMonitor
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.core.telemetry.OutcomeType
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
import com.joinforage.forage.android.ui.ForagePINEditText
import java.util.UUID

/**
 * Singleton responsible for implementing the SDK API
 */
object ForageSDK : ForageSDKApi {
    private var panEntry: String = ""
    private val logger = Log.getInstance()

    override suspend fun tokenizeEBTCard(
        merchantAccount: String,
        bearerToken: String,
        customerId: String,
        reusable: Boolean
    ): ForageApiResponse<String> {
        val currentEntry = panEntry

        logger.i(
            "[HTTP] Tokenizing Payment Method",
            attributes = mapOf(
                "merchant_ref" to merchantAccount,
                "customer_id" to customerId
            )
        )

        return TokenizeCardService(
            okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                bearerToken,
                merchantAccount,
                idempotencyKey = UUID.randomUUID().toString(),
                traceId = logger.getTraceIdValue()
            ),
            httpUrl = ForageConstants.provideHttpUrl(),
            logger = logger
        ).tokenizeCard(
            cardNumber = currentEntry,
            customerId = customerId,
            reusable = reusable
        )
    }

    override suspend fun checkBalance(
        context: Context,
        pinForageEditText: ForagePINEditText,
        merchantAccount: String,
        bearerToken: String,
        paymentMethodRef: String
    ): ForageApiResponse<String> {
        logger.i(
            "[HTTP] Submitting balance check for Payment Method $paymentMethodRef",
            attributes = mapOf(
                "merchant_ref" to merchantAccount,
                "payment_method_ref" to paymentMethodRef
            )
        )

        // This block is used for Metrics Tracking!
        // ------------------------------------------------------
        val measurement = CustomerPerceivedResponseMonitor.newMeasurement(
            vault = pinForageEditText.getCollector(merchantAccount).getVaultType(),
            vaultAction = ActionType.BALANCE,
            logger
        )
        measurement.start()
        // ------------------------------------------------------

        val response = CheckBalanceRepository(
            pinCollector = pinForageEditText.getCollector(
                merchantAccount
            ),
            encryptionKeyService = EncryptionKeyService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount,
                    traceId = logger.getTraceIdValue()
                ),
                httpUrl = ForageConstants.provideHttpUrl(),
                logger = logger
            ),
            paymentMethodService = PaymentMethodService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount,
                    traceId = logger.getTraceIdValue()
                ),
                httpUrl = ForageConstants.provideHttpUrl(),
                logger = logger
            ),
            messageStatusService = MessageStatusService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount,
                    traceId = logger.getTraceIdValue()
                ),
                httpUrl = ForageConstants.provideHttpUrl(),
                logger = logger
            ),
            logger = logger
        ).checkBalance(
            paymentMethodRef = paymentMethodRef
        )
        measurement.end()

        val outcome = if (response is ForageApiResponse.Failure) {
            OutcomeType.FAILURE
        } else {
            OutcomeType.SUCCESS
        }

        measurement.setEventOutcome(outcome).logResult()
        return response
    }

    override suspend fun capturePayment(
        context: Context,
        pinForageEditText: ForagePINEditText,
        merchantAccount: String,
        bearerToken: String,
        paymentRef: String
    ): ForageApiResponse<String> {
        logger.i(
            "[HTTP] Submitting capture request for Payment $paymentRef",
            attributes = mapOf(
                "merchant_ref" to merchantAccount,
                "payment_ref" to paymentRef
            )
        )

        // This block is used for Metrics Tracking!
        // ------------------------------------------------------
        val measurement = CustomerPerceivedResponseMonitor.newMeasurement(
            vault = pinForageEditText.getCollector(merchantAccount).getVaultType(),
            vaultAction = ActionType.CAPTURE,
            logger
        )
        measurement.start()
        // ------------------------------------------------------

        val response = CapturePaymentRepository(
            pinCollector = pinForageEditText.getCollector(
                merchantAccount
            ),
            encryptionKeyService = EncryptionKeyService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount,
                    traceId = logger.getTraceIdValue()
                ),
                httpUrl = ForageConstants.provideHttpUrl(),
                logger = logger
            ),
            paymentService = PaymentService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount,
                    traceId = logger.getTraceIdValue()
                ),
                httpUrl = ForageConstants.provideHttpUrl(),
                logger = logger
            ),
            paymentMethodService = PaymentMethodService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount,
                    traceId = logger.getTraceIdValue()
                ),
                httpUrl = ForageConstants.provideHttpUrl(),
                logger = logger
            ),
            messageStatusService = MessageStatusService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    bearerToken,
                    merchantAccount,
                    traceId = logger.getTraceIdValue()
                ),
                httpUrl = ForageConstants.provideHttpUrl(),
                logger = logger
            ),
            logger = logger
        ).capturePayment(
            paymentRef = paymentRef
        )
        measurement.end()

        val outcome = if (response is ForageApiResponse.Failure) {
            OutcomeType.FAILURE
        } else {
            OutcomeType.SUCCESS
        }

        measurement.setEventOutcome(outcome).logResult()


        return response
    }

    internal fun storeEntry(entry: String) {
        panEntry = entry
    }
}
