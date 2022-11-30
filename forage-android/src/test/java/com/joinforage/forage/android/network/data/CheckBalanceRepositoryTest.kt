package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.core.Logger
import com.joinforage.forage.android.fixtures.givenContentId
import com.joinforage.forage.android.fixtures.givenEncryptionKey
import com.joinforage.forage.android.fixtures.returnsEncryptionKeySuccessfully
import com.joinforage.forage.android.fixtures.returnsFailed
import com.joinforage.forage.android.fixtures.returnsSendToProxy
import com.joinforage.forage.android.fixtures.returnsUnauthorized
import com.joinforage.forage.android.fixtures.returnsUnauthorizedEncryptionKey
import com.joinforage.forage.android.model.Message
import com.joinforage.forage.android.network.CheckBalanceResponseService
import com.joinforage.forage.android.network.EncryptionKeyService
import com.joinforage.forage.android.network.MessageStatusService
import com.joinforage.forage.android.network.OkHttpClientBuilder
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.jorgecastillo.hiroaki.internal.MockServerSuite
import me.jorgecastillo.hiroaki.matchers.times
import me.jorgecastillo.hiroaki.verify
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckBalanceRepositoryTest : MockServerSuite() {
    private lateinit var repository: CheckBalanceRepository
    private val pinCollector = TestPinCollector()
    private val testData = ExpectedData()
    private val moshi: Moshi = Moshi.Builder().build()

    @Before
    override fun setup() {
        super.setup()

        repository = CheckBalanceRepository(
            pinCollector = pinCollector,
            encryptionKeyService = EncryptionKeyService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(testData.bearerToken),
                httpUrl = server.url("")
            ),
            messageStatusService = MessageStatusService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    testData.bearerToken,
                    merchantAccount = testData.merchantAccount
                ),
                httpUrl = server.url("")
            ),
            checkBalanceResponseService = CheckBalanceResponseService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    testData.bearerToken,
                    merchantAccount = testData.merchantAccount
                ),
                httpUrl = server.url("")
            ),
            logger = Logger.getInstance(enableLogging = false)
        )
    }

    @Test
    fun `it should return a failure when the getting the encryption key fails`() = runTest {
        server.givenEncryptionKey().returnsUnauthorizedEncryptionKey()

        val response = repository.checkBalance(testData.paymentMethodRef, testData.cardToken)

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val clientError = response as ForageApiResponse.Failure

        assertThat(clientError.message).contains("Authentication credentials were not provided.")
    }

    @Test
    fun `it should return a failure when the VGS returns a failure`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()

        val failureResponse = ForageApiResponse.Failure("Some error message from VGS")

        pinCollector.setCollectPinForBalanceCheckResponse(
            paymentMethodRef = testData.paymentMethodRef,
            cardToken = testData.cardToken,
            encryptionKey = testData.encryptionKey,
            response = failureResponse
        )

        val response = repository.checkBalance(testData.paymentMethodRef, testData.cardToken)

        assertThat(response).isEqualTo(failureResponse)
    }

    @Test
    fun `it should return a failure when the get message returns a failure`() = runTest {
        val contentId = "45639248-03f2-498d-8aa8-9ebd1c60ee65"

        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()

        pinCollector.setCollectPinForBalanceCheckResponse(
            paymentMethodRef = testData.paymentMethodRef,
            cardToken = testData.cardToken,
            encryptionKey = testData.encryptionKey,
            ForageApiResponse.Success(
                getMessageResponse(contentId)
            )
        )

        server.givenContentId(contentId).returnsUnauthorized()

        val response = repository.checkBalance(testData.paymentMethodRef, testData.cardToken)

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val clientError = response as ForageApiResponse.Failure

        assertThat(clientError.message).contains("No merchant account FNS number was provided.")
    }

    @Test
    fun `it should return a failure when the get message returns failed`() = runTest {
        val paymentMethodRef = "a9fd8105c9"
        val cardToken = "tok_sandbox_nctiHzM8Nx8xygpvLeaaBT"

        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        pinCollector.setCollectPinForBalanceCheckResponse(
            paymentMethodRef = paymentMethodRef,
            cardToken = cardToken,
            encryptionKey = "tok_sandbox_eZeWfkq1AkqYdiAJC8iweE",
            ForageApiResponse.Success(
                getMessageResponse("45639248-03f2-498d-8aa8-9ebd1c60ee65")
            )
        )
        server.givenContentId("45639248-03f2-498d-8aa8-9ebd1c60ee65")
            .returnsFailed()

        val response = repository.checkBalance(paymentMethodRef, cardToken)

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val clientError = response as ForageApiResponse.Failure

        assertThat(clientError.message).contains("Received failure response from EBT network")
    }

    @Test
    fun `it should return the last message when it reaches the max attempts`() = runTest {
        val paymentMethodRef = "a9fd8105c9"
        val cardToken = "tok_sandbox_nctiHzM8Nx8xygpvLeaaBT"
        val contentId = "45639248-03f2-498d-8aa8-9ebd1c60ee65"

        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        pinCollector.setCollectPinForBalanceCheckResponse(
            paymentMethodRef = paymentMethodRef,
            cardToken = cardToken,
            encryptionKey = "tok_sandbox_eZeWfkq1AkqYdiAJC8iweE",
            ForageApiResponse.Success(
                getMessageResponse(contentId)
            )
        )

        repeat(MAX_ATTEMPTS) {
            server.givenContentId(contentId)
                .returnsSendToProxy()
        }

        val response = repository.checkBalance(paymentMethodRef, cardToken)

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val clientError = response as ForageApiResponse.Failure

        val jsonAdapter: JsonAdapter<Message> = moshi.adapter(Message::class.java)
        val messageResponse = jsonAdapter.fromJson(clientError.message)

        assertThat(messageResponse).isEqualTo(getSendToProxyResponse(contentId))
        server.verify("api/message/$contentId")
            .called(
                times = times(MAX_ATTEMPTS)
            )
    }

    companion object {
        private const val MAX_ATTEMPTS = 10
        fun getMessageResponse(contentId: String): String {
            return JSONObject().apply {
                put("content_id", contentId)
                put("message_type", "0200")
                put("status", "sent_to_proxy")
                put("failed", false)
                put("errors", emptyList<String>())
            }.toString()
        }

        fun getSendToProxyResponse(contentId: String) = Message(
            contentId = contentId,
            messageType = "0200",
            status = "sent_to_proxy",
            failed = false,
            errors = emptyList()
        )
    }

    private data class ExpectedData(
        val bearerToken: String = "AbCaccesstokenXyz",
        val paymentMethodRef: String = "1f148fe399",
        val cardToken: String = "tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7",
        val encryptionKey: String = "tok_sandbox_eZeWfkq1AkqYdiAJC8iweE",
        val merchantAccount: String = "1234567"
    )
}
