package com.joinforage.forage.android.ecom.services.vault.bt

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiError
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.ForageError
import com.joinforage.forage.android.core.services.forageapi.network.UnknownErrorApiResponse
import com.joinforage.forage.android.core.services.vault.VaultResponseParser

class BTResponseParser(btRes: Result<Any?>) : VaultResponseParser {
    override val isNullResponse: Boolean = false
    override val vaultErrorMsg: String = btRes.exceptionOrNull().toString()
    override val rawResponse: String = btRes.toString()

    override val vaultError: ForageApiResponse.Failure?
    override val forageError: ForageApiResponse.Failure?
    override val successfulResponse: ForageApiResponse.Success<String>?

    val isSuccessful: Boolean

    init {
        // BT returns a Result<Any?> so it's never null
        vaultError = parseVaultError(btRes)
        forageError = parseForageError(BaseResponseRegExp(btRes))
        isSuccessful = btRes.isSuccess && vaultError == null && forageError == null
        successfulResponse = parseSuccessfulResponse(btRes)
    }

    private fun parseVaultError(vaultResponse: BasisTheoryResponse): ForageApiResponse.Failure? {
        if (vaultResponse.isSuccess) return null
        val resRegExp = BtResponseRegExp(vaultResponse)
        return try {
            // In AbstractVaultSubmitter we track the forageError
            // and the forage status code of the UnknownErrorApiResponse
            // via the VaultProxyResponseMonitor. This keeps us informed
            // of *when* erroroneous BT response happen. Unfortunately,
            // we do not currently track the specifics of the proxy_error
            // that that BT returned to us.
            // TODO: log the specific error that BT responds with when
            //  resRegExp.containsProxyError == true
            if (resRegExp.containsProxyError) UnknownErrorApiResponse else null
        } catch (_: Exception) {
            null
        }
    }

    private fun parseForageError(resRegExp: BaseResponseRegExp): ForageApiResponse.Failure? {
        if (resRegExp.bodyText == null || resRegExp.statusCode == null) return null
        return try {
            val forageApiError = ForageApiError.ForageApiErrorMapper.from(resRegExp.bodyText)
            val firstError = forageApiError.errors[0]
            return ForageApiResponse.Failure.fromError(
                ForageError(resRegExp.statusCode, firstError.code, firstError.message)
            )
        } catch (_: Exception) {
            // if we throw (likely a NullPointerException) when trying to extract the ForageApiError,
            // that means the response is not a ForageApiError
            null
        }
    }

    private fun parseSuccessfulResponse(vaultResponse: Result<Any?>): ForageApiResponse.Success<String>? {
        return if (!isSuccessful) {
            null
        } else {
            // note: Result.toString() wraps the actual response as
            // "Success(<actual-value-here>)"
            ForageApiResponse.Success(vaultResponse.getOrNull().toString())
        }
    }
}
