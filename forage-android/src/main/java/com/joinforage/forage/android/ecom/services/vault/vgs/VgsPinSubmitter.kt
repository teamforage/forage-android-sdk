package com.joinforage.forage.android.ecom.services.vault.vgs

import android.content.Context
import com.joinforage.forage.android.core.services.VaultType
import com.joinforage.forage.android.core.services.vault.StopgapGlobalState
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeys
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiError
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.ForageError
import com.joinforage.forage.android.core.services.forageapi.network.UnknownErrorApiResponse
import com.joinforage.forage.android.core.services.vault.AbstractVaultSubmitter
import com.joinforage.forage.android.core.ui.element.ForagePinElement
import com.joinforage.forage.android.core.services.vault.VaultProxyRequest
import com.verygoodsecurity.vgscollect.VGSCollectLogger
import com.verygoodsecurity.vgscollect.core.HTTPMethod
import com.verygoodsecurity.vgscollect.core.VGSCollect
import com.verygoodsecurity.vgscollect.core.VgsCollectResponseListener
import com.verygoodsecurity.vgscollect.core.model.network.VGSRequest
import com.verygoodsecurity.vgscollect.core.model.network.VGSResponse
import com.verygoodsecurity.vgscollect.widget.VGSEditText
import org.json.JSONException
import kotlin.coroutines.suspendCoroutine


internal class VgsPinSubmitter(
    context: Context,
    foragePinEditText: ForagePinElement,
    logger: Log,
    private val buildVaultProvider: (context: Context) -> VGSCollect = { buildVGSCollect(context) }
) : AbstractVaultSubmitter(
    context = context,
    foragePinEditText = foragePinEditText,
    logger = logger,
) {
    override val vaultType: VaultType = VaultType.VGS_VAULT_TYPE
    override suspend fun submitProxyRequest(
        vaultProxyRequest: VaultProxyRequest
    ): ForageApiResponse<String> = suspendCoroutine { continuation ->
        val vgsCollect = buildVaultProvider(context)
        vgsCollect.bindView(foragePinEditText.getTextElement() as VGSEditText)

        vgsCollect.addOnResponseListeners(object : VgsCollectResponseListener {
            override fun onResponse(response: VGSResponse?) {
                // Clear all information collected before by VGSCollect
                vgsCollect.onDestroy()

                continuation.resumeWith(
                    Result.success(vaultToForageResponse(VGSResponseParser(response)))
                )
            }
        })

        val request: VGSRequest = VGSRequest.VGSRequestBuilder()
            .setMethod(HTTPMethod.POST)
            .setPath(vaultProxyRequest.path)
            .setCustomHeader(vaultProxyRequest.headers)
            .setCustomData(buildBaseRequestBody(vaultProxyRequest))
            .build()

        vgsCollect.asyncSubmit(request)
    }

    override fun parseEncryptionKey(encryptionKeys: EncryptionKeys): String {
        return encryptionKeys.vgsAlias
    }

    override fun getVaultToken(paymentMethod: PaymentMethod): String? =
        pickVaultTokenByIndex(paymentMethod, 0)

    companion object {
        // this code assumes that .setForageConfig() has been called
        // on a Forage***EditText before PROXY_ID or API_KEY get
        // referenced
        private val VAULT_ID = StopgapGlobalState.envConfig.vgsVaultId
        private val VGS_ENVIRONMENT = StopgapGlobalState.envConfig.vgsVaultType

        private fun buildVGSCollect(context: Context): VGSCollect {
            VGSCollectLogger.isEnabled = false
            return VGSCollect.Builder(context, VAULT_ID)
                .setEnvironment(VGS_ENVIRONMENT)
                .create()
        }
    }

    fun toVaultErrorOrNull(vaultResponse: VGSResponse?): ForageApiResponse.Failure? {
        if (vaultResponse == null) return null
        if (vaultResponse is VGSResponse.SuccessResponse) return null

        val errorResponse = vaultResponse as VGSResponse.ErrorResponse
        return try {
            // converting the response to a ForageApiError should throw
            // in the case of a VGS error
            ForageApiError.ForageApiErrorMapper.from(errorResponse.body ?: "")

            // if it does NOT throw, then it's a ForageApiError
            // so we return null
            return null
        } catch (_: JSONException) {
            // if it DOES throw, then it was not a ForageApiError so
            // it must be a VGS error
            UnknownErrorApiResponse
        }
    }

    fun toForageErrorOrNull(vaultResponse: VGSResponse?): ForageApiResponse.Failure? {
        if (vaultResponse == null) return null
        if (vaultResponse is VGSResponse.SuccessResponse) return null

        val errorResponse = vaultResponse as VGSResponse.ErrorResponse
        return try {
            // if the response is a ForageApiError, then this block
            // should not throw
            val forageApiError = ForageApiError.ForageApiErrorMapper.from(errorResponse.body ?: "")
            val error = forageApiError.errors[0]
            ForageApiResponse.Failure.fromError(
                ForageError(
                    errorResponse.errorCode,
                    error.code,
                    error.message
                )
            )
        } catch (_: JSONException) {
            // if we throw when trying to extract the ForageApiError,
            // that means the response is not a ForageApiError
            null
        }
    }

    /**
     * @pre toVaultErrorOrNull(vaultResponse) == null && toForageErrorOrNull(vaultResponse) == null
     */
    fun toForageSuccessOrNull(vaultResponse: VGSResponse?): ForageApiResponse.Success<String>? {
        if (vaultResponse == null) return null
        if (vaultResponse is VGSResponse.ErrorResponse) return null

        // The caller should have already performed the error checks.
        // We add these error checks as a safeguard, just in case.
        if (toVaultErrorOrNull(vaultResponse) != null ||
            toForageErrorOrNull(vaultResponse) != null
        ) {
            return null
        }

        val successResponse = vaultResponse as VGSResponse.SuccessResponse
        return ForageApiResponse.Success(successResponse.body.toString())
    }

    fun parseVaultErrorMessage(vaultResponse: VGSResponse?): String {
        return vaultResponse?.body.toString()
    }
}
