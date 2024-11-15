package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.vault.DeferPaymentCaptureRepository
import com.joinforage.forage.android.ecom.ui.element.ForagePINEditText
import com.joinforage.forage.android.fixtures.givenPaymentMethodRef
import com.joinforage.forage.android.fixtures.givenPaymentRef
import com.joinforage.forage.android.fixtures.returnsFailedPayment
import com.joinforage.forage.android.fixtures.returnsFailedPaymentMethod
import com.joinforage.forage.android.fixtures.returnsPayment
import com.joinforage.forage.android.fixtures.returnsPaymentMethod
import com.joinforage.forage.android.mock.MockServiceFactory
import com.joinforage.forage.android.mock.MockVaultSubmitter
import kotlinx.coroutines.test.runTest
import me.jorgecastillo.hiroaki.internal.MockServerSuite
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class DeferPaymentCaptureRepositoryTest : MockServerSuite() {
    private lateinit var repository: DeferPaymentCaptureRepository
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
        ).createDeferPaymentCaptureRepository(mock(ForagePINEditText::class.java))
    }

    @Test
    fun `it should return a failure when Rosetta returns a failure`() = runTest {
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentMethodRef().returnsPaymentMethod()

        val failureResponse = ForageApiResponse.Failure(500, "unknown_server_error", "Some error message from VGS")
        setMockVaultResponse(failureResponse)

        val response = executeDeferPaymentCapture()

        assertThat(response).isEqualTo(failureResponse)
    }

    @Test
    fun `it should return a failure when the get payment returns a failure`() = runTest {
        server.givenPaymentRef().returnsFailedPayment()

        val response = executeDeferPaymentCapture()

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
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentMethodRef().returnsFailedPaymentMethod()

        val response = executeDeferPaymentCapture()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val failureResponse = response as ForageApiResponse.Failure
        val expectedMessage = "EBT Card could not be found"
        val expectedForageCode = "not_found"
        val expectedStatusCode = 404

        assertThat(failureResponse.error.message).isEqualTo(expectedMessage)
        assertThat(failureResponse.error.code).isEqualTo(expectedForageCode)
        assertThat(failureResponse.error.httpStatusCode).isEqualTo(expectedStatusCode)
    }

    @Test
    fun `it should fail on Vault proxy PIN submission`() = runTest {
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentMethodRef().returnsPaymentMethod()

        val expectedMessage = "You don't have access to this endpoint"
        val expectedForageCode = "permission_denied"
        val expectedStatusCode = 401
        setMockVaultResponse(ForageApiResponse.Failure(expectedStatusCode, expectedForageCode, expectedMessage))

        val response = executeDeferPaymentCapture()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val failureResponse = response as ForageApiResponse.Failure

        assertThat(failureResponse.error.message).isEqualTo(expectedMessage)
        assertThat(failureResponse.error.code).isEqualTo(expectedForageCode)
        assertThat(failureResponse.error.httpStatusCode).isEqualTo(expectedStatusCode)
    }

    @Test
    fun `it should succeed with empty string response`() = runTest {
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentMethodRef().returnsPaymentMethod()
        setMockVaultResponse(ForageApiResponse.Success(""))

        val response = executeDeferPaymentCapture()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Success::class.java)
        when (response) {
            is ForageApiResponse.Success -> {
                assertThat(response.data).isEqualTo("")
            }
            else -> {
                assertThat(false)
            }
        }
    }

    private suspend fun executeDeferPaymentCapture(): ForageApiResponse<String> {
        return repository.deferPaymentCapture(
            merchantId = expectedData.merchantId,
            paymentRef = expectedData.paymentRef,
            sessionToken = expectedData.sessionToken
        )
    }

    private fun setMockVaultResponse(response: ForageApiResponse<String>) {
        vaultSubmitter.setSubmitResponse(
            path = "/api/payments/${expectedData.paymentRef}/collect_pin/",
            response = response
        )
    }
}
