package com.joinforage.forage.android.model

import com.joinforage.forage.android.getStringOrNull
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject

/**
 * @property city The name of the city.
 * @property country Either us or US. Defaults to US if not provided.
 * @property line1 The first line of the street address.
 * @property line2 The second line of the street address.
 * @property state The two-letter abbreviation, can be upper or lowercase, for the US state.
 * @property zipcode The zip or postal code.
 */
@Parcelize
data class Address(
    val city: String,
    val country: String,
    val line1: String,
    val line2: String,
    val state: String,
    val zipcode: String,
): ForageModel

/**
 * @property created A UTC-8 timestamp of when the Receipt was created, represented as an ISO 8601 date-time string.
 * @property ebtCashAmount The USD amount charged/refunded to the EBT Cash balance of the EBT Card, represented as a numeric string.
 * @property isVoided A boolean that indicates whether the Receipt is voided.
 * @property last4 The last four digits of the EBT Card number that was charged.
 * @property message A message from the EBT payment network that must be displayed to the EBT cardholder.
 * @property salesTaxApplied The USD amount of taxes charged to the customer’s non-EBT payment instrument, represented as a numeric string.
 * @property snapAmount The USD amount charged/refunded to the SNAP balance of the EBT Card, represented as a numeric string.
 * @property balance The remaining balance on the EBT Card after the Payment was processed.
 */
@Parcelize
data class Receipt(
    val created: String,
    val ebtCashAmount: String,
    val isVoided: Boolean,
    val last4: String,
    val message: String,
    val otherAmount: String,
    val salesTaxApplied: String,
    val snapAmount: String,
    val balance: Balance?,
): ForageModel

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
 * @property lastProcessingError The code and message values corresponding to the most recent Payments API error.
 * @property merchant A string that represents a unique merchant ID that Forage provides during onboarding.
 * @property metadata A map of key-value pairs that you can use to store additional information about the Payment.
 * @property paymentMethodRef The unique reference hash for the existing Forage PaymentMethod that was charged in this transaction.
 * @property receipt Most of the information that you're required to display to the customer, according to FNS regulations.
 * [receipt] is null if the data that populates the receipt is not yet available.
 * @property ref A string identifier that refers to an instance in Forage's database of a Payment object,
 * which is a  one-time charge to a previously created PaymentMethod.
 * @property refunds A list of unique reference hashes for the Refund objects
 * that were created for this Payment.
 * @property status The status of the Payment. [Learn more](https://docs.joinforage.app/reference/payments#payment-lifecycle)
 * @property successDate A UTC-8 timestamp of when the Payment was successfully processed, represented as an ISO 8601 date-time string.
 */
@Parcelize
data class Payment(
    val amount: String,
    val created: String,
    val deliveryAddress: Address,
    val description: String,
    val fundingType: String,
    val isDelivery: Boolean,
    val lastProcessingError: String?,
    val merchant: String,
    val metadata: Map<String, String>,
    val paymentMethodRef: String,
    val receipt: Receipt?,
    val ref: String,
    val refunds: List<String>,
    val status: String,
    val successDate: String?,
    val updated: String,
): ForageModel {
    internal object ModelMapper {
        fun from(jsonString: String): Payment {
            val jsonObject = JSONObject(jsonString)
            println("MOSS: $jsonObject")
            val receipt = if (!jsonObject.isNull("receipt")) {
                toReceipt(jsonObject.getJSONObject("receipt"))
            } else {
                null
            }

            return Payment(
                amount = jsonObject.getString("amount"),
                created = jsonObject.getString("created"),
                deliveryAddress = toAddress(jsonObject.getJSONObject("delivery_address")),
                description = jsonObject.getString("description"),
                fundingType = jsonObject.getString("funding_type"),
                isDelivery = jsonObject.getBoolean("is_delivery"),
                lastProcessingError = jsonObject.getStringOrNull("last_processing_error"),
                merchant = jsonObject.getString("merchant"),
                metadata = jsonObject.getJSONObject("metadata").toMap(),
                paymentMethodRef = jsonObject.getString("payment_method"),
                receipt = receipt,
                ref = jsonObject.getString("ref"),
                refunds = jsonObject.getJSONArray("refunds").toListOfStrings(),
                status = jsonObject.getString("status"),
                successDate = jsonObject.getStringOrNull("success_date"),
                updated = jsonObject.getString("updated")
            )
        }
    }
}

internal fun toAddress(jsonObject: JSONObject): Address {
    val city = jsonObject.getString("city")
    val country = jsonObject.getString("country")
    val line1 = jsonObject.getString("line1")
    val line2 = jsonObject.getString("line2")
    val zipcode = jsonObject.getString("zipcode")
    val state = jsonObject.getString("state")

    return Address(
        city = city,
        country = country,
        line1 = line1,
        line2 = line2,
        zipcode = zipcode,
        state = state
    )
}

internal fun toReceipt(jsonObject: JSONObject): Receipt {
    val balance = if (!jsonObject.isNull("balance")) {
        Balance.EbtBalance.ModelMapper.fromApiResponse(jsonObject.getJSONObject("balance"))
    } else {
        null
    }

    return Receipt(
        balance = balance,
        created = jsonObject.getString("created"),
        ebtCashAmount = jsonObject.getString("ebt_cash_amount"),
        isVoided = jsonObject.getBoolean("is_voided"),
        last4 = jsonObject.getString("last_4"),
        message = jsonObject.getString("message"),
        otherAmount = jsonObject.getString("other_amount"),
        salesTaxApplied = jsonObject.getString("sales_tax_applied"),
        snapAmount = jsonObject.getString("snap_amount"),
    )
}

internal fun JSONArray.toListOfStrings(): List<String> =
    List(this.length()) { index -> this.getString(index) }

internal fun JSONObject.toMap(): Map<String, String> = keys().asSequence().associateWith { get(it).toString() }