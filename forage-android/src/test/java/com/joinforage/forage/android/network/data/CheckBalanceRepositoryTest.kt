package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtBalance
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.vault.CheckBalanceRepository
import com.joinforage.forage.android.ecom.ui.element.ForagePINEditText
import com.joinforage.forage.android.fixtures.givenContentId
import com.joinforage.forage.android.fixtures.givenEncryptionKey
import com.joinforage.forage.android.fixtures.givenPaymentMethodRef
import com.joinforage.forage.android.fixtures.returnsEncryptionKeySuccessfully
import com.joinforage.forage.android.fixtures.returnsFailed
import com.joinforage.forage.android.fixtures.returnsMessageCompletedSuccessfully
import com.joinforage.forage.android.fixtures.returnsPaymentMethod
import com.joinforage.forage.android.fixtures.returnsPaymentMethodWithBalance
import com.joinforage.forage.android.fixtures.returnsSendToProxy
import com.joinforage.forage.android.fixtures.returnsUnauthorized
import com.joinforage.forage.android.fixtures.returnsUnauthorizedEncryptionKey
import com.joinforage.forage.android.mock.MockServiceFactory
import com.joinforage.forage.android.mock.MockVaultSubmitter
import com.joinforage.forage.android.mock.getVaultMessageResponse
import kotlinx.coroutines.test.runTest
import me.jorgecastillo.hiroaki.internal.MockServerSuite
import me.jorgecastillo.hiroaki.matchers.times
import me.jorgecastillo.hiroaki.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class CheckBalanceRepositoryTest : MockServerSuite() {
    private lateinit var repository: CheckBalanceRepository
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
        ).createCheckBalanceRepository(mock(ForagePINEditText::class.java))
    }

    @Test
    fun `it should return a failure when the getting the encryption key fails`() = runTest {
        server.givenEncryptionKey().returnsUnauthorizedEncryptionKey()

        val response = executeCheckBalance()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val clientError = response as ForageApiResponse.Failure

        assertThat(clientError.errors[0].message).contains("Authentication credentials were not provided.")
    }

    @Test
    fun `it should return a failure when VGS returns a failure`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentMethodRef().returnsPaymentMethod()

        val failureResponse = ForageApiResponse.Failure(500, "unknown_server_error", "Some error message from VGS")
        setMockVaultResponse(failureResponse)

        val response = executeCheckBalance()

        assertThat(response).isEqualTo(failureResponse)
    }

    @Test
    fun `it should return a failure when the get message returns a failure`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentMethodRef().returnsPaymentMethod()
        setMockVaultResponse(ForageApiResponse.Success(getVaultMessageResponse(expectedData.contentId)))
        server.givenContentId(expectedData.contentId).returnsUnauthorized()

        val response = executeCheckBalance()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val clientError = response as ForageApiResponse.Failure

        assertThat(clientError.errors[0].message).contains("No merchant account FNS number was provided.")
    }

    @Test
    fun `it should return a failure when the get message returns failed`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentMethodRef().returnsPaymentMethod()
        setMockVaultResponse(ForageApiResponse.Success(getVaultMessageResponse(expectedData.contentId)))
        server.givenContentId(expectedData.contentId)
            .returnsFailed()

        val response = executeCheckBalance()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val firstError = (response as ForageApiResponse.Failure).errors.first()

        assertThat(firstError.code).contains("ebt_error_91")
        assertThat(firstError.code).contains("ebt_error_91")
        assertThat(firstError.message).contains("Authorizer not available (time-out) - Host Not Available")
    }

    @Test
    fun `it should succeed`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        // Get Payment Method is called twice!
        server.givenPaymentMethodRef().returnsPaymentMethod()
        server.givenPaymentMethodRef().returnsPaymentMethodWithBalance()
        setMockVaultResponse(ForageApiResponse.Success(getVaultMessageResponse(expectedData.contentId)))
        server.givenContentId(expectedData.contentId)
            .returnsMessageCompletedSuccessfully()

        val response = executeCheckBalance()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Success::class.java)
        when (response) {
            is ForageApiResponse.Success -> {
                val balance = expectedData.balance as EbtBalance
                assertThat(response.data).contains(balance.cash)
                assertThat(response.data).contains(balance.snap)
            }
            else -> {
                assertThat(false)
            }
        }
    }

    @Test
    fun `it should return an error when it reaches the max attempts`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentMethodRef().returnsPaymentMethod()
        setMockVaultResponse(ForageApiResponse.Success(getVaultMessageResponse(expectedData.contentId)))

        repeat(MAX_POLL_MESSAGE_ATTEMPTS) {
            server.givenContentId(expectedData.contentId)
                .returnsSendToProxy()
        }

        val response = executeCheckBalance()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val clientError = response as ForageApiResponse.Failure
        val expectedMessage = "Unknown Server Error"
        val expectedForageCode = "unknown_server_error"
        val expectedStatusCode = 500

        assertThat(clientError.errors[0].message).isEqualTo(expectedMessage)
        assertThat(clientError.errors[0].code).isEqualTo(expectedForageCode)
        assertThat(clientError.errors[0].httpStatusCode).isEqualTo(expectedStatusCode)
        val interpolateId = expectedData.contentId
        server.verify("api/message/$interpolateId")
            .called(
                times = times(MAX_POLL_MESSAGE_ATTEMPTS)
            )
    }

    private suspend fun executeCheckBalance(): ForageApiResponse<String> {
        return repository.checkBalance(
            merchantId = expectedData.merchantId,
            paymentMethodRef = expectedData.paymentMethodRef,
            sessionToken = expectedData.sessionToken
        )
    }

    private fun setMockVaultResponse(response: ForageApiResponse<String>) {
        vaultSubmitter.setSubmitResponse(
            path = "/api/payment_methods/${expectedData.paymentMethodRef}/balance/",
            response = response
        )
    }

    companion object {
        private const val MAX_POLL_MESSAGE_ATTEMPTS = 10
    }
}
