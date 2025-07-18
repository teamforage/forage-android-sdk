package com.joinforage.forage.android.ecom.services

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.mock.MockForagePaymentSheet.Companion.TEST_HSA_FSA_CARD
import com.joinforage.forage.android.mock.MockForagePaymentSheet.Companion.createMockForagePaymentSheet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ForageSDKTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `Verify expired session token fails when creating HSA payment method`() {
        val params = TokenizeCreditCardParams(
            foragePaymentSheet = createMockForagePaymentSheet(
                cardholderName = "John Doe",
                cardNumber = TEST_HSA_FSA_CARD,
                expiration = "12/34",
                securityCode = "111",
                zipCode = "10001"
            ),
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
