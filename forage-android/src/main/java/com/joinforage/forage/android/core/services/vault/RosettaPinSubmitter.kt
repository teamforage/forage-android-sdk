package com.joinforage.forage.android.core.services.vault

import com.joinforage.forage.android.core.services.forageapi.engine.ForageErrorResponseException
import com.joinforage.forage.android.core.services.forageapi.engine.IHttpEngine
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtCard
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.forageapi.requests.ClientApiRequest

internal class RosettaPinSubmitter(
    val plainTextPin: String,
    val collector: ISecurePinCollector,
    val httpEngine: IHttpEngine
) {
    suspend fun submit(request: ClientApiRequest): ForageApiResponse.Success<String> = try {
        ForageApiResponse.Success(httpEngine.sendRequest(request))
    } catch (e: ForageErrorResponseException) {
        throw VaultForageErrorResponseException(
            ForageApiResponse.Failure(e.forageError)
        )
    }

    fun getVaultToken(paymentMethod: PaymentMethod): String =
        pickVaultTokenByIndex(paymentMethod, 2)

    companion object {
        const val TOKEN_DELIMITER = ","

        fun pickVaultTokenByIndex(paymentMethod: PaymentMethod, index: Int): String {
            val tokensString = (paymentMethod.card as EbtCard).token
            val tokensList = tokensString.split(TOKEN_DELIMITER)

            val noTokenStoredInVault = tokensList.size <= index
            if (noTokenStoredInVault) throw MissingTokenException(paymentMethod.ref)

            return tokensList[index]
        }
    }

    internal class VaultForageErrorResponseException(
        val failure: ForageApiResponse.Failure
    ) : Exception()

    internal class MissingTokenException(
        val paymentMethodRef: String
    ) : Exception()
}
