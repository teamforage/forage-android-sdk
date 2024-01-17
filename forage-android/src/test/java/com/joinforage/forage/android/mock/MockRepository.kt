package com.joinforage.forage.android.mock

import com.joinforage.forage.android.collect.VaultSubmitter
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.model.Balance
import com.joinforage.forage.android.network.EncryptionKeyService
import com.joinforage.forage.android.network.MessageStatusService
import com.joinforage.forage.android.network.OkHttpClientBuilder
import com.joinforage.forage.android.network.PaymentMethodService
import com.joinforage.forage.android.network.PollingService
import com.joinforage.forage.android.network.TokenizeCardService
import com.joinforage.forage.android.network.data.BaseVaultRequestParams
import com.joinforage.forage.android.network.data.CheckBalanceRepository
import com.joinforage.forage.android.network.model.PaymentMethodRequestBody
import com.joinforage.forage.android.pos.PosVaultRequestParams
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
internal fun createMockCheckBalanceRepository(
    vaultSubmitter: VaultSubmitter,
    server: MockWebServer,
    logger: Log
): CheckBalanceRepository {
    val testData = CheckBalanceExpectedData()

    return CheckBalanceRepository(
        vaultSubmitter = vaultSubmitter,
        encryptionKeyService = EncryptionKeyService(
            okHttpClient = OkHttpClientBuilder.provideOkHttpClient(testData.sessionToken),
            httpUrl = server.url("").toUrl().toString(),
            logger = logger
        ),
        paymentMethodService = PaymentMethodService(
            okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                testData.sessionToken,
                merchantId = testData.merchantId
            ),
            httpUrl = server.url("").toUrl().toString(),
            logger = logger
        ),
        pollingService = PollingService(
            messageStatusService = MessageStatusService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    testData.sessionToken,
                    merchantId = testData.merchantId
                ),
                httpUrl = server.url("").toUrl().toString(),
                logger = logger
            ),
            logger = logger
        ),
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
