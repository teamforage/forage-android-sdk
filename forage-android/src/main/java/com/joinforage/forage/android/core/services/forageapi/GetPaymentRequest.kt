package com.joinforage.forage.android.core.services.forageapi

import com.joinforage.forage.android.core.services.ForageConfig

internal class GetPaymentRequest(
    paymentRef: String,
    forageConfig: ForageConfig,
    traceId: String
) : ClientApiRequest.GetRequest(
    path = "api/payments/$paymentRef/",
    forageConfig,
    traceId,
    Headers.ApiVersion.V_2023_05_15

)
