package com.joinforage.forage.android.core.services.vault

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtBalance
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethodService
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.telemetry.UserAction
import java.util.UUID

internal class CheckBalanceRepository(
    private val vaultSubmitter: VaultSubmitter,
    private val paymentMethodService: PaymentMethodService,
    private val logger: Log
) {
    suspend fun checkBalance(
        merchantId: String,
        sessionToken: String,
        paymentMethodRef: String,
        getVaultRequestParams: ((PaymentMethod) -> VaultSubmitterParams) = { paymentMethod ->
            buildVaultRequestParams(
                merchantId = merchantId,
                paymentMethod = paymentMethod,
                sessionToken = sessionToken
            )
        }
    ): ForageApiResponse<String> {
        val paymentMethod = when (val response = paymentMethodService.getPaymentMethod(paymentMethodRef)) {
            is ForageApiResponse.Success -> PaymentMethod(response.data)
            else -> return response
        }

        val response = vaultSubmitter.submit(
            getVaultRequestParams(paymentMethod)
        )
        return if (response is ForageApiResponse.Success) {
            // response comes as (snap, non_snap) but we've historically
            // returned (snap, cash) in our SDK public API. So, we need
            // to parse the JSON and convert to (snap, cash).
            // we also need to return a ForageApiResponse.Success
            // so, the journey looks like
            // ForageApiResponse.Success (this is response)
            // -> EbtBalance (this is balanceResponse)
            // -> ForageApiResponse.Success (you are here / the line just below)
            // -> EbtBalance (at a future point if you use typed responses!!)
            EbtBalance.fromVaultResponse(response).toForageApiResponse()
        } else {
            response
        }
    }

    private fun buildVaultRequestParams(
        merchantId: String,
        paymentMethod: PaymentMethod,
        sessionToken: String
    ): VaultSubmitterParams {
        return VaultSubmitterParams(
            idempotencyKey = UUID.randomUUID().toString(),
            merchantId = merchantId,
            path = AbstractVaultSubmitter.balancePath(paymentMethod.ref),
            paymentMethod = paymentMethod,
            sessionToken = sessionToken,
            userAction = UserAction.BALANCE
        )
    }
}
