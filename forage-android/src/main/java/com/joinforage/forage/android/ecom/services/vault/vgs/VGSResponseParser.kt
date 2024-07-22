package com.joinforage.forage.android.ecom.services.vault.vgs

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.UnknownErrorApiResponse
import com.joinforage.forage.android.core.services.vault.VaultResponseParser
import com.verygoodsecurity.vgscollect.core.model.network.VGSResponse
import org.json.JSONException

internal class VGSResponseParser(vgsRes: VGSResponse?) : VaultResponseParser {
    override val isNullResponse: Boolean
    override val vaultError: ForageApiResponse.Failure?
    override val forageError: ForageApiResponse.Failure?
    override val successfulResponse: ForageApiResponse.Success<String>?

    override val vaultErrorMsg: String = vgsRes?.body.toString()
    override val rawResponse: String = vgsRes.toString()

    init {
        if (vgsRes == null) {
            isNullResponse = true
            vaultError = null
            forageError = null
            successfulResponse = null
        } else {
            isNullResponse = false
            vaultError = parseVaultError(vgsRes)
            forageError = parseForageError(vgsRes)
            successfulResponse = parseSuccessfulResponse(vgsRes)
        }
    }

    private fun parseVaultError(res: VGSResponse): ForageApiResponse.Failure? {
        if (res is VGSResponse.SuccessResponse) return null

        // given that it's not a success and it's not a forage error
        // that only leaves a VGS error.
        // TODO: investigate how to get identify the actual VGS error
        //  for logging purposes
        return if (parseForageError(res) == null) UnknownErrorApiResponse else null
    }

    private fun parseForageError(res: VGSResponse): ForageApiResponse.Failure? {
        if (res is VGSResponse.SuccessResponse) return null

        val errorRes = res as VGSResponse.ErrorResponse
        return try {
            // if the response is a ForageApiError, then this block
            // should not throw
            ForageApiResponse.Failure(errorRes.errorCode, errorRes.body ?: "")
        } catch (_: JSONException) {
            // if we throw when trying to extract the ForageApiError,
            // that means the response is not a ForageApiError
            null
        }
    }

    private fun parseSuccessfulResponse(res: VGSResponse): ForageApiResponse.Success<String>? {
        return when (res) {
            is VGSResponse.ErrorResponse -> null
            is VGSResponse.SuccessResponse ->
                ForageApiResponse.Success(res.body.toString())
        }
    }
}
