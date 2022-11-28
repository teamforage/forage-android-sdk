package com.joinforage.forage.android.network.model

internal sealed class Response(
    val code: Int = -1,
    val body: String? = null
) {
    data class SuccessResponse(
        val successCode: Int = -1,
        val rawResponse: String? = null
    ) : Response(successCode, rawResponse) {

        override fun toString(): String {
            return "Code: $successCode \n $body"
        }
    }

    data class ErrorResponse(
        val localizeMessage: String = "Can't connect to server",
        val errorCode: Int = -1,
        private val rawResponse: String? = null
    ) : Response(errorCode, rawResponse) {
        override fun toString(): String {
            return "Code: $errorCode\n $localizeMessage\n $body"
        }
    }
}
