package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.core.telemetry.Log
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
import com.joinforage.forage.android.mock.MockRepositoryFactory
import com.joinforage.forage.android.mock.getVaultMessageResponse
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
class CheckBalanceRepositoryTest : MockServerSuite() {
    private lateinit var repository: CheckBalanceRepository
    private val pinCollector = TestPinCollector()
    private val testData = MockRepositoryFactory.ExpectedData

    @Before
    override fun setup() {
        super.setup()

        val logger = Log.getSilentInstance()
        repository = MockRepositoryFactory(
            logger = logger,
            server = server
        ).createCheckBalanceRepository(pinCollector)
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

        pinCollector.setBalanceCheckResponse(
            paymentMethodRef = testData.paymentMethodRef,
            vaultRequestParams = testData.vaultRequestParams,
            response = failureResponse
        )

        val response = repository.checkBalance(testData.paymentMethodRef)

        assertThat(response).isEqualTo(failureResponse)
    }

    @Test
    fun `it should return a failure when the get message returns a failure`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentMethodRef().returnsPaymentMethod()

        pinCollector.setBalanceCheckResponse(
            paymentMethodRef = testData.paymentMethodRef,
            vaultRequestParams = testData.vaultRequestParams,
            ForageApiResponse.Success(getVaultMessageResponse(testData.contentId))
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
        pinCollector.setBalanceCheckResponse(
            paymentMethodRef = testData.paymentMethodRef,
            vaultRequestParams = testData.vaultRequestParams,
            ForageApiResponse.Success(getVaultMessageResponse(testData.contentId))
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
        server.givenPaymentMethodRef().returnsPaymentMethodWithBalance()
        pinCollector.setBalanceCheckResponse(
            paymentMethodRef = testData.paymentMethodRef,
            vaultRequestParams = testData.vaultRequestParams,
            ForageApiResponse.Success(getVaultMessageResponse(testData.contentId))
        )
        server.givenContentId(testData.contentId)
            .returnsMessageCompletedSuccessfully()

        val response = repository.checkBalance(testData.paymentMethodRef)

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Success::class.java)
        when (response) {
            is ForageApiResponse.Success -> {
                assertThat(response.data).contains(testData.balance.cash)
                assertThat(response.data).contains(testData.balance.snap)
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
        pinCollector.setBalanceCheckResponse(
            paymentMethodRef = testData.paymentMethodRef,
            vaultRequestParams = testData.vaultRequestParams,
            ForageApiResponse.Success(getVaultMessageResponse(testData.contentId))
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
    }
}
