package com.joinforage.forage.android.ecom.services.forageapi.requests

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.requests.ClientApiRequest
import com.joinforage.forage.android.core.services.forageapi.requests.Headers
import com.joinforage.forage.android.core.services.forageapi.requests.makeApiUrl
import com.joinforage.forage.android.core.services.forageapi.requests.makeVaultUrl
import com.joinforage.forage.android.ecom.services.vault.CreditCardParams
import org.json.JSONObject

internal class PostEcomPaymentMethodRequest(
    forageConfig: ForageConfig,
    traceId: String,
    rawPan: String,
    customerId: String?,
    reusable: Boolean
) : ClientApiRequest.PostRequest(
    url = makeApiUrl(forageConfig, "api/payment_methods/"),
    forageConfig = forageConfig,
    traceId = traceId,
    apiVersion = Headers.ApiVersion.V_2023_05_15,
    headers = Headers(),
    body = JSONObject().apply {
        put("customer_id", customerId)
        put("reusable", reusable)
        put("type", "ebt")
        put(
            "card",
            JSONObject().apply {
                put("number", rawPan)
            }
        )
    }
)

internal class PostEcomCreditPaymentMethodRequest(
    forageConfig: ForageConfig,
    traceId: String,
    creditCardParams: CreditCardParams
) : ClientApiRequest.PostRequest(
    url = makeVaultUrl(forageConfig, "proxy/api/payment_methods/"),
    forageConfig = forageConfig,
    traceId = traceId,
    apiVersion = Headers.ApiVersion.V_2023_05_15,
    headers = Headers(),
    body = JSONObject().apply {
        put("customer_id", creditCardParams.customerId)
        put("reusable", creditCardParams.reusable)
        put("type", "credit")
        put(
            "card",
            JSONObject().apply {
                put("number", creditCardParams.cardNumber)
                put("name", creditCardParams.name)
                put("zip_code", creditCardParams.zipCode)
                put("exp_month", creditCardParams.expiration.first)
                put("exp_year", creditCardParams.expiration.second)
                put("security_code", creditCardParams.cvc)
                put("is_hsa_fsa", creditCardParams.isHsaFsa)
            }
        )
    }
)
