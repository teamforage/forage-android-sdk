package com.joinforage.forage.android.model

import com.joinforage.forage.android.core.services.forageapi.payment.Address
import com.joinforage.forage.android.core.services.forageapi.payment.Payment
import com.joinforage.forage.android.core.services.forageapi.payment.Receipt
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtBalance
import com.joinforage.forage.android.mock.MockServiceFactory
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test

class PaymentModelTests {
    internal companion object {
        val testData = MockServiceFactory.ExpectedData
        val paymentRef = testData.paymentRef
        val amount = "123.45"
        val created = "2023-04-26T18:50:57.049025-07:00"
        val deliveryAddress = Address(
            city = "New York",
            country = "US",
            line1 = null,
            line2 = null,
            state = "NY",
            zipcode = "10012"
        )
        val description = "Testing the Android SDK"
        val fundingType = "ebt_snap"
        val isDelivery = false
        val merchantFns = testData.merchantId
        val paymentMethodRef = testData.paymentMethodRef
        val status = "succeeded"
        val successDate = "2023-04-27T01:50:59.350429Z"
        val updatedDate = "2023-04-26T18:50:59.379628-07:00"

        val baseTestPaymentJsonString = """
           {
                "ref": "$paymentRef",
                "amount": "$amount",
                "created": "$created",
                "delivery_address": {
                    "city": "${deliveryAddress.city}",
                    "country": "${deliveryAddress.country}",
                    "line1": ${deliveryAddress.line1},
                    "line2": ${deliveryAddress.line2},
                    "state": "${deliveryAddress.state}",
                    "zipcode": "${deliveryAddress.zipcode}"
                },
                "description": "Testing the Android SDK",
                "funding_type": "ebt_snap",
                "is_delivery": false,
                "last_processing_error": null,
                "merchant": "$merchantFns",
                "metadata": null,
                "payment_method": "$paymentMethodRef",
                "receipt": null,
                "refunds": [],
                "status": "$status",
                "success_date": "$successDate",
                "updated": "$updatedDate"
           }
        """.trimIndent()

        val baseReceiptJsonString = """
            {
                "created": "$created",
                "ebt_cash_amount": "1.23",
                "is_voided": false,
                "last_4": "4201",
                "message": "Receipt message",
                "other_amount": "4.56",
                "sales_tax_applied": "0.01",
                "snap_amount": "5.67",
                "balance": null
            }
        """.trimIndent()

        val thinPaymentJsonString = """
            {
              "metadata": {},
              "funding_type": "ebt_snap",
              "description": "a description",
              "payment_method": "$paymentMethodRef"
            }
        """.trimIndent()
    }

    @Test
    fun `test only non-null fields`() {
        val payment = Payment(baseTestPaymentJsonString)

        assertEquals(
            Payment(
                amount = amount,
                created = created,
                deliveryAddress = deliveryAddress,
                description = description,
                fundingType = fundingType,
                isDelivery = isDelivery,
                merchant = merchantFns,
                metadata = null,
                paymentMethodRef = paymentMethodRef,
                receipt = null,
                ref = paymentRef,
                refunds = emptyList(),
                status = status,
                successDate = successDate,
                updated = updatedDate
            ),
            payment
        )
    }

    @Test
    fun `with metadata`() {
        val jsonObject = JSONObject(baseTestPaymentJsonString)

        val metadata = JSONObject().put("key", "value")
        metadata.put("1", 2)
        jsonObject.put("metadata", metadata)

        val payment = Payment(jsonObject)
        val expectedMetadata = mapOf("key" to "value", "1" to "2")
        assertEquals(expectedMetadata, payment.metadata)
    }

    @Test
    fun `receipt without balance`() {
        val receiptJsonObject = JSONObject(baseReceiptJsonString)
        val jsonObject = JSONObject(baseTestPaymentJsonString)
        jsonObject.put("receipt", receiptJsonObject)

        val payment = Payment(jsonObject)

        assertNull(payment.receipt!!.balance)
        assertEquals(
            Receipt(
                balance = null,
                created = created,
                ebtCashAmount = "1.23",
                isVoided = false,
                last4 = "4201",
                message = "Receipt message",
                snapAmount = "5.67"
            ),
            payment.receipt
        )
    }

    @Test fun `receipt with balance`() {
        val receiptJsonObject = JSONObject(baseReceiptJsonString)
        val balanceJsonObject = JSONObject()
        balanceJsonObject.put("snap", "12.34")
        balanceJsonObject.put("non_snap", "5.67")
        receiptJsonObject.put("balance", balanceJsonObject)

        val baseJsonObject = JSONObject(baseTestPaymentJsonString)
        baseJsonObject.put("receipt", receiptJsonObject)

        val payment = Payment(baseJsonObject)

        assertEquals(
            Receipt(
                balance = EbtBalance("12.34", "5.67"),
                created = created,
                ebtCashAmount = "1.23",
                isVoided = false,
                last4 = "4201",
                message = "Receipt message",
                snapAmount = "5.67"
            ),
            payment.receipt
        )
    }

    @Test
    fun `with refunds list`() {
        val baseJson = JSONObject(baseTestPaymentJsonString)
        val refundsJson = """["refun123", "abcde456", "defg890"]""".trimIndent()
        baseJson.put("refunds", JSONArray(refundsJson))

        val payment = Payment(baseJson)

        assertEquals(listOf("refun123", "abcde456", "defg890"), payment.refunds)
    }

    @Test
    fun `getPaymentMethodRef succeeds`() {
        val actualPaymentMethodRef = Payment.getPaymentMethodRef(thinPaymentJsonString)
        assertEquals(paymentMethodRef, actualPaymentMethodRef)
    }
}
