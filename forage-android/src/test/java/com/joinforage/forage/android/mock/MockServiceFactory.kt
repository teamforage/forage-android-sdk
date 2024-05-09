package com.joinforage.forage.android.mock

import com.joinforage.forage.android.ecom.services.ForageSDK
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.Balance
import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeyService
import com.joinforage.forage.android.core.ui.element.state.USState
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtBalance
import com.joinforage.forage.android.core.services.forageapi.polling.MessageStatusService
import com.joinforage.forage.android.core.services.forageapi.network.OkHttpClientBuilder
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethodService
import com.joinforage.forage.android.core.services.forageapi.payment.PaymentService
import com.joinforage.forage.android.core.services.forageapi.polling.PollingService
import com.joinforage.forage.android.core.services.vault.TokenizeCardService
import com.joinforage.forage.android.core.services.vault.BaseVaultRequestParams
import com.joinforage.forage.android.core.services.vault.CapturePaymentRepository
import com.joinforage.forage.android.core.services.vault.CheckBalanceRepository
import com.joinforage.forage.android.core.services.vault.DeferPaymentCaptureRepository
import com.joinforage.forage.android.core.ui.element.ForagePinElement
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
        val cardUsState: USState = USState.PENNSYLVANIA
        const val cardFingerprint: String = "470dda97b63f016a962de150cf53ad72a93aaea4c2a59de2541e0994f48e02ef"

        // PIN-related interactions
        const val paymentRef: String = "6ae6a45ff1"
        const val paymentMethodRef: String = "1f148fe399"
        const val contentId: String = "45639248-03f2-498d-8aa8-9ebd1c60ee65"
        val balance: Balance = EbtBalance(
            snap = "100.00",
            cash = "100.00"
        )
        val vaultRequestParams: BaseVaultRequestParams = BaseVaultRequestParams(
            cardNumberToken = "tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7",
            encryptionKey = "tok_sandbox_eZeWfkq1AkqYdiAJC8iweE"
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

    private fun emptyUrl() = server.url("").toUrl().toString()

    override fun createTokenizeCardService() = TokenizeCardService(
        okHttpClient = okHttpClient,
        httpUrl = emptyUrl(),
        logger = logger
    )

    override fun createCheckBalanceRepository(foragePinEditText: ForagePinElement): CheckBalanceRepository {
        return CheckBalanceRepository(
            vaultSubmitter = mockVaultSubmitter,
            encryptionKeyService = encryptionKeyService,
            paymentMethodService = paymentMethodService,
            pollingService = pollingService,
            logger = logger
        )
    }

    override fun createCapturePaymentRepository(foragePinEditText: ForagePinElement): CapturePaymentRepository {
        return CapturePaymentRepository(
            vaultSubmitter = mockVaultSubmitter,
            encryptionKeyService = encryptionKeyService,
            paymentService = paymentService,
            paymentMethodService = paymentMethodService,
            pollingService = pollingService,
            logger = logger
        )
    }

    override fun createDeferPaymentCaptureRepository(foragePinEditText: ForagePinElement): DeferPaymentCaptureRepository {
        return DeferPaymentCaptureRepository(
            vaultSubmitter = mockVaultSubmitter,
            encryptionKeyService = encryptionKeyService,
            paymentService = paymentService,
            paymentMethodService = paymentMethodService
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
