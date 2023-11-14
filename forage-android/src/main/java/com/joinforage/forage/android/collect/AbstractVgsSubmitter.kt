package com.joinforage.forage.android.collect

import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.ForageConstants
import com.joinforage.forage.android.network.model.ForageApiError
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.verygoodsecurity.vgscollect.VGSCollectLogger
import com.verygoodsecurity.vgscollect.core.HTTPMethod
import com.verygoodsecurity.vgscollect.core.VGSCollect
import com.verygoodsecurity.vgscollect.core.VgsCollectResponseListener
import com.verygoodsecurity.vgscollect.core.model.network.VGSRequest
import com.verygoodsecurity.vgscollect.core.model.network.VGSResponse
import org.json.JSONException
import kotlin.coroutines.suspendCoroutine

private const val VGS_TOKEN_INDEX = 0

internal abstract class AbstractVgsSubmitter : AbstractVaultSubmitter<VGSResponse?, String>() {
    abstract override val logger: SubmitBtLogger
    abstract override val proxyRoundTripMetric: ProxyResponseTimeMetric

    // VGS needs context for some reason while BT manages to get by without it
    abstract val context: android.content.Context

    override fun getVaultToken(paymentMethod: PaymentMethod): String? = pickVaultTokenByIndex(paymentMethod, VGS_TOKEN_INDEX)

    // common AbstractSubmitter methods

    override suspend fun submitProxyRequest(request: VaultProxyRequest): ForageApiResponse<String> = suspendCoroutine { continuation ->
        VGSCollectLogger.isEnabled = false
        val vgs = VGSCollect.Builder(context, envConfig.vgsVaultId)
            .setEnvironment(envConfig.vgsVaultType)
            .create()
        vgs.bindView(pinInput.getTextInputEditText())

        vgs.addOnResponseListeners(object : VgsCollectResponseListener {
            override fun onResponse(response: VGSResponse?) {
                // VGS exposes / wants us to call onDestroy, unlike BT
                vgs.onDestroy()

                // return the with ForageApiResponse
                continuation.resumeWith(
                    Result.success(
                        handleProxyResponse(response)
                    )
                )
            }
        })

        val body = mapOf(
            ForageConstants.RequestBody.CARD_NUMBER_TOKEN to request.ebtVaultToken
        )
        val vgsRequest = VGSRequest.VGSRequestBuilder()
            .setMethod(HTTPMethod.POST)
            .setPath(request.path)
            .setCustomHeader(request.headers)
            .setCustomData(body)
            .build()

        proxyRoundTripMetric.start()
        vgs.asyncSubmit(vgsRequest)
    }

    override fun toVaultProviderApiErrorOrNull(vaultResponse: VGSResponse?) : ForageApiResponse.Failure? {
        // null responses here means it's not a vgs error
        if (vaultResponse == null) return null

        // success responses also means it's not a vgs error
        if (vaultResponse is VGSResponse.SuccessResponse) return null

        val errorResponse = vaultResponse as VGSResponse.ErrorResponse
        return try {
            // converting the response to a ForageApiError should throw
            // in the case of a vgs error
            ForageApiError.ForageApiErrorMapper.from(errorResponse.toString())

            // if it does NOT throw, then it's a ForageApiError
            // so we return null
            return null
        } catch (_: JSONException) {
            // if it DOES throw, then it was not a ForageApiError so
            // it must be a vgs error
            ForageApiResponse.Failure.fromError(
                ForageError(
                    errorResponse.errorCode, "user_error", "Invalid Data"
                )
            )
        }
    }
    override fun toForageApiErrorOrNull(vaultResponse: VGSResponse?) : ForageApiResponse.Failure? {
        // null responses here means it's not a vgs error
        if (vaultResponse == null) return null

        // success responses also means it's not a vgs error
        if (vaultResponse is VGSResponse.SuccessResponse) return null

        val errorResponse = vaultResponse as VGSResponse.ErrorResponse
        return try {
            // if the response is a ForageApiError, then this block
            // should not throw
            val forageApiError = ForageApiError.ForageApiErrorMapper.from(errorResponse.toString())
            val error = forageApiError.errors[0]
            ForageApiResponse.Failure.fromError(
                ForageError(
                    errorResponse.errorCode, error.code, error.message
                )
            )
        } catch (_: JSONException) {
            // if we throw when trying to extract the ForageApiError,
            // that means the response is not a ForageApiError
            null
        }
    }
    override fun toForageApiSuccessOrNull(vaultResponse: VGSResponse?) : ForageApiResponse.Success<String>? {
        // null responses here means it's not a success
        if (vaultResponse == null) return null

        // error responses also means it's not a success
        if (vaultResponse is VGSResponse.ErrorResponse) return null

        val successResponse = vaultResponse as VGSResponse.SuccessResponse
        return ForageApiResponse.Success(successResponse.body.toString())
    }
    override fun parseVaultProviderApiError(vaultResponse: VGSResponse?) : String {
        return vaultResponse?.body.toString()
    }
}
