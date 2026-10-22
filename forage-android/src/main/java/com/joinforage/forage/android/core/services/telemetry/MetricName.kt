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
    CUSTOMER_PERCEIVED_RESPONSE("customer_perceived_response"),

    /*
    MOMENT_READY refers to the time from createMoment being called until the moment's page
    sends input_ready, when the customer can start using it.
     */
    MOMENT_READY("moment_ready"),

    /*
    MOMENT_OUTCOME refers to how a moment ended (completed, dismissed or failed) and the time from
    createMoment being called until it ended.
     */
    MOMENT_OUTCOME("moment_outcome");
}
