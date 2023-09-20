package com.joinforage.forage.android

import com.joinforage.forage.android.core.Log
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
import com.joinforage.forage.android.ui.ForageContext
import com.joinforage.forage.android.ui.AbstractForageElement
import java.util.UUID

/**
 * Singleton responsible for implementing the SDK API
 */
class ForageSDK : ForageSDKInterface {

    private fun _getForageContextOrThrow(element: AbstractForageElement): ForageContext {
        val context = element.getForageContext()
        // TODO: create a custom Exception instead of using IllegalArgumentException
        return context ?: throw IllegalArgumentException(
            "You need to call element.setForageContext(forageContext: ForageContext) on a ForageElement before you can call submit."
        )
    }

    override suspend fun tokenizeEBTCard(params: TokenizeEBTCardParams): ForageApiResponse<String> {
        val (foragePanEditText, customerId, reusable) = params
        val (merchantId, sessionToken) = _getForageContextOrThrow(foragePanEditText)

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
            reusable = reusable
        )
    }

    override suspend fun checkBalance(params: CheckBalanceParams): ForageApiResponse<String> {
        val (foragePinEditText, paymentMethodRef) = params
        val (merchantId, sessionToken) = _getForageContextOrThrow(foragePinEditText)

        // TODO: replace Log.getInstance() with Log() in future PR
        val logger = Log.getInstance()
        logger.i(
            "[HTTP] Submitting balance check for Payment Method $paymentMethodRef",
            attributes = mapOf(
                "merchant_ref" to merchantId,
                "payment_method_ref" to paymentMethodRef
            )
        )
        return CheckBalanceRepository(
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
            logger = logger
        ).checkBalance(
            paymentMethodRef = paymentMethodRef
        )
    }

    override suspend fun capturePayment(params: CapturePaymentParams): ForageApiResponse<String> {
        val (foragePinEditText, paymentRef) = params
        val (merchantId, sessionToken) = _getForageContextOrThrow(foragePinEditText)

        // TODO: replace Log.getInstance() with Log() in future PR
        val logger = Log.getInstance()
        logger.i(
            "[HTTP] Submitting capture request for Payment $paymentRef",
            attributes = mapOf(
                "merchant_ref" to merchantId,
                "payment_ref" to paymentRef
            )
        )
        return CapturePaymentRepository(
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
            logger = logger
        ).capturePayment(
            paymentRef = paymentRef
        )
    }
}
