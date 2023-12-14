package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.VaultType
import com.joinforage.forage.android.collect.CollectorConstants
import com.joinforage.forage.android.collect.PinCollector
import com.joinforage.forage.android.model.EncryptionKeys
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError

/**
 * Fake test implementation of PinCollector that could be used to replace VGS on tests
 */
internal class TestPinCollector : PinCollector {
    private var submitBalanceCheckResponses =
        HashMap<CheckBalanceWrapper, ForageApiResponse<String>>()
    private var submitPaymentCaptureResponses =
        HashMap<CapturePaymentWrapper, ForageApiResponse<String>>()
    private var submitCollectPinResponses =
        HashMap<CollectPinWrapper, ForageApiResponse<String>>()

    override suspend fun submitBalanceCheck(
        paymentMethodRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String> {
        return submitBalanceCheckResponses.getOrDefault(
            CheckBalanceWrapper(
                paymentMethodRef,
                cardToken,
                encryptionKey
            ),
            ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", "Unknown Server Error")))
        )
    }

    override suspend fun submitPaymentCapture(
        paymentRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String> {
        return submitPaymentCaptureResponses.getOrDefault(
            CapturePaymentWrapper(
                paymentRef = paymentRef,
                cardToken = cardToken,
                encryptionKey = encryptionKey
            ),
            ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", "Unknown Server Error")))
        )
    }

    override suspend fun collectPinForDeferredCapture(
        paymentRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String> {
        return submitCollectPinResponses.getOrDefault(
            CollectPinWrapper(
                paymentRef = paymentRef,
                cardToken = cardToken,
                encryptionKey = encryptionKey
            ),
            ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", "Unknown Server Error")))
        )
    }

    override fun parseEncryptionKey(encryptionKeys: EncryptionKeys): String {
        return encryptionKeys.vgsAlias
    }

    override fun parseVaultToken(paymentMethod: PaymentMethod): String {
        val token = paymentMethod.card.token
        if (token.contains(CollectorConstants.TOKEN_DELIMITER)) {
            return token.split(CollectorConstants.TOKEN_DELIMITER)[0]
        }
        return token
    }

    override fun getVaultType(): VaultType {
        return VaultType.VGS_VAULT_TYPE
    }

    fun setBalanceCheckResponse(
        paymentMethodRef: String,
        cardToken: String,
        encryptionKey: String,
        response: ForageApiResponse<String>
    ) {
        submitBalanceCheckResponses[
            CheckBalanceWrapper(
                paymentMethodRef,
                cardToken,
                encryptionKey
            )
        ] =
            response
    }

    fun setCapturePaymentResponse(
        paymentRef: String,
        cardToken: String,
        encryptionKey: String,
        response: ForageApiResponse<String>
    ) {
        submitPaymentCaptureResponses[
            CapturePaymentWrapper(
                paymentRef,
                cardToken,
                encryptionKey
            )
        ] =
            response
    }

    fun setCollectPinResponse(
        paymentRef: String,
        cardToken: String,
        encryptionKey: String,
        response: ForageApiResponse<String>
    ) {
        submitCollectPinResponses[
            CollectPinWrapper(
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

    private data class CollectPinWrapper(
        val paymentRef: String,
        val cardToken: String,
        val encryptionKey: String
    )

    companion object {
        fun sendToProxyResponse(contentId: String): String =
            "{\"content_id\":\"$contentId\",\"message_type\":\"0200\",\"status\":\"sent_to_proxy\",\"failed\":false,\"errors\":[]}"
    }
}
