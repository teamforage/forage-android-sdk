package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.fixtures.givenEncryptionKey
import com.joinforage.forage.android.fixtures.returnsEncryptionKeySuccessfully
import com.joinforage.forage.android.fixtures.returnsUnauthorizedEncryptionKey
import com.joinforage.forage.android.network.EncryptionKeyService
import com.joinforage.forage.android.network.OkHttpClientBuilder
import com.joinforage.forage.android.network.model.ForageApiResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.jorgecastillo.hiroaki.internal.MockServerSuite
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckBalanceRepositoryTest : MockServerSuite() {
    private lateinit var repository: CheckBalanceRepository
    private val pinCollector = TestPinCollector()
    private val testData = ExpectedData()

    @Before
    override fun setup() {
        super.setup()

        repository = CheckBalanceRepository(
            pinCollector = pinCollector,
            encryptionKeyService = EncryptionKeyService(
                okHttpClient = OkHttpClientBuilder.provideOkHttpClient(testData.bearerToken),
                httpUrl = server.url("")
            )
        )
    }

    @Test
    fun `it should return a failure when the get encryption key returns a failure`() = runTest {
        server.givenEncryptionKey().returnsUnauthorizedEncryptionKey()

        val response = repository.checkBalance(
            paymentMethodRef = "",
            cardToken = ""
        )

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val failureResponse = response as ForageApiResponse.Failure

        assertThat(failureResponse.message).contains("Authentication credentials were not provided.")
    }

    @Test
    fun `it should return the successful response when VGS returns successfully`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()

        val expectedCheckBalanceResponse = TestPinCollector.checkBalanceResponse()

        pinCollector.setCollectPinForBalanceCheckResponse(
            paymentMethodRef = testData.paymentMethodRef,
            cardToken = testData.cardToken,
            encryptionKey = testData.encryptionKey,
            response = ForageApiResponse.Success(
                expectedCheckBalanceResponse
            )
        )

        val response = repository.checkBalance(
            paymentMethodRef = testData.paymentMethodRef,
            cardToken = testData.cardToken
        )

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Success::class.java)
        val failureResponse = response as ForageApiResponse.Success

        assertThat(failureResponse.data).isEqualTo(expectedCheckBalanceResponse)
    }

    @Test
    fun `it should return a failure response when VGS returns a failure`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()

        val expectedCheckBalanceResponse = TestPinCollector.checkBalanceInvalidCardNumberResponse()

        pinCollector.setCollectPinForBalanceCheckResponse(
            paymentMethodRef = testData.paymentMethodRef,
            cardToken = testData.cardToken,
            encryptionKey = testData.encryptionKey,
            response = ForageApiResponse.Failure(
                expectedCheckBalanceResponse
            )
        )

        val response = repository.checkBalance(
            paymentMethodRef = testData.paymentMethodRef,
            cardToken = testData.cardToken
        )

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        val failureResponse = response as ForageApiResponse.Failure

        assertThat(failureResponse.message).isEqualTo(expectedCheckBalanceResponse)
    }

    private data class ExpectedData(
        val bearerToken: String = "AbCaccesstokenXyz",
        val paymentMethodRef: String = "1f148fe399",
        val cardToken: String = "tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7",
        val encryptionKey: String = "tok_sandbox_eZeWfkq1AkqYdiAJC8iweE"
    )
}
