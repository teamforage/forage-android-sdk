package com.joinforage.forage.android.collect

import com.basistheory.android.service.BasisTheoryElements
import com.basistheory.android.service.ProxyRequest
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.ForageConstants
import com.joinforage.forage.android.network.model.ForageApiError
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import org.json.JSONException

private const val BT_TOKEN_INDEX = 1

internal abstract class AbstractBtSubmitter : AbstractVaultSubmitter<Result<Any?>, String>() {
    abstract override val logger: SubmitBtLogger
    abstract override val proxyRoundTripMetric: ProxyResponseTimeMetric

    override fun getVaultToken(paymentMethod: PaymentMethod): String? = pickVaultTokenByIndex(paymentMethod, BT_TOKEN_INDEX)

    // Basis Theory requires a few extra headers beyond the
    // common headers to make proxy requests
    override fun buildProxyRequest(ebtVaultToken: String) = super
        .buildProxyRequest(ebtVaultToken)
        .setHeader(ForageConstants.Headers.BT_PROXY_KEY, envConfig.btProxyID)
        .setHeader(ForageConstants.Headers.CONTENT_TYPE, "application/json")

    // common AbstractSubmitter methods

    override suspend fun submitProxyRequest(request: VaultProxyRequest): ForageApiResponse<String> {
        val bt = BasisTheoryElements.builder()
            .apiKey(envConfig.btAPIKey)
            .build()

        val proxyRequest: ProxyRequest = ProxyRequest().apply {
            headers = request.headers
            body = object {
                val pin = pinInput.getTextElement()
                val card_number_token = request.ebtVaultToken
            }
            path = request.path
        }

        proxyRoundTripMetric.start()
        val response = runCatching {
            bt.proxy.post(proxyRequest)
        }

        return handleProxyResponse(response)
    }

    override fun toVaultProviderApiErrorOrNull(vaultResponse: Result<Any?>) : ForageApiResponse.Failure? {
        // for basis theory, Success responses mean Basis Theory did not
        // mess up. So, it will be a Forage Success or Forage Error
        // but it won't be a Basis Theory Error
        if (vaultResponse.isSuccess) return null

        // if the result is a Failure, we need to fail gracefully and
        // return a ForageApiError
        return UnknownErrorApiResponse
    }
    override fun toForageApiErrorOrNull(vaultResponse: Result<Any?>) : ForageApiResponse.Failure? {
        // for basis theory, Failed response are a Basis Theory Api Error
        // and don't have anything to do with Forage
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
    override fun toForageApiSuccessOrNull(vaultResponse: Result<Any?>) : ForageApiResponse.Success<String>? {
        // for basis theory, Failed response are a Basis Theory Api Error
        // and don't have anything to do with Forage
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
            // if it DOES throw, then it was not a ForageApiERror so
            // is it can only be a successful response!
            ForageApiResponse.Success(forageResponse)
        }
    }
    override fun parseVaultProviderApiError(vaultResponse: Result<Any?>) : String {
        return vaultResponse.exceptionOrNull().toString()
    }

}
