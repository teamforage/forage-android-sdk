package com.joinforage.forage.android.ecom.services.vault

import com.joinforage.forage.android.core.services.forageapi.requests.ClientApiRequest
import com.joinforage.forage.android.core.services.vault.VaultPaymentMethod

internal interface IEcomBuildRequestDelegate {
    suspend fun buildRequest(
        paymentMethod: VaultPaymentMethod,
        idempotencyKey: String,
        traceId: String
    ): ClientApiRequest
}
