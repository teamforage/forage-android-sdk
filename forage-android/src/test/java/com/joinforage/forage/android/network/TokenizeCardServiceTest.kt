package com.joinforage.forage.android.network

import com.joinforage.forage.android.fixtures.givenCardNumberWithUserId
import com.joinforage.forage.android.fixtures.givenCardNumberWithoutUserId
import com.joinforage.forage.android.fixtures.returnsPaymentMethodFailed
import com.joinforage.forage.android.fixtures.returnsPaymentMethodSuccessfully
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.network.model.PaymentMethodRequestBody
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.jorgecastillo.hiroaki.Method
import me.jorgecastillo.hiroaki.headers
import me.jorgecastillo.hiroaki.internal.MockServerSuite
import me.jorgecastillo.hiroaki.matchers.times
import me.jorgecastillo.hiroaki.models.json
import me.jorgecastillo.hiroaki.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class TokenizeCardServiceTest : MockServerSuite() {
    private lateinit var tokenizeCardService: TokenizeCardService
    private lateinit var idempotencyKey: String
    private val expectedApiVersion: String = "2023-03-31"
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
        server.givenCardNumberWithoutUserId(testData.cardNumber).returnsPaymentMethodSuccessfully(false)

        tokenizeCardService.tokenizeCard(testData.cardNumber)

        server.verify("api/payment_methods/").called(
            times = times(1),
            method = Method.POST,
            headers = headers(
                "Authorization" to "Bearer ${testData.bearerToken}",
                "Merchant-Account" to testData.merchantAccount,
                "IDEMPOTENCY-KEY" to idempotencyKey,
                "API-VERSION" to expectedApiVersion
            )
        )
    }

    @Test
    fun `it should respond with the payment method on success without a user_id`() = runTest {
        server.givenCardNumberWithoutUserId(testData.cardNumber).returnsPaymentMethodSuccessfully(false)

        tokenizeCardService.tokenizeCard(testData.cardNumber)

        server.verify("api/payment_methods/").called(
            times = times(1),
            method = Method.POST,
            headers = headers(
                "Authorization" to "Bearer ${testData.bearerToken}",
                "Merchant-Account" to testData.merchantAccount,
                "IDEMPOTENCY-KEY" to idempotencyKey,
                "API-VERSION" to expectedApiVersion
            )
        )
    }

    @Test
    fun `it should respond with the payment method on success with a user_id`() = runTest {
        server.givenCardNumberWithUserId(testData.cardNumber, testData.userId).returnsPaymentMethodSuccessfully(true)

        tokenizeCardService.tokenizeCard(testData.cardNumber, testData.userId)

        server.verify("api/payment_methods/").called(
            times = times(1),
            method = Method.POST,
            jsonBody = json {
                "type" / "ebt"
                "reusable" / true
                "card" / json {
                    "number" / testData.cardNumber
                }
                "user_id" / "test-user-id" // includes user_id
            }
        )
    }

    @Test
    fun `it should respond with an error on failure to create Payment Method`() = runTest {
        server.givenCardNumberWithoutUserId(testData.cardNumber).returnsPaymentMethodFailed()

        val paymentMethodResponse = tokenizeCardService.tokenizeCard(testData.cardNumber)
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
        val userId: String = "test-user-id",
        val paymentMethodRequestBody: PaymentMethodRequestBody = PaymentMethodRequestBody(cardNumber, userId)
    )
}
