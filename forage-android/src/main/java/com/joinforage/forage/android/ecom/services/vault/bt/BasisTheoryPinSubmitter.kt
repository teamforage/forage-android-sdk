package com.joinforage.forage.android.ecom.services.vault.bt

import android.content.Context
import com.basistheory.android.service.BasisTheoryElements
import com.basistheory.android.service.ProxyRequest
import com.basistheory.android.view.TextElement
import com.joinforage.forage.android.core.services.ForageConstants
import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeys
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiError
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.ForageError
import com.joinforage.forage.android.core.services.forageapi.network.UnknownErrorApiResponse
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.VaultType
import com.joinforage.forage.android.core.services.vault.StopgapGlobalState
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.vault.AbstractVaultSubmitter
import com.joinforage.forage.android.core.services.vault.VaultProxyRequest
import com.joinforage.forage.android.core.services.vault.VaultSubmitterParams
import com.joinforage.forage.android.core.ui.element.ForagePinElement

internal typealias BasisTheoryResponse = Result<Any?>

internal class BasisTheoryPinSubmitter(
    context: Context,
    foragePinEditText: ForagePinElement,
    logger: Log,
    private val buildVaultProvider: () -> BasisTheoryElements = { buildBt() }
) : AbstractVaultSubmitter(
    context = context,
    foragePinEditText = foragePinEditText,
    logger = logger,
) {
    override val vaultType: VaultType = VaultType.BT_VAULT_TYPE
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
                pin = foragePinEditText.getTextElement() as TextElement,
                card_number_token = vaultProxyRequest.vaultToken
            )
            path = vaultProxyRequest.path
        }

        val vaultResponse = runCatching {
            bt.proxy.post(proxyRequest)
        }

        if (vaultResponse == null) {
            logger.e("[$vaultType] Received null response from $vaultType")
            return UnknownErrorApiResponse
        }

        val vaultError = toVaultErrorOrNull(vaultResponse)
        if (vaultError != null) {
            val rawVaultError = parseVaultErrorMessage(vaultResponse)
            logger.e("[$vaultType] Received error from $vaultType: $rawVaultError")
            return vaultError
        }

        val forageApiErrorResponse = toForageErrorOrNull(vaultResponse)
        if (forageApiErrorResponse != null) {
            val firstError = forageApiErrorResponse.errors[0]
            logger.e("[$vaultType] Received ForageError from $vaultType: $firstError")
            return forageApiErrorResponse
        }

        val forageApiSuccess = toForageSuccessOrNull(vaultResponse)
        if (forageApiSuccess != null) {
            logger.i("[$vaultType] Received successful response from $vaultType")
            return forageApiSuccess
        }
        logger.e("[$vaultType] Received malformed response from $vaultType: $vaultResponse")

        return UnknownErrorApiResponse
    }

    fun parseVaultErrorMessage(vaultResponse: BasisTheoryResponse): String {
        return vaultResponse.exceptionOrNull().toString()
    }

    fun toForageSuccessOrNull(vaultResponse: BasisTheoryResponse): ForageApiResponse.Success<String>? {
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

    fun toForageErrorOrNull(vaultResponse: BasisTheoryResponse): ForageApiResponse.Failure? {
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

    fun toVaultErrorOrNull(vaultResponse: BasisTheoryResponse): ForageApiResponse.Failure? {
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
