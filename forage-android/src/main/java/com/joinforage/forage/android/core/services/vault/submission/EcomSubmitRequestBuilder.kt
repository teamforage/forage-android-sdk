package com.joinforage.forage.android.core.services.vault.submission

import com.joinforage.forage.android.core.services.forageapi.ClientApiRequest
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.vault.RosettaPinSubmitter
import com.joinforage.forage.android.core.services.vault.VaultPaymentMethod
import com.joinforage.forage.android.core.services.vault.requests.IEcomBuildRequestDelegate
import com.joinforage.forage.android.core.services.vault.requests.ISubmitRequestBuilder

internal class EcomSubmitRequestBuilder(
    private val delegate: IEcomBuildRequestDelegate
) : ISubmitRequestBuilder {
    override suspend fun buildRequest(
        paymentMethod: PaymentMethod,
        idempotencyKey: String,
        traceId: String,
        vaultSubmitter: RosettaPinSubmitter
    ): ClientApiRequest {
        val vaultPaymentMethod = VaultPaymentMethod(
            ref = paymentMethod.ref,
            token = vaultSubmitter.getVaultToken(paymentMethod)
        )
        return delegate.buildRequest(
            vaultPaymentMethod,
            idempotencyKey,
            traceId
        )
    }
}