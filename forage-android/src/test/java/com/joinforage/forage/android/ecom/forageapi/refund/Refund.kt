package com.joinforage.forage.android.ecom.forageapi.refund

import com.joinforage.forage.android.core.services.getStringOrNull
import com.joinforage.forage.android.core.services.hasNonNull
import com.joinforage.forage.android.core.services.toMap
import org.json.JSONObject

/**
 * @property amount A positive decimal number that represents how much the PaymentMethod
 * was charged in USD. Precision to the penny is supported. The minimum amount that can
 * be charged is 0.01. To differentiate between a SNAP and an EBT Cash charge on
 * the same EBT Card, use [fundingType].
 *
 * @property created A UTC-8 timestamp of when the Payment was created, represented as an ISO 8601 date-time string.
 * @property deliveryAddress The address for delivery or pickup of the Order.
 * @property description A human-readable description of the Payment.
 * @property fundingType The payment instrument type. Use [fundingType] to differentiate between a SNAP (`ebt_snap`) and an EBT Cash (`ebt_cash`) charge on the same EBT Card.
 * @property isDelivery A boolean that indicates whether the Payment is for delivery (true) or pickup (false).
 * @property merchant A string that represents a unique merchant ID that Forage provides during onboarding.
 * @property metadata A map of key-value pairs that you can use to store additional information about the Payment.
 * @property paymentMethodRef The unique reference hash for the existing Forage PaymentMethod that was charged in this transaction.
 * @property receipt Most of the information that you're required to display to the customer, according to FNS regulations.
 * [receipt] is null if the data that populates the receipt is not yet available.
 * @property ref A string identifier that refers to an instance in Forage's database of a Payment object,
 * which is a one-time charge to a previously created PaymentMethod.
 * @property refunds A list of unique reference hashes for the Refund objects
 * that were created for this Payment.
 * @property status The status of the Payment. [Learn more](https://docs.joinforage.app/reference/payments#payment-lifecycle)
 * @property successDate A UTC-8 timestamp of when the Payment was successfully processed, represented as an ISO 8601 date-time string.
 * @property updated A UTC-8 timestamp of when the Payment was last updated, represented as an ISO 8601 date-time string.
 */
data class Refund(
    val amount: String,
    val created: String,
    val fundingType: String,
    val metadata: Map<String, Any>?,
    val receipt: RefundReceipt?,
    val ref: String,
    val status: String,
    val updated: String,

    // refund specific fields
    val paymentRef: String,
    val reason: String,
    val externalOrderId: String?
) {
    internal constructor(jsonString: String) : this(JSONObject(jsonString))
    internal constructor(jsonObject: JSONObject) : this(
        amount = jsonObject.getString("amount"),
        created = jsonObject.getString("created"),
        fundingType = jsonObject.getString("funding_type"),
        metadata = if (jsonObject.hasNonNull("metadata")) {
            jsonObject.getJSONObject("metadata").toMap()
        } else {
            null
        },
        receipt = if (jsonObject.hasNonNull("receipt")) {
            RefundReceipt(jsonObject.getJSONObject("receipt"))
        } else {
            null
        },
        ref = jsonObject.getString("ref"),
        status = jsonObject.getString("status"),
        updated = jsonObject.getString("updated"),
        paymentRef = jsonObject.getString("payment_ref"),
        reason = jsonObject.getString("reason"),
        externalOrderId = jsonObject.getStringOrNull("external_order_id")
    )

    internal companion object {
        /**
         * @djoksimo (2024-05-08) The deferred flows return a "thin" Payment object
         * where most fields are null or empty.
         * This utility helps us safely grab the paymentMethodRef
         * for VaultSubmitter requests without unpacking the "full" Payment object.
         */
        internal fun getPaymentMethodRef(jsonString: String): String {
            return JSONObject(jsonString).getString("payment_method")
        }
    }
}
