package com.joinforage.forage.android.mock

import com.joinforage.forage.android.core.EnvConfig
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.model.Balance
import com.joinforage.forage.android.network.EncryptionKeyService
import com.joinforage.forage.android.network.MessageStatusService
import com.joinforage.forage.android.network.OkHttpClientBuilder
import com.joinforage.forage.android.network.PaymentMethodService
import com.joinforage.forage.android.network.PaymentService
import com.joinforage.forage.android.network.PollingService
import com.joinforage.forage.android.network.TokenizeCardService
import com.joinforage.forage.android.network.data.BaseVaultRequestParams
import com.joinforage.forage.android.network.data.CapturePaymentRepository
import com.joinforage.forage.android.network.data.CheckBalanceRepository
import com.joinforage.forage.android.network.data.DeferPaymentCaptureRepository
import com.joinforage.forage.android.network.data.TestPinCollector
import com.joinforage.forage.android.network.data.TestVaultSubmitter
import com.joinforage.forage.android.network.model.PaymentMethodRequestBody
import com.joinforage.forage.android.pos.PosRefundPaymentRepository
import com.joinforage.forage.android.pos.PosRefundService
import com.joinforage.forage.android.pos.PosVaultRequestParams
import com.joinforage.forage.android.ui.ForagePINEditText
import com.joinforage.forage.android.vault.AbstractVaultSubmitter
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.json.JSONObject

internal fun createMockTokenizeCardService(
    server: MockWebServer,
    testData: TokenizeCardExpectedData,
    logger: Log
): TokenizeCardService {
    return TokenizeCardService(
        okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
            sessionToken = testData.sessionToken,
            merchantId = testData.merchantId
        ),
        httpUrl = server.url("").toUrl().toString(),
        logger = logger
    )
}

internal class MockRepositoryFactory(
    private val logger: Log,
    private val server: MockWebServer
) {
    object ExpectedData {
        const val sessionToken: String = "AbCaccesstokenXyz"
        const val paymentRef: String = "1e248fe399"
        const val paymentMethodRef: String = "1f148fe399"
        const val merchantId: String = "1234567"
        const val contentId: String = "45639248-03f2-498d-8aa8-9ebd1c60ee65"
        val balance: Balance = Balance(
            snap = "100.00",
            cash = "100.00"
        )
        val vaultRequestParams: BaseVaultRequestParams = BaseVaultRequestParams(
            cardNumberToken = "tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7",
            encryptionKey = "tok_sandbox_eZeWfkq1AkqYdiAJC8iweE"
        )
        val posVaultRequestParams: PosVaultRequestParams = PosVaultRequestParams(
            cardNumberToken = "tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7",
            encryptionKey = "tok_sandbox_eZeWfkq1AkqYdiAJC8iweE",
            posTerminalId = "pos-terminal-id-123"
        )
    }

        private val okHttpClient by lazy {
            OkHttpClientBuilder.provideOkHttpClient(
                sessionToken = ExpectedData.sessionToken,
                merchantId = ExpectedData.merchantId,
                traceId = logger.getTraceIdValue()
            )
        }
        private val encryptionKeyService by lazy { createEncryptionKeyService() }
        private val paymentMethodService by lazy { createPaymentMethodService() }
        private val paymentService by lazy { createPaymentService() }
        private val messageStatusService by lazy { createMessageStatusService() }
        private val pollingService by lazy { createPollingService() }
        private val posRefundService by lazy { PosRefundService(emptyUrl(), logger, okHttpClient) }

        private fun emptyUrl() = server.url("").toUrl().toString()

        fun createTokenizeCardService() = TokenizeCardService(
            okHttpClient = okHttpClient,
            httpUrl = emptyUrl(),
            logger = logger
        )

        fun createCheckBalanceRepository(pinCollector: TestPinCollector): CheckBalanceRepository {
            return CheckBalanceRepository(
                pinCollector = pinCollector,
                encryptionKeyService = encryptionKeyService,
                paymentMethodService = paymentMethodService,
                pollingService = pollingService,
                logger = logger
            )
        }

        fun createCapturePaymentRepository(pinCollector: TestPinCollector): CapturePaymentRepository {
            return CapturePaymentRepository(
                pinCollector = pinCollector,
                encryptionKeyService = encryptionKeyService,
                paymentService = paymentService,
                paymentMethodService = paymentMethodService,
                pollingService = pollingService
            )
        }

        fun createDeferPaymentCaptureRepository(pinCollector: TestPinCollector): DeferPaymentCaptureRepository {
            return DeferPaymentCaptureRepository(
                pinCollector = pinCollector,
                encryptionKeyService = encryptionKeyService,
                paymentService = paymentService,
                paymentMethodService = paymentMethodService
            )
        }

        fun createPosRefundPaymentRepository(vaultSubmitter: TestVaultSubmitter): PosRefundPaymentRepository {
            return PosRefundPaymentRepository(
                vaultSubmitter = vaultSubmitter,
                encryptionKeyService = encryptionKeyService,
                paymentMethodService = paymentMethodService,
                paymentService = paymentService,
                pollingService = pollingService,
                logger = logger,
                refundService = posRefundService
            )
        }

        private fun createEncryptionKeyService() = EncryptionKeyService(emptyUrl(), okHttpClient, logger)
        private fun createPaymentMethodService() = PaymentMethodService(emptyUrl(), okHttpClient, logger)
        private fun createPaymentService() = PaymentService(emptyUrl(), okHttpClient, logger)
        private fun createMessageStatusService() = MessageStatusService(emptyUrl(), okHttpClient, logger)
        private fun createPollingService() = PollingService(
            messageStatusService = messageStatusService,
            logger = logger
        )

}

internal data class TokenizeCardExpectedData(
    val merchantId: String = "12345678",
    val sessionToken: String = "AbCaccesstokenXyz",
    val cardNumber: String = "5076801234567845",
    val customerId: String = "test-android-customer-id",
    val track2Data: String = "5077081212341234=491212012345",
    val reusable: Boolean = false,
    val paymentMethodRequestBody: PaymentMethodRequestBody = PaymentMethodRequestBody(cardNumber = cardNumber, customerId = customerId)
)

internal data class CheckBalanceExpectedData(
    val sessionToken: String = "AbCaccesstokenXyz",
    val paymentMethodRef: String = "1f148fe399",
    val merchantId: String = "1234567",
    val contentId: String = "45639248-03f2-498d-8aa8-9ebd1c60ee65",
    val balance: Balance = Balance(
        snap = "100.00",
        cash = "100.00"
    ),
    val vaultRequestParams: BaseVaultRequestParams = BaseVaultRequestParams(
        cardNumberToken = "tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7",
        encryptionKey = "tok_sandbox_eZeWfkq1AkqYdiAJC8iweE"
    ),
    val posVaultRequestParams: PosVaultRequestParams = PosVaultRequestParams(
        cardNumberToken = "tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7",
        encryptionKey = "tok_sandbox_eZeWfkq1AkqYdiAJC8iweE",
        posTerminalId = "pos-terminal-id-123"
    )
)

internal fun getVaultMessageResponse(contentId: String): String {
    return JSONObject().apply {
        put("content_id", contentId)
        put("message_type", "0200")
        put("status", "sent_to_proxy")
        put("failed", false)
        put("errors", JSONArray(emptyList<String>()))
    }.toString()
}
