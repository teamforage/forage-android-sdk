package com.joinforage.forage.android.core.services.forageapi.paymentmethod

import org.json.JSONObject

/**
 * @property last4 The last 4 digits of the card number.
 * @property brand
 * @property expMonth
 * @property expYear
 * @property bin
 * @property isHsaFsa
 * @property pspCustomerId
 * @property paymentMethodId
 */
data class StripeCreditDebitCard(
    override val last4: String,
    override val brand: String,
    override val expMonth: Int,
    override val expYear: Int,
    override val isHsaFsa: Boolean,
    val pspCustomerId: String,
    val paymentMethodId: String
) : CreditDebitCard {
    internal constructor(jsonObject: JSONObject) : this(
        last4 = jsonObject.getString("last_4"),
        brand = jsonObject.getString("brand"),
        expMonth = jsonObject.getInt("exp_month"),
        expYear = jsonObject.getInt("exp_year"),
        isHsaFsa = jsonObject.getBoolean("is_hsa_fsa"),
        pspCustomerId = jsonObject.getString("psp_customer_id"),
        paymentMethodId = jsonObject.getString("payment_method_id")
    )
}
