package com.joinforage.forage.android.network

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.ForageError
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtCard
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.vault.TokenizeCardService
import com.joinforage.forage.android.fixtures.givenPaymentMethod
import com.joinforage.forage.android.fixtures.returnsMissingCustomerIdPaymentMethodSuccessfully
import com.joinforage.forage.android.fixtures.returnsNonReusablePaymentMethodSuccessfully
import com.joinforage.forage.android.fixtures.returnsPaymentMethodFailed
import com.joinforage.forage.android.fixtures.returnsPaymentMethodSuccessfully
import com.joinforage.forage.android.mock.MockServiceFactory
import com.joinforage.forage.android.mock.MockVaultSubmitter
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
    private val testData = MockServiceFactory.ExpectedData

    @Before
    override fun setup() {
        super.setup()

        tokenizeCardService = MockServiceFactory(
            mockVaultSubmitter = MockVaultSubmitter(),
            logger = Log.getSilentInstance(),
            server = server
        ).createTokenizeCardService()
    }

    @Test
    fun `it should send the correct headers + body to tokenize the card`() = runTest {
        val testCustomerId = UUID.randomUUID().toString()
        server.givenPaymentMethod(testData.cardNumber, testCustomerId).returnsPaymentMethodSuccessfully()

        tokenizeCardService.tokenizeCard(testData.cardNumber, customerId = testCustomerId)

        server.verify("api/payment_methods/").called(
            times = times(1),
            method = Method.POST,
            headers = headers(
                "Authorization" to "Bearer ${testData.sessionToken}",
                "Merchant-Account" to testData.merchantId
            ),
            jsonBody = json {
                "type" / "ebt"
                "reusable" / true
                "card" / json {
                    "number" / testData.cardNumber
                }
                "customer_id" / testCustomerId
            }
        )
    }

    @Test
    fun `it should respond with the payment method on success`() = runTest {
        server.givenPaymentMethod(testData.cardNumber, testData.customerId).returnsPaymentMethodSuccessfully()

        val paymentMethodResponse = tokenizeCardService.tokenizeCard(testData.cardNumber, testData.customerId)
        assertThat(paymentMethodResponse).isExactlyInstanceOf(ForageApiResponse.Success::class.java)

        val response =
            PaymentMethod((paymentMethodResponse as ForageApiResponse.Success).data)
        assertThat(response).isEqualTo(
            PaymentMethod(
                ref = "1f148fe399",
                type = "ebt",
                balance = null,
                card = EbtCard(
                    last4 = "7845",
                    token = "tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7",
                    usState = testData.cardUsState,
                    fingerprint = testData.cardFingerprint
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
            PaymentMethod((paymentMethodResponse as ForageApiResponse.Success).data)
        assertThat(response).isEqualTo(
            PaymentMethod(
                ref = "2f148fe399",
                type = "ebt",
                balance = null,
                card = EbtCard(
                    last4 = "7845",
                    token = "tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7",
                    usState = testData.cardUsState,
                    fingerprint = testData.cardFingerprint
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
            PaymentMethod((paymentMethodResponse as ForageApiResponse.Success).data)
        assertThat(response).isEqualTo(
            PaymentMethod(
                ref = "1f148fe399",
                type = "ebt",
                balance = null,
                card = EbtCard(
                    last4 = "7845",
                    token = "tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7",
                    usState = testData.cardUsState,
                    fingerprint = testData.cardFingerprint
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
}
