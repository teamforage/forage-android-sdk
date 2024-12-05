package com.joinforage.forage.android.core.services.forageapi

import com.joinforage.forage.android.core.services.ForageConfig

internal class GetPaymentMethodRequest(
    paymentMethodRef: String,
    forageConfig: ForageConfig,
    traceId: String
) : ClientApiRequest.GetRequest(
    path = "api/payment_methods/$paymentMethodRef/",
    forageConfig,
    traceId,
    Headers.ApiVersion.V_2023_05_15
)
