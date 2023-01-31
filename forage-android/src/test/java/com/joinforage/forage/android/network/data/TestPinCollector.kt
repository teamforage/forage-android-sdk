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
            ForageApiResponse.Failure(500, "server_error", "Unknown Server Error")
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
            ForageApiResponse.Failure(500, "server_error", "Unknown Server Error")
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
        fun sendToProxyResponse(contentId: String): String =
            "{\"content_id\":\"$contentId\",\"message_type\":\"0200\",\"status\":\"sent_to_proxy\",\"failed\":false,\"errors\":[]}"
    }
}
