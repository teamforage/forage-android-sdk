package com.joinforage.forage.android.ecom.services

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.StripeCreditDebitCard
import com.joinforage.forage.android.core.services.mockBaseUrl
import com.joinforage.forage.android.mock.TestForagePaymentSheet
import com.joinforage.forage.android.mock.TestForagePaymentSheet.Companion.MOCK_SESSION_TOKEN
import com.joinforage.forage.android.mock.TestForagePaymentSheet.Companion.TEST_HSA_FSA_CARD
import com.joinforage.forage.android.mock.TestForagePaymentSheet.Companion.TEST_MERCHANT_ID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ForageSDKTest {
    companion object {
        private lateinit var mockWebServer: MockWebServer

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            mockWebServer = MockWebServer()
            mockWebServer.start()
            mockBaseUrl = mockWebServer.url("/").toString()
        }

        @AfterClass
        @JvmStatic
        fun afterClass() {
            mockWebServer.shutdown()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `Verify HSA payment method creation success`() {
        val body = """
            {
                "ref": "b860006ec0",
                "type": "credit",
                "reusable": true,
                "card": {
                    "brand": "visa",
                    "exp_month": 1,
                    "exp_year": 2029,
                    "last_4": "2839",
                    "created": "2025-07-28T06:15:51.973920-07:00",
                    "is_hsa_fsa": true,
                    "psp_customer_id": "cus_SdvCfjGupCoyZo",
                    "payment_method_id": "pm_1RpquAP4HzVVeE0mQH7U9bbF"
                },
                "balance": null,
                "billing_details": null
            }
        """.trimIndent()
        val mockResponse = MockResponse().setResponseCode(201).setBody(body)
        mockWebServer.enqueue(mockResponse)

        val foragePaymentSheet = TestForagePaymentSheet().create().apply {
            setForageConfig(ForageConfig(TEST_MERCHANT_ID, MOCK_SESSION_TOKEN))
            cardholderNameValue = "John Doe"
            cardNumberValue = TEST_HSA_FSA_CARD
            expirationValueAsString = "12/34"
            securityCodeValue = "123"
            zipCodeValue = "12345"
        }

        val params = TokenizeCreditCardParams(
            foragePaymentSheet = foragePaymentSheet,
            customerId = "test-customer-id",
            reusable = true
        )

        runTest {
            val response = ForageSDK().tokenizeCreditCard(params)
            assertThat(response).isInstanceOf(ForageApiResponse.Success::class.java)
            response as ForageApiResponse.Success
            val expectedPaymentMethod = PaymentMethod(
                card = StripeCreditDebitCard(
                    last4 = "2839",
                    brand = "visa",
                    expMonth = 1,
                    expYear = 2029,
                    isHsaFsa = true,
                    pspCustomerId = "cus_SdvCfjGupCoyZo",
                    paymentMethodId = "pm_1RpquAP4HzVVeE0mQH7U9bbF"
                ),
                ref = "b860006ec0",
                type = "credit",
                balance = null,
                customerId = null, // API does not return customerId
                reusable = true
            )
            assertThat(response.toPaymentMethod()).isEqualTo(expectedPaymentMethod)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `Verify HSA payment method creation failure`() {
        val body = """
            {
                "path": "/api/payment_methods/",
                "errors": [
                    {
                        "code": "expired_session_token",
                        "message": "This token has expired",
                        "source": {
                            "resource": "Session_Token",
                            "ref": ""
                        }
                    }
                ]
            }
        """.trimIndent()
        val mockResponse = MockResponse().setResponseCode(401).setBody(body)
        mockWebServer.enqueue(mockResponse)

        val foragePaymentSheet = TestForagePaymentSheet().create().apply {
            setForageConfig(ForageConfig(TEST_MERCHANT_ID, MOCK_SESSION_TOKEN))
            cardholderNameValue = "John Doe"
            cardNumberValue = TEST_HSA_FSA_CARD
            expirationValueAsString = "12/34"
            securityCodeValue = "123"
            zipCodeValue = "12345"
        }

        val params = TokenizeCreditCardParams(
            foragePaymentSheet = foragePaymentSheet,
            customerId = "test-customer-id",
            reusable = true
        )

        runTest {
            val response = ForageSDK().tokenizeCreditCard(params)
            assertThat(response).isInstanceOf(ForageApiResponse.Failure::class.java)
            response as ForageApiResponse.Failure
            assertThat(response.error.code).isEqualTo("expired_session_token")
        }
    }
}
