package com.joinforage.forage.android.network

import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.fixtures.givenPaymentMethod
import com.joinforage.forage.android.fixtures.returnsMissingCustomerIdPaymentMethodSuccessfully
import com.joinforage.forage.android.fixtures.returnsNonReusablePaymentMethodSuccessfully
import com.joinforage.forage.android.fixtures.returnsPaymentMethodFailed
import com.joinforage.forage.android.fixtures.returnsPaymentMethodSuccessfully
import com.joinforage.forage.android.model.Card
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
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
            httpUrl = server.url("").toUrl().toString(),
            logger = Log.getSilentInstance()
        )
    }

    @Test
    fun `it should send the correct headers to tokenize the card`() = runTest {
        val testCustomerId = UUID.randomUUID().toString()
        server.givenPaymentMethod(testData.cardNumber, testCustomerId).returnsPaymentMethodSuccessfully()

        tokenizeCardService.tokenizeCard(testData.cardNumber, customerId = testCustomerId)

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
        server.givenPaymentMethod(testData.cardNumber, testData.customerId).returnsPaymentMethodSuccessfully()

        val paymentMethodResponse = tokenizeCardService.tokenizeCard(testData.cardNumber, testData.customerId)
        assertThat(paymentMethodResponse).isExactlyInstanceOf(ForageApiResponse.Success::class.java)

        val response =
            PaymentMethod.ModelMapper.from((paymentMethodResponse as ForageApiResponse.Success).data)
        assertThat(response).isEqualTo(
            PaymentMethod(
                ref = "1f148fe399",
                type = "ebt",
                balance = null,
                card = Card(
                    last4 = "7845",
                    token = "tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7"
                ),
                customerId = "test-android-customer-id",
                reusable = true
            )
        )
    }

    @Test
    fun `it should successfully create a PaymentMethod with a missing customerId`() = runTest {
        server.givenPaymentMethod(testData.cardNumber).returnsMissingCustomerIdPaymentMethodSuccessfully()

        val paymentMethodResponse = tokenizeCardService.tokenizeCard(testData.cardNumber)
        assertThat(paymentMethodResponse).isExactlyInstanceOf(ForageApiResponse.Success::class.java)

        val response =
            PaymentMethod.ModelMapper.from((paymentMethodResponse as ForageApiResponse.Success).data)
        assertThat(response).isEqualTo(
            PaymentMethod(
                ref = "2f148fe399",
                type = "ebt",
                balance = null,
                card = Card(
                    last4 = "7845",
                    token = "tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7"
                ),
                reusable = true,
                customerId = null
            )
        )
    }

    @Test
    fun `it should handle the reusable parameter when provided`() = runTest {
        val testCustomerId = UUID.randomUUID().toString()
        val reusable = false
        server.givenPaymentMethod(testData.cardNumber, testCustomerId, reusable).returnsNonReusablePaymentMethodSuccessfully()

        val paymentMethodResponse = tokenizeCardService.tokenizeCard(testData.cardNumber, testCustomerId, reusable)
        assertThat(paymentMethodResponse).isExactlyInstanceOf(ForageApiResponse.Success::class.java)

        val response =
            PaymentMethod.ModelMapper.from((paymentMethodResponse as ForageApiResponse.Success).data)
        assertThat(response).isEqualTo(
            PaymentMethod(
                ref = "1f148fe399",
                type = "ebt",
                balance = null,
                card = Card(
                    last4 = "7845",
                    token = "tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7"
                ),
                customerId = "test-android-customer-id",
                reusable = false
            )
        )
    }

    @Test
    fun `it should handle the absence of reusable parameter`() = runTest {
        val testCustomerId = UUID.randomUUID().toString()
        server.givenPaymentMethod(testData.cardNumber, testCustomerId).returnsPaymentMethodSuccessfully()

        val paymentMethodResponse = tokenizeCardService.tokenizeCard(testData.cardNumber, testCustomerId)
        assertThat(paymentMethodResponse).isExactlyInstanceOf(ForageApiResponse.Success::class.java)
    }

    @Test
    fun `it should respond with an error on failure to create Payment Method`() = runTest {
        server.givenPaymentMethod(testData.cardNumber, testData.customerId).returnsPaymentMethodFailed()

        val paymentMethodResponse = tokenizeCardService.tokenizeCard(testData.cardNumber, testData.customerId)
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
        val customerId: String = "test-android-customer-id",
        val reusable: Boolean = false,
        val paymentMethodRequestBody: PaymentMethodRequestBody = PaymentMethodRequestBody(cardNumber = cardNumber, customerId = customerId)
    )
}
