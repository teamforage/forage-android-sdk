package com.joinforage.forage.android.pos

import com.joinforage.forage.android.ui.ForageElement
import com.joinforage.forage.android.ui.ForagePINEditText

/**
 * **[PosForageConfig] is only valid for in-store POS Terminal transactions.**
 *
 * The configuration details that Forage needs to create a functional [ForageElement].
 *
 * Pass a [PosForageConfig] instance in a call to
 * [setPosForageConfig][com.joinforage.forage.android.ui.ForageElement.setPosForageConfig] to
 * configure an Element.
 *
 * @property merchantId A unique Merchant ID that Forage provides during onboarding onboarding preceded by "mid/".
 * For example, `mid/123ab45c67`. The Merchant ID can be found in the Forage [Sandbox](https://dashboard.sandbox.joinforage.app/login/)
 * or [Production](https://dashboard.joinforage.app/login/) Dashboard.
 *
 * @property sessionToken A short-lived token that authenticates front-end requests to Forage.
 * To create one, send a server-side `POST` request from your backend to the
 * [`/session_token/`](https://docs.joinforage.app/reference/create-session-token) endpoint.
 *
 * @constructor Creates an instance of the [PosForageConfig] data class.
 */
data class PosForageConfig(
    val merchantId: String,
    val sessionToken: String
)

/**
 * The [PosTokenizeCardParams] are only valid for in-store POS Terminal transactions.
 * This data class is not supported for online transactions.
 * The information encoded on Track 2 of the EBT Card’s magnetic stripe,
 * excluding the start and stop sentinels and any LRC characters.
 *
 * @property posForageConfig The [PosForageConfig] configuration details required to authenticate with the Forage API.
 * @property track2Data The information encoded on Track 2 of the EBT Card’s magnetic stripe,
 * excluding the start and stop sentinels and any LRC characters.
 * @property reusable Optional. Indicates whether the tokenized card can be
 * reused for multiple transactions. Defaults to true.
 */
data class PosTokenizeCardParams(
    val posForageConfig: PosForageConfig,
    val track2Data: String,
    val reusable: Boolean = true
)

/**
 * The [PosRefundPaymentParams] are only valid for in-store POS Terminal transactions.
 *
 * @property foragePinEditText The [ForagePINEditText] instance that collected the customer's PIN for the refund.
 * @property paymentRef A unique string identifier that refers to the Payment to be refunded
 * as it’s represented in Forage's database. Forage returns the paymentRef in response
 * to a successful capturePayment call or in response to a request from your server that creates a Payment.
 * @property amount A positive decimal number that represents how much of the original
 * payment to refund in USD. Precision to the penny is supported.
 * **The minimum amount that can be refunded is 0.01.**
 * @property reason A string that describes why the Payment is to be refunded.
 * @property metadata Optional. A map of merchant-defined key-value pairs.
 * For example, some merchants attach their credit card processor’s ID for
 * the customer making the refund.
 */
data class PosRefundPaymentParams(
    val foragePinEditText: ForagePINEditText,
    val paymentRef: String,
    val amount: Float,
    val reason: String,
    val metadata: Map<String, String>? = null
)
