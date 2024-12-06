package com.joinforage.forage.android.core.services.vault.requests

import com.joinforage.forage.android.core.services.forageapi.ClientApiRequest
import com.joinforage.forage.android.core.services.vault.VaultPaymentMethod

// TODO: move this into the /ecom folder and delete from POS repo
internal interface IEcomBuildRequestDelegate {
    suspend fun buildRequest(
        paymentMethod: VaultPaymentMethod,
        idempotencyKey: String,
        traceId: String
    ): ClientApiRequest
}
