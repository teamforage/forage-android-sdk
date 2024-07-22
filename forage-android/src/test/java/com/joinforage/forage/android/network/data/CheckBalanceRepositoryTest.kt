package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.vault.CheckBalanceRepository
import com.joinforage.forage.android.ecom.ui.element.ForagePINEditText
import com.joinforage.forage.android.fixtures.givenEncryptionKey
import com.joinforage.forage.android.fixtures.givenPaymentMethodRef
import com.joinforage.forage.android.fixtures.returnsEncryptionKeySuccessfully
import com.joinforage.forage.android.fixtures.returnsPaymentMethod
import com.joinforage.forage.android.fixtures.returnsUnauthorizedEncryptionKey
import com.joinforage.forage.android.mock.MockServiceFactory
import com.joinforage.forage.android.mock.MockVaultSubmitter
import kotlinx.coroutines.test.runTest
import me.jorgecastillo.hiroaki.internal.MockServerSuite
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
