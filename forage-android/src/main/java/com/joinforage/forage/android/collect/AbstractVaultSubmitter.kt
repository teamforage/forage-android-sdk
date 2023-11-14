package com.joinforage.forage.android.collect

import com.joinforage.forage.android.core.EnvConfig
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.ForageConstants
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.ui.ForagePINEditText

internal val UnknownErrorApiResponse = ForageApiResponse.Failure.fromError(
    ForageError(500, "unknown_server_error", "Unknown Server Error")
)

internal abstract class AbstractVaultSubmitter<VaultResponse, SuccessData> {

    // config
    abstract val merchantId: String
    abstract val paymentMethod: PaymentMethod
    abstract val encryptionKey: String
    abstract val idempotencyKey: String
    abstract val envConfig: EnvConfig

    // dependencies
    abstract val logger: SubmitVaultLogger
    abstract val proxyRoundTripMetric: ProxyRoundTripMetric
    abstract val pinInput: ForagePINEditText

    // abstract methods
    abstract fun getVaultToken(paymentMethod: PaymentMethod) : String?
    abstract suspend fun submitProxyRequest(request: VaultProxyRequest) : ForageApiResponse<SuccessData>

    abstract fun toVaultProviderApiErrorOrNull(vaultResponse: VaultResponse) : ForageApiResponse.Failure?
    abstract fun toForageApiErrorOrNull(vaultResponse: VaultResponse) : ForageApiResponse.Failure?
    abstract fun toForageApiSuccessOrNull(vaultResponse: VaultResponse) : ForageApiResponse.Success<SuccessData>?
    abstract fun parseVaultProviderApiError(vaultResponse: VaultResponse) : String

    // concrete methods
    internal fun pickVaultTokenByIndex(paymentMethod: PaymentMethod, index: Int) : String? {
        val tokensString = paymentMethod.card.token
        val tokensList = tokensString.split(CollectorConstants.TOKEN_DELIMITER)

        // vgs is always the first token in the string
        val noTokenStoredInVault = tokensList.size <= index
        if (noTokenStoredInVault) return null

        // grab bt token
        return tokensList[index]
    }

    private fun handleVaultMissingTokenError(ebtVaultToken: String?) : ForageApiResponse<SuccessData> {
        logger.logVaultMissingCardTokenError(ebtVaultToken)
        return UnknownErrorApiResponse
    }

    internal open fun buildProxyRequest(ebtVaultToken: String) = VaultProxyRequest.emptyRequest()
        .setHeader(ForageConstants.Headers.X_KEY, encryptionKey)
        .setHeader(ForageConstants.Headers.MERCHANT_ACCOUNT, merchantId)
        .setHeader(ForageConstants.Headers.IDEMPOTENCY_KEY, idempotencyKey)
        .setHeader(ForageConstants.Headers.TRACE_ID, logger.getTraceId())
        .setToken(ebtVaultToken)

    internal fun handleProxyResponse(response: VaultResponse) : ForageApiResponse<SuccessData> {
        proxyRoundTripMetric.end()

        val vaultApiError = toVaultProviderApiErrorOrNull(response)
        if (vaultApiError != null)
            return handleVaultProviderApiError(vaultApiError, response)

        val forageApiError = toForageApiErrorOrNull(response)
        if (forageApiError != null)
            return handleForageApiError(forageApiError)

        val forageApiSuccess = toForageApiSuccessOrNull(response)
        if (forageApiSuccess != null)
            return handleForageApiSuccess(forageApiSuccess)

        return handleUnknownApiError()
    }
    private fun handleVaultProviderApiError(forageResponse: ForageApiResponse.Failure, vaultResponse: VaultResponse) : ForageApiResponse.Failure {
        val errorMsg = parseVaultProviderApiError(vaultResponse)
        logger.logVaultApiError(errorMsg)
        proxyRoundTripMetric.captureAsVaultApiError()
        return forageResponse
    }
    private fun handleForageApiError(forageResponse: ForageApiResponse.Failure) : ForageApiResponse.Failure {
        val error = forageResponse.errors[0]
        logger.logForageApiError(error)
        proxyRoundTripMetric.captureAsForageApiError()
        return forageResponse
    }
    private fun handleForageApiSuccess(forageResponse: ForageApiResponse.Success<SuccessData>) : ForageApiResponse.Success<SuccessData> {
        logger.logForageApiSuccess()
        proxyRoundTripMetric.captureAsSuccessfulResponse()
        return forageResponse
    }
    private fun handleUnknownApiError() : ForageApiResponse.Failure {
        logger.logUnknownApiError()
        proxyRoundTripMetric.captureAsUnknownApiError()
        return UnknownErrorApiResponse
    }

    private fun clearPinText() {
        pinInput.getTextElement().setText("")
    }

    suspend fun submit() : ForageApiResponse<SuccessData> {
        val token = getVaultToken(paymentMethod)

        // if a vault provider is missing a token, we will
        // gracefully fail here
        if (token.isNullOrEmpty()) return handleVaultMissingTokenError(token)

        // send the proxy request
        val request = buildProxyRequest(token)
        val response = submitProxyRequest(request)

        // once we hear back from the proxy request, clear
        // the PIN input and return the response
        clearPinText()
        return response
    }
}
