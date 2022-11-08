package com.joinforage.forage.android

import com.joinforage.forage.android.model.Card
import com.joinforage.forage.android.network.core.postCall
import com.joinforage.forage.android.network.model.PaymentMethod
import com.joinforage.forage.android.network.model.PaymentMethodRequestBody
import com.joinforage.forage.android.network.model.toJSONObject
import kotlinx.coroutines.runBlocking
import me.jorgecastillo.hiroaki.Method
import me.jorgecastillo.hiroaki.headers
import me.jorgecastillo.hiroaki.internal.MockServerSuite
import me.jorgecastillo.hiroaki.matchers.times
import me.jorgecastillo.hiroaki.verify
import okhttp3.HttpUrl
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class CreatePaymentMethodTest : MockServerSuite() {
    private val testData = ExpectedData()
    private lateinit var mockServerUrl: HttpUrl

    @Before
    override fun setup() {
        super.setup()
        mockServerUrl = server.url("payment_methods/")
    }

    @Test
    fun `it should send the correct body to create a payment method`() {
        server.givenCardToken(testData.cardNumber).returnsPaymentMethodSuccessfully()

        val paymentMethodResponse = runBlocking {
            runBlocking {
                postCall(
                    requestBuilder = Request.Builder().url(mockServerUrl),
                    json = testData.paymentMethodRequestBody.toJSONObject().toString(),
                    merchantAccount = testData.merchantAccount,
                    bearerToken = testData.bearerToken
                ).execute()
            }
        }

        val paymentMethodResponseBody = paymentMethodResponse.body?.string()
        assertThat(paymentMethodResponseBody).isNotNull

        val response = PaymentMethod.ModelMapper.from(paymentMethodResponseBody!!)
        assertThat(response).isEqualTo(
            PaymentMethod(
                ref = "e4fdc7b865",
                type = "ebt",
                balance = null,
                card = Card(
                    last4 = "7845",
                    token = "tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7"
                )
            )
        )
    }

    @Test
    fun `it should send the correct headers to create a payment method`() {
        server.givenCardToken(testData.cardNumber).returnsPaymentMethodSuccessfully()

        runBlocking {
            postCall(
                requestBuilder = Request.Builder().url(mockServerUrl),
                json = testData.paymentMethodRequestBody.toJSONObject().toString(),
                merchantAccount = testData.merchantAccount,
                bearerToken = testData.bearerToken
            ).execute()
        }

        server.verify("payment_methods/").called(
            times = times(1),
            method = Method.POST,
            headers = headers(
                "Authorization" to "Bearer ${testData.bearerToken}",
                "Merchant-Account" to testData.merchantAccount
            )
        )
    }

    private data class ExpectedData(
        val merchantAccount: String = "12345678",
        val bearerToken: String = "21a12ef352b649caa97499bed2e77350",
        val cardNumber: String = "5076801234567845",
        val paymentMethodRequestBody: PaymentMethodRequestBody = PaymentMethodRequestBody(cardNumber)
    )
}
