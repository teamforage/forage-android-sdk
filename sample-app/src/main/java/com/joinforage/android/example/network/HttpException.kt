package com.joinforage.android.example.network

/**
 * Exception thrown when an HTTP request fails with a non-2xx status code.
 */
class HttpException(
    val statusCode: Int,
    val responseBody: String,
    message: String = "HTTP $statusCode"
) : Exception(message)
