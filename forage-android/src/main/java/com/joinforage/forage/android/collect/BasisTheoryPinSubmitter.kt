package com.joinforage.forage.android.collect

import android.content.Context
import com.basistheory.android.service.BasisTheoryElements
import com.basistheory.android.service.ProxyRequest
import com.joinforage.forage.android.VaultType
import com.joinforage.forage.android.core.StopgapGlobalState
import com.joinforage.forage.android.model.EncryptionKeys
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.ForageConstants
import com.joinforage.forage.android.network.model.ForageApiError
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.network.model.UnknownErrorApiResponse
import com.joinforage.forage.android.ui.ForagePINEditText
import org.json.JSONException

internal typealias BasisTheoryResponse = Result<Any?>

internal class BasisTheoryPinSubmitter(
    context: Context,
    foragePinEditText: ForagePINEditText
) : AbstractVaultSubmitter<BasisTheoryResponse>(
    context = context,
    foragePinEditText = foragePinEditText,
    vaultType = VaultType.BT_VAULT_TYPE
) {
    override fun parseEncryptionKey(encryptionKeys: EncryptionKeys): String {
        return encryptionKeys.btAlias
    }

    // Basis Theory requires a few extra headers beyond the
    // common headers to make proxy requests
    override fun buildProxyRequest(
        encryptionKey: String,
        idempotencyKey: String,
        merchantId: String,
        vaultToken: String
    ) = super
        .buildProxyRequest(
            encryptionKey = encryptionKey,
            idempotencyKey = idempotencyKey,
            merchantId = merchantId,
            vaultToken = vaultToken
        )
        .setHeader(ForageConstants.Headers.BT_PROXY_KEY, PROXY_ID)
        .setHeader(ForageConstants.Headers.CONTENT_TYPE, "application/json")

    override suspend fun submitProxyRequest(vaultProxyRequest: VaultProxyRequest): ForageApiResponse<String> {
        val bt = buildBt()

        val proxyRequest: ProxyRequest = ProxyRequest().apply {
            headers = vaultProxyRequest.headers
            body = ProxyRequestObject(
                pin = foragePinEditText.getTextElement(),
                card_number_token = vaultProxyRequest.vaultToken
            )
            path = vaultProxyRequest.path
        }

        val vaultResponse = runCatching {
            bt.proxy.post(proxyRequest)
        }

        return vaultToForageResponse(vaultResponse)
    }

    override fun parseVaultError(vaultResponse: BasisTheoryResponse): String {
        return vaultResponse.exceptionOrNull().toString()
    }

    override fun toForageSuccessOrNull(vaultResponse: BasisTheoryResponse): ForageApiResponse.Success<String>? {
        // for Basis Theory, a failed response is a Basis Theory Api Error
        // and doesn't have anything to do with Forage
        if (vaultResponse.isFailure) return null

        val forageResponse = vaultResponse.getOrNull().toString()
        return try {
            // converting the response to a ForageApiError should throw
            // if forageResponse was Successful
            ForageApiError.ForageApiErrorMapper.from(forageResponse)

            // if it does NOT throw, then it's a ForageApiError
            // so we return null here
            return null
        } catch (_: JSONException) {
            // if it DOES throw, then it was not a ForageApiError so
            // is it can only be a successful response!
            ForageApiResponse.Success(forageResponse)
        }
    }

    override fun toForageErrorOrNull(vaultResponse: BasisTheoryResponse): ForageApiResponse.Failure? {
        // is a Basis Theory error and doesn't have to do with Forage
        if (vaultResponse.isFailure) return null

        return try {
            // if the response is a ForageApiError, then this block
            // should not throw
            val forageResponse = vaultResponse.getOrNull().toString()
            val forageApiError = ForageApiError.ForageApiErrorMapper.from(forageResponse)
            val error = forageApiError.errors[0]
            ForageApiResponse.Failure.fromError(
                ForageError(400, error.code, error.message)
            )
        } catch (_: JSONException) {
            // if we throw when trying to extract the ForageApiError,
            // that means the response is not a ForageApiError
            null
        }
    }

    override fun toVaultErrorOrNull(vaultResponse: BasisTheoryResponse): ForageApiResponse.Failure? {
        // for basis theory, Success responses mean Basis Theory did not
        // mess up. So, it will be a Forage Success or Forage Error
        // but it won't be a Basis Theory Error
        if (vaultResponse.isSuccess) return null

        // if the result is a Failure, we need to fail gracefully and
        // return a ForageApiError
        return UnknownErrorApiResponse
    }

    override fun getVaultToken(paymentMethod: PaymentMethod): String? = pickVaultTokenByIndex(paymentMethod, 1)

    companion object {
        // this code assumes that .setForageConfig() has been called
        // on a Forage***EditText before PROXY_ID or API_KEY get
        // referenced
        private val PROXY_ID = StopgapGlobalState.envConfig.btProxyID
        private val API_KEY = StopgapGlobalState.envConfig.btAPIKey

        private fun buildBt(): BasisTheoryElements {
            return BasisTheoryElements.builder()
                .apiKey(API_KEY)
                .build()
        }
    }
}
