package com.joinforage.forage.android.pos

import com.joinforage.forage.android.core.ForagePinElement
import com.joinforage.forage.android.ui.ForageElement

/**
 * **[PosForageConfig] is only valid for in-store POS Terminal transactions via [ForageTerminalSDK].**
 *
 * The configuration details that Forage needs to create a functional [ForageElement].
 *
 * Pass a [PosForageConfig] instance in a call to
 * [setPosForageConfig][com.joinforage.forage.android.ui.ForageElement.setPosForageConfig] to
 * configure an Element.
 * [PosForageConfig] is also passed as a parameter to [PosTokenizeCardParams].
 *
 * @property merchantId A unique Merchant ID that Forage provides during onboarding
 * preceded by "mid/".
 * For example, `mid/123ab45c67`. The Merchant ID can be found in the Forage
 * [Sandbox](https://dashboard.sandbox.joinforage.app/login/)
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
 * A model that represents the parameters that [ForageTerminalSDK] requires to tokenize a card via
 * a magnetic swipe from a physical POS Terminal.
 * This data class is not supported for online-only transactions.
 * [PosTokenizeCardParams] are passed to the
 * [tokenizeCard][com.joinforage.forage.android.pos.ForageTerminalSDK.tokenizeCard] method.
 *
 * @property posForageConfig **Required**. The [PosForageConfig] configuration details required to
 * authenticate with the Forage API.
 * @property track2Data **Required**. The information encoded on Track 2 of the card’s magnetic
 * stripe, excluding the start and stop sentinels and any LRC characters. _Example value_:
 * `"123456789123456789=123456789123"`
 * @property reusable Optional. A boolean that indicates whether the same card can be used to create
 * multiple payments. Defaults to true.
 */
data class PosTokenizeCardParams(
    val posForageConfig: PosForageConfig,
    val track2Data: String,
    val reusable: Boolean = true
)

/**
 * A model that represents the parameters that Forage requires to collect a card PIN and defer
 * the refund of the payment to the server.
 * [PosDeferPaymentRefundParams] are passed to the
 * [deferPaymentRefund][com.joinforage.forage.android.pos.ForageTerminalSDK.deferPaymentRefund] method.
 *
 * @property foragePinEditText A reference to a [ForagePINEditText] instance.
 * [setPosForageConfig][com.joinforage.forage.android.ui.ForageElement.setPosForageConfig] must
 * be called on the instance before it can be passed.
 * @property paymentRef A unique string identifier for a previously created
 * [`Payment`](https://docs.joinforage.app/reference/payments) in Forage's
 * database, returned by the
 * [Create a `Payment`](https://docs.joinforage.app/reference/create-a-payment) endpoint.
 */
data class PosDeferPaymentRefundParams(
    val foragePinEditText: ForagePINEditText,
    val paymentRef: String
)

/**
 * A model that represents the parameters that [ForageTerminalSDK] requires to refund a Payment.
 * [PosRefundPaymentParams] are passed to the
 * [refundPayment][com.joinforage.forage.android.pos.ForageTerminalSDK.refundPayment] method.
 *
 * @property foragePinEditText **Required**. A reference to the [ForagePINEditText] instance that collected
 * the card PIN for the refund.
 *  [setForageConfig][com.joinforage.forage.android.ui.ForageElement.setForageConfig] must be
 *  called on the instance before it can be passed.
 * @property paymentRef **Required**. A unique string identifier for a previously created
 * [`Payment`](https://docs.joinforage.app/reference/payments) in Forage's database, returned by the
 *  [Create a `Payment`](https://docs.joinforage.app/reference/create-a-payment) endpoint.
 * @property amount **Required**. A positive decimal number that represents how much of the original
 * payment to refund in USD. Precision to the penny is supported.
 * The minimum amount that can be refunded is `0.01`.
 * @property reason **Required**. A string that describes why the payment is to be refunded.
 * @property metadata Optional. A map of merchant-defined key-value pairs. For example, some
 * merchants attach their credit card processor’s ID for the customer making the refund.
 */
data class PosRefundPaymentParams(
    val foragePinEditText: ForagePinElement,
    val paymentRef: String,
    val amount: Float,
    val reason: String,
    val metadata: Map<String, String>? = null
)
