package com.joinforage.forage.android.pos

import com.joinforage.forage.android.ui.ForageConfig
import com.joinforage.forage.android.ui.ForagePINEditText

/**
 * The [PosTokenizeCardParams] are only valid for in-store POS Terminal transactions.
 * This data class is not supported for online transactions.
 * The information encoded on Track 2 of the EBT Card’s magnetic stripe,
 * excluding the start and stop sentinels and any LRC characters.
 *
 * @property forageConfig The configuration details required to authenticate with the Forage API.
 * @property track2Data The information encoded on Track 2 of the EBT Card’s magnetic stripe,
 * excluding the start and stop sentinels and any LRC characters.
 * @property reusable Optional. Indicates whether the tokenized card can be
 * reused for multiple transactions. Defaults to true.
 */
data class PosTokenizeCardParams(
    val forageConfig: ForageConfig,
    val track2Data: String,
    val reusable: Boolean = true
)

/**
 * The [PosRefundPaymentParams] are only valid for in-store POS Terminal transactions.
 *
 * @property amount A positive decimal number that represents how much of the original
 * payment to refund in USD. Precision to the penny is supported.
 *
 * **The minimum amount that can be refunded is 0.01.**
 *
 * @property foragePinEditText The [ForagePINEditText] instance that collected the customer's PIN for the refund.
 * @property paymentRef A unique string identifier that refers to the Payment to be refunded
 * as it’s represented in Forage's database. Forage returns the paymentRef in response
 * to a successful capturePayment call or in response to a request from your server that creates a Payment.
 * @reason reason A string that describes why the Payment is to be refunded.
 * @property metadata Optional. A map of merchant-defined key-value pairs.
 * For example, some merchants attach their credit card processor’s ID for
 * the customer making the refund.
 */
data class PosRefundPaymentParams(
    val amount: Float,
    val foragePinEditText: ForagePINEditText,
    val paymentRef: String,
    val reason: String,
    val metadata: Map<String, String>? = null
)
