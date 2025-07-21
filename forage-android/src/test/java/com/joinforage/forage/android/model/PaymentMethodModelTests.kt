package com.joinforage.forage.android.model

import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtBalance
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtCard
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.StripeCreditDebitCard
import com.joinforage.forage.android.core.ui.element.state.pan.USState
import junit.framework.TestCase.assertEquals
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertFailsWith

class PaymentMethodModelTests {
    companion object {
        val ref = "abce123"

        val ebtType = "ebt"
        val ebtCard = EbtCard(
            last4 = "4201",
            number = null,
            token = "moss123",
            fingerprint = "fingerprint1234",
            usState = null
        )
        val ebtBaseJsonString = """
            {
                "ref": "$ref",
                "type": "$ebtType",
                "card": {
                    "last_4": "${ebtCard.last4}",
                    "token": "${ebtCard.token}",
                    "fingerprint": "${ebtCard.fingerprint}"
                }
            }
        """.trimIndent()

        val creditType = "credit"
        val creditCard = StripeCreditDebitCard(
            last4 = "4201",
            brand = "visa",
            expMonth = 12,
            expYear = 2050,
            isHsaFsa = true,
            pspCustomerId = "cus_12345678901234",
            paymentMethodId = "pm_123456789012345678901234"
        )
        val creditBaseJsonString = """
            {
                "ref": "$ref",
                "type": "$creditType",
                "card": {
                    "last_4": "${creditCard.last4}",
                    "brand": "${creditCard.brand}",
                    "exp_month": ${creditCard.expMonth},
                    "exp_year": ${creditCard.expYear},
                    "is_hsa_fsa": ${creditCard.isHsaFsa},
                    "psp_customer_id": "${creditCard.pspCustomerId}",
                    "payment_method_id": "${creditCard.paymentMethodId}",
                }
            }
        """.trimIndent()

        val benefitBaseJsonString = """
            {
                "ref": "$ref",
                "type": "benefit",
                "card": {}
            }
        """.trimIndent()
    }

    @Test
    fun `EBT card type without optional fields`() {
        val paymentMethod = PaymentMethod(ebtBaseJsonString)

        assertEquals(
            PaymentMethod(
                card = ebtCard,
                ref = ref,
                type = ebtType,
                balance = null,
                customerId = null,
                reusable = true // should default to true
            ),
            paymentMethod
        )
    }

    @Test
    fun `Credit card type without optional fields`() {
        val paymentMethod = PaymentMethod(creditBaseJsonString)

        assertEquals(
            PaymentMethod(
                card = creditCard,
                ref = ref,
                type = creditType,
                balance = null,
                customerId = null,
                reusable = true // should default to true
            ),
            paymentMethod
        )
    }

    @Test
    fun `Unknown card type without optional fields`() {
        assertFailsWith<IllegalStateException> { PaymentMethod(benefitBaseJsonString) }
    }

    @Test
    fun `With null fields`() {
        val baseJsonObject = JSONObject(ebtBaseJsonString)
        baseJsonObject.put("balance", JSONObject.NULL)
        baseJsonObject.put("customer_id", JSONObject.NULL)
        baseJsonObject.put("reusable", JSONObject.NULL)

        val paymentMethod = PaymentMethod(baseJsonObject)
        assertEquals(
            PaymentMethod(
                card = ebtCard,
                ref = ref,
                type = ebtType,
                balance = null,
                customerId = null,
                reusable = true // should default to true
            ),
            paymentMethod
        )
    }

    @Test
    fun `With balance`() {
        val balanceJsonObject = JSONObject(
            """
            {
                "snap": "10.00",
                "non_snap": "10.00"
            }
            """.trimIndent()
        )

        val baseJsonObject = JSONObject(ebtBaseJsonString)
        baseJsonObject.put("balance", balanceJsonObject)

        val paymentMethod = PaymentMethod(baseJsonObject)
        assertEquals(
            PaymentMethod(
                card = ebtCard,
                ref = ref,
                type = ebtType,
                balance = EbtBalance(balanceJsonObject),
                customerId = null,
                reusable = true
            ),
            paymentMethod
        )
    }

    @Test
    fun `With customer id`() {
        val customerId = "customer123"

        val baseJsonObject = JSONObject(ebtBaseJsonString)
        baseJsonObject.put("customer_id", customerId)

        val paymentMethod = PaymentMethod(baseJsonObject)
        assertEquals(
            PaymentMethod(
                card = ebtCard,
                ref = ref,
                type = ebtType,
                balance = null,
                customerId = customerId,
                reusable = true
            ),
            paymentMethod
        )
    }

    @Test
    fun `With reusable false`() {
        val baseJsonObject = JSONObject(ebtBaseJsonString)
        baseJsonObject.put("reusable", false)

        val paymentMethod = PaymentMethod(baseJsonObject)
        assertEquals(
            PaymentMethod(
                card = ebtCard,
                ref = ref,
                type = ebtType,
                balance = null,
                customerId = null,
                reusable = false
            ),
            paymentMethod
        )
    }

    @Test
    fun `With card (US) state`() {
        val state = "CA"
        val cardWithState = ebtCard.copy(usState = USState.CALIFORNIA)

        val cardJsonObject = JSONObject(ebtBaseJsonString).getJSONObject("card")
        cardJsonObject.put("state", state)

        val baseJsonObject = JSONObject(ebtBaseJsonString)
        baseJsonObject.put("card", cardJsonObject)

        val paymentMethod = PaymentMethod(baseJsonObject)
        assertEquals(
            PaymentMethod(
                card = cardWithState,
                ref = ref,
                type = ebtType,
                balance = null,
                customerId = null,
                reusable = true
            ),
            paymentMethod
        )
    }
}
