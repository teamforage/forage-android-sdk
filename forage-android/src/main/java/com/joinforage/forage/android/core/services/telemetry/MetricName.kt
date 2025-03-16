package com.joinforage.forage.android.core.services.telemetry

internal enum class MetricName(val value: String) {
    /*
    VAULT_RESPONSE refers to a response from the VGS or BT submit actions.
     */
    VAULT_RESPONSE("vault_response"),

    /*
    CUSTOMER_PERCEIVED_RESPONSE refers to the response from a balance or capture action. There are
    multiple chained requests that come from the client when executing a balance or capture action.
    Ex of a balance action:
    [GET] EncryptionKey -> [GET] PaymentMethod -> [POST] to VGS/BT -> [GET] Poll for Response ->
    [GET] PaymentMethod -> Return Balance
     */
    CUSTOMER_PERCEIVED_RESPONSE("customer_perceived_response");
}
