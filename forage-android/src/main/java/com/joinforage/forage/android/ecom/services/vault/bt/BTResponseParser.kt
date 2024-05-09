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
        isSuccessful = btRes.isSuccess && vaultError != null && forageError != null
        successfulResponse = parseSuccessfulResponse(btRes)
    }

    private fun parseVaultError(vaultResponse: BasisTheoryResponse): ForageApiResponse.Failure? {
        if (vaultResponse.isSuccess) return null
        val resRegExp = BtResponseRegExp(vaultResponse)
        return try {
            // TODO: add DD metric to track frequency of proxy errors
            if(resRegExp.containsProxyError) UnknownErrorApiResponse else null
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
        return if (!isSuccessful) null
        else {
            // note: Result.toString() wraps the actual response as
            // "Success(<actual-value-here>)"
            ForageApiResponse.Success(vaultResponse.getOrNull().toString())
        }
    }
}