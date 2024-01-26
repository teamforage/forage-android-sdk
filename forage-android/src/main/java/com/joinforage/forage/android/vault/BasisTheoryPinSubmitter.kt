package com.joinforage.forage.android.vault

import android.content.Context
import com.basistheory.android.service.BasisTheoryElements
import com.basistheory.android.service.ProxyRequest
import com.basistheory.android.view.TextElement
import com.joinforage.forage.android.VaultType
import com.joinforage.forage.android.core.StopgapGlobalState
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.model.EncryptionKeys
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.ForageConstants
import com.joinforage.forage.android.network.model.ForageApiError
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.network.model.UnknownErrorApiResponse
import com.joinforage.forage.android.ui.ForagePINEditText

internal typealias BasisTheoryResponse = Result<Any?>

internal class BasisTheoryPinSubmitter(
    context: Context,
    foragePinEditText: ForagePINEditText,
    logger: Log,
    private val buildVaultProvider: () -> BasisTheoryElements = { buildBt() }
) : AbstractVaultSubmitter<BasisTheoryResponse>(
    context = context,
    foragePinEditText = foragePinEditText,
    logger = logger,
    vaultType = VaultType.BT_VAULT_TYPE
) {
    override fun parseEncryptionKey(encryptionKeys: EncryptionKeys): String {
        return encryptionKeys.btAlias
    }

    // Basis Theory requires a few extra headers beyond the
    // common headers to make proxy requests
    override fun buildProxyRequest(
        params: VaultSubmitterParams,
        encryptionKey: String,
        vaultToken: String
    ) = super
        .buildProxyRequest(
            params = params,
            encryptionKey = encryptionKey,
            vaultToken = vaultToken
        )
        .setHeader(ForageConstants.Headers.BT_PROXY_KEY, PROXY_ID)
        .setHeader(ForageConstants.Headers.CONTENT_TYPE, "application/json")

    override suspend fun submitProxyRequest(vaultProxyRequest: VaultProxyRequest): ForageApiResponse<String> {
        val bt = buildVaultProvider()

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

    override fun parseVaultErrorMessage(vaultResponse: BasisTheoryResponse): String {
        return vaultResponse.exceptionOrNull().toString()
    }

    override fun toForageSuccessOrNull(vaultResponse: BasisTheoryResponse): ForageApiResponse.Success<String>? {
        // The caller should have already performed the error checks.
        // We add these error checks as a safeguard, just in case.
        if (vaultResponse.isFailure ||
            toVaultErrorOrNull(vaultResponse) != null ||
            toForageErrorOrNull(vaultResponse) != null
        ) {
            return null
        }

        // note: Result.toString() wraps the actual response as
        // "Success(<actual-value-here>)"
        return ForageApiResponse.Success(vaultResponse.getOrNull().toString())
    }

    override fun toForageErrorOrNull(vaultResponse: BasisTheoryResponse): ForageApiResponse.Failure? {
        return try {
            val matchedStatusCode = getBasisTheoryExceptionStatusCode(vaultResponse)!!
            val basisTheoryExceptionBody = getBasisTheoryExceptionBody(vaultResponse)!!

            val forageApiError = ForageApiError.ForageApiErrorMapper.from(basisTheoryExceptionBody)
            val firstError = forageApiError.errors[0]

            return ForageApiResponse.Failure.fromError(
                ForageError(matchedStatusCode, firstError.code, firstError.message)
            )
        } catch (_: Exception) {
            // if we throw (likely a NullPointerException) when trying to extract the ForageApiError,
            // that means the response is not a ForageApiError
            null
        }
    }

    override fun toVaultErrorOrNull(vaultResponse: BasisTheoryResponse): ForageApiResponse.Failure? {
        if (vaultResponse.isSuccess) return null

        try {
            // try to parse as an error from Basis Theory, indicated by the presence
            // of a "proxy_error" in the raw response body.
            val basisTheoryExceptionBody = getBasisTheoryExceptionBody(vaultResponse)
            if (basisTheoryExceptionBody!!.contains("proxy_error")) {
                return UnknownErrorApiResponse
            }
            return null
        } catch (_: Exception) {
            return null
        }
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

        /**
         * com.basistheory.ApiException isn't currently
         * publicly-exposed by the basis-theory-android package
         * so we parse the raw exception message to retrieve the body of the BasisTheory
         * errors
         *
         * @return the BasisTheory JSON response body string if the response is a BasisTheory error,
         * or null if it is not
         */
        private fun getBasisTheoryExceptionBody(vaultResponse: BasisTheoryResponse): String? {
            val rawBasisTheoryError = vaultResponse.exceptionOrNull()?.message ?: return null
            val bodyRegex = "HTTP response body: (.+?)\\n".toRegex()
            return bodyRegex.find(rawBasisTheoryError)?.groupValues?.get(1)
        }

        /**
         * @return the BasisTheory exception HTTP status code (Int) if the response is a BasisTheory error,
         * or null if it is not
         */
        private fun getBasisTheoryExceptionStatusCode(vaultResponse: BasisTheoryResponse): Int? {
            val rawBasisTheoryError = vaultResponse.exceptionOrNull()?.message ?: return null
            val statusCodeRegex = "HTTP response code: (\\d+)".toRegex()
            return statusCodeRegex.find(rawBasisTheoryError)?.groupValues?.get(1)?.toIntOrNull()
        }
    }
}
