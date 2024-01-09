package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.collect.BaseVaultRequestParams
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.fixtures.givenContentId
import com.joinforage.forage.android.fixtures.givenEncryptionKey
import com.joinforage.forage.android.fixtures.givenPaymentMethodRef
import com.joinforage.forage.android.fixtures.givenPaymentRef
import com.joinforage.forage.android.fixtures.returnsEncryptionKeySuccessfully
import com.joinforage.forage.android.fixtures.returnsExpiredCard
import com.joinforage.forage.android.fixtures.returnsFailedPayment
import com.joinforage.forage.android.fixtures.returnsFailedPaymentMethod
import com.joinforage.forage.android.fixtures.returnsMessageCompletedSuccessfully
import com.joinforage.forage.android.fixtures.returnsPayment
import com.joinforage.forage.android.fixtures.returnsPaymentMethod
import com.joinforage.forage.android.fixtures.returnsSendToProxy
import com.joinforage.forage.android.fixtures.returnsUnauthorized
import com.joinforage.forage.android.fixtures.returnsUnauthorizedEncryptionKey
import com.joinforage.forage.android.model.Payment
import com.joinforage.forage.android.network.EncryptionKeyService
import com.joinforage.forage.android.network.MessageStatusService
import com.joinforage.forage.android.network.OkHttpClientBuilder
import com.joinforage.forage.android.network.PaymentMethodService
import com.joinforage.forage.android.network.PaymentService
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

        val logger = Log.getSilentInstance()
        repository = CapturePaymentRepository(
            pinCollector = pinCollector,
            encryptionKeyService = EncryptionKeyService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(testData.bearerToken),
                httpUrl = server.url("").toUrl().toString(),
                logger = logger
            ),
            messageStatusService = MessageStatusService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    testData.bearerToken,
                    merchantAccount = testData.merchantAccount
                ),
                httpUrl = server.url("").toUrl().toString(),
                logger = logger
            ),
            paymentService = PaymentService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    testData.bearerToken,
                    merchantAccount = testData.merchantAccount
                ),
                httpUrl = server.url("").toUrl().toString(),
                logger = logger
            ),
            paymentMethodService = PaymentMethodService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                    testData.bearerToken,
                    merchantAccount = testData.merchantAccount
                ),
                httpUrl = server.url("").toUrl().toString(),
                logger = logger
            ),
            logger = logger
        )
    }

    @Test
    fun `it should return a failure when the getting the encryption key fails`() = runTest {
        server.givenEncryptionKey().returnsUnauthorizedEncryptionKey()

        val response = repository.capturePayment(testData.paymentRef)

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val clientError = response as ForageApiResponse.Failure

        assertThat(clientError.errors[0].message).contains("Authentication credentials were not provided.")
    }

    @Test
    fun `it should return a failure when VGS returns a failure`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentMethodRef().returnsPaymentMethod()

        val failureResponse = ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", "Some error message from VGS")))

        pinCollector.setCapturePaymentResponse(
            paymentRef = testData.paymentRef,
            vaultRequestParams = testData.vaultRequestParams,
            response = failureResponse
        )

        val response = repository.capturePayment(testData.paymentRef)

        assertThat(response).isEqualTo(failureResponse)
    }

    @Test
    fun `it should return a failure when the get message returns a failure`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentMethodRef().returnsPaymentMethod()
        pinCollector.setCapturePaymentResponse(
            paymentRef = testData.paymentRef,
            vaultRequestParams = testData.vaultRequestParams,
            response = ForageApiResponse.Success(
                TestPinCollector.sendToProxyResponse(ExpectedData().contentId)
            )
        )

        server.givenContentId(ExpectedData().contentId)
            .returnsUnauthorized()

        val response = repository.capturePayment(testData.paymentRef)

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
    fun `it should return a failure when the get payment returns a failure`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentRef().returnsFailedPayment()

        val response = repository.capturePayment(testData.paymentRef)

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val failureResponse = response as ForageApiResponse.Failure
        val expectedMessage = "Cannot find payment."
        val expectedForageCode = "not_found"
        val expectedStatusCode = 404
        assertThat(failureResponse.errors[0].message).isEqualTo(expectedMessage)
        assertThat(failureResponse.errors[0].code).isEqualTo(expectedForageCode)
        assertThat(failureResponse.errors[0].httpStatusCode).isEqualTo(expectedStatusCode)
    }

    @Test
    fun `it should return a failure when the get payment method returns a failure`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentMethodRef().returnsFailedPaymentMethod()

        val response = repository.capturePayment(testData.paymentRef)

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val failureResponse = response as ForageApiResponse.Failure
        val expectedMessage = "EBT Card could not be found"
        val expectedForageCode = "not_found"
        val expectedStatusCode = 404

        assertThat(failureResponse.errors[0].message).isEqualTo(expectedMessage)
        assertThat(failureResponse.errors[0].code).isEqualTo(expectedForageCode)
        assertThat(failureResponse.errors[0].httpStatusCode).isEqualTo(expectedStatusCode)
    }

    @Test
    fun `it should return a failure when sqs message is failed`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentMethodRef().returnsPaymentMethod()
        pinCollector.setCapturePaymentResponse(
            paymentRef = testData.paymentRef,
            vaultRequestParams = testData.vaultRequestParams,
            response = ForageApiResponse.Success(
                TestPinCollector.sendToProxyResponse(ExpectedData().contentId)
            )
        )

        server.givenContentId(ExpectedData().contentId)
            .returnsExpiredCard()

        val response = repository.capturePayment(testData.paymentRef)

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
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentMethodRef().returnsPaymentMethod()
        pinCollector.setCapturePaymentResponse(
            paymentRef = testData.paymentRef,
            vaultRequestParams = testData.vaultRequestParams,
            response = ForageApiResponse.Success(
                TestPinCollector.sendToProxyResponse(ExpectedData().contentId)
            )
        )

        repeat(MAX_ATTEMPTS) {
            server.givenContentId(ExpectedData().contentId)
                .returnsSendToProxy()
        }

        val response = repository.capturePayment(testData.paymentRef)

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val failureResponse = response as ForageApiResponse.Failure
        val expectedMessage = "Unknown Server Error"
        val expectedForageCode = "unknown_server_error"
        val expectedStatusCode = 500

        assertThat(failureResponse.errors[0].message).isEqualTo(expectedMessage)
        assertThat(failureResponse.errors[0].code).isEqualTo(expectedForageCode)
        assertThat(failureResponse.errors[0].httpStatusCode).isEqualTo(expectedStatusCode)
        val interpolateId = ExpectedData().contentId
        server.verify("api/message/$interpolateId")
            .called(
                times = times(MAX_ATTEMPTS)
            )
    }

    @Test
    fun `it should succeed`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentMethodRef().returnsPaymentMethod()
        server.givenContentId(testData.contentId)
            .returnsMessageCompletedSuccessfully()
        pinCollector.setCapturePaymentResponse(
            paymentRef = testData.paymentRef,
            vaultRequestParams = testData.vaultRequestParams,
            response = ForageApiResponse.Success(
                TestPinCollector.sendToProxyResponse(testData.contentId)
            )
        )

        val response = repository.capturePayment(testData.paymentRef)

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Success::class.java)
        when (response) {
            is ForageApiResponse.Success -> {
                val paymentMethod = Payment.ModelMapper.from(response.data).paymentMethod
                assertThat(paymentMethod).isEqualTo(testData.paymentMethod)
            }
            else -> {
                assertThat(false)
            }
        }
    }

    companion object {
        private const val MAX_ATTEMPTS = 10
    }

    private data class ExpectedData(
        val bearerToken: String = "AbCaccesstokenXyz",
        val paymentRef: String = "6ae6a45ff1",
        val vaultRequestParams: BaseVaultRequestParams = BaseVaultRequestParams(
            "tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7",
            "tok_sandbox_eZeWfkq1AkqYdiAJC8iweE"
        ),
        val merchantAccount: String = "1234567",
        val contentId: String = "36058ff7-0e9d-4025-94cd-80ef04a3bb1c",
        val paymentMethod: String = "1f148fe399"
    )
}
