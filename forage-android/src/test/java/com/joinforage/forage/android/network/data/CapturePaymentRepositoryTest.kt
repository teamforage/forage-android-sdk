package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.vault.CapturePaymentRepository
import com.joinforage.forage.android.ecom.ui.element.ForagePINEditText
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
import com.joinforage.forage.android.mock.MockVaultSubmitter
import kotlinx.coroutines.test.runTest
import me.jorgecastillo.hiroaki.internal.MockServerSuite
import me.jorgecastillo.hiroaki.matchers.times
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class CapturePaymentRepositoryTest : MockServerSuite() {
    private lateinit var repository: CapturePaymentRepository
    private lateinit var vaultSubmitter: MockVaultSubmitter
    private val expectedData = MockServiceFactory.ExpectedData

    @Before
    override fun setup() {
        super.setup()

        val logger = Log.getSilentInstance()
        vaultSubmitter = MockVaultSubmitter()
        repository = MockServiceFactory(
            mockVaultSubmitter = vaultSubmitter,
            logger = logger,
            server = server
        ).createCapturePaymentRepository(mock(ForagePINEditText::class.java))
    }

    private suspend fun executeCapturePayment(): ForageApiResponse<String> {
        return repository.capturePayment(
            merchantId = expectedData.merchantId,
            paymentRef = expectedData.paymentRef,
            sessionToken = expectedData.sessionToken
        )
    }

    private fun setMockVaultResponse(response: ForageApiResponse<String>) {
        vaultSubmitter.setSubmitResponse(
            path = "/api/payments/${expectedData.paymentRef}/capture/",
            response = response
        )
    }

    @Test
    fun `it should return a failure when the getting the encryption key fails`() = runTest {
        server.givenEncryptionKey().returnsUnauthorizedEncryptionKey()

        val response = executeCapturePayment()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val clientError = response as ForageApiResponse.Failure

        assertThat(clientError.error.message).contains("Authentication credentials were not provided.")
    }

    @Test
    fun `it should return a failure when VGS returns a failure`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentMethodRef().returnsPaymentMethod()
        val failureResponse = ForageApiResponse.Failure(500, "unknown_server_error", "Some error message from VGS")
        setMockVaultResponse(failureResponse)

        val response = executeCapturePayment()

        assertThat(response).isEqualTo(failureResponse)
    }

    @Test
    fun `it should return a failure when the get payment returns a failure`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentRef().returnsFailedPayment()

        val response = executeCapturePayment()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val failureResponse = response as ForageApiResponse.Failure
        val expectedMessage = "Cannot find payment."
        val expectedForageCode = "not_found"
        val expectedStatusCode = 404
        assertThat(failureResponse.error.message).isEqualTo(expectedMessage)
        assertThat(failureResponse.error.code).isEqualTo(expectedForageCode)
        assertThat(failureResponse.error.httpStatusCode).isEqualTo(expectedStatusCode)
    }

    @Test
    fun `it should return a failure when the get payment method returns a failure`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentMethodRef().returnsFailedPaymentMethod()

        val response = executeCapturePayment()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val failureResponse = response as ForageApiResponse.Failure
        val expectedMessage = "EBT Card could not be found"
        val expectedForageCode = "not_found"
        val expectedStatusCode = 404

        assertThat(failureResponse.error.message).isEqualTo(expectedMessage)
        assertThat(failureResponse.error.code).isEqualTo(expectedForageCode)
        assertThat(failureResponse.error.httpStatusCode).isEqualTo(expectedStatusCode)
    }

    companion object {
        private const val MAX_POLL_MESSAGE_ATTEMPTS = 10
    }
}
