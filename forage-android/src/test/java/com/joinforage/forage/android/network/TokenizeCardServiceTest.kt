package com.joinforage.forage.android.network

import com.joinforage.forage.android.fixtures.givenCardToken
import com.joinforage.forage.android.fixtures.returnsPaymentMethodFailed
import com.joinforage.forage.android.fixtures.returnsPaymentMethodSuccessfully
import com.joinforage.forage.android.model.Card
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.network.model.PaymentMethod
import com.joinforage.forage.android.network.model.PaymentMethodRequestBody
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.jorgecastillo.hiroaki.Method
import me.jorgecastillo.hiroaki.headers
import me.jorgecastillo.hiroaki.internal.MockServerSuite
import me.jorgecastillo.hiroaki.matchers.times
import me.jorgecastillo.hiroaki.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class TokenizeCardServiceTest : MockServerSuite() {
    private lateinit var tokenizeCardService: TokenizeCardService
    private lateinit var idempotencyKey: String
    private val testData = ExpectedData()

    @Before
    override fun setup() {
        super.setup()

        idempotencyKey = UUID.randomUUID().toString()

        tokenizeCardService = TokenizeCardService(
            okHttpClient = OkHttpClientBuilder.provideOkHttpClient(
                testData.bearerToken,
                testData.merchantAccount,
                idempotencyKey
            ),
            httpUrl = server.url("")
        )
    }

    @Test
    fun `it should send the correct headers to tokenize the card`() = runTest {
        val testUserId = UUID.randomUUID().toString()
        server.givenCardToken(testData.cardNumber, testUserId).returnsPaymentMethodSuccessfully()

        tokenizeCardService.tokenizeCard(testData.cardNumber, userId = testUserId)

        server.verify("api/payment_methods/").called(
            times = times(1),
            method = Method.POST,
            headers = headers(
                "Authorization" to "Bearer ${testData.bearerToken}",
                "Merchant-Account" to testData.merchantAccount,
                "IDEMPOTENCY-KEY" to idempotencyKey
            )
        )
    }

    @Test
    fun `it should respond with the payment method on success`() = runTest {
        server.givenCardToken(testData.cardNumber, testData.userId).returnsPaymentMethodSuccessfully()

        val paymentMethodResponse = tokenizeCardService.tokenizeCard(testData.cardNumber, testData.userId)
        assertThat(paymentMethodResponse).isExactlyInstanceOf(ForageApiResponse.Success::class.java)

        val response =
            PaymentMethod.ModelMapper.from((paymentMethodResponse as ForageApiResponse.Success).data)
        assertThat(response).isEqualTo(
            PaymentMethod(
                ref = "e4fdc7b865",
                type = "ebt",
                balance = null,
                card = Card(
                    last4 = "7845",
                    token = "tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7"
                ),
                userId = "test-android-user-id"
            )
        )
    }

    @Test
    fun `it should respond with an error on failure to create Payment Method`() = runTest {
        server.givenCardToken(testData.cardNumber, testData.userId).returnsPaymentMethodFailed()

        val paymentMethodResponse = tokenizeCardService.tokenizeCard(testData.cardNumber, testData.userId)
        assertThat(paymentMethodResponse).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)

        val response = paymentMethodResponse as ForageApiResponse.Failure
        assertThat(response.errors[0]).isEqualTo(
            ForageError(400, "cannot_parse_request_body", "EBT Cards must be 16-19 digits long!")
        )
    }

    private data class ExpectedData(
        val merchantAccount: String = "12345678",
        val bearerToken: String = "AbCaccesstokenXyz",
        val cardNumber: String = "5076801234567845",
        val userId: String = "test-android-user-id",
        val paymentMethodRequestBody: PaymentMethodRequestBody = PaymentMethodRequestBody(cardNumber=cardNumber, userId=userId)
    )
}
