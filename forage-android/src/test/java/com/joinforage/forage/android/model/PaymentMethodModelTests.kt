package com.joinforage.forage.android.model

import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtBalance
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtCard
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.ui.element.state.pan.USState
import junit.framework.TestCase.assertEquals
import org.json.JSONObject
import org.junit.Test

class PaymentMethodModelTests {
    companion object {
        val ref = "abce123"
        val type = "ebt"
        val card = EbtCard(
            last4 = "4201",
            token = "moss123",
            fingerprint = "fingerprint1234",
            usState = null
        )

        val baseJsonString = """
            {
                "ref": "$ref",
                "type": "$type",
                "card": {
                    "last_4": "${card.last4}",
                    "token": "${card.token}",
                    "fingerprint": "${card.fingerprint}"
                }
            }
        """.trimIndent()
    }

    @Test
    fun `Without optional fields`() {
        val paymentMethod = PaymentMethod(baseJsonString)

        assertEquals(
            PaymentMethod(
                card = card,
                ref = ref,
                type = type,
                balance = null,
                customerId = null,
                reusable = true // should default to true
            ),
            paymentMethod
        )
    }

    @Test
    fun `With null fields`() {
        val baseJsonObject = JSONObject(baseJsonString)
        baseJsonObject.put("balance", JSONObject.NULL)
        baseJsonObject.put("customer_id", JSONObject.NULL)
        baseJsonObject.put("reusable", JSONObject.NULL)

        val paymentMethod = PaymentMethod(baseJsonObject)
        assertEquals(
            PaymentMethod(
                card = card,
                ref = ref,
                type = type,
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

        val baseJsonObject = JSONObject(baseJsonString)
        baseJsonObject.put("balance", balanceJsonObject)

        val paymentMethod = PaymentMethod(baseJsonObject)
        assertEquals(
            PaymentMethod(
                card = card,
                ref = ref,
                type = type,
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

        val baseJsonObject = JSONObject(baseJsonString)
        baseJsonObject.put("customer_id", customerId)

        val paymentMethod = PaymentMethod(baseJsonObject)
        assertEquals(
            PaymentMethod(
                card = card,
                ref = ref,
                type = type,
                balance = null,
                customerId = customerId,
                reusable = true
            ),
            paymentMethod
        )
    }

    @Test
    fun `With reusable false`() {
        val baseJsonObject = JSONObject(baseJsonString)
        baseJsonObject.put("reusable", false)

        val paymentMethod = PaymentMethod(baseJsonObject)
        assertEquals(
            PaymentMethod(
                card = card,
                ref = ref,
                type = type,
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
        val cardWithState = card.copy(usState = USState.CALIFORNIA)

        val cardJsonObject = JSONObject(baseJsonString).getJSONObject("card")
        cardJsonObject.put("state", state)

        val baseJsonObject = JSONObject(baseJsonString)
        baseJsonObject.put("card", cardJsonObject)

        val paymentMethod = PaymentMethod(baseJsonObject)
        assertEquals(
            PaymentMethod(
                card = cardWithState,
                ref = ref,
                type = type,
                balance = null,
                customerId = null,
                reusable = true
            ),
            paymentMethod
        )
    }
}
