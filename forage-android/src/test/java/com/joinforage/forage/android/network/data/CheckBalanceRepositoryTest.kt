package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.core.Logger
import com.joinforage.forage.android.fixtures.givenContentId
import com.joinforage.forage.android.fixtures.givenEncryptionKey
import com.joinforage.forage.android.fixtures.givenPaymentMethodRef
import com.joinforage.forage.android.fixtures.returnsEncryptionKeySuccessfully
import com.joinforage.forage.android.fixtures.returnsFailed
import com.joinforage.forage.android.fixtures.returnsMessageCompletedSuccessfully
import com.joinforage.forage.android.fixtures.returnsPaymentMethod
import com.joinforage.forage.android.fixtures.returnsSendToProxy
import com.joinforage.forage.android.fixtures.returnsUnauthorized
import com.joinforage.forage.android.fixtures.returnsUnauthorizedEncryptionKey
import com.joinforage.forage.android.model.Message
import com.joinforage.forage.android.network.CheckBalanceResponseService
import com.joinforage.forage.android.network.EncryptionKeyService
import com.joinforage.forage.android.network.MessageStatusService
import com.joinforage.forage.android.network.OkHttpClientBuilder
import com.joinforage.forage.android.network.PaymentMethodService
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
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
            paymentMethodService = PaymentMethodService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    testData.bearerToken,
                    merchantAccount = testData.merchantAccount
                ),
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

        val response = repository.checkBalance(testData.paymentMethodRef)

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val clientError = response as ForageApiResponse.Failure

        assertThat(clientError.errors[0].message).contains("Authentication credentials were not provided.")
    }

    @Test
    fun `it should return a failure when VGS returns a failure`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentMethodRef().returnsPaymentMethod()

        val failureResponse = ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", "Some error message from VGS")))

        pinCollector.setCollectPinForBalanceCheckResponse(
            paymentMethodRef = testData.paymentMethodRef,
            cardToken = testData.cardToken,
            encryptionKey = testData.encryptionKey,
            response = failureResponse
        )

        val response = repository.checkBalance(testData.paymentMethodRef)

        assertThat(response).isEqualTo(failureResponse)
    }

    @Test
    fun `it should return a failure when the get message returns a failure`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentMethodRef().returnsPaymentMethod()

        pinCollector.setCollectPinForBalanceCheckResponse(
            paymentMethodRef = testData.paymentMethodRef,
            cardToken = testData.cardToken,
            encryptionKey = testData.encryptionKey,
            ForageApiResponse.Success(
                getMessageResponse(testData.contentId)
            )
        )

        server.givenContentId(testData.contentId).returnsUnauthorized()

        val response = repository.checkBalance(testData.paymentMethodRef)

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val clientError = response as ForageApiResponse.Failure

        assertThat(clientError.errors[0].message).contains("No merchant account FNS number was provided.")
    }

    @Test
    fun `it should return a failure when the get message returns failed`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentMethodRef().returnsPaymentMethod()
        pinCollector.setCollectPinForBalanceCheckResponse(
            paymentMethodRef = testData.paymentMethodRef,
            cardToken = testData.cardToken,
            encryptionKey = testData.encryptionKey,
            ForageApiResponse.Success(
                getMessageResponse(testData.contentId)
            )
        )
        server.givenContentId(testData.contentId)
            .returnsFailed()

        val response = repository.checkBalance(testData.paymentMethodRef)

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val clientError = response as ForageApiResponse.Failure

        assertThat(clientError.errors[0].message).contains("Received failure response from EBT network")
    }

    @Test
    fun `it should succeed`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        // Get Payment Method is called twice!
        server.givenPaymentMethodRef().returnsPaymentMethod()
        server.givenPaymentMethodRef().returnsPaymentMethod()
        pinCollector.setCollectPinForBalanceCheckResponse(
            paymentMethodRef = testData.paymentMethodRef,
            cardToken = testData.cardToken,
            encryptionKey = testData.encryptionKey,
            ForageApiResponse.Success(
                getMessageResponse(testData.contentId)
            )
        )
        server.givenContentId(testData.contentId)
            .returnsMessageCompletedSuccessfully()

        val response = repository.checkBalance(testData.paymentMethodRef)

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Success::class.java)
    }

    @Test
    fun `it should return an error when it reaches the max attempts`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentMethodRef().returnsPaymentMethod()
        pinCollector.setCollectPinForBalanceCheckResponse(
            paymentMethodRef = testData.paymentMethodRef,
            cardToken = testData.cardToken,
            encryptionKey = testData.encryptionKey,
            ForageApiResponse.Success(
                getMessageResponse(testData.contentId)
            )
        )

        repeat(MAX_ATTEMPTS) {
            server.givenContentId(testData.contentId)
                .returnsSendToProxy()
        }

        val response = repository.checkBalance(testData.paymentMethodRef)

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val clientError = response as ForageApiResponse.Failure
        val expectedMessage = "Unknown Server Error"
        val expectedForageCode = "unknown_server_error"
        val expectedStatusCode = 500

        assertThat(clientError.errors[0].message).isEqualTo(expectedMessage)
        assertThat(clientError.errors[0].code).isEqualTo(expectedForageCode)
        assertThat(clientError.errors[0].httpStatusCode).isEqualTo(expectedStatusCode)
        val interpolateId = testData.contentId
        server.verify("api/message/$interpolateId")
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
        val merchantAccount: String = "1234567",
        val contentId: String = "45639248-03f2-498d-8aa8-9ebd1c60ee65"
    )
}
