package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.VaultType
import com.joinforage.forage.android.collect.CollectorConstants
import com.joinforage.forage.android.collect.VaultSubmitter
import com.joinforage.forage.android.core.telemetry.UserAction
import com.joinforage.forage.android.model.EncryptionKeys
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError

/**
 * Fake test implementation of PinCollector that could be used to replace VGS on tests
 */
internal class TestVaultSubmitter : VaultSubmitter {
    private var submitBalanceCheckResponses =
        HashMap<CheckBalanceWrapper, ForageApiResponse<String>>()
    private var submitPaymentCaptureResponses =
        HashMap<CapturePaymentWrapper, ForageApiResponse<String>>()
    private var submitCollectPinResponses =
        HashMap<DeferPaymentCaptureWrapper, ForageApiResponse<String>>()

    override suspend fun submit(
        encryptionKeys: EncryptionKeys,
        merchantId: String,
        path: String,
        paymentMethod: PaymentMethod,
        userAction: UserAction,
    ): ForageApiResponse<String> {
        return when (userAction) {
            UserAction.BALANCE -> submitBalanceCheckResponses.getOrDefault(
                CheckBalanceWrapper(
                    merchantId = merchantId,
                    httpPath = path,
                    paymentMethodRef = paymentMethod.ref
                ),
                ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", "Unknown Server Error")))
            )
            UserAction.CAPTURE -> ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", "Unknown Server Error")))
            UserAction.DEFER_CAPTURE -> ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", "Unknown Server Error")))
        }
    }

//    override suspend fun submitPaymentCapture(
//        paymentRef: String,
//        vaultRequestParams: BaseVaultRequestParams
//    ): ForageApiResponse<String> {
//        return submitPaymentCaptureResponses.getOrDefault(
//            CapturePaymentWrapper(
//                paymentRef = paymentRef,
//                vaultRequestParams
//            ),
//            ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", "Unknown Server Error")))
//        )
//    }

//    override suspend fun submitDeferPaymentCapture(
//        paymentRef: String,
//        vaultRequestParams: BaseVaultRequestParams
//    ): ForageApiResponse<String> {
//        return submitCollectPinResponses.getOrDefault(
//            DeferPaymentCaptureWrapper(
//                paymentRef = paymentRef,
//                vaultRequestParams
//            ),
//            ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", "Unknown Server Error")))
//        )
//    }

    override fun getVaultType(): VaultType {
        return VaultType.VGS_VAULT_TYPE
    }

    fun setBalanceCheckResponse(
        httpPath: String,
        merchantId: String,
        paymentMethodRef: String,
        response: ForageApiResponse<String>
    ) {
        submitBalanceCheckResponses[
            CheckBalanceWrapper(
                httpPath = httpPath,
                merchantId = merchantId,
                paymentMethodRef = paymentMethodRef,
            )
        ] =
            response
    }

    fun setCapturePaymentResponse(
        paymentRef: String,
        vaultRequestParams: BaseVaultRequestParams,
        response: ForageApiResponse<String>
    ) {
//        submitPaymentCaptureResponses[
//            CapturePaymentWrapper(
//                paymentRef,
//                vaultRequestParams
//            )
//        ] =
//            response
    }

    fun setCollectPinResponse(
        paymentRef: String,
        vaultRequestParams: BaseVaultRequestParams,
        response: ForageApiResponse<String>
    ) {
//        submitCollectPinResponses[
//            DeferPaymentCaptureWrapper(
//                paymentRef,
//                vaultRequestParams
//            )
//        ] =
//            response
    }

    private data class CheckBalanceWrapper(
        val merchantId: String,
        val httpPath: String,
        val paymentMethodRef: String
    )

    private data class CapturePaymentWrapper(
        val merchantId: String,
        val httpPath: String,
        val paymentMethodRef: String
    )

    private data class DeferPaymentCaptureWrapper(
        val merchantId: String,
        val httpPath: String,
        val paymentMethodRef: String
    )

    companion object {
        fun sendToProxyResponse(contentId: String): String =
            "{\"content_id\":\"$contentId\",\"message_type\":\"0200\",\"status\":\"sent_to_proxy\",\"failed\":false,\"errors\":[]}"
    }
}
