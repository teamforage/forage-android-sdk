package com.joinforage.forage.android.ecom.services.vault.bt

internal class BtFailureResponseRegExp(res: Result<Any?>) {
    private val bodyRegex = ("HTTP response body: (.+?)\\n".toRegex())
    private val statusCodeRegex = "HTTP response code: (\\d+)".toRegex()

    val bodyText: String?
    val statusCode: Int?

    val containsProxyError: Boolean

    init {
        // NOTE: The response will either be a BT original response
        // or if there was no problem with BT, the response can also
        // be a Forage original response (e.g. a success or an error)
        // For this reason, `ResponseRegExp` is used to parse
        val message = res.exceptionOrNull()?.message.toString()
        bodyText = bodyRegex.find(message)?.groupValues?.get(1)
        statusCode = statusCodeRegex.find(message)?.groupValues?.get(1)?.toIntOrNull()

        // com.basistheory.ApiException isn't currently
        // publicly-exposed by the basis-theory-android package
        // so we parse the raw exception message to retrieve the body of the BasisTheory
        // errors
        containsProxyError = bodyText?.contains("proxy_error") ?: false
    }
}
// TODO: write unit tests for this class....is it actually possible for .get(1) to throw???
