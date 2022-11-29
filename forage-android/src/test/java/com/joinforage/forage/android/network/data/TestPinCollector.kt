package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.collect.PinCollector
import com.joinforage.forage.android.network.model.ForageApiResponse

/**
 * Fake test implementation of PinCollector that could be used to replace VGS on tests
 */
class TestPinCollector : PinCollector {
    private var collectPinForBalanceCheckResponses =
        HashMap<CheckBalanceWrapper, ForageApiResponse<String>>()
    private var collectPinForCapturePaymentResponses =
        HashMap<CapturePaymentWrapper, ForageApiResponse<String>>()

    override suspend fun collectPinForBalanceCheck(
        paymentMethodRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String> {
        return collectPinForBalanceCheckResponses.getOrDefault(
            CheckBalanceWrapper(
                paymentMethodRef,
                cardToken,
                encryptionKey
            ),
            ForageApiResponse.Failure("")
        )
    }

    override suspend fun collectPinForCapturePayment(
        paymentRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String> {
        return collectPinForCapturePaymentResponses.getOrDefault(
            CapturePaymentWrapper(
                paymentRef = paymentRef,
                cardToken = cardToken,
                encryptionKey = encryptionKey
            ),
            ForageApiResponse.Failure("")
        )
    }

    fun setCollectPinForBalanceCheckResponse(
        paymentMethodRef: String,
        cardToken: String,
        encryptionKey: String,
        response: ForageApiResponse<String>
    ) {
        collectPinForBalanceCheckResponses[
            CheckBalanceWrapper(
                paymentMethodRef,
                cardToken,
                encryptionKey
            )
        ] =
            response
    }

    fun setCollectPinForCapturePaymentResponse(
        paymentRef: String,
        cardToken: String,
        encryptionKey: String,
        response: ForageApiResponse<String>
    ) {
        collectPinForCapturePaymentResponses[
            CapturePaymentWrapper(
                paymentRef,
                cardToken,
                encryptionKey
            )
        ] =
            response
    }

    private data class CheckBalanceWrapper(
        val paymentMethodRef: String,
        val cardToken: String,
        val encryptionKey: String
    )

    private data class CapturePaymentWrapper(
        val paymentRef: String,
        val cardToken: String,
        val encryptionKey: String
    )

    companion object {
        fun checkBalanceResponse(): String =
            "{\"snap\":\"100.00\",\"non_snap\":\"100.00\",\"updated\":\"2022-11-29T06:26:57.127792-08:00\"}"

        fun checkBalanceInvalidCardNumberResponse(): String =
            "{\"path\":\"/api/payment_methods/52caa546d2/balance/\",\"errors\":[{\"code\":\"ebt_error_14\",\"message\":\"Invalid card number - Re-enter Transaction\",\"source\":{\"resource\":\"Payment_Methods\",\"ref\":\"52caa546d2\"}}]}"
    }
}
