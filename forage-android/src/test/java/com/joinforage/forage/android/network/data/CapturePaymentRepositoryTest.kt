package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.core.Logger
import com.joinforage.forage.android.fixtures.*
import com.joinforage.forage.android.network.CapturePaymentResponseService
import com.joinforage.forage.android.network.EncryptionKeyService
import com.joinforage.forage.android.network.MessageStatusService
import com.joinforage.forage.android.network.OkHttpClientBuilder
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.jorgecastillo.hiroaki.internal.MockServerSuite
import me.jorgecastillo.hiroaki.matchers.times
import me.jorgecastillo.hiroaki.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CapturePaymentRepositoryTest : MockServerSuite() {
    private lateinit var repository: CapturePaymentRepository
    private val pinCollector = TestPinCollector()
    private val testData = ExpectedData()

    @Before
    override fun setup() {
        super.setup()

        repository = CapturePaymentRepository(
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
            capturePaymentResponseService = CapturePaymentResponseService(
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

        val response = repository.capturePayment(testData.paymentRef, testData.cardToken)

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val clientError = response as ForageApiResponse.Failure

        assertThat(clientError.errors[0].message).contains("Authentication credentials were not provided.")
    }

    @Test
    fun `it should return a failure when VGS returns a failure`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()

        val failureResponse = ForageApiResponse.Failure(listOf<ForageError>(ForageError(500, "unknown_server_error", "Some error message from VGS")))

        pinCollector.setCollectPinForCapturePaymentResponse(
            paymentRef = testData.paymentRef,
            cardToken = testData.cardToken,
            encryptionKey = testData.encryptionKey,
            response = failureResponse
        )

        val response = repository.capturePayment(testData.paymentRef, testData.cardToken)

        assertThat(response).isEqualTo(failureResponse)
    }

    @Test
    fun `it should return a failure when the get message returns a failure`() = runTest {
        val paymentRef = "a57f07506c"
        val cardToken = "tok_sandbox_nctiHzM8Nx8xygpvLeaaBT"
        val contentId = "36058ff7-0e9d-4025-94cd-80ef04a3bb1c"

        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        pinCollector.setCollectPinForCapturePaymentResponse(
            paymentRef = paymentRef,
            cardToken = cardToken,
            encryptionKey = "tok_sandbox_eZeWfkq1AkqYdiAJC8iweE",
            response = ForageApiResponse.Success(
                TestPinCollector.sendToProxyResponse(contentId)
            )
        )

        server.givenContentId(contentId)
            .returnsUnauthorized()

        val response = repository.capturePayment(paymentRef, cardToken)

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val failureResponse = response as ForageApiResponse.Failure
        val expectedMessage = "No merchant account FNS number was provided."
        val expectedForageCode = "missing_merchant_account"
        val expectedStatusCode = 401

        assertThat(failureResponse.errors[0].message).isEqualTo(expectedMessage)
        assertThat(failureResponse.errors[0].code).isEqualTo(expectedForageCode)
        assertThat(failureResponse.errors[0].httpStatusCode).isEqualTo(expectedStatusCode)
    }

    @Test
    fun `it should return a failure when sqs message is failed`() = runTest {
        val paymentRef = "a57f07506c"
        val cardToken = "tok_sandbox_nctiHzM8Nx8xygpvLeaaBT"
        val contentId = "36058ff7-0e9d-4025-94cd-80ef04a3bb1c"

        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        pinCollector.setCollectPinForCapturePaymentResponse(
            paymentRef = paymentRef,
            cardToken = cardToken,
            encryptionKey = "tok_sandbox_eZeWfkq1AkqYdiAJC8iweE",
            response = ForageApiResponse.Success(
                TestPinCollector.sendToProxyResponse(contentId)
            )
        )

        server.givenContentId(contentId)
            .returnsExpiredCard()

        val response = repository.capturePayment(paymentRef, cardToken)

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val failureResponse = response as ForageApiResponse.Failure
        val expectedMessage = "Expired card - Expired Card"
        val expectedForageCode = "ebt_error_54"
        val expectedStatusCode = 400

        assertThat(failureResponse.errors[0].message).isEqualTo(expectedMessage)
        assertThat(failureResponse.errors[0].code).isEqualTo(expectedForageCode)
        assertThat(failureResponse.errors[0].httpStatusCode).isEqualTo(expectedStatusCode)
    }

    @Test
    fun `it should return a failure when max attempts are tried`() = runTest {
        val paymentRef = "a57f07506c"
        val cardToken = "tok_sandbox_nctiHzM8Nx8xygpvLeaaBT"
        val contentId = "36058ff7-0e9d-4025-94cd-80ef04a3bb1c"

        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        pinCollector.setCollectPinForCapturePaymentResponse(
            paymentRef = paymentRef,
            cardToken = cardToken,
            encryptionKey = "tok_sandbox_eZeWfkq1AkqYdiAJC8iweE",
            response = ForageApiResponse.Success(
                TestPinCollector.sendToProxyResponse(contentId)
            )
        )

        repeat(MAX_ATTEMPTS) {
            server.givenContentId(contentId)
                .returnsSendToProxy()
        }

        val response = repository.capturePayment(paymentRef, cardToken)

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val failureResponse = response as ForageApiResponse.Failure
        val expectedMessage = "Unknown Server Error"
        val expectedForageCode = "unknown_server_error"
        val expectedStatusCode = 500

        assertThat(failureResponse.errors[0].message).isEqualTo(expectedMessage)
        assertThat(failureResponse.errors[0].code).isEqualTo(expectedForageCode)
        assertThat(failureResponse.errors[0].httpStatusCode).isEqualTo(expectedStatusCode)
        server.verify("api/message/$contentId")
            .called(
                times = times(MAX_ATTEMPTS)
            )
    }

    companion object {
        private const val MAX_ATTEMPTS = 10
    }

    private data class ExpectedData(
        val bearerToken: String = "AbCaccesstokenXyz",
        val paymentRef: String = "1f148fe399",
        val cardToken: String = "tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7",
        val encryptionKey: String = "tok_sandbox_eZeWfkq1AkqYdiAJC8iweE",
        val merchantAccount: String = "1234567"
    )
}
