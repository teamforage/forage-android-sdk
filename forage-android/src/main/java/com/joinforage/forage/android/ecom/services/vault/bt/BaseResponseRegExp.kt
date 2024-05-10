package com.joinforage.forage.android.ecom.services.vault.bt

open class BaseResponseRegExp(res: Result<Any?>) {
    private val bodyRegex = ("HTTP response body: (.+?)\\n".toRegex())
    private val statusCodeRegex = "HTTP response code: (\\d+)".toRegex()

    val bodyText: String?
    val statusCode: Int?

    init {
        // NOTE: The response will either be a BT original response
        // or if there was no problem with BT, the response can also
        // be a Forage original response (e.g. a success or an error)
        // For this reason, `ResponseRegExp` is used to parse
        val message = res.exceptionOrNull()?.message
        if (message != null) {
            bodyText = bodyRegex.find(message)?.groupValues?.get(1)
            statusCode = statusCodeRegex.find(message)?.groupValues?.get(1)?.toIntOrNull()
        } else {
            bodyText = null
            statusCode = null
        }
    }
}
