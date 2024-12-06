package com.joinforage.forage.android.core.services.vault.requests

import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.forageapi.requests.ClientApiRequest
import com.joinforage.forage.android.core.services.vault.RosettaPinSubmitter

internal interface ISubmitRequestBuilder {
    suspend fun buildRequest(
        paymentMethod: PaymentMethod,
        idempotencyKey: String,
        traceId: String,
        vaultSubmitter: RosettaPinSubmitter
    ): ClientApiRequest
}
