package com.joinforage.forage.android.pos.services.forageapi.refund

import com.joinforage.forage.android.core.services.forageapi.paymentmethod.Balance
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtBalance
import org.json.JSONObject

/**
 * @property created A UTC-8 timestamp of when the Receipt was created, represented as an ISO 8601 date-time string.
 * @property ebtCashAmount The USD amount charged/refunded to the EBT Cash balance of the EBT Card, represented as a numeric string.
 * @property isVoided A boolean that indicates whether the Receipt is voided.
 * @property last4 The last four digits of the EBT Card number that was charged.
 * @property message A message from the EBT payment network that must be displayed to the EBT cardholder.
 * @property snapAmount The USD amount charged/refunded to the SNAP balance of the EBT Card, represented as a numeric string.
 * @property balance The remaining balance on the EBT Card after the Payment was processed.
 */
data class RefundReceipt(
    val created: String,
    val ebtCashAmount: String,
    val isVoided: Boolean,
    val last4: String,
    val message: String,
    val snapAmount: String,
    val balance: Balance?,
    // refund specific fields
    val refNumber: String, //  "c377d0fd53",
    val otherAmount: String, //  "0.00",
    val salesTaxApplied: String, //  "0.00",
    val transactionType: String, //  "Refund",
    val sequenceNumber: String //  "RE000d9d
) {
    internal constructor(jsonObject: JSONObject) : this(
        balance = if (!jsonObject.isNull("balance")) {
            EbtBalance(jsonObject.getJSONObject("balance"))
        } else {
            null
        },
        created = jsonObject.getString("created"),
        ebtCashAmount = jsonObject.getString("ebt_cash_amount"),
        isVoided = jsonObject.getBoolean("is_voided"),
        last4 = jsonObject.getString("last_4"),
        message = jsonObject.getString("message"),
        snapAmount = jsonObject.getString("snap_amount"),

        // refund specific fields
        refNumber = jsonObject.getString("ref_number"),
        otherAmount = jsonObject.getString("other_amount"),
        salesTaxApplied = jsonObject.getString("sales_tax_applied"),
        transactionType = jsonObject.getString("transaction_type"),
        sequenceNumber = jsonObject.getString("sequence_number")
    )
}