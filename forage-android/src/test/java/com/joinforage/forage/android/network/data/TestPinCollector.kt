package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.VaultType
import com.joinforage.forage.android.model.EncryptionKeys
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.vault.CollectorConstants
import com.joinforage.forage.android.vault.PinCollector
import com.joinforage.forage.android.vault.VaultSubmitter
import com.joinforage.forage.android.vault.VaultSubmitterParams

internal class MockVaultSubmitter(
    private val vaultType: VaultType
) : VaultSubmitter {
    data class RequestContainer(
        val merchantId: String,
        val path: String,
        val paymentMethodRef: String,
        val idempotencyKey: String
    )

    private var responses =
        HashMap<RequestContainer, ForageApiResponse<String>>()

    fun setSubmitResponse(
        params: RequestContainer,
        response: ForageApiResponse<String>
    ) {
        responses[params] = response
    }

    override suspend fun submit(params: VaultSubmitterParams): ForageApiResponse<String> {
        return responses.getOrDefault(
            RequestContainer(
                merchantId = params.merchantId,
                path = params.path,
                paymentMethodRef = params.paymentMethod.ref,
                idempotencyKey = params.idempotencyKey
            ),
            ForageApiResponse.Failure.fromError(
                ForageError(
                    500,
                    "unknown_server_error",
                    "Unknown Server Error"
                )
            )
        )
    }

    override fun getVaultType(): VaultType {
        return vaultType
    }
}

/**
 * Fake test implementation of PinCollector that could be used to replace VGS on tests
 */
internal class TestPinCollector : PinCollector {
    private var submitBalanceCheckResponses =
        HashMap<CheckBalanceWrapper, ForageApiResponse<String>>()
    private var submitPaymentCaptureResponses =
        HashMap<CapturePaymentWrapper, ForageApiResponse<String>>()
    private var submitCollectPinResponses =
        HashMap<DeferPaymentCaptureWrapper, ForageApiResponse<String>>()

    override suspend fun submitBalanceCheck(
        paymentMethodRef: String,
        vaultRequestParams: BaseVaultRequestParams
    ): ForageApiResponse<String> {
        return submitBalanceCheckResponses.getOrDefault(
            CheckBalanceWrapper(
                paymentMethodRef,
                vaultRequestParams
            ),
            ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", "Unknown Server Error")))
        )
    }

    override suspend fun submitPaymentCapture(
        paymentRef: String,
        vaultRequestParams: BaseVaultRequestParams
    ): ForageApiResponse<String> {
        return submitPaymentCaptureResponses.getOrDefault(
            CapturePaymentWrapper(
                paymentRef = paymentRef,
                vaultRequestParams
            ),
            ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", "Unknown Server Error")))
        )
    }

    override suspend fun submitDeferPaymentCapture(
        paymentRef: String,
        vaultRequestParams: BaseVaultRequestParams
    ): ForageApiResponse<String> {
        return submitCollectPinResponses.getOrDefault(
            DeferPaymentCaptureWrapper(
                paymentRef = paymentRef,
                vaultRequestParams
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
        vaultRequestParams: BaseVaultRequestParams,
        response: ForageApiResponse<String>
    ) {
        submitBalanceCheckResponses[
            CheckBalanceWrapper(
                paymentMethodRef,
                vaultRequestParams
            )
        ] =
            response
    }

    fun setCapturePaymentResponse(
        paymentRef: String,
        vaultRequestParams: BaseVaultRequestParams,
        response: ForageApiResponse<String>
    ) {
        submitPaymentCaptureResponses[
            CapturePaymentWrapper(
                paymentRef,
                vaultRequestParams
            )
        ] =
            response
    }

    fun setCollectPinResponse(
        paymentRef: String,
        vaultRequestParams: BaseVaultRequestParams,
        response: ForageApiResponse<String>
    ) {
        submitCollectPinResponses[
            DeferPaymentCaptureWrapper(
                paymentRef,
                vaultRequestParams
            )
        ] =
            response
    }

    private data class CheckBalanceWrapper(
        val paymentMethodRef: String,
        val vaultRequestParams: BaseVaultRequestParams
    )

    private data class CapturePaymentWrapper(
        val paymentRef: String,
        val vaultRequestParams: BaseVaultRequestParams
    )

    private data class DeferPaymentCaptureWrapper(
        val paymentRef: String,
        val vaultRequestParams: BaseVaultRequestParams
    )

    companion object {
        fun sendToProxyResponse(contentId: String): String =
            "{\"content_id\":\"$contentId\",\"message_type\":\"0200\",\"status\":\"sent_to_proxy\",\"failed\":false,\"errors\":[]}"
    }
}
