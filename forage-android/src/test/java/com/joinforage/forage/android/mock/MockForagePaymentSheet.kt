package com.joinforage.forage.android.mock

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.ecom.ui.element.ForagePaymentSheet
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class MockForagePaymentSheet {
    companion object {
        const val TEST_MERCHANT_ID = "1234567"
        const val EXPIRED_SANDBOX_SESSION_TOKEN =
            "sandbox_eyJhIjogMjQ3NDQ0NCwgInNrIjogIk92Y25aNTI5QjJuZ0p2N0pZN2laeHc9PSIsICJ0IjogNDJ9.aHe7Tw.cx77tQZ6-c_9nGpvE3h4VgsUTTlP21Soa-ofDDnWF1o"
        const val TEST_HSA_FSA_CARD = "4000051230002839"

        fun createMockForagePaymentSheet(
            merchantId: String = TEST_MERCHANT_ID,
            sessionToken: String = EXPIRED_SANDBOX_SESSION_TOKEN,
            cardholderName: String = "",
            cardNumber: String = "",
            expiration: String = "",
            securityCode: String = "",
            zipCode: String = ""
        ): ForagePaymentSheet {
            val foragePaymentSheet: ForagePaymentSheet = mock()

            val forageConfig = ForageConfig(merchantId, sessionToken)
            whenever(foragePaymentSheet.getForageConfig()).thenReturn(forageConfig)

            val context: Context = ApplicationProvider.getApplicationContext()
            whenever(foragePaymentSheet.context).thenReturn(context)

            whenever(foragePaymentSheet.cardholderNameValue).thenReturn(cardholderName)
            whenever(foragePaymentSheet.cardNumberValue).thenReturn(cardNumber)
            whenever(foragePaymentSheet.expirationValueAsString).thenReturn(expiration)
            whenever(foragePaymentSheet.expirationValue).thenCallRealMethod()
            whenever(foragePaymentSheet.securityCodeValue).thenReturn(securityCode)
            whenever(foragePaymentSheet.zipCodeValue).thenReturn(zipCode)
            whenever(foragePaymentSheet.getElementState()).thenCallRealMethod()

            return foragePaymentSheet
        }
    }
}
