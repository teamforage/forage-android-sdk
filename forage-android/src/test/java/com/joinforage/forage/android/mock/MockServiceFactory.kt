package com.joinforage.forage.android.mock

import com.joinforage.forage.android.ForageSDK
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
import com.joinforage.forage.android.network.data.DeferPaymentRefundRepository
import com.joinforage.forage.android.pos.PosRefundPaymentRepository
import com.joinforage.forage.android.pos.PosRefundService
import com.joinforage.forage.android.pos.PosVaultRequestParams
import com.joinforage.forage.android.ui.ForagePINEditText
import okhttp3.mockwebserver.MockWebServer

internal class MockServiceFactory(
    private val mockVaultSubmitter: MockVaultSubmitter,
    private val logger: Log,
    private val server: MockWebServer
) : ForageSDK.ServiceFactory(
    sessionToken = ExpectedData.sessionToken,
    merchantId = ExpectedData.merchantId,
    logger = logger
) {
    object ExpectedData {
        const val sessionToken: String = "AbCaccesstokenXyz"
        const val merchantId: String = "1234567"

        // card tokenization
        const val cardNumber: String = "5076801234567845"
        const val customerId: String = "test-android-customer-id"

        // PIN-related interactions
        const val paymentRef: String = "6ae6a45ff1"
        const val paymentMethodRef: String = "1f148fe399"
        const val contentId: String = "45639248-03f2-498d-8aa8-9ebd1c60ee65"
        val balance: Balance = Balance(
            snap = "100.00",
            cash = "100.00"
        )
        val vaultRequestParams: BaseVaultRequestParams = BaseVaultRequestParams(
            cardNumberToken = "tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7",
            encryptionKey = "tok_sandbox_eZeWfkq1AkqYdiAJC8iweE"
        )

        // POS
        const val posTerminalId: String = "pos-terminal-id-123"
        const val refundRef: String = "refund123"
        const val track2Data: String = "5077081212341234=491212012345"
        const val refundAmount: Float = 1.23f
        const val refundReason: String = "I feel like refunding this payment!"
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
    private val posRefundService by lazy { createPosRefundService() }

    private fun emptyUrl() = server.url("").toUrl().toString()

    override fun createTokenizeCardService() = TokenizeCardService(
        okHttpClient = okHttpClient,
        httpUrl = emptyUrl(),
        logger = logger
    )

    override fun createCheckBalanceRepository(foragePinEditText: ForagePINEditText): CheckBalanceRepository {
        return CheckBalanceRepository(
            vaultSubmitter = mockVaultSubmitter,
            encryptionKeyService = encryptionKeyService,
            paymentMethodService = paymentMethodService,
            pollingService = pollingService,
            logger = logger
        )
    }

    override fun createCapturePaymentRepository(foragePinEditText: ForagePINEditText): CapturePaymentRepository {
        return CapturePaymentRepository(
            vaultSubmitter = mockVaultSubmitter,
            encryptionKeyService = encryptionKeyService,
            paymentService = paymentService,
            paymentMethodService = paymentMethodService,
            pollingService = pollingService,
            logger = logger
        )
    }

    override fun createDeferPaymentCaptureRepository(foragePinEditText: ForagePINEditText): DeferPaymentCaptureRepository {
        return DeferPaymentCaptureRepository(
            vaultSubmitter = mockVaultSubmitter,
            encryptionKeyService = encryptionKeyService,
            paymentService = paymentService,
            paymentMethodService = paymentMethodService
        )
    }

    override fun createDeferPaymentRefundRepository(foragePinEditText: ForagePINEditText): DeferPaymentRefundRepository {
        return DeferPaymentRefundRepository(
            vaultSubmitter = mockVaultSubmitter,
            encryptionKeyService = encryptionKeyService,
            paymentMethodService = paymentMethodService,
            paymentService = paymentService
        )
    }

    override fun createRefundPaymentRepository(foragePinEditText: ForagePINEditText): PosRefundPaymentRepository {
        return PosRefundPaymentRepository(
            vaultSubmitter = mockVaultSubmitter,
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
    private fun createPosRefundService() = PosRefundService(emptyUrl(), logger, okHttpClient)
}
