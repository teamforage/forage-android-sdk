package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.fixtures.givenEncryptionKey
import com.joinforage.forage.android.fixtures.givenPaymentMethodRef
import com.joinforage.forage.android.fixtures.givenPaymentRef
import com.joinforage.forage.android.fixtures.returnsEncryptionKeySuccessfully
import com.joinforage.forage.android.fixtures.returnsFailedPayment
import com.joinforage.forage.android.fixtures.returnsFailedPaymentMethod
import com.joinforage.forage.android.fixtures.returnsPayment
import com.joinforage.forage.android.fixtures.returnsPaymentMethod
import com.joinforage.forage.android.fixtures.returnsUnauthorizedEncryptionKey
import com.joinforage.forage.android.mock.MockServiceFactory
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.ui.ForagePINEditText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.jorgecastillo.hiroaki.internal.MockServerSuite
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class DeferPaymentCaptureRepositoryTest : MockServerSuite() {
    private lateinit var repository: DeferPaymentCaptureRepository
    private val pinCollector = TestPinCollector()
    private val testData = MockServiceFactory.ExpectedData

    @Before
    override fun setup() {
        super.setup()

        val logger = Log.getSilentInstance()
        repository = MockServiceFactory(
            mockVaultSubmitter = MockVaultSubmitter(),
            mockPinCollector = pinCollector,
            logger = logger,
            server = server
        ).createDeferPaymentCaptureRepository(mock(ForagePINEditText::class.java))
    }

    @Test
    fun `it should return a failure when the getting the encryption key fails`() = runTest {
        server.givenEncryptionKey().returnsUnauthorizedEncryptionKey()

        val response = repository.deferPaymentCapture(testData.paymentRef)

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val clientError = response as ForageApiResponse.Failure

        assertThat(clientError.errors[0].message).contains("Authentication credentials were not provided.")
    }

    @Test
    fun `it should return a failure when VGS returns a failure`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentMethodRef().returnsPaymentMethod()

        val failureResponse = ForageApiResponse.Failure(listOf<ForageError>(ForageError(500, "unknown_server_error", "Some error message from VGS")))

        pinCollector.setCollectPinResponse(
            paymentRef = testData.paymentRef,
            vaultRequestParams = testData.vaultRequestParams,
            response = failureResponse
        )

        val response = repository.deferPaymentCapture(testData.paymentRef)

        assertThat(response).isEqualTo(failureResponse)
    }

    @Test
    fun `it should return a failure when the get payment returns a failure`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentRef().returnsFailedPayment()

        val response = repository.deferPaymentCapture(testData.paymentRef)

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

        val response = repository.deferPaymentCapture(testData.paymentRef)

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
    fun `it should fail on pin collection`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentMethodRef().returnsPaymentMethod()

        val expectedMessage = "You don't have access to this endpoint"
        val expectedForageCode = "permission_denied"
        val expectedStatusCode = 401

        pinCollector.setCollectPinResponse(
            paymentRef = testData.paymentRef,
            vaultRequestParams = testData.vaultRequestParams,
            response = ForageApiResponse.Failure(listOf(ForageError(expectedStatusCode, expectedForageCode, expectedMessage)))
        )

        val response = repository.deferPaymentCapture(testData.paymentRef)

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val failureResponse = response as ForageApiResponse.Failure

        assertThat(failureResponse.errors[0].message).isEqualTo(expectedMessage)
        assertThat(failureResponse.errors[0].code).isEqualTo(expectedForageCode)
        assertThat(failureResponse.errors[0].httpStatusCode).isEqualTo(expectedStatusCode)
    }

    @Test
    fun `it should succeed`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentMethodRef().returnsPaymentMethod()
        pinCollector.setCollectPinResponse(
            paymentRef = testData.paymentRef,
            vaultRequestParams = testData.vaultRequestParams,
            response = ForageApiResponse.Success("")
        )

        val response = repository.deferPaymentCapture(testData.paymentRef)

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Success::class.java)
        when (response) {
            is ForageApiResponse.Success -> {
                assertThat(true)
            }
            else -> {
                assertThat(false)
            }
        }
    }
}
