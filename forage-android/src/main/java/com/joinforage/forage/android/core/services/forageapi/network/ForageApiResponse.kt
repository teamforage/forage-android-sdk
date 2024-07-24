package com.joinforage.forage.android.core.services.forageapi.network

import com.joinforage.forage.android.core.services.forageapi.network.error.ForageError
import com.joinforage.forage.android.core.services.forageapi.payment.Payment
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.Balance
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtBalance
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.forageapi.polling.ForageErrorDetails

internal val UnknownErrorApiResponse =
    ForageApiResponse.Failure(500, "unknown_server_error", "Unknown Server Error")

/**
 * A model that represents the possible types of responses from the Forage API.
 */
sealed class ForageApiResponse<out T> {
    /**
     * A model that represents a success response from the API.
     *
     * In most cases, `data` is a string representation of a JSON object from the Forage API,
     * for example:
     * ```
     * if (response is ForageApiResponse.Success) {
     *   response.data // { "ref": "abcde123", ... }
     * }
     * ```
     *
     * Use the [toPaymentMethod], [toBalance], or [toPayment] methods to convert the [data] string
     * to a [PaymentMethod], [Balance], or [Payment] instance, respectively.
     */
    data class Success<out T>(val data: T) : ForageApiResponse<T>() {
        /**
         * Converts the [data] string to a [PaymentMethod] instance.
         * ```kotlin
         * when (forageApiResponse) {
         *     is ForageApiResponse.Success -> {
         *         val paymentMethod = forageApiResponse.toPaymentMethod()
         *         // Unpack paymentMethod.ref, paymentMethod.card, etc.
         *         val card = paymentMethod.card
         *         // Unpack card.last4, ...
         *         if (card is EbtCard) {
         *             // Unpack ebtCard.usState, ...
         *         }
         *     }
         * }
         * ```
         * @return A [PaymentMethod] instance.
         */
        fun toPaymentMethod(): PaymentMethod {
            return PaymentMethod(data as String)
        }

        /**
         * Converts the [data] string to a [Balance] instance.
         * ```kotlin
         * when (forageApiResponse) {
         *     is ForageApiResponse.Success -> {
         *         val balance = forageApiResponse.toBalance() as EbtBalance
         *         if (balance is EbtBalance) {
         *             // Unpack balance.snap, balance.cash
         *         }
         *     }
         * }
         * ```
         * @return A [Balance] instance.
         */
        fun toBalance(): Balance {
            // The ForageApiResponse.data string is already formatted to
            // { snap: ..., cash: ... }
            // so we use fromSdkResponse() instead of the typical constructor
            return EbtBalance.fromSdkResponse(data as String)
        }

        /**
         * Converts the [data] string to a [Payment] instance.
         * ```kotlin
         * when (forageApiResponse) {
         *     is ForageApiResponse.Success -> {
         *         val payment = forageApiResponse.toPayment()
         *         // Unpack payment.ref, payment.amount, payment.receipt, etc.
         *     }
         * }
         * ```
         * @return A [Payment] instance.
         */
        fun toPayment(): Payment {
            return Payment(data as String)
        }
    }

    /**
     * A model that represents a failure response from the API.
     *
     * @property errors A list of [ForageError] instances that you can unpack to programmatically
     * handle the error and display the appropriate
     * [customer-facing message](https://docs.joinforage.app/docs/document-error-messages#template-error-table-for-fns-documentation).
     */
    data class Failure internal constructor(val error: ForageError) : ForageApiResponse<Nothing>() {

        @Deprecated(
            message = "Use `.error` property instead of `.errors[0]`. There will only ever by 1 error in the list",
            level = DeprecationLevel.WARNING,
            replaceWith = ReplaceWith("error")
        )
        val errors: List<ForageError> = listOf(error)

        internal constructor(httpStatusCode: Int, code: String, message: String, details: ForageErrorDetails? = null) :
            this(ForageError(httpStatusCode, code, message, details))

        internal constructor(httpStatusCode: Int, jsonString: String) :
            this(ForageError(httpStatusCode, jsonString))

        override fun toString(): String = error.toString()
    }
}
