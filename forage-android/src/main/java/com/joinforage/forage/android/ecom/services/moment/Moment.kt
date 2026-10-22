package com.joinforage.forage.android.ecom.services.moment

import com.joinforage.forage.android.core.services.forageapi.network.error.ForageError

/**
 * A Forage-hosted page that [createMoment][com.joinforage.forage.android.ecom.services.ForageSDK.createMoment]
 * opens to complete one task with the customer, like entering their EBT Card PIN.
 */
sealed class Moment {
    // where the moment's page lives and the params it goes in its URL
    internal abstract val name: String
    internal abstract val path: String
    internal abstract val params: Map<String, String>

    /**
     * Collects the customer's EBT Card PIN for a previously created
     * [`Payment`](https://docs.joinforage.app/reference/payments).
     *
     * @property paymentRef The `ref` of the `Payment`, returned by the
     * [Create a `Payment`](https://docs.joinforage.app/reference/create-a-payment) endpoint.
     */
    data class PaymentCollectPin(val paymentRef: String) : Moment() {
        override val name get() = "payment_collect_pin"
        override val path get() = "/payment/collect-pin"
        override val params get() = mapOf("paymentRef" to paymentRef)
    }
}

/**
 * The outcome of a [Moment].
 */
sealed class MomentResult {
    /**
     * The customer completed the moment.
     */
    object Completed : MomentResult() {
        override fun toString() = "Completed"
    }

    /**
     * The customer closed the moment without completing it.
     */
    object Dismissed : MomentResult() {
        override fun toString() = "Dismissed"
    }

    /**
     * The moment failed, or couldn't be opened.
     *
     * @property error A [ForageError] that describes what went wrong.
     */
    data class Failed(val error: ForageError) : MomentResult()
}
