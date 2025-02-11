package com.joinforage.forage.android.core.services.vault.submission

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtBalance

internal class BalanceCheckSubmission(
    private val delegate: ISubmitDelegate
) {

    suspend fun submit(): ForageApiResponse<String> {
        return when (val response = delegate.rawSubmit()) {
            is ForageApiResponse.Success -> {
                // Convert response format from (snap, non_snap) to (snap, cash)
                // This maintains backwards compatibility with SDK public API
                EbtBalance.fromVaultResponse(response).toForageApiResponse()
            }
            is ForageApiResponse.Failure -> response
        }
    }
}
